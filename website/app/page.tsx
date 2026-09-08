"use client";

import { FormEvent, useEffect, useState } from "react";
import { LanguageSwitch, Localized } from "./language";
import SanadAssistant from "./sanad-assistant";
import { Button } from "./ui/Button";

const services = [
  {
    id: "lab",
    icon: "✦",
    tone: "teal",
    title: "سحب العينات من المنزل",
    description: "فني مختبر مؤهل يجيك للمنزل لسحب العينة وإرسالها للمعمل.",
    cta: "اطلب الخدمة",
  },
  {
    id: "nurse",
    icon: "♡",
    tone: "mint",
    title: "التمريض المنزلي",
    description: "تقييم تمريضي، قياسات حيوية، غيار جروح بسيط، وتعليم الأسرة حسب خطة الرعاية.",
    cta: "اطلب ممرض/ة",
  },
  {
    id: "physio",
    icon: "◒",
    tone: "sand",
    title: "العلاج الطبيعي",
    description: "تقييم حركة وتمارين منزلية آمنة للتأهيل، التوازن، والقدرة على الحركة.",
    cta: "احجز جلسة",
  },
  {
    id: "doctor",
    icon: "＋",
    tone: "blue",
    title: "حجز موعد مع طبيب",
    description: "اختار طبيباً معتمداً ويوم الحضور في العيادة حسب الجدول المتاح.",
    cta: "افتح دليل الأطباء",
  },
];

function BrandMark({ placement = "symbol" }: { placement?: "header" | "symbol" | "footer" }) {
  return (
    <span className={`brand-mark brand-mark-${placement}`} aria-hidden="true">
      <img src="/sehatak-logo.webp" alt="" />
    </span>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="m5 12 4 4L19 6" />
    </svg>
  );
}

type ServiceId = "lab" | "nurse" | "physio" | "doctor";
const bookingSteps = ["الخدمة", "التفاصيل", "الموقع", "الموعد", "المريض", "التأكيد"];

function useDialog(onClose: () => void) {
  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    window.addEventListener("keydown", closeOnEscape);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [onClose]);
}

