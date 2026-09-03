import { asc, eq } from "drizzle-orm";
import { getDb } from "../../../db";
import { doctors } from "../../../db/schema";

export async function GET() {
  try {
    const records = await getDb().select({
      id: doctors.id,
      fullName: doctors.fullName,
      specialty: doctors.specialty,
      clinicLocation: doctors.clinicLocation,
      city: doctors.city,
      bookingCost: doctors.bookingCost,
      currency: doctors.currency,
      availability: doctors.availability,
      clinicDays: doctors.clinicDays,
      clinicStartTime: doctors.clinicStartTime,
      clinicEndTime: doctors.clinicEndTime,
      slotDurationMinutes: doctors.slotDurationMinutes,
      phone: doctors.phone,
    }).from(doctors)
      .where(eq(doctors.status, "active"))
      .orderBy(asc(doctors.fullName));
    return Response.json({ doctors: records }, { headers: { "Cache-Control": "no-store" } });
  } catch (error) {
    console.error("public doctor list failed", error);
    return Response.json({ error: "تعذر تحميل دليل الأطباء." }, { status: 500 });
  }
}
