import { desc } from "drizzle-orm";
import { hasAdminSession } from "../../../admin-auth";
import { parseDoctorInput } from "../../../doctor-data";
import { getDb } from "../../../../db";
import { doctors } from "../../../../db/schema";

function unauthorized() {
  return Response.json({ error: "يلزم تسجيل الدخول للإدارة." }, { status: 401 });
}

export async function GET() {
  if (!(await hasAdminSession())) return unauthorized();
  const records = await getDb().select().from(doctors).orderBy(desc(doctors.createdAt));
  return Response.json({ doctors: records }, { headers: { "Cache-Control": "no-store" } });
}

export async function POST(request: Request) {
  if (!(await hasAdminSession())) return unauthorized();
  try {
    const parsed = parseDoctorInput(await request.json());
    if (!parsed.data) return Response.json({ error: parsed.error }, { status: 400 });
    const [doctor] = await getDb().insert(doctors).values(parsed.data).returning();
    return Response.json({ doctor }, { status: 201 });
  } catch (error) {
    console.error("doctor creation failed", error);
    return Response.json({ error: "تعذر حفظ الطبيب." }, { status: 500 });
  }
}