function BookingModal({
  initialService,
  onClose,
}: {
  initialService?: ServiceId;
  onClose: () => void;
}) {
  useDialog(onClose);
  const [step, setStep] = useState(initialService ? 1 : 0);
  const [service, setService] = useState<ServiceId>(initialService ?? "lab");
  const [slot, setSlot] = useState("10:00 – 12:00");
  const [requestedDate, setRequestedDate] = useState("غداً");
  const [form, setForm] = useState({
    serviceDetails: initialService === "nurse" ? "تقييم تمريضي وقياس العلامات الحيوية" : initialService === "physio" ? "تقييم الحركة ووضع خطة تمارين" : "تحليل دم شامل",
    serviceNote: "", city: "ود مدني", neighborhood: "", locationNote: "",
    patientName: "", patientPhone: "", age: "", gender: "", medicalNote: "",
  });
  const [done, setDone] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [reference, setReference] = useState("");
  const selectedService = services.find((item) => item.id === service);

  function updateForm(field: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function selectService(nextService: ServiceId) {
    if (nextService === "doctor") {
      window.location.assign("/doctors");
      return;
    }
    setService(nextService);
    updateForm("serviceDetails", nextService === "nurse" ? "تقييم تمريضي وقياس العلامات الحيوية" : nextService === "physio" ? "تقييم الحركة ووضع خطة تمارين" : "تحليل دم شامل");
  }

  async function next() {
    setError("");
    if (step === 1 && !form.serviceDetails.trim()) return setError("اختر تفاصيل الخدمة المطلوبة.");
    if (step === 2 && (!form.city.trim() || !form.neighborhood.trim() || !form.locationNote.trim())) return setError("أكمل المدينة والحي ووصف الموقع.");
    if (step === 3 && !slot) return setError("اختر الوقت المناسب.");
    if (step === 4 && (!form.patientName.trim() || !/^((09\d{8})|(\+2499\d{8})|(002499\d{8}))$/.test(form.patientPhone.replace(/[\s()-]/g, "")))) {
      return setError("أدخل اسم المستفيد ورقم هاتف سوداني صحيح مثل 09XXXXXXXX.");
    }
    if (step < 5) return setStep((current) => current + 1);

    setSubmitting(true);
    try {
      const response = await fetch("/api/service-requests", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          serviceType: service,
          patientName: form.patientName,
          patientPhone: form.patientPhone,
          requestedDate,
          requestedTime: slot,
          city: form.city,
          neighborhood: form.neighborhood,
          locationNote: form.locationNote,
          serviceDetails: [form.serviceDetails, form.serviceNote, form.age && `العمر: ${form.age}`, form.gender && `الجنس: ${form.gender}`, form.medicalNote].filter(Boolean).join(" — "),
        }),
      });
      const data = await response.json() as { requestCode?: string; error?: string };
      if (!response.ok) throw new Error(data.error || "تعذر إرسال الطلب.");
      setReference(data.requestCode || "");
      setDone(true);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "تعذر إرسال الطلب.");
    } finally {
      setSubmitting(false);
    }
  }

  if (done) {
    return <Localized>{(
      <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label="ملخص الطلب التجريبي">
        <div className="booking-modal confirmation-modal">
          <button className="close-button" onClick={onClose} aria-label="إغلاق">×</button>
          <div className="success-orb">✓</div>
          <span className="eyebrow">تم استلام طلبك</span>
          <h2>طلبك في طريقه للمراجعة</h2>
          <p className="modal-copy">سيتواصل معك فريق صحتك لتأكيد تفاصيل الخدمة وتعيين مقدم الخدمة.</p>
          <div className="request-number">{reference}<span>رقم الطلب</span></div>
          <div className="summary-list">
            <div><span>الخدمة</span><strong>{selectedService?.title}</strong></div>
            <div><span>الموعد</span><strong>{requestedDate} · {slot}</strong></div>
            <div><span>الموقع</span><strong>{form.city} · {form.neighborhood}</strong></div>
            <div><span>التكلفة</span><strong>تُؤكد بعد مراجعة التفاصيل</strong></div>
          </div>
          <Button variant="primary" className="full" onClick={onClose}>العودة للرئيسية ←</Button>
        </div>
      </div>
    )}</Localized>;
  }

  return <Localized>{(
    <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label="طلب خدمة منزلية">
      <div className="booking-modal">
        <button className="close-button" onClick={onClose} aria-label="إغلاق">×</button>
        <div className="modal-heading">
          <div><span className="eyebrow">طلب خدمة منزلية</span><h2>نرتبها معاك خطوة بخطوة</h2></div>
          <span className="step-count">{step + 1} / 6</span>
        </div>
        <div className="stepper">
          {bookingSteps.map((item, index) => (
            <div className={index <= step ? "step-dot active" : "step-dot"} key={item}>
              <span>{index < step ? "✓" : index + 1}</span><small>{item}</small>
            </div>
          ))}
        </div>

        {step === 0 && (
          <section className="modal-section">
            <h3>شنو الخدمة المحتاجها؟</h3>
            <div className="modal-service-list">
              {services.filter((item) => item.id !== "doctor").map((item) => (
                <button
                  key={item.id}
                  className={service === item.id ? "modal-service selected" : "modal-service"}
                  onClick={() => selectService(item.id as ServiceId)}
                >
                  <span className={`service-icon ${item.tone}`}>{item.icon}</span>
                  <span><strong>{item.title}</strong><small>{item.description}</small></span>
                  {service === item.id && <b>✓</b>}
                </button>
              ))}
            </div>
          </section>
        )}

        {step === 1 && (
          <section className="modal-section">
            <h3>{service === "lab" ? "شنو الفحص المطلوب؟" : service === "nurse" ? "شنو نوع التمريض المحتاجه؟" : service === "physio" ? "شنو هدف الجلسة؟" : "شنو سبب الزيارة؟"}</h3>
            {service === "lab" && (
              <>
                <label>نوع التحليل<select value={form.serviceDetails} onChange={(event) => updateForm("serviceDetails", event.target.value)}><option>تحليل دم شامل</option><option>سكر صائم</option><option>وظائف كلى</option><option>وظائف كبد</option><option>دهون الدم</option><option>تحليل بول</option></select></label>
                <label>هل عندك طلب فحص من طبيب؟<select value={form.serviceNote} onChange={(event) => updateForm("serviceNote", event.target.value)}><option value="">اختر</option><option>يوجد طلب فحص من طبيب</option><option>لا يوجد طلب فحص من طبيب</option></select></label>
                <label>ملاحظات إضافية<textarea value={form.serviceNote} onChange={(event) => updateForm("serviceNote", event.target.value)} placeholder="اكتب أي تفاصيل تساعدنا نخدمك أفضل" /></label>
              </>
            )}
            {service === "nurse" && (
              <>
                <label>الخدمة المطلوبة<select value={form.serviceDetails} onChange={(event) => updateForm("serviceDetails", event.target.value)}><option>تقييم تمريضي وقياس العلامات الحيوية</option><option>غيار جرح بسيط حسب وصفة أو خطة رعاية</option><option>الوقاية من قرح الضغط وتغيير الوضعية</option><option>متابعة بعد الخروج وتعليم الأسرة</option></select></label>
                <label>هل توجد وصفة أو خطة رعاية؟<select value={form.serviceNote} onChange={(event) => updateForm("serviceNote", event.target.value)}><option value="">اختر</option><option>نعم، توجد وصفة أو خطة رعاية</option><option>لا، أحتاج تقييم الممرض/ة أولاً</option></select></label>
                <p className="safety-inline">✓ لا تشمل الخدمة الطوارئ، تغيير جرعات الأدوية، أو أي إجراء غير موصوف.</p>
              </>
            )}
            {service === "physio" && (
              <>
                <label>الهدف من الجلسة<select value={form.serviceDetails} onChange={(event) => updateForm("serviceDetails", event.target.value)}><option>تقييم الحركة ووضع خطة تمارين</option><option>تأهيل بعد إصابة أو عملية</option><option>تمارين التوازن وتقليل خطر السقوط</option><option>تحسين المشي والانتقال من السرير</option></select></label>
                <label>هل لديك تشخيص أو تقرير طبي؟<select value={form.serviceNote} onChange={(event) => updateForm("serviceNote", event.target.value)}><option value="">اختر</option><option>نعم، يوجد تشخيص أو تقرير طبي</option><option>لا، أحتاج تقييم أخصائي أولاً</option></select></label>
                <p className="safety-inline">✓ يبدأ العلاج بتقييم أخصائي، ولا يستخدم للطوارئ أو بعد إصابة حادة غير مُقيّمة.</p>
              </>
            )}
          </section>
        )}

        {step === 2 && (
          <section className="modal-section">
            <h3>وين محتاج الخدمة؟</h3>
            <label>المدينة<select value={form.city} onChange={(event) => updateForm("city", event.target.value)}><option>ود مدني</option><option>الخرطوم</option><option>سنار</option></select></label>
            <label>الحي<input value={form.neighborhood} onChange={(event) => updateForm("neighborhood", event.target.value)} placeholder="مثال: حي الدرجة" /></label>
            <label>وصف الموقع<input value={form.locationNote} onChange={(event) => updateForm("locationNote", event.target.value)} placeholder="اسم الشارع أو رقم المنزل" /></label>
          </section>
        )}

        {step === 3 && (
          <section className="modal-section">
            <h3>اختار الوقت المناسب</h3>
            <div className="date-choice"><button className={requestedDate === "غداً" ? "selected" : ""} onClick={() => setRequestedDate("غداً")}>غداً<small>أقرب موعد</small></button><button className={requestedDate === "بعد غد" ? "selected" : ""} onClick={() => setRequestedDate("بعد غد")}>بعد غد<small>المواعيد المتاحة</small></button></div>
            <div className="slot-grid">
              {["8:00 – 10:00", "10:00 – 12:00", "12:00 – 2:00", "2:00 – 4:00", "4:00 – 6:00", "6:00 – 8:00"].map((item) => (
                <button className={slot === item ? "selected" : ""} onClick={() => setSlot(item)} key={item}>◷ {item}</button>
              ))}
            </div>
          </section>
        )}

        {step === 4 && (
          <section className="modal-section">
            <h3>بيانات الشخص المستفيد</h3>
            <div className="two-fields"><label>الاسم<input value={form.patientName} onChange={(event) => updateForm("patientName", event.target.value)} placeholder="الاسم الكامل" /></label><label>رقم الهاتف<input value={form.patientPhone} onChange={(event) => updateForm("patientPhone", event.target.value)} placeholder="09X XXX XXXX" /></label></div>
            <div className="two-fields"><label>العمر<input value={form.age} onChange={(event) => updateForm("age", event.target.value)} inputMode="numeric" placeholder="العمر" /></label><label>الجنس<select value={form.gender} onChange={(event) => updateForm("gender", event.target.value)}><option value="">اختر</option><option>ذكر</option><option>أنثى</option></select></label></div>
            <label>ملاحظات طبية <small>اختياري</small><textarea value={form.medicalNote} onChange={(event) => updateForm("medicalNote", event.target.value)} placeholder="أي معلومات مهمة للفريق" /></label>
            <p className="privacy-note">✓ بياناتك الطبية لا يتم مشاركتها إلا مع مقدم الخدمة المكلف بطلبك.</p>
          </section>
        )}

        {step === 5 && (
          <section className="modal-section">
            <h3>راجع طلبك قبل الإرسال</h3>
            <div className="price-card">
              <div><span>الخدمة</span><strong>{selectedService?.title}</strong></div>
              <div><span>الموعد المطلوب</span><strong>{requestedDate} · {slot}</strong></div>
              <div><span>الموقع</span><strong>{form.city} · {form.neighborhood}</strong></div>
              <div className="price-total"><span>التكلفة</span><strong>تُحدد بعد المراجعة</strong></div>
            </div>
            <p className="demo-disclaimer">إرسال الطلب لا يعني تأكيد الموعد أو السعر. سيتواصل الفريق معك بعد مراجعته.</p>
          </section>
        )}

        <div className="modal-actions">
          {step > 0 && <Button variant="secondary" onClick={() => setStep((current) => current - 1)}>رجوع</Button>}
          <Button variant="primary" disabled={submitting} onClick={() => void next()}>{submitting ? "جاري الإرسال..." : step === 5 ? "إرسال الطلب" : "التالي"} ←</Button>
        </div>
        {error && <div className="form-error">{error}</div>}
      </div>
    </div>
  )}</Localized>;
}

