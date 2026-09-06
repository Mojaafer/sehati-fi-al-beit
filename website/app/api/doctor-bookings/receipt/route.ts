import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { doctorBookings } from "../../../../db/schema";
import { checkRateLimit, tooManyRequests } from "../../../../lib/rate-limit";
import { removePrivateFiles, uploadPrivateFile } from "../../../../lib/storage";

export async function POST(request: Request) {
  try {
    // A booking code is the only credential here, so this endpoint is also the place someone
    // would guess codes from. 30/hour is far above what a patient uploading one receipt needs.
    const limit = await checkRateLimit("booking-receipt", request, 30, 3600);
    if (!limit.allowed) return tooManyRequests(limit.retryAfterSeconds);

    const form = await request.formData();
    const bookingCode = typeof form.get("bookingCode") === "string" ? String(form.get("bookingCode")).trim().slice(0, 32) : "";
    const file = form.get("receipt");
    if (bookingCode.length < 6) return Response.json({ error: "رقم الحجز غير صحيح." }, { status: 400 });
    if (!(file instanceof File) || file.size === 0) return Response.json({ error: "ارفع صورة إشعار التحويل." }, { status: 400 });
    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      return Response.json({ error: "المسموح صورة PNG أو JPG أو WEBP فقط." }, { status: 400 });
    }
    if (file.size > 5 * 1024 * 1024) return Response.json({ error: "حجم الصورة يجب ألا يتجاوز 5 ميغابايت." }, { status: 413 });

    const db = getDb();
    const [booking] = await db.select().from(doctorBookings)
      .where(eq(doctorBookings.bookingCode, bookingCode)).limit(1);
    if (!booking) return Response.json({ error: "رقم الحجز غير صحيح." }, { status: 404 });
    if (!(["pending_payment", "payment_rejected"] as string[]).includes(booking.status)) {
      return Response.json({ error: "لا يمكن رفع إشعار لهذا الطلب في حالته الحالية." }, { status: 400 });
    }

    const extension = file.type === "image/png" ? ".png" : file.type === "image/webp" ? ".webp" : ".jpg";
    const objectKey = `doctor-bookings/${booking.id}/receipt-${crypto.randomUUID()}${extension}`;
    const previousObjectKey = booking.receiptObjectKey;
    await uploadPrivateFile(objectKey, await file.arrayBuffer(), file.type);

    // Upload, then point the row at it, then drop the old object. Deleting first meant a failed
    // update destroyed the patient's previous receipt and left the row referencing a key that no
    // longer exists — with no way to recover the image.
    try {
      await db.update(doctorBookings).set({
        receiptFileName: file.name.slice(0, 255),
        receiptMimeType: file.type,
        receiptObjectKey: objectKey,
        status: "receipt_submitted",
        updatedAt: new Date().toISOString(),
      }).where(eq(doctorBookings.id, booking.id));
    } catch (error) {
      // The row still points at the previous receipt, so the object just written is unreachable.
      await removePrivateFiles([objectKey]).catch(() => {});
      throw error;
    }

    // Only now is the old object unreferenced. A failure here costs storage, not data, so it must
    // not turn a successful upload into an error for the patient.
    if (previousObjectKey && previousObjectKey !== objectKey) {
      try {
        await removePrivateFiles([previousObjectKey]);
      } catch (error) {
        console.error("failed to remove replaced receipt object", previousObjectKey, error);
      }
    }

    return Response.json({ success: true, status: "receipt_submitted" });
  } catch (error) {
    console.error("booking receipt upload failed", error);
    return Response.json({ error: "تعذر رفع إشعار التحويل." }, { status: 500 });
  }
}
