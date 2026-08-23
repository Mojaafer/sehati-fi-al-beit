import { eq } from "drizzle-orm";
import { hasAdminSession } from "../../../../admin-auth";
import { parseDoctorInput } from "../../../../doctor-data";
import { getDb } from "../../../../../db";
import { doctorBookings, doctors } from "../../../../../db/schema";

function idFrom(raw: string): number | null {
  const id = Number(raw);
  return Number.isInteger(id) && id > 0 ? id : null;
}

export async function PATCH(request: Request, context: { params: Promise<{ id: string }> }) {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const id = idFrom((await context.params).id);
  if (!id) return Response.json({ error: "رقم الطبيب غير صحيح." }, { status: 400 });
  const parsed = parseDoctorInput(await request.json());
  if (!parsed.data) return Response.json({ error: parsed.error }, { status: 400 });
  const [doctor] = await getDb().update(doctors).set(parsed.data).where(eq(doctors.id, id)).returning();
  if (!doctor) return Response.json({ error: "الطبيب غير موجود." }, { status: 404 });
  return Response.json({ doctor });
}

export async function DELETE(_request: Request, context: { params: Promise<{ id: string }> }) {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const id = idFrom((await context.params).id);
  if (!id) return Response.json({ error: "رقم الطبيب غير صحيح." }, { status: 400 });
  const db = getDb();
  const [linkedBooking] = await db.select({ id: doctorBookings.id }).from(doctorBookings)
    .where(eq(doctorBookings.doctorId, id)).limit(1);
  if (linkedBooking) {
    return Response.json({ error: "لا يمكن حذف طبيب لديه حجوزات. أخفِه من الدليل بدلاً من ذلك." }, { status: 409 });
  }
  const [doctor] = await db.delete(doctors).where(eq(doctors.id, id)).returning({ id: doctors.id });
  if (!doctor) return Response.json({ error: "الطبيب غير موجود." }, { status: 404 });
  return Response.json({ success: true });
}
