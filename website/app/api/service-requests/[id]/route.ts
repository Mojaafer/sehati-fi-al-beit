import { eq } from "drizzle-orm";
import { hasAdminSession } from "../../../admin-auth";
import { getDb } from "../../../../db";
import { adminNotifications, serviceRequests } from "../../../../db/schema";

export async function DELETE(_request: Request, context: { params: Promise<{ id: string }> }) {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const id = Number((await context.params).id);
  if (!Number.isInteger(id) || id < 1) return Response.json({ error: "رقم الطلب غير صحيح." }, { status: 400 });
  const db = getDb();
  await db.delete(adminNotifications).where(eq(adminNotifications.serviceRequestId, id));
  const [deleted] = await db.delete(serviceRequests).where(eq(serviceRequests.id, id)).returning({ id: serviceRequests.id });
  if (!deleted) return Response.json({ error: "الطلب غير موجود." }, { status: 404 });
  return Response.json({ success: true });
}
