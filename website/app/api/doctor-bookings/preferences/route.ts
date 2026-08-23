import { eq } from "drizzle-orm";
import { getDb } from "../../../../db";
import { doctorBookings } from "../../../../db/schema";

export async function PATCH(request: Request) {
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
}