function ProviderRegistrationModal({ onClose }: { onClose: () => void }) {
  useDialog(onClose);
  const [profession, setProfession] = useState<ServiceId>("doctor");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [reference, setReference] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      const response = await fetch("/api/provider-applications", {
        method: "POST",
        body: new FormData(event.currentTarget),
      });
      const data = await response.json() as { reference?: string; error?: string };
      if (!response.ok) throw new Error(data.error || "تعذر إرسال الطلب.");
      setReference(data.reference || "");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "تعذر إرسال الطلب.");
    } finally {
      setSubmitting(false);
    }
  }

  if (reference) {
    return <Localized>{(
      <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label="تم إرسال طلب مقدم الخدمة">
        <div className="booking-modal confirmation-modal">
          <button className="close-button" onClick={onClose} aria-label="إغلاق">×</button>
          <div className="success-orb">✓</div>
          <span className="eyebrow">تم إرسال الطلب بنجاح</span>
          <h2>شكراً لانضمامك لينا</h2>
          <p className="modal-copy">حنراجع بياناتك ومستنداتك، ونتواصل معاك عبر الهاتف لتأكيد الخطوات القادمة.</p>
          <div className="request-number">{reference}<span>رقم الطلب</span></div>
          <p className="provider-success-note">✓ لا يستطيع مقدم الخدمة استقبال طلبات قبل إكمال التحقق من الهوية والمؤهل والترخيص المهني.</p>
          <Button variant="primary" className="full" onClick={onClose}>العودة للرئيسية ←</Button>
        </div>
      </div>
    )}</Localized>;
  }

  const professions = [
    ["doctor", "✚", "طبيب/ة"],
    ["lab", "✦", "فني/ة مختبرات"],
    ["physio", "◒", "علاج طبيعي"],
    ["nurse", "♡", "ممرض/ة"],
  ] as const;

  return <Localized>{(
    <div className="modal-backdrop" role="dialog" aria-modal="true" aria-label="انضمام مقدم خدمة">
      <form className="booking-modal provider-modal" onSubmit={submit}>
        <button type="button" className="close-button" onClick={onClose} aria-label="إغلاق">×</button>
        <div className="modal-heading"><div><span className="eyebrow">بوابة مقدمي الخدمات</span><h2>سجّل مهنتك، وخلي خبرتك أقرب للناس</h2><p className="modal-copy">أنت تحدد الأيام والساعات المتاح فيها، ونحن نراجع بياناتك قبل تفعيل استقبال الطلبات.</p></div></div>
        <div className="verification-steps"><span><b>1</b> ارفع مستنداتك</span><span><b>2</b> نراجع بياناتك</span><span><b>3</b> ابدأ استقبال الطلبات</span></div>

        <section className="modal-section">
          <h3>اختار مهنتك</h3>
          <input type="hidden" name="profession" value={profession} />
          <div className="profession-grid">
            {professions.map(([id, icon, label]) => (
              <button type="button" key={id} className={profession === id ? "profession-card selected" : "profession-card"} onClick={() => setProfession(id)}>
                <span>{icon}</span><strong>{label}</strong>{profession === id && <b>✓</b>}
              </button>
            ))}
          </div>
        </section>

        <section className="modal-section provider-fields">
          <h3>بياناتك الأساسية</h3>
          <div className="two-fields"><label>الاسم الكامل<input name="fullName" required placeholder="كما في الهوية" /></label><label>رقم الهاتف<input name="phone" required placeholder="09X XXX XXXX" /></label></div>
          <div className="two-fields"><label>التخصص<input name="specialty" placeholder="مثال: طب أطفال" /></label><label>سنوات الخبرة<select name="experience"><option>اختر</option><option>أقل من سنة</option><option>1 – 3 سنوات</option><option>4 – 7 سنوات</option><option>أكثر من 7 سنوات</option></select></label></div>
          <div className="two-fields"><label>المدينة<select name="city"><option>ود مدني</option><option>الخرطوم</option><option>سنار</option></select></label><label>مكان العمل الحالي<input name="workplace" placeholder="اسم المستشفى أو المركز" /></label></div>
          <label>الأوقات المتاحة<input name="availability" placeholder="مثال: السبت والثلاثاء، 5 – 10 مساءً" /></label>
        </section>

        <section className="modal-section">
          <h3>مستندات التحقق</h3>
          <p className="form-helper">الهوية والمؤهل مطلوبان. الحد الأقصى 5 ميغابايت لكل ملف، PDF أو صورة.</p>
          <div className="document-grid">
            <label className="document-upload">↑ <span><strong>السيرة الذاتية</strong><small>PDF أو صورة</small></span><input name="cv" type="file" accept=".pdf,image/*" /></label>
            <label className="document-upload">↑ <span><strong>الشهادة *</strong><small>المؤهل الأكاديمي</small></span><input name="qualification" required type="file" accept=".pdf,image/*" /></label>
            <label className="document-upload">↑ <span><strong>الترخيص المهني</strong><small>إن وجد</small></span><input name="license" type="file" accept=".pdf,image/*" /></label>
            <label className="document-upload">↑ <span><strong>بطاقة الهوية *</strong><small>صورة واضحة</small></span><input name="identity" required type="file" accept=".pdf,image/*" /></label>
          </div>
        </section>

        <p className="privacy-note">✓ نستخدم بياناتك للتحقق والتواصل معك فقط، ولا نعرض معلوماتك الحساسة للمرضى.</p>
        {error && <p className="form-error">{error}</p>}
        <div className="modal-actions"><Button variant="secondary" onClick={onClose}>إلغاء</Button><Button variant="primary" disabled={submitting}>{submitting ? "جاري الإرسال..." : "إرسال طلب الانضمام ←"}</Button></div>
      </form>
    </div>
  )}</Localized>;
}

