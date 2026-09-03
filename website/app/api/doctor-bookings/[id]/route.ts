import { eq } from "drizzle-orm";
import { hasAdminSession } from "../../../admin-auth";
import { getDb } from "../../../../db";
import { doctorBookings } from "../../../../db/schema";
import { removePrivateFiles } from "../../../../lib/storage";

const reviewStatuses = new Set(["payment_confirmed", "payment_rejected", "confirmed", "cancelled"]);

export async function PATCH(request: Request, context: { params: Promise<{ id: string }> }) {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const id = Number((await context.params).id);
  if (!Number.isInteger(id) || id < 1) return Response.json({ error: "رقم الحجز غير صحيح." }, { status: 400 });
  try {
    const input = await request.json() as { status?: unknown; adminNote?: unknown };
    if (typeof input.status !== "string" || !reviewStatuses.has(input.status)) {
      return Response.json({ error: "بيانات التحديث غير صحيحة." }, { status: 400 });
    }
    const [updated] = await getDb().update(doctorBookings).set({
      status: input.status as "payment_confirmed" | "payment_rejected" | "confirmed" | "cancelled",
      adminNote: typeof input.adminNote === "string" ? input.adminNote.trim().slice(0, 3000) || null : null,
      reviewedAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }).where(eq(doctorBookings.id, id)).returning({ id: doctorBookings.id });
    if (!updated) return Response.json({ error: "الحجز غير موجود." }, { status: 404 });
    return Response.json({ success: true });
  } catch (error) {
    console.error("doctor booking status update failed", error);
    return Response.json({ error: "بيانات التحديث غير صحيحة." }, { status: 400 });
  }
}

export async function DELETE(_request: Request, context: { params: Promise<{ id: string }> }) {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const id = Number((await context.params).id);
  if (!Number.isInteger(id) || id < 1) return Response.json({ error: "رقم الحجز غير صحيح." }, { status: 400 });
  const db = getDb();
  const [booking] = await db.select({ receiptObjectKey: doctorBookings.receiptObjectKey }).from(doctorBookings)
    .where(eq(doctorBookings.id, id)).limit(1);
  if (!booking) return Response.json({ error: "الحجز غير موجود." }, { status: 404 });
  await db.delete(doctorBookings).where(eq(doctorBookings.id, id));
  if (booking.receiptObjectKey) await removePrivateFiles([booking.receiptObjectKey]);
  return Response.json({ success: true });
}
