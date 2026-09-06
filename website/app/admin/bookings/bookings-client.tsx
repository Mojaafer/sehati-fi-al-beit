"use client";

import { useCallback, useEffect, useState } from "react";
import AdminNav from "../admin-nav";
import { AdminHeader } from "../../ui/AdminHeader";
import { Button } from "../../ui/Button";
import { Localized, useLanguage } from "../../language";

type Booking = {
  id: number;
  bookingCode: string;
  doctorId: number;
  doctorFullName: string | null;
  patientName: string;
  patientPhone: string;
  requestedDate: string;
  patientNote: string | null;
  whatsappOptIn: number;
  status: "pending_payment" | "receipt_submitted" | "payment_confirmed" | "payment_rejected" | "confirmed" | "cancelled";
  receiptUrl: string | null;
  supervisorName: string | null;
  supervisorPhone: string | null;
  createdAt: string;
};

const statusLabels: Record<string, string> = {
  pending_payment: "بانتظار التحويل", receipt_submitted: "الإشعار للمراجعة",
  payment_confirmed: "تم اعتماد الدفع", payment_rejected: "الإشعار مرفوض",
  confirmed: "الموعد مؤكد", cancelled: "ملغى",
};

function whatsappLink(phone: string, bookingCode: string, supervisor = false) {
  const normalized = phone.replace(/\D/g, "").replace(/^0/, "249");
  const message = supervisor
    ? `مرحباً، معك فريق صحتك. بخصوص طلب الحضور ${bookingCode}، نرجو مراجعة الطلب وتنسيق الاستقبال.`
    : `مرحباً، معك فريق صحتك. عندنا تحديث بخصوص طلب الحضور ${bookingCode}.`;
  return `https://wa.me/${normalized}?text=${encodeURIComponent(message)}`;
}

export default function AdminBookingsClient() {
  const { direction, locale } = useLanguage();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch("/api/doctor-bookings", { cache: "no-store" });
      if (response.status === 401) return window.location.assign("/admin/login");
      const data = await response.json() as { bookings?: Booking[]; error?: string };
      if (!response.ok) setError(data.error || "تعذر تحميل الحجوزات.");
      else setBookings(data.bookings || []);
    } catch {
      // A dropped connection, or a 500 answering with HTML instead of JSON, used to reject here
      // and leave the screen stuck on its spinner for good.
      setError("تعذر تحميل الحجوزات.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { const id = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(id); }, [load]);

  async function update(id: number, status: string, adminNote?: string) {
    setSavingId(id); setError(""); setNotice("");
    try {
      const response = await fetch(`/api/doctor-bookings/${id}`, {
        method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ status, adminNote }),
      });
      if (response.status === 401) return window.location.assign("/admin/login");
      const data = await response.json() as { error?: string };
      if (!response.ok) setError(data.error || "تعذر تحديث الحجز.");
      else { setNotice("تم تحديث حالة الحجز."); await load(); }
    } catch {
      setError("تعذر تحديث الحجز.");
    } finally {
      setSavingId(null);
    }
  }

  async function remove(id: number) {
    if (!window.confirm("هل تريد حذف طلب الحجز وصورة الإشعار نهائياً؟")) return;
    setSavingId(id);
    try {
      const response = await fetch(`/api/doctor-bookings/${id}`, { method: "DELETE" });
      if (response.status === 401) return window.location.assign("/admin/login");
      const data = await response.json() as { error?: string };
      if (!response.ok) setError(data.error || "تعذر حذف الحجز.");
      else { setNotice("تم حذف طلب الحجز."); await load(); }
    } catch {
      setError("تعذر حذف الحجز.");
    } finally {
      setSavingId(null);
    }
  }

  return <><AdminNav /><Localized><main className="admin-page" dir={direction}>
    <AdminHeader eyebrow="مراجعة الحجوزات" title="طلبات حجز الأطباء" description="الحضور بأسبقية الوصول، ولا يعتمد الطلب قبل مراجعة التحويل." actions={<Button variant="secondary" onClick={() => void load()}>تحديث القائمة</Button>} />
    {error && <div className="admin-error">{error}</div>}{notice && <div className="admin-success">{notice}</div>}
    <section className="admin-card-list">
      {loading ? <div className="admin-empty">جاري تحميل الحجوزات...</div> : bookings.length ? bookings.map((booking) => (
        <article className="admin-record-card" key={booking.id}>
          <header><div><span className={`record-status status-${booking.status}`}>{statusLabels[booking.status]}</span><small>{booking.bookingCode}</small><h2>{booking.patientName}</h2><p>{booking.doctorFullName || `الطبيب #${booking.doctorId}`} · {booking.patientPhone}</p></div><strong>{new Date(booking.requestedDate).toLocaleDateString(locale, { dateStyle: "long" })}<small>الدخول بأسبقية الحضور</small></strong></header>
          {booking.patientNote && <p className="record-note">ملاحظة طالب الخدمة: {booking.patientNote}</p>}
          <div className="record-links">
            {booking.receiptUrl && <a href={booking.receiptUrl} target="_blank" rel="noreferrer">فتح صورة إشعار التحويل</a>}
            {booking.whatsappOptIn === 1 && <a href={whatsappLink(booking.patientPhone, booking.bookingCode)} target="_blank" rel="noreferrer">واتساب طالب الخدمة</a>}
            {booking.supervisorPhone && <a href={whatsappLink(booking.supervisorPhone, booking.bookingCode, true)} target="_blank" rel="noreferrer">واتساب المشرف {booking.supervisorName ? `(${booking.supervisorName})` : ""}</a>}
          </div>
          {booking.whatsappOptIn !== 1 && <small className="record-muted">لم يوافق طالب الخدمة على تحديثات واتساب.</small>}
          <footer>
            {booking.status === "receipt_submitted" && <><button disabled={savingId === booking.id} onClick={() => void update(booking.id, "payment_confirmed")}>اعتماد التحويل</button><button className="warning" disabled={savingId === booking.id} onClick={() => void update(booking.id, "payment_rejected", "يرجى رفع صورة واضحة لإشعار التحويل.")}>رفض الإشعار</button></>}
            {booking.status === "payment_confirmed" && <button disabled={savingId === booking.id} onClick={() => void update(booking.id, "confirmed")}>تأكيد الموعد</button>}
            {!(["cancelled", "confirmed"] as string[]).includes(booking.status) && <button className="warning" disabled={savingId === booking.id} onClick={() => void update(booking.id, "cancelled")}>إلغاء</button>}
            <button className="danger" disabled={savingId === booking.id} onClick={() => void remove(booking.id)}>حذف نهائي</button>
          </footer>
        </article>
      )) : <div className="admin-empty">لا توجد طلبات حجز حتى الآن.</div>}
    </section>
  </main></Localized></>;
}
