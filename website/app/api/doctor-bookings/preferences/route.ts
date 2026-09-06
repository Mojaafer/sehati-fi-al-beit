import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { doctorBookings } from "../../../../db/schema";
import { checkRateLimit, tooManyRequests } from "../../../../lib/rate-limit";

export async function PATCH(request: Request) {
  try {
    // The booking code is the only credential, and the 404 tells the caller whether a code exists,
    // so this is the cheapest place to enumerate from. A patient toggles this switch once.
    const limit = await checkRateLimit("booking-preferences", request, 40, 3600);
    if (!limit.allowed) return tooManyRequests(limit.retryAfterSeconds);

    const input = await request.json() as { bookingCode?: unknown; whatsappOptIn?: unknown };
    const bookingCode = typeof input.bookingCode === "string" ? input.bookingCode.trim().slice(0, 32) : "";
    if (bookingCode.length < 6 || typeof input.whatsappOptIn !== "boolean") {
      return Response.json({ error: "بيانات الطلب غير صحيحة." }, { status: 400 });
    }
    const [updated] = await getDb().update(doctorBookings).set({
      whatsappOptIn: input.whatsappOptIn ? 1 : 0,
      updatedAt: new Date().toISOString(),
    }).where(eq(doctorBookings.bookingCode, bookingCode)).returning({ id: doctorBookings.id });
    if (!updated) return Response.json({ error: "رقم الحجز غير صحيح." }, { status: 404 });
    return Response.json({ success: true });
  } catch (error) {
    console.error("preferences update failed", error);
    return Response.json({ error: "بيانات الطلب غير صحيحة." }, { status: 400 });
  }
}
