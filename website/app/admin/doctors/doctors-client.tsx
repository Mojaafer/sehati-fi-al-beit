"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import AdminNav from "../admin-nav";
import { dayOptions, parseDays, type ClinicDay } from "../../doctor-data";
import { Localized, useLanguage } from "../../language";

type Doctor = {
  id: number;
  fullName: string;
  specialty: string;
  clinicLocation: string;
  city: string;
  bookingCost: number;
  currency: string;
  clinicDays: string;
  clinicStartTime: string;
  clinicEndTime: string;
  slotDurationMinutes: number;
  phone: string;
  supervisorName: string | null;
  supervisorPhone: string | null;
  paymentInstructions: string;
  paymentInstructionsVisible: number;
  status: "active" | "inactive";
};

type DoctorForm = Omit<Doctor, "id" | "clinicDays" | "supervisorName" | "supervisorPhone"> & {
  clinicDays: ClinicDay[];
  supervisorName: string;
  supervisorPhone: string;
};

const emptyForm: DoctorForm = {
  fullName: "", specialty: "", clinicLocation: "", city: "ود مدني", bookingCost: 0,
  currency: "SDG", clinicDays: ["monday", "wednesday"], clinicStartTime: "10:00",
  clinicEndTime: "14:00", slotDurationMinutes: 30, phone: "", supervisorName: "",
  supervisorPhone: "", paymentInstructions: "الدفع إلى حساب منصة صحتك بعد إرسال طلب الحجز.",
  paymentInstructionsVisible: 1, status: "active",
};

function toForm(doctor: Doctor): DoctorForm {
  return {
    ...doctor,
    clinicDays: parseDays(doctor.clinicDays),
    supervisorName: doctor.supervisorName || "",
    supervisorPhone: doctor.supervisorPhone || "",
  };
}

