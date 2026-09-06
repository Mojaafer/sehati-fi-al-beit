import { desc, eq } from "drizzle-orm";
import { hasAdminSession } from "../../admin-auth";
import { clinicDayValues, isSudanesePhone, makeCode, parseDays, text } from "../../doctor-data";
import { getDb } from "../../../db";
import { doctorBookings, doctors } from "../../../db/schema";
import { pageParams, splitPage } from "../../../lib/pagination";
import { checkRateLimit, tooManyRequests } from "../../../lib/rate-limit";

export async function POST(request: Request) {
  try {
    // Public endpoint that writes a row per call. 20/hour per IP leaves room for a family sharing
    // one connection — Sudanese mobile networks put many subscribers behind a single address —
    // while stopping a script from filling the bookings table.
    const limit = await checkRateLimit("doctor-booking-create", request, 20, 3600);
    if (!limit.allowed) return tooManyRequests(limit.retryAfterSeconds);

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

export async function GET(request: Request) {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const page = pageParams(request);
  const rows = await getDb().select({
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
    .orderBy(desc(doctorBookings.createdAt))
    .limit(page.fetchLimit).offset(page.offset);
  const { rows: bookings, hasMore } = splitPage(rows, page, "doctor bookings list");
  return Response.json({
    bookings: bookings.map((booking) => ({
      ...booking,
      hasReceipt: Boolean(booking.hasReceipt),
      receiptUrl: booking.hasReceipt ? `/api/doctor-bookings/${booking.id}/receipt` : null,
    })),
    hasMore,
  }, { headers: { "Cache-Control": "no-store" } });
}
