import { desc, eq } from "drizzle-orm";
import { hasAdminSession } from "../../../admin-auth";
import { getDb } from "../../../../db";
import { adminNotifications } from "../../../../db/schema";

export async function GET() {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const notifications = await getDb().select().from(adminNotifications)
    .orderBy(desc(adminNotifications.createdAt)).limit(30);
  return Response.json({
    notifications,
    unreadCount: notifications.filter((notification) => notification.isRead === 0).length,
  }, { headers: { "Cache-Control": "no-store" } });
}

export async function PATCH(request: Request) {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const input = await request.json() as { id?: unknown; all?: unknown };
  const db = getDb();
  const now = new Date().toISOString();
  if (input.all === true) {
    await db.update(adminNotifications).set({ isRead: 1, readAt: now })
      .where(eq(adminNotifications.isRead, 0));
    return Response.json({ success: true });
  }
  const id = Number(input.id);
  if (!Number.isInteger(id) || id < 1) return Response.json({ error: "رقم الإشعار غير صحيح." }, { status: 400 });
  const [updated] = await db.update(adminNotifications).set({ isRead: 1, readAt: now })
    .where(eq(adminNotifications.id, id)).returning({ id: adminNotifications.id });
  if (!updated) return Response.json({ error: "الإشعار غير موجود." }, { status: 404 });
  return Response.json({ success: true });
}
