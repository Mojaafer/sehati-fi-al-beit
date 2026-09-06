import { desc } from "drizzle-orm";
import { hasAdminSession } from "../../admin-auth";
import { isSudanesePhone, makeCode, text } from "../../doctor-data";
import { getDb } from "../../../db";
import { adminNotifications, serviceRequests } from "../../../db/schema";
import { pageParams, splitPage } from "../../../lib/pagination";
import { checkRateLimit, tooManyRequests } from "../../../lib/rate-limit";

const serviceTypes = new Set(["lab", "nurse", "physio"]);
const serviceLabels: Record<string, string> = {
  lab: "سحب عينات",
  nurse: "تمريض منزلي",
  physio: "علاج طبيعي",
};

export async function POST(request: Request) {
  try {
    // Each accepted call writes a service request *and* an admin notification, so unthrottled
    // abuse would bury the real requests in the admin inbox.
    const limit = await checkRateLimit("service-request-create", request, 20, 3600);
    if (!limit.allowed) return tooManyRequests(limit.retryAfterSeconds);

    const input = await request.json() as Record<string, unknown>;
    const serviceType = text(input.serviceType, 20);
    const patientName = text(input.patientName, 160);
    const patientPhone = text(input.patientPhone, 40);
    const requestedDate = text(input.requestedDate, 20);
    const requestedTime = text(input.requestedTime, 30);
    const city = text(input.city, 100);
    const neighborhood = text(input.neighborhood, 160);
    const locationNote = text(input.locationNote, 1200);
    const serviceDetails = text(input.serviceDetails, 2500);

    if (!serviceTypes.has(serviceType)) return Response.json({ error: "نوع الخدمة غير صحيح." }, { status: 400 });
    if (patientName.length < 2) return Response.json({ error: "اكتب اسم المستفيد." }, { status: 400 });
    if (!isSudanesePhone(patientPhone)) return Response.json({ error: "اكتب رقم هاتف سوداني صحيح." }, { status: 400 });
    if (!requestedDate || !requestedTime) return Response.json({ error: "اختر اليوم والوقت." }, { status: 400 });
    if (city.length < 2 || neighborhood.length < 2 || locationNote.length < 3) {
      return Response.json({ error: "أكمل المدينة والحي ووصف الموقع." }, { status: 400 });
    }
    if (serviceDetails.length < 3) return Response.json({ error: "أكمل تفاصيل الخدمة المطلوبة." }, { status: 400 });

    const requestCode = makeCode("HM");
    const db = getDb();
    const [record] = await db.insert(serviceRequests).values({
      requestCode,
      serviceType: serviceType as "lab" | "nurse" | "physio",
      patientName,
      patientPhone,
      requestedDate,
      requestedTime,
      city,
      neighborhood,
      locationNote,
      serviceDetails,
    }).returning({ id: serviceRequests.id });
    if (!record) return Response.json({ error: "تعذر حفظ الطلب." }, { status: 500 });

    await db.insert(adminNotifications).values({
      kind: "home_care_request",
      serviceRequestId: record.id,
      title: "طلب خدمة منزلية جديد",
      body: `${serviceLabels[serviceType]} — ${patientName} — ${requestCode}`,
    });
    return Response.json({ id: record.id, requestCode, status: "pending" }, { status: 201 });
  } catch (error) {
    console.error("service request creation failed", error);
    return Response.json({ error: "تعذر إرسال الطلب. حاول مرة أخرى." }, { status: 500 });
  }
}

export async function GET(request: Request) {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const page = pageParams(request);
  const rows = await getDb().select().from(serviceRequests)
    .orderBy(desc(serviceRequests.createdAt))
    .limit(page.fetchLimit).offset(page.offset);
  const { rows: requests, hasMore } = splitPage(rows, page, "service requests list");
  return Response.json({ requests, hasMore }, { headers: { "Cache-Control": "no-store" } });
}
