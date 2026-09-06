"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { clinicDayValues, dayLabels, parseDays, type ClinicDay } from "../doctor-data";
import { Localized, useLanguage } from "../language";

type Doctor = {
  id: number; fullName: string; specialty: string; clinicLocation: string; city: string;
  bookingCost: number; currency: string; clinicDays: string; clinicStartTime: string;
  clinicEndTime: string; slotDurationMinutes: number; phone: string;
  paymentInstructions?: string | null; paymentInstructionsVisible?: number;
};

function formatTime(value: string) {
  const [hours, minutes] = value.split(":").map(Number);
  return `${hours % 12 || 12}:${String(minutes).padStart(2, "0")} ${hours < 12 ? "ص" : "م"}`;
}

function dateOptions(doctor: Doctor) {
  const result: Array<{ value: string; label: string }> = [];
  const days = parseDays(doctor.clinicDays);
  const start = new Date(); start.setHours(12, 0, 0, 0);
  for (let offset = 0; offset <= 45; offset += 1) {
    const date = new Date(start); date.setDate(start.getDate() + offset);
    if (!days.includes(clinicDayValues[date.getDay()])) continue;
    result.push({
      value: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`,
      label: date.toLocaleDateString("ar-SD", { weekday: "long", month: "long", day: "numeric" }),
    });
  }
  return result;
}

export default function DoctorsClient() {
  const { language, locale } = useLanguage();
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedDoctor, setSelectedDoctor] = useState<Doctor | null>(null);
  const [bookingCode, setBookingCode] = useState("");
  const [paymentInstructions, setPaymentInstructions] = useState<string | null>(null);
  const [receipt, setReceipt] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState("");
  const [form, setForm] = useState({ patientName: "", patientPhone: "", requestedDate: "", patientNote: "", whatsappOptIn: false });

  const load = useCallback(async () => {
    try {
      const response = await fetch("/api/doctors", { cache: "no-store" });
      const data = await response.json() as { doctors?: Doctor[]; error?: string };
      if (!response.ok) setError(data.error || "تعذر تحميل دليل الأطباء.");
      else setDoctors(data.doctors || []);
    } catch {
      // A dropped connection, or a 500 answering with HTML instead of JSON, used to reject here
      // and leave a visitor staring at "جاري تحميل الأطباء..." for good.
      setError("تعذر تحميل دليل الأطباء.");
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { const id = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(id); }, [load]);
  useEffect(() => {
    if (!selectedDoctor) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setSelectedDoctor(null);
    }
    window.addEventListener("keydown", closeOnEscape);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [selectedDoctor]);

  const availableDates = useMemo(() => selectedDoctor ? dateOptions(selectedDoctor).map((date) => ({ ...date, label: new Date(`${date.value}T12:00:00`).toLocaleDateString(locale, { weekday: "long", month: "long", day: "numeric" }) })) : [], [selectedDoctor, locale]);

  function openBooking(doctor: Doctor) {
    const dates = dateOptions(doctor);
    setSelectedDoctor(doctor); setBookingCode(""); setPaymentInstructions(null); setReceipt(null); setNotice(""); setError("");
    setForm({ patientName: "", patientPhone: "", requestedDate: dates[0]?.value || "", patientNote: "", whatsappOptIn: false });
  }

  async function submitBooking(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!selectedDoctor) return;
    setSubmitting(true); setError("");
    try {
      const response = await fetch("/api/doctor-bookings", {
        method: "POST", headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ doctorId: selectedDoctor.id, ...form }),
      });
      const data = await response.json() as { bookingCode?: string; paymentInstructions?: string | null; error?: string };
      if (!response.ok) setError(data.error || "تعذر إنشاء طلب الحجز.");
      else { setBookingCode(data.bookingCode || ""); setPaymentInstructions(data.paymentInstructions ?? null); setNotice("تم إنشاء الطلب. ارفع صورة إشعار التحويل لإرساله للمراجعة."); }
    } catch {
      setError("تعذر إنشاء طلب الحجز.");
    } finally {
      setSubmitting(false);
    }
  }

  async function uploadReceipt() {
    if (!receipt || !bookingCode) return;
    setSubmitting(true); setError("");
    try {
      const payload = new FormData(); payload.set("bookingCode", bookingCode); payload.set("receipt", receipt);
      const response = await fetch("/api/doctor-bookings/receipt", { method: "POST", body: payload });
      const data = await response.json() as { error?: string };
      if (!response.ok) setError(data.error || "تعذر رفع الإشعار.");
      else { setNotice("تم رفع إشعار التحويل، وسيراجعه فريق صحتك."); setReceipt(null); }
    } catch {
      setError("تعذر رفع الإشعار.");
    } finally {
      setSubmitting(false);
    }
  }

  async function optOut() {
    try {
      const response = await fetch("/api/doctor-bookings/preferences", {
        method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ bookingCode, whatsappOptIn: false }),
      });
      if (response.ok) { setForm((current) => ({ ...current, whatsappOptIn: false })); setNotice("تم إلغاء تحديثات واتساب لهذا الطلب."); }
    } catch {
      setError("تعذر إلغاء تحديثات واتساب، حاول مرة أخرى.");
    }
  }

  return <Localized><main className="doctors-page" dir={language === "ar" ? "rtl" : "ltr"}><div className="doctors-container">
    <div className="doctors-page-top"><Link className="doctors-logo" href="/" aria-label="منصة صحتك"><img src="/sehatak-logo.webp" alt="صحتك — Sehatak" /></Link><Link className="doctors-back" href="/">← العودة للرئيسية</Link></div>
    <header className="doctors-heading"><span className="eyebrow">دليل الأطباء</span><h1>اختار الطبيب المناسب ليك</h1><p>اختر يوم الحضور من جدول العيادة. الدخول بأسبقية الحضور، والطلب لا يتأكد إلا بعد مراجعة إشعار التحويل.</p></header>
    {error && !selectedDoctor && <div className="admin-error">{error}</div>}
    {loading ? <div className="admin-empty">جاري تحميل الأطباء...</div> : doctors.length ? <section className="doctors-grid">{doctors.map((doctor) => {
      const days = parseDays(doctor.clinicDays);
      return <article className="doctor-public-card" key={doctor.id}><div className="doctor-card-top"><span>✚</span><b>متاح للحجز</b></div><h2>{doctor.fullName}</h2><strong>{doctor.specialty}</strong><div className="doctor-card-details"><p>⌖ {doctor.clinicLocation} · {doctor.city}</p><p>☎ {doctor.phone}</p><p><b>جدول العيادة:</b> {days.length ? `${days.map((day) => dayLabels[day]).join("، ")} من ${formatTime(doctor.clinicStartTime)} إلى ${formatTime(doctor.clinicEndTime)}` : "لم يحدد بعد"}</p></div><div className="doctor-price"><span>تكلفة الحجز</span><b>{doctor.bookingCost.toLocaleString()} {doctor.currency}</b></div><button disabled={!days.length} onClick={() => openBooking(doctor)}>{days.length ? "ابدأ طلب الحجز" : "لم يحدد الطبيب مواعيده بعد"}</button></article>;
    })}</section> : <div className="admin-empty">سيظهر الأطباء المعتمدون هنا قريباً.</div>}
  </div>
  {selectedDoctor && <div className="modal-backdrop"><section className="doctor-booking-modal" role="dialog" aria-modal="true" aria-label="حجز موعد طبيب">
    <button className="close-button" onClick={() => setSelectedDoctor(null)} aria-label="إغلاق">×</button>
    <span className="eyebrow">حجز موعد في العيادة</span><h2>{selectedDoctor.fullName}</h2><p>{selectedDoctor.specialty} · {selectedDoctor.clinicLocation}</p>
    {error && <div className="admin-error">{error}</div>}{notice && <div className="admin-success">{notice}</div>}
    {!bookingCode ? <form className="doctor-booking-form" onSubmit={submitBooking}><div className="booking-guidance">اختر يوم الحضور فقط. لا يوجد وقت محجوز لكل مريض؛ الدخول يكون بأسبقية الحضور داخل ساعات العيادة.</div><label>الاسم الكامل<input required value={form.patientName} onChange={(event) => setForm({ ...form, patientName: event.target.value })} /></label><label>رقم الهاتف<input required placeholder="09XXXXXXXX" value={form.patientPhone} onChange={(event) => setForm({ ...form, patientPhone: event.target.value })} /></label><label>يوم الحضور<select required value={form.requestedDate} onChange={(event) => setForm({ ...form, requestedDate: event.target.value })}>{availableDates.map((date) => <option value={date.value} key={date.value}>{date.label}</option>)}</select></label><label>ملاحظة اختيارية<textarea value={form.patientNote} onChange={(event) => setForm({ ...form, patientNote: event.target.value })} /></label><label className="whatsapp-consent"><input type="checkbox" checked={form.whatsappOptIn} onChange={(event) => setForm({ ...form, whatsappOptIn: event.target.checked })} /><span>أوافق على استلام تحديثات الحجز عبر واتساب على الرقم المدخل.</span></label><button disabled={submitting || !availableDates.length}>{submitting ? "جاري الإرسال..." : "إرسال طلب الحضور"}</button></form> : <div className="doctor-payment-step"><div className="booking-code"><strong>رقم الطلب: {bookingCode}</strong><p>احتفظ بالرقم. {paymentInstructions || "تعليمات الدفع ستظهر بعد تأكيدها من الإدارة."}</p></div><label className="receipt-upload">↑<strong>ارفع صورة إشعار التحويل</strong><small>PNG أو JPG أو WEBP — بحد أقصى 5 ميغابايت</small><input type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => setReceipt(event.target.files?.[0] || null)} /></label><button disabled={!receipt || submitting} onClick={() => void uploadReceipt()}>{submitting ? "جاري الرفع..." : "إرسال إشعار التحويل"}</button>{form.whatsappOptIn && <button className="secondary" onClick={() => void optOut()}>إلغاء تحديثات واتساب</button>}</div>}
  </section></div>}
  </main></Localized>;
}
