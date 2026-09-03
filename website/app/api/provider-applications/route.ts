import { desc, eq } from "drizzle-orm";
import { hasAdminSession } from "../../admin-auth";
import { getDb } from "../../../db";
import { providerApplications, providerDocuments } from "../../../db/schema";
import { removePrivateFiles, uploadPrivateFile } from "../../../lib/storage";

const professions = new Set(["doctor", "lab", "physio", "nurse"]);
const applicationStatuses = new Set(["new", "under_review", "needs_documents", "verified", "rejected"]);
const reviewStatuses = new Set(["pending", "accepted", "rejected"]);
const documentTypes = ["cv", "qualification", "license", "identity"] as const;
const maxFileSize = 5 * 1024 * 1024;

function field(form: FormData, name: string, maxLength: number) {
  const value = form.get(name);
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

function jsonError(message: string, status = 400) {
  return Response.json({ error: message }, { status });
}

export async function POST(request: Request) {
  try {
    const form = await request.formData();
    const profession = field(form, "profession", 30);
    const fullName = field(form, "fullName", 160);
    const phone = field(form, "phone", 40);
    const city = field(form, "city", 100);

    if (!professions.has(profession)) return jsonError("اختر المهنة بصورة صحيحة.");
    if (fullName.length < 2) return jsonError("اكتب الاسم الكامل.");
    if (phone.length < 6) return jsonError("اكتب رقم هاتف صحيح.");
    if (city.length < 2) return jsonError("اختر المدينة.");

    const files: Array<{ documentType: typeof documentTypes[number]; file: File }> = [];
    const allowedDocumentMimeTypes = new Set(["application/pdf", "image/jpeg", "image/png", "image/webp"]);
    for (const documentType of documentTypes) {
      const value = form.get(documentType);
      if (!(value instanceof File) || value.size === 0) continue;
      if (value.size > maxFileSize) return jsonError(`حجم الملف ${value.name} أكبر من 5 ميغابايت.`);
      if (!allowedDocumentMimeTypes.has(value.type)) {
        return jsonError("المستندات المقبولة هي PDF أو صور (JPG, PNG, WEBP) فقط.");
      }
      files.push({ documentType, file: value });
    }

    if (!files.some((item) => item.documentType === "identity")) {
      return jsonError("صورة الهوية مطلوبة لبدء التحقق.");
    }
    if (!files.some((item) => item.documentType === "qualification")) {
      return jsonError("صورة المؤهل الأكاديمي مطلوبة.");
    }

    const db = getDb();
    const [application] = await db.insert(providerApplications).values({
      profession: profession as "doctor" | "lab" | "physio" | "nurse",
      fullName,
      phone,
      specialty: field(form, "specialty", 160) || null,
      experience: field(form, "experience", 80) || null,
      city,
      workplace: field(form, "workplace", 180) || null,
      availability: field(form, "availability", 2000) || null,
    }).returning({ id: providerApplications.id });

    if (!application) return jsonError("تعذر إنشاء الطلب.", 500);

    try {
      for (const item of files) {
        const extension = item.file.name.includes(".")
          ? item.file.name.slice(item.file.name.lastIndexOf(".")).replace(/[^a-zA-Z0-9.]/g, "")
          : "";
        const objectKey = `provider-applications/${application.id}/${item.documentType}-${crypto.randomUUID()}${extension}`;
        await uploadPrivateFile(
          objectKey,
          await item.file.arrayBuffer(),
          item.file.type || "application/octet-stream",
        );
        await db.insert(providerDocuments).values({
          applicationId: application.id,
          documentType: item.documentType,
          fileName: item.file.name.slice(0, 255),
          mimeType: item.file.type || "application/octet-stream",
          objectKey,
        });
      }
    } catch (error) {
      await db.update(providerApplications)
        .set({ status: "needs_documents", updatedAt: new Date().toISOString() })
        .where(eq(providerApplications.id, application.id));
      throw error;
    }

    return Response.json({
      id: application.id,
      reference: `PR-${new Date().getFullYear()}-${String(application.id).padStart(5, "0")}`,
    }, { status: 201 });
  } catch (error) {
    console.error("provider application submission failed", error);
    return jsonError("حصلت مشكلة أثناء إرسال الطلب. حاول مرة أخرى.", 500);
  }
}

export async function GET() {
  if (!(await hasAdminSession())) return jsonError("يلزم تسجيل الدخول للإدارة.", 401);

  try {
    const db = getDb();
    const applications = await db.select().from(providerApplications)
      .orderBy(desc(providerApplications.createdAt), desc(providerApplications.id));
    const documents = await db.select().from(providerDocuments)
      .orderBy(desc(providerDocuments.id));

    return Response.json({
      applications: applications.map((application) => ({
        ...application,
        documents: documents
          .filter((document) => document.applicationId === application.id)
          .map((document) => ({
            id: document.id,
            applicationId: document.applicationId,
            documentType: document.documentType,
            fileName: document.fileName,
            mimeType: document.mimeType,
            reviewStatus: document.reviewStatus,
            createdAt: document.createdAt,
            downloadUrl: `/api/provider-documents/${document.id}`,
          })),
      })),
    });
  } catch (error) {
    console.error("provider application list failed", error);
    return jsonError("تعذر تحميل الطلبات.", 500);
  }
}

export async function PATCH(request: Request) {
  if (!(await hasAdminSession())) return jsonError("يلزم تسجيل الدخول للإدارة.", 401);

  try {
    const payload = await request.json() as {
      target?: "application" | "document";
      id?: number;
      status?: string;
      reviewStatus?: string;
      adminNote?: string;
    };
    if (!Number.isInteger(payload.id) || Number(payload.id) < 1) return jsonError("رقم الطلب غير صحيح.");
    const db = getDb();

    if (payload.target === "application") {
      if (!payload.status || !applicationStatuses.has(payload.status)) return jsonError("حالة الطلب غير صحيحة.");
      await db.update(providerApplications).set({
        status: payload.status as "new" | "under_review" | "needs_documents" | "verified" | "rejected",
        adminNote: payload.adminNote?.trim().slice(0, 3000) || null,
        updatedAt: new Date().toISOString(),
      }).where(eq(providerApplications.id, Number(payload.id)));
      return Response.json({ success: true });
    }

    if (payload.target === "document") {
      if (!payload.reviewStatus || !reviewStatuses.has(payload.reviewStatus)) return jsonError("حالة المستند غير صحيحة.");
      await db.update(providerDocuments).set({
        reviewStatus: payload.reviewStatus as "pending" | "accepted" | "rejected",
      }).where(eq(providerDocuments.id, Number(payload.id)));
      return Response.json({ success: true });
    }

    return jsonError("نوع التحديث غير صحيح.");
  } catch (error) {
    console.error("provider application update failed", error);
    return jsonError("تعذر حفظ التحديث.", 500);
  }
}

export async function DELETE(request: Request) {
  if (!(await hasAdminSession())) return jsonError("يلزم تسجيل الدخول للإدارة.", 401);
  try {
    const payload = await request.json() as { id?: unknown };
    const id = Number(payload.id);
    if (!Number.isInteger(id) || id < 1) return jsonError("رقم الطلب غير صحيح.");
    const db = getDb();
    const documents = await db.select({ objectKey: providerDocuments.objectKey }).from(providerDocuments)
      .where(eq(providerDocuments.applicationId, id));
    await db.delete(providerDocuments).where(eq(providerDocuments.applicationId, id));
    const [deleted] = await db.delete(providerApplications).where(eq(providerApplications.id, id))
      .returning({ id: providerApplications.id });
    if (!deleted) return jsonError("الطلب غير موجود أو تم حذفه مسبقاً.", 404);
    await removePrivateFiles(documents.map((document) => document.objectKey));
    return Response.json({ success: true });
  } catch (error) {
    console.error("provider application deletion failed", error);
    return jsonError("تعذر حذف الطلب.", 500);
  }
}
