import { desc, eq } from "drizzle-orm";
import { hasAdminSession } from "../../admin-auth";
import { clinicDayValues, isSudanesePhone, makeCode, type ClinicDay } from "../../doctor-data";
import { getDb } from "../../../db";
import { doctorBookings, doctors } from "../../../db/schema";

function text(value: unknown, maxLength: number): string {
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

function parseDays(value: string): ClinicDay[] {
  try {
    const parsed = JSON.parse(value) as unknown;
    return Array.isArray(parsed)
      ? parsed.filter((day): day is ClinicDay => typeof day === "string" && clinicDayValues.includes(day as ClinicDay))
      : [];
  } catch {
    return [];
  }
}

export async function POST(request: Request) {
  try {
    const input = await request.json() as Record<string, unknown>;
    const doctorId = Number(input.doctorId);
    const patientName = text(input.patientName, 160);
    const patientPhone = text(input.patientPhone, 40);
    const requestedDate = text(input.requestedDate, 20);
    const patientNote = text(input.patientNote, 2000);
    if (!Number.isInteger(doctorId) || doctorId < 1) return Response.json({ error: "اختر الطبيب." }, { status: 400 });
    if (patientName.length < 2) return Response.json({ error: "اكتب الاسم الكامل." }, { status: 400 });
    if (!isSudanesePhone(patientPhone)) return Response.json({ error: "اكتب رقم هاتف سوداني صحيح." }, { status: 400 });
    if (!/^\d{4}-\d{2}-\d{2}$/.test(requestedDate)) return Response.json({ error: "اختر يوم الحضور." }, { status: 400 });

    const selectedDate = new Date(`${requestedDate}T12:00:00Z`);
    const today = new Date();
    today.setUTCHours(0, 0, 0, 0);
    if (Number.isNaN(selectedDate.getTime()) || selectedDate.getTime() < today.getTime()
      || selectedDate.getTime() > today.getTime() + 46 * 24 * 60 * 60 * 1000) {
      return Response.json({ error: "اختر يوماً متاحاً خلال الفترة القادمة." }, { status: 400 });
    }

    const db = getDb();
    const [doctor] = await db.select().from(doctors)
      .where(eq(doctors.id, doctorId)).limit(1);
    if (!doctor || doctor.status !== "active") return Response.json({ error: "الطبيب غير متاح حالياً." }, { status: 404 });
    const selectedDay = clinicDayValues[selectedDate.getUTCDay()];
    if (!parseDays(doctor.clinicDays).includes(selectedDay)) {
      return Response.json({ error: "اختر يوماً من أيام عمل عيادة الطبيب." }, { status: 400 });
    }

    const bookingCode = makeCode("BK");
    const [booking] = await db.insert(doctorBookings).values({
      bookingCode,
      doctorId,
      patientName,
      patientPhone,
      requestedDate,
      requestedTime: "first_come",
      patientNote: patientNote || null,
      whatsappOptIn: input.whatsappOptIn === true ? 1 : 0,
    }).returning({ id: doctorBookings.id });
    return Response.json({
      id: booking?.id,
      bookingCode,
      status: "pending_payment",
      paymentInstructions: doctor.paymentInstructionsVisible ? doctor.paymentInstructions : null,
    }, { status: 201 });
  } catch (error) {
    console.error("doctor booking creation failed", error);
    return Response.json({ error: "تعذر إنشاء طلب الحجز." }, { status: 500 });
  }
}

export async function GET() {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const bookings = await getDb().select({
    id: doctorBookings.id,
    bookingCode: doctorBookings.bookingCode,
    doctorId: doctorBookings.doctorId,
    doctorFullName: doctors.fullName,
    patientName: doctorBookings.patientName,
    patientPhone: doctorBookings.patientPhone,
    requestedDate: doctorBookings.requestedDate,
    requestedTime: doctorBookings.requestedTime,
    patientNote: doctorBookings.patientNote,
    whatsappOptIn: doctorBookings.whatsappOptIn,
    status: doctorBookings.status,
    receiptFileName: doctorBookings.receiptFileName,
    receiptMimeType: doctorBookings.receiptMimeType,
    hasReceipt: doctorBookings.receiptObjectKey,
    adminNote: doctorBookings.adminNote,
    supervisorName: doctors.supervisorName,
    supervisorPhone: doctors.supervisorPhone,
    createdAt: doctorBookings.createdAt,
  }).from(doctorBookings).leftJoin(doctors, eq(doctorBookings.doctorId, doctors.id))
    .orderBy(desc(doctorBookings.createdAt));
  return Response.json({
    bookings: bookings.map((booking) => ({
      ...booking,
      hasReceipt: Boolean(booking.hasReceipt),
      receiptUrl: booking.hasReceipt ? `/api/doctor-bookings/${booking.id}/receipt` : null,
    })),
  }, { headers: { "Cache-Control": "no-store" } });
}
