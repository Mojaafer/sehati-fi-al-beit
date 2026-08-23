import { eq } from "drizzle-orm";
import { hasAdminSession } from "../../../../admin-auth";
import { getDb } from "../../../../../db";
import { doctorBookings } from "../../../../../db/schema";
import { downloadPrivateFile } from "../../../../../lib/storage";

export async function GET(_request: Request, context: { params: Promise<{ id: string }> }) {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });
  const id = Number((await context.params).id);
  if (!Number.isInteger(id) || id < 1) return Response.json({ error: "Invalid booking id" }, { status: 400 });
  const [booking] = await getDb().select({
    objectKey: doctorBookings.receiptObjectKey,
    fileName: doctorBookings.receiptFileName,
    mimeType: doctorBookings.receiptMimeType,
  }).from(doctorBookings).where(eq(doctorBookings.id, id)).limit(1);
  if (!booking?.objectKey) return Response.json({ error: "Receipt not found" }, { status: 404 });
  const object = await downloadPrivateFile(booking.objectKey);
  if (!object) return Response.json({ error: "Stored receipt not found" }, { status: 404 });
  return new Response(object, { headers: {
    "Content-Type": booking.mimeType || "application/octet-stream",
    "Content-Disposition": `inline; filename*=UTF-8''${encodeURIComponent(booking.fileName || "receipt")}`,
    "Cache-Control": "private, no-store",
    "X-Content-Type-Options": "nosniff",
  } });
}
