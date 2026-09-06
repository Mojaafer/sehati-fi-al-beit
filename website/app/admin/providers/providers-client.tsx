"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import AdminNav from "../admin-nav";
import { AdminHeader } from "../../ui/AdminHeader";
import { Button } from "../../ui/Button";
import { Localized, useLanguage } from "../../language";

type DocumentRecord = {
  id: number;
  documentType: string;
  fileName: string;
  mimeType: string;
  reviewStatus: "pending" | "accepted" | "rejected";
  downloadUrl: string;
};

type Application = {
  id: number;
  profession: "doctor" | "lab" | "physio" | "nurse";
  fullName: string;
  phone: string;
  specialty: string | null;
  experience: string | null;
  city: string;
  workplace: string | null;
  availability: string | null;
  status: "new" | "under_review" | "needs_documents" | "verified" | "rejected";
  adminNote: string | null;
  createdAt: string;
  documents: DocumentRecord[];
};

const labels: Record<string, string> = {
  doctor: "طبيب/ة",
  lab: "فني/ة مختبرات",
  physio: "أخصائي/ة علاج طبيعي",
  nurse: "ممرض/ة",
  new: "جديد",
  under_review: "قيد المراجعة",
  needs_documents: "يحتاج مستندات",
  verified: "تم التحقق",
  rejected: "مرفوض",
  pending: "قيد الفحص",
  accepted: "مقبول",
};