export default function Home() {
  const [bookingOpen, setBookingOpen] = useState(false);
  const [initialService, setInitialService] = useState<ServiceId | undefined>();
  const [providerOpen, setProviderOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  function openBooking(service?: ServiceId) {
    if (service === "doctor") {
      window.location.assign("/doctors");
      return;
    }
    setInitialService(service);
    setBookingOpen(true);
  }

  function closeMenu() {
    setMenuOpen(false);
  }

  return <Localized>{(
    <main dir="rtl">
      <div className="top-strip">
        <div className="site-container top-strip-inner">
          <span><b>✓</b> سلامتك أولويتنا — مقدمو خدمات يتم التحقق منهم</span>
          <span>⌖ نبدأ من ود مدني</span>
        </div>
      </div>

      <header className="site-header">
        <div className="site-container header-inner">
          <a className="brand" href="#home" aria-label="صحتك - الرئيسية">
            <BrandMark placement="header" />
            <span className="brand-tagline"><strong>صحتك</strong><small>الرعاية لحد باب بيتك</small></span>
          </a>
          <nav className={menuOpen ? "public-nav open" : "public-nav"} aria-label="التنقل الرئيسي">
            <a href="#services" onClick={closeMenu}>الخدمات</a>
            <a href="#how" onClick={closeMenu}>كيف تعمل المنصة</a>
            <a href="#providers" onClick={closeMenu}>لمقدمي الخدمات</a>
            <a href="/doctors" onClick={closeMenu}>حجز موعد طبيب</a>
            <a href="#about" onClick={closeMenu}>عن المنصة</a>
            <a href="#faq" onClick={closeMenu}>الأسئلة الشائعة</a>
          </nav>
          <div className="header-tools"><LanguageSwitch placement="header" /><button className="public-menu-button" type="button" aria-label="القائمة" aria-expanded={menuOpen} onClick={() => setMenuOpen((current) => !current)}><span /><span /><span /></button></div>
          <button className="header-cta" onClick={() => openBooking()}>اطلب خدمة <span>←</span></button>
        </div>
      </header>

      <section className="hero-section" id="home">
        <div className="hero-texture" aria-hidden="true" />
        <div className="site-container hero-grid">
          <div className="hero-copy">
            <span className="eyebrow-pill"><i /> خدمة منزلية تبدأ من ود مدني</span>
            <h1>الرعاية الصحية...<br /><em>لحد باب بيتك</em></h1>
            <p>اطلب خدمات صحية منزلية من مقدمي خدمات موثّقين، وحدد الوقت والمكان المناسب ليك.</p>
            <div className="hero-actions">
              <Button variant="primary" onClick={() => openBooking()}>اطلب خدمة الآن <span>←</span></Button>
              <a href="#services" className="text-link">استكشف الخدمات <span>↖</span></a>
            </div>
            <div className="hero-note">
              <div className="avatar-stack"><span>م</span><span>أ</span><span>س</span></div>
              <p>معاك خطوة بخطوة<strong>دعم ومتابعة حتى تكتمل خدمتك</strong></p>
            </div>
          </div>

          <div className="hero-visual">
            <img src="/sanad-identity.webp" alt="سَند، مساعد منصة صحتك، يقدم شعار المنصة" />
            <aside className="float-card float-top">
              <b>◷</b>
              <span><strong>الوقت المناسب ليك</strong><small>اختار الموعد براحتك</small></span>
            </aside>
            <aside className="float-card float-bottom">
              <b>✓</b>
              <span><strong>سلامتك أولويتنا</strong><small>بياناتك في أمان</small></span>
            </aside>
          </div>
        </div>
      </section>

      <section className="trust-bar" aria-label="مزايا المنصة">
        <div className="site-container trust-grid">
          {["مقدمون موثّقون", "اختيار الوقت المناسب", "متابعة الطلب", "أسعار واضحة قبل التأكيد", "دعم ومتابعة"].map((item) => (
            <span key={item}><b><CheckIcon /></b>{item}</span>
          ))}
        </div>
      </section>

      <section className="section services-section" id="services">
        <div className="site-container">
          <div className="section-heading split-heading">
            <div><span className="eyebrow">خدماتنا الحالية</span><h2>خدمة صحية، <em>بشكل أبسط</em></h2></div>
            <p>اختر خدمة منزلية، أو افتح دليل الأطباء لحجز يوم حضور في العيادة.</p>
          </div>
          <div className="service-grid">
            {services.map((service, index) => (
              <button
                className={`service-card ${service.tone}`}
                key={service.id}
                onClick={() => openBooking(service.id as ServiceId)}
              >
                <div className="service-card-top">
                  <span className={`service-icon ${service.tone}`}>{service.icon}</span>
                </div>
                <span className="service-index">0{index + 1}</span>
                <h3>{service.title}</h3>
                <p>{service.description}</p>
                <span className="service-cta">{service.cta} <span>←</span></span>
              </button>
            ))}
          </div>
        </div>
      </section>

      <section className="trust-section" id="about">
        <div className="site-container trust-layout">
          <div className="trust-visual">
            <div className="shield-orbit">
              <BrandMark />
              <span>تحقق واضح<br /><strong>قبل الخدمة</strong></span>
            </div>
            <div className="document-card">
              <span>✓</span><p><strong>الهوية</strong><small>مطابقة البيانات</small></p>
              <span>✓</span><p><strong>المؤهل</strong><small>مراجعة الشهادة</small></p>
              <span>✓</span><p><strong>الترخيص</strong><small>التسجيل المهني</small></p>
            </div>
          </div>
          <div className="trust-copy">
            <span className="eyebrow light">نخليها واضحة من البداية</span>
            <h2>سلامتك <em>أولويتنا</em></h2>
            <p>يتم التحقق من بيانات ووثائق مقدمي الخدمات قبل تفعيل حساباتهم. وأنت تعرف السعر، الموعد، ومقدم الخدمة قبل تأكيد طلبك.</p>
            <div className="trust-points">
              <article><b>✓</b><span><strong>تحققنا من مقدم الخدمة</strong><small>هوية، مؤهل، وترخيص مهني</small></span></article>
              <article><b>♡</b><span><strong>متابعة بعد الطلب</strong><small>نحن معاك حتى تكتمل الخدمة</small></span></article>
              <article><b>⌂</b><span><strong>خصوصيتك محفوظة</strong><small>بياناتك لا تُشارك إلا عند الحاجة</small></span></article>
            </div>
          </div>
        </div>
      </section>

      <section className="section how-section" id="how">
        <div className="site-container">
          <div className="section-heading centered">
            <span className="eyebrow">كيف تعمل المنصة؟</span>
            <h2>من الطلب <em>لحد باب البيت</em></h2>
            <p>أربع خطوات بسيطة، بدون لف ودوران.</p>
          </div>
          <div className="how-grid">
            {[
              ["✦", "اختر الخدمة", "قول لينا محتاج شنو"],
              ["⌖", "حدد موقعك وموعدك", "اختار المكان والوقت المناسب"],
              ["✓", "أكد الطلب والدفع", "راجع التفاصيل قبل التأكيد"],
              ["⌂", "مقدم الخدمة يجيك", "ونتابع معاك لحد النهاية"],
            ].map(([icon, title, copy], index) => (
              <article className="how-step" key={title}>
                <div className="how-icon"><span>{icon}</span><b>0{index + 1}</b></div>
                <h3>{title}</h3><p>{copy}</p>
              </article>
            ))}
          </div>
        </div>
      </section>

      <section className="provider-section" id="providers">
        <div className="site-container provider-layout">
          <div className="provider-copy">
            <span className="eyebrow light">للمهنيين الصحيين</span>
            <h2>اشتغل معانا،<br /><em>بوقتك وخبرتك</em></h2>
            <p>أنت تحدد الأيام والساعات المتاح فيها. نقدم ليك طريقة مرنة لتقديم خدمات صحية إضافية لأهل مدينتك.</p>
            <div className="eligible-list">
              <span>✚ أطباء للحجز بالعيادة</span><span>♡ تمريض</span><span>✦ فنيو مختبر</span><span>◒ علاج طبيعي</span>
            </div>
            <Button variant="light" onClick={() => setProviderOpen(true)}>إرسال طلب الانضمام <span>←</span></Button>
          </div>
          <div className="provider-visual" id="provider-steps">
            <span className="provider-kicker">طلب انضمام مقدم خدمة</span>
            <ol>
              <li><b>1</b><span><strong>ارفع مستنداتك</strong><small>الهوية، المؤهل، والترخيص</small></span></li>
              <li><b>2</b><span><strong>نراجع بياناتك</strong><small>فريق التحقق يتأكد من المعلومات</small></span></li>
              <li><b>3</b><span><strong>ابدأ استقبال الطلبات</strong><small>بعد اكتمال التحقق فقط</small></span></li>
            </ol>
            <div className="provider-status"><span>✓</span><p><strong>مافي مقدم خدمة بيظهر قبل التحقق</strong><small>الثقة جزء من طريقة عمل المنصة</small></p></div>
          </div>
        </div>
      </section>

      <section className="section faq-section" id="faq">
        <div className="site-container faq-layout">
          <div><span className="eyebrow">أسئلة سريعة</span><h2>قبل ما <em>تطلب</em></h2><p>إجابات واضحة عن التغطية، التحقق، المواعيد، الدفع، وخصوصية بياناتك.</p></div>
          <div className="faq-list">
            <details><summary>هل الخدمة متوفرة خارج ود مدني؟</summary><p>الخدمة متوفرة حالياً داخل ود مدني خلال الفترة التجريبية، والتوسع لبقية المدن سيتم بصورة تدريجية.</p></details>
            <details><summary>كيف أتأكد من مقدم الخدمة؟</summary><p>تراجع منصة صحتك الهوية والمؤهل والترخيص المهني قبل السماح لمقدم الخدمة باستقبال الطلبات.</p></details>
            <details><summary>هل أقدر أختار الوقت بنفسي؟</summary><p>في الخدمات المنزلية تختار الفترة المناسبة عند إرسال الطلب. أما الأطباء فتختار يوم الحضور من جدول العيادة، والدخول بأسبقية الحضور.</p></details>
            <details><summary>ما هي طرق الدفع المتاحة؟</summary><p>الدفع يتم إلى الحسابات البنكية الخاصة بمنصة صحتك عبر بنكك أو التطبيقات البنكية الأخرى، ثم ترفع صورة إشعار التحويل عند طلبها.</p></details>
            <details><summary>هل يتأكد الموعد مباشرة بعد إرسال الطلب؟</summary><p>لا. تتم مراجعة الطلب وإشعار التحويل أولاً، ثم يظهر التأكيد بعد قبول الطلب واستكمال الخطوات.</p></details>
            <details><summary>من يرى بياناتي وإشعار التحويل؟</summary><p>تُستخدم البيانات لمعالجة طلبك ومراجعة الدفع، ولا يصل إليها إلا الأشخاص المصرح لهم بإدارة الطلب.</p></details>
          </div>
        </div>
      </section>

      <section className="safety-note">
        <div className="site-container safety-inner">
          <b>!</b>
          <div><strong>هذه المنصة ليست بديلاً لخدمات الطوارئ.</strong><p>في حالة النزيف الشديد، صعوبة التنفس، فقدان الوعي، أو ألم الصدر الشديد، توجه لأقرب قسم طوارئ أو اتصل بخدمات الطوارئ المتاحة في منطقتك.</p></div>
        </div>
      </section>

      <footer>
        <div className="site-container footer-grid">
          <a className="brand footer-brand" href="#home"><BrandMark placement="footer" /><span><strong>صحتك</strong><small>الرعاية لحد باب بيتك</small></span></a>
          <div><strong>روابط سريعة</strong><a href="#services">الخدمات</a><a href="/doctors">دليل الأطباء</a><a href="#providers">لمقدمي الخدمات</a></div>
          <div><strong>نطاق التجربة</strong><span>ود مدني، السودان</span><span>نسخة MVP تجريبية</span><a href="/admin/login">دخول الإدارة</a></div>
        </div>
        <div className="site-container footer-bottom"><span>© 2026 صحتك</span><span>صُنعت لأهل السودان، من ود مدني</span></div>
      </footer>
      {bookingOpen && <BookingModal initialService={initialService} onClose={() => setBookingOpen(false)} />}
      {providerOpen && <ProviderRegistrationModal onClose={() => setProviderOpen(false)} />}
      <SanadAssistant onService={(service) => openBooking(service)} />
    </main>
  )}</Localized>;
}
