"use client";

import { useCallback, useEffect, useState } from "react";
import AdminNav from "../admin-nav";
import { AdminHeader } from "../../ui/AdminHeader";
import { Button } from "../../ui/Button";
import { Localized, useLanguage } from "../../language";

type ServiceRequest = {
  id: number;
  requestCode: string;
  serviceType: "lab" | "nurse" | "physio";
  patientName: string;
  patientPhone: string;
  requestedDate: string;
  requestedTime: string;
  city: string;
  neighborhood: string | null;
  locationNote: string | null;
  serviceDetails: string | null;
  status: "pending" | "accepted" | "rejected" | "cancelled";
  createdAt: string;
};

const labels: Record<string, string> = { lab: "سحب العينات", nurse: "التمريض المنزلي", physio: "العلاج الطبيعي", pending: "جديد", accepted: "مقبول", rejected: "مرفوض", cancelled: "ملغى" };

export default function AdminServiceRequestsClient() {
  const { direction } = useLanguage();
  const [requests, setRequests] = useState<ServiceRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch("/api/service-requests", { cache: "no-store" });
      if (response.status === 401) return window.location.assign("/admin/login");
      const data = await response.json() as { requests?: ServiceRequest[]; error?: string };
      if (!response.ok) setError(data.error || "تعذر تحميل الطلبات.");
      else setRequests(data.requests || []);
    } catch {
      // A dropped connection, or a 500 answering with HTML instead of JSON, used to reject here
      // and leave the screen stuck on its spinner for good.
      setError("تعذر تحميل الطلبات.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { const id = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(id); }, [load]);

  async function remove(id: number) {
    if (!window.confirm("هل تريد حذف طلب الخدمة نهائياً؟")) return;
    setDeletingId(id); setError(""); setNotice("");
    try {
      const response = await fetch(`/api/service-requests/${id}`, { method: "DELETE" });
      if (response.status === 401) return window.location.assign("/admin/login");
      const data = await response.json() as { error?: string };
      if (!response.ok) setError(data.error || "تعذر حذف الطلب.");
      else { setNotice("تم حذف طلب الخدمة."); await load(); }
    } catch {
      setError("تعذر حذف الطلب.");
    } finally {
      setDeletingId(null);
    }
  }

  return <><AdminNav /><Localized><main className="admin-page" dir={direction}>
    <AdminHeader eyebrow="الخدمات المنزلية" title="طلبات الخدمات المنزلية" description="طلبات التمريض وسحب العينات والعلاج الطبيعي المحفوظة من الموقع." actions={<Button variant="secondary" onClick={() => void load()}>تحديث القائمة</Button>} />
    {error && <div className="admin-error">{error}</div>}{notice && <div className="admin-success">{notice}</div>}
    <section className="admin-card-list">
      {loading ? <div className="admin-empty">جاري تحميل الطلبات...</div> : requests.length ? requests.map((request) => (
        <article className="admin-record-card" key={request.id}>
          <header><div><span className="record-status">{labels[request.status]}</span><small>{request.requestCode}</small><h2>{labels[request.serviceType]}</h2><p>{request.patientName} · {request.patientPhone}</p></div><strong>{request.requestedDate}<small>{request.requestedTime}</small></strong></header>
          <div className="service-request-details"><p><b>الموقع:</b> {request.city}{request.neighborhood ? ` · ${request.neighborhood}` : ""}</p>{request.locationNote && <p><b>وصف الموقع:</b> {request.locationNote}</p>}{request.serviceDetails && <p><b>تفاصيل الخدمة:</b> {request.serviceDetails}</p>}</div>
          <footer><a className="record-action-link" href={`tel:${request.patientPhone}`}>اتصال بطالب الخدمة</a><button className="danger" disabled={deletingId === request.id} onClick={() => void remove(request.id)}>حذف نهائي</button></footer>
        </article>
      )) : <div className="admin-empty">لا توجد طلبات خدمات منزلية حتى الآن.</div>}
    </section>
  </main></Localized></>;
}