export default function AdminDoctorsClient() {
  const { direction } = useLanguage();
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [form, setForm] = useState<DoctorForm>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadDoctors = useCallback(async () => {
    setLoading(true);
    const response = await fetch("/api/admin/doctors", { cache: "no-store" });
    if (response.status === 401) return window.location.assign("/admin/login");
    const data = await response.json() as { doctors?: Doctor[]; error?: string };
    if (!response.ok) setError(data.error || "تعذر تحميل الأطباء.");
    else setDoctors(data.doctors || []);
    setLoading(false);
  }, []);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => void loadDoctors(), 0);
    return () => window.clearTimeout(timeoutId);
  }, [loadDoctors]);

  function updateField<K extends keyof DoctorForm>(key: K, value: DoctorForm[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function selectDoctor(doctor: Doctor) {
    setSelectedId(doctor.id);
    setForm(toForm(doctor));
    setError(""); setNotice("");
  }

  function startCreate() {
    setSelectedId(null);
    setForm(emptyForm);
    setError(""); setNotice("");
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true); setError(""); setNotice("");
    const response = await fetch(selectedId ? `/api/admin/doctors/${selectedId}` : "/api/admin/doctors", {
      method: selectedId ? "PATCH" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(form),
    });
    const data = await response.json() as { error?: string };
    if (!response.ok) setError(data.error || "تعذر حفظ الطبيب.");
    else {
      if (!selectedId) { setSelectedId(null); setForm(emptyForm); }
      setNotice(selectedId ? "تم تحديث بيانات الطبيب." : "تمت إضافة الطبيب إلى الدليل.");
      await loadDoctors();
    }
    setSaving(false);
  }

  async function remove(doctor: Doctor) {
    if (!window.confirm(`هل تريد حذف ${doctor.fullName} نهائياً؟`)) return;
    const response = await fetch(`/api/admin/doctors/${doctor.id}`, { method: "DELETE" });
    const data = await response.json() as { error?: string };
    if (!response.ok) setError(data.error || "تعذر حذف الطبيب.");
    else { setNotice("تم حذف الطبيب."); startCreate(); await loadDoctors(); }
  }

  async function toggleVisibility(doctor: Doctor) {
    const next = { ...toForm(doctor), status: doctor.status === "active" ? "inactive" as const : "active" as const };
    const response = await fetch(`/api/admin/doctors/${doctor.id}`, {
      method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify(next),
    });
    if (response.ok) { setNotice(next.status === "active" ? "تم إظهار الطبيب للزوار." : "تم إخفاء الطبيب من الدليل."); await loadDoctors(); }
    else { const data = await response.json() as { error?: string }; setError(data.error || "تعذر تغيير الظهور."); }
  }

  return <><AdminNav /><Localized><main className="admin-page" dir={direction}>
    <header className="admin-header"><div><span className="eyebrow">إدارة الدليل الطبي</span><h1>الأطباء والعيادات</h1><p>أضف الأطباء وحدد أيام وساعات العيادة وتعليمات الدفع.</p></div><div className="admin-header-actions"><button className="secondary-button" onClick={startCreate}>إضافة طبيب</button><button className="secondary-button" onClick={() => void loadDoctors()}>تحديث</button></div></header>
    {error && <div className="admin-error">{error}</div>}{notice && <div className="admin-success">{notice}</div>}
    <div className="admin-grid admin-doctors-grid">
      <section className="application-list">
        {loading ? <div className="admin-empty">جاري تحميل الأطباء...</div> : doctors.length ? doctors.map((doctor) => (
          <article className={selectedId === doctor.id ? "doctor-admin-row selected" : "doctor-admin-row"} key={doctor.id}>
            <button className="doctor-admin-main" onClick={() => selectDoctor(doctor)}>
              <span className={doctor.status === "active" ? "doctor-visibility active" : "doctor-visibility"}>{doctor.status === "active" ? "ظاهر للزوار" : "مخفي"}</span>
              <strong>{doctor.fullName}</strong><p>{doctor.specialty} · {doctor.city}</p><small>{doctor.clinicLocation} · {doctor.bookingCost.toLocaleString()} {doctor.currency}</small>
            </button>
            <div><button onClick={() => void toggleVisibility(doctor)}>{doctor.status === "active" ? "إخفاء" : "إظهار"}</button><button className="danger" onClick={() => void remove(doctor)}>حذف</button></div>
          </article>
        )) : <div className="admin-empty">لا يوجد أطباء مضافون بعد.</div>}
      </section>
      <form className="admin-dark-form" onSubmit={save}>
        <div><span>{selectedId ? "تعديل سجل الطبيب" : "إضافة سجل جديد"}</span><h2>بيانات الطبيب</h2></div>
        <div className="admin-form-grid">
          <label>اسم الطبيب<input required value={form.fullName} onChange={(event) => updateField("fullName", event.target.value)} /></label>
          <label>التخصص<input required value={form.specialty} onChange={(event) => updateField("specialty", event.target.value)} /></label>
          <label>موقع العيادة<input required value={form.clinicLocation} onChange={(event) => updateField("clinicLocation", event.target.value)} /></label>
          <label>المدينة<input required value={form.city} onChange={(event) => updateField("city", event.target.value)} /></label>
          <label>رقم التواصل<input required value={form.phone} onChange={(event) => updateField("phone", event.target.value)} /></label>
          <label>تكلفة الحجز<input required min="0" type="number" value={form.bookingCost} onChange={(event) => updateField("bookingCost", Number(event.target.value))} /></label>
          <label>العملة<input required value={form.currency} onChange={(event) => updateField("currency", event.target.value)} /></label>
          <label>اسم مشرف الجدول<input value={form.supervisorName} onChange={(event) => updateField("supervisorName", event.target.value)} /></label>
          <label>هاتف المشرف<input value={form.supervisorPhone} onChange={(event) => updateField("supervisorPhone", event.target.value)} /></label>
        </div>
        <fieldset><legend>أيام عمل العيادة</legend><div className="admin-day-grid">{dayOptions.map((day) => <label key={day.value}><input type="checkbox" checked={form.clinicDays.includes(day.value)} onChange={() => updateField("clinicDays", form.clinicDays.includes(day.value) ? form.clinicDays.filter((item) => item !== day.value) : [...form.clinicDays, day.value])} />{day.label}</label>)}</div><div className="admin-form-grid three"><label>من<input type="time" value={form.clinicStartTime} onChange={(event) => updateField("clinicStartTime", event.target.value)} /></label><label>إلى<input type="time" value={form.clinicEndTime} onChange={(event) => updateField("clinicEndTime", event.target.value)} /></label><label>مدة الموعد<select value={form.slotDurationMinutes} onChange={(event) => updateField("slotDurationMinutes", Number(event.target.value))}><option value="15">15 دقيقة</option><option value="30">30 دقيقة</option><option value="60">60 دقيقة</option></select></label></div></fieldset>
        <label>تعليمات الدفع<textarea required value={form.paymentInstructions} onChange={(event) => updateField("paymentInstructions", event.target.value)} /></label>
        <div className="admin-checkboxes"><label><input type="checkbox" checked={form.paymentInstructionsVisible === 1} onChange={(event) => updateField("paymentInstructionsVisible", event.target.checked ? 1 : 0)} /> إظهار تعليمات الدفع</label><label><input type="checkbox" checked={form.status === "active"} onChange={(event) => updateField("status", event.target.checked ? "active" : "inactive")} /> ظاهر في الدليل</label></div>
        <button className="admin-submit" disabled={saving}>{saving ? "جاري الحفظ..." : selectedId ? "حفظ التعديلات" : "حفظ الطبيب"}</button>
      </form>
    </div>
  </main></Localized></>;
}