export default function AdminProvidersClient({ displayName }: { displayName: string }) {
  const { direction } = useLanguage();
  const [applications, setApplications] = useState<Application[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [noteDrafts, setNoteDrafts] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const loadApplications = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await fetch("/api/provider-applications", { cache: "no-store" });
      // The other admin screens all bounce to the login page when the 8-hour session expires;
      // without this one an expired session showed a bare "unauthorised" error instead.
      if (response.status === 401) return window.location.assign("/admin/login");
      const data = await response.json() as { applications?: Application[]; error?: string };
      if (!response.ok) throw new Error(data.error || "تعذر تحميل الطلبات.");
      setApplications(data.applications || []);
      setSelectedId((current) => current ?? data.applications?.[0]?.id ?? null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "تعذر تحميل الطلبات.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => void loadApplications(), 0);
    return () => window.clearTimeout(timeoutId);
  }, [loadApplications]);
  const selected = useMemo(
    () => applications.find((application) => application.id === selectedId) ?? null,
    [applications, selectedId],
  );
  const note = selected ? noteDrafts[selected.id] ?? selected.adminNote ?? "" : "";

  async function update(payload: Record<string, unknown>) {
    setSaving(true);
    setError("");
    try {
      const response = await fetch("/api/provider-applications", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (response.status === 401) return window.location.assign("/admin/login");
      const data = await response.json() as { error?: string };
      if (!response.ok) throw new Error(data.error || "تعذر حفظ التحديث.");
      await loadApplications();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "تعذر حفظ التحديث.");
    } finally {
      setSaving(false);
    }
  }

  async function removeApplication(id: number) {
    if (!window.confirm("هل تريد حذف طلب مقدم الخدمة ومستنداته نهائياً؟")) return;
    setSaving(true); setError("");
    try {
      const response = await fetch("/api/provider-applications", {
        method: "DELETE",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id }),
      });
      if (response.status === 401) return window.location.assign("/admin/login");
      const data = await response.json() as { error?: string };
      if (!response.ok) throw new Error(data.error || "تعذر حذف الطلب.");
      setSelectedId(null);
      await loadApplications();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "تعذر حذف الطلب.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <><AdminNav /><Localized><main className="admin-page" dir={direction}>
      <AdminHeader
        eyebrow="مركز التحقق"
        title="طلبات مقدمي الخدمة"
        description={`مرحباً ${displayName}. راجع الهوية والمؤهل والترخيص قبل تفعيل مقدم الخدمة.`}
        before={<Link href="/" className="admin-back">← العودة للموقع</Link>}
        actions={<div className="admin-header-actions"><Button variant="secondary" onClick={() => void loadApplications()}>تحديث القائمة</Button></div>}
      />

      {error && <div className="admin-error">{error}</div>}
      <div className="admin-grid">
        <section className="application-list">
          {loading ? (
            <div className="admin-empty">جاري تحميل الطلبات...</div>
          ) : applications.length === 0 ? (
            <div className="admin-empty">لا توجد طلبات حتى الآن.</div>
          ) : applications.map((application) => (
            <button
              key={application.id}
              className={selected?.id === application.id ? "application-row selected" : "application-row"}
              onClick={() => setSelectedId(application.id)}
            >
              <span><b>{labels[application.profession]}</b><small>PR-{String(application.id).padStart(5, "0")}</small></span>
              <strong>{application.fullName}</strong>
              <p>{application.city} · {application.phone}</p>
              <em className={`status status-${application.status}`}>{labels[application.status]}</em>
            </button>
          ))}
        </section>

        <aside className="application-details">
          {selected ? (
            <>
              <div className="detail-heading">
                <span>تفاصيل الطلب #{selected.id}</span>
                <h2>{selected.fullName}</h2>
                <p>{labels[selected.profession]} · {selected.city}</p>
              </div>
              <dl>
                <div><dt>الهاتف</dt><dd>{selected.phone}</dd></div>
                <div><dt>التخصص</dt><dd>{selected.specialty || "غير محدد"}</dd></div>
                <div><dt>الخبرة</dt><dd>{selected.experience || "غير محددة"}</dd></div>
                <div><dt>مكان العمل</dt><dd>{selected.workplace || "غير محدد"}</dd></div>
                <div><dt>التوفر</dt><dd>{selected.availability || "غير محدد"}</dd></div>
              </dl>

              <section className="admin-section">
                <h3>المستندات</h3>
                {selected.documents.length ? selected.documents.map((document) => (
                  <article className="admin-document" key={document.id}>
                    <a href={document.downloadUrl} target="_blank" rel="noreferrer">↗ {document.fileName}</a>
                    <span>{labels[document.reviewStatus]}</span>
                    <div>
                      {(["accepted", "pending", "rejected"] as const).map((reviewStatus) => (
                        <button
                          key={reviewStatus}
                          disabled={saving}
                          onClick={() => void update({ target: "document", id: document.id, reviewStatus })}
                        >
                          {labels[reviewStatus] || "رفض"}
                        </button>
                      ))}
                    </div>
                  </article>
                )) : <p className="admin-muted">لم يتم رفع مستندات.</p>}
              </section>

              <section className="admin-section">
                <h3>ملاحظات الإدارة</h3>
                <textarea
                  value={note}
                  onChange={(event) => setNoteDrafts((current) => ({ ...current, [selected.id]: event.target.value }))}
                  placeholder="اكتب ملاحظة لفريق المراجعة"
                />
                <button disabled={saving} onClick={() => void update({ target: "application", id: selected.id, status: selected.status, adminNote: note })}>حفظ الملاحظة</button>
              </section>

              <div className="status-actions">
                <button disabled={saving} onClick={() => void update({ target: "application", id: selected.id, status: "verified", adminNote: note })}>اعتماد الطلب</button>
                <button disabled={saving} onClick={() => void update({ target: "application", id: selected.id, status: "needs_documents", adminNote: note })}>طلب مستندات</button>
                <button className="reject" disabled={saving} onClick={() => void update({ target: "application", id: selected.id, status: "rejected", adminNote: note })}>رفض الطلب</button>
                <button className="delete" disabled={saving} onClick={() => void removeApplication(selected.id)}>حذف الطلب نهائياً</button>
              </div>
            </>
          ) : <div className="admin-empty dark">اختر طلباً لعرض التفاصيل.</div>}
        </aside>
      </div>
    </main></Localized></>
  );
}
