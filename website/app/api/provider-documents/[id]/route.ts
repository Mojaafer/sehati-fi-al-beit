import { eq } from "drizzle-orm";
import { hasAdminSession } from "../../../admin-auth";
import { getDb } from "../../../../db";
import { providerDocuments } from "../../../../db/schema";
import { downloadPrivateFile } from "../../../../lib/storage";

export async function GET(
  _request: Request,
  context: { params: Promise<{ id: string }> },
) {
  if (!(await hasAdminSession())) return Response.json({ error: "Unauthorized" }, { status: 401 });

  const { id: rawId } = await context.params;
  const id = Number(rawId);
  if (!Number.isInteger(id) || id < 1) {
    return Response.json({ error: "Invalid document id" }, { status: 400 });
  }

  const db = getDb();
  const [document] = await db.select().from(providerDocuments)
    .where(eq(providerDocuments.id, id)).limit(1);
  if (!document) return Response.json({ error: "Document not found" }, { status: 404 });

  const object = await downloadPrivateFile(document.objectKey);
  if (!object) return Response.json({ error: "Stored file not found" }, { status: 404 });

  return new Response(object, {
    headers: {
      "Content-Type": document.mimeType,
      "Content-Disposition": `inline; filename*=UTF-8''${encodeURIComponent(document.fileName)}`,
      "Cache-Control": "private, no-store",
      "X-Content-Type-Options": "nosniff",
    },
  });
}
