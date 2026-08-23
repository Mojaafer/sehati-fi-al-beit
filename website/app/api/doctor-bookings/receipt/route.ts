import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { doctorBookings } from "../../../../db/schema";
import { removePrivateFiles, uploadPrivateFile } from "../../../../lib/storage";

export async function POST(request: Request) {
  try {
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
    await uploadPrivateFile(objectKey, await file.arrayBuffer(), file.type);
    if (booking.receiptObjectKey) await removePrivateFiles([booking.receiptObjectKey]);
    await db.update(doctorBookings).set({
      receiptFileName: file.name.slice(0, 255),
      receiptMimeType: file.type,
      receiptObjectKey: objectKey,
      status: "receipt_submitted",
      updatedAt: new Date().toISOString(),
    }).where(eq(doctorBookings.id, booking.id));
    return Response.json({ success: true, status: "receipt_submitted" });
  } catch (error) {
    console.error("booking receipt upload failed", error);
    return Response.json({ error: "تعذر رفع إشعار التحويل." }, { status: 500 });
  }
}
