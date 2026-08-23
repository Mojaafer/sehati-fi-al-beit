"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import { Localized } from "./language";

type ServiceId = "lab" | "nurse" | "physio";
type Reply = { text: string; action?: { kind: "service"; id: ServiceId; label: string } | { kind: "doctor"; label: string } };
type Message = { role: "user" | "assistant"; text: string; action?: Reply["action"] };

function replyTo(value: string): Reply {
  const text = value.trim().toLowerCase();
  if (/طوارئ|نزيف شديد|صعوبة تنفس|فقدان وعي|ألم صدر شديد|جلطة|emergency|severe bleeding|breathing|unconscious|chest pain|stroke/.test(text)) {
    return { text: "دي ممكن تكون حالة طارئة. توجّه فوراً لأقرب قسم طوارئ أو اتصل بخدمة الطوارئ المتاحة في منطقتك." };
  }
  if (/خارج ود مدني|التغطية|الولايات|المدن|outside wad madani|coverage|cities|states/.test(text)) {
    return { text: "الخدمة متوفرة حالياً داخل ود مدني في الفترة التجريبية، والتوسع للمدن والولايات الأخرى حيكون بصورة تدريجية." };
  }
  if (/خصوصية|بياناتي|إشعار التحويل|privacy|my data|transfer receipt/.test(text)) {
    return { text: "بياناتك وإشعار التحويل تُستخدم لمعالجة الطلب ومراجعة الدفع، ولا يصل إليها إلا فريق الإدارة المصرح له." };
  }
  if (/دفع|بنكك|تحويل|حساب بنكي|payment|bank|transfer/.test(text)) {
    return { text: "الدفع يكون إلى حسابات منصة صحتك عبر بنكك أو التطبيقات البنكية الأخرى، ثم ترفع صورة إشعار التحويل عند طلبها." };
  }
  if (/طبيب|دكتور|كشف|عيادة|موعد طبي|doctor|clinic|appointment/.test(text)) {
    return { text: "حجز الطبيب يكون للحضور في العيادة، وليس زيارة منزلية. اختر الطبيب ويوم الحضور من الدليل، والدخول بأسبقية الحضور.", action: { kind: "doctor", label: "افتح دليل الأطباء" } };
  }
  if (/تحليل|عينة|دم|مختبر|lab|sample|blood|test/.test(text)) {
    return { text: "خدمة سحب العينات تتم في المنزل بواسطة فني مختبر، ثم تُرسل العينة للمعمل.", action: { kind: "service", id: "lab", label: "اطلب سحب العينات" } };
  }
  if (/علاج طبيعي|تأهيل|حركة|مشي|توازن|physio|rehab|mobility|walking|balance/.test(text)) {
    return { text: "أقدر أفتح ليك طلب العلاج الطبيعي المنزلي لتقييم الحركة ووضع خطة مناسبة.", action: { kind: "service", id: "physio", label: "اطلب العلاج الطبيعي" } };
  }
  if (/ممرض|تمريض|غيار جرح|قياس|رعاية منزلية|nurse|nursing|wound|home care/.test(text)) {
    return { text: "أقدر أفتح ليك طلب التمريض المنزلي. الخدمة لا تشمل الطوارئ أو تغيير جرعات الأدوية.", action: { kind: "service", id: "nurse", label: "اطلب التمريض المنزلي" } };
  }
  return { text: "أنا سَند، أساعدك تختار بين سحب العينات، التمريض المنزلي، العلاج الطبيعي، أو حجز موعد طبيب في العيادة. محتاج أي واحدة؟" };
}

export default function SanadAssistant({ onService }: { onService: (service: ServiceId) => void }) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState<Message[]>([
    { role: "assistant", text: "مرحباً، أنا سَند. قول لي محتاج خدمة شنو وأنا أوجّهك للمكان المناسب داخل صحتك." },
  ]);

  useEffect(() => {
    if (!open) return;
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") setOpen(false);
    }
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [open]);

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const value = input.trim();
    if (!value) return;
    const reply = replyTo(value);
    setMessages((current) => [...current, { role: "user", text: value }, { role: "assistant", text: reply.text, action: reply.action }]);
    setInput("");
  }

  return <Localized><>
    <button className="sanad-float" onClick={() => setOpen(true)} aria-label="فتح مساعد سند"><span><img src="/sanad-identity.webp" alt="" /></span><strong>اسأل سَند</strong><small>مساعدك من صحتك</small></button>
    {open && <div className="sanad-backdrop"><section className="sanad-modal" role="dialog" aria-modal="true" aria-label="محادثة سند" dir="rtl">
      <header><div className="sanad-avatar"><img src="/sanad-identity.webp" alt="" /></div><div><strong>سَند</strong><small>مساعد صحتك</small></div><button onClick={() => setOpen(false)} aria-label="إغلاق">×</button></header>
      <p className="sanad-safety">معلومات عن خدمات المنصة والحجز فقط، وليست تشخيصاً أو بديلاً عن الطبيب.</p>
      <div className="sanad-messages">{messages.map((message, index) => {
        const action = message.action;
        return <article className={message.role} key={`${message.role}-${index}`}><p>{message.text}</p>{action?.kind === "service" && <button onClick={() => { setOpen(false); onService(action.id); }}>{action.label}</button>}{action?.kind === "doctor" && <Link href="/doctors">{action.label}</Link>}</article>;
      })}</div>
      <div className="sanad-prompts"><button onClick={() => setInput("أريد حجز موعد مع طبيب")}>حجز طبيب</button><button onClick={() => setInput("أحتاج سحب عينة")}>سحب عينة</button><button onClick={() => setInput("أحتاج تمريض منزلي")}>تمريض منزلي</button></div>
      <form onSubmit={submit}><input value={input} onChange={(event) => setInput(event.target.value)} maxLength={500} placeholder="اكتب الخدمة المحتاجها..." /><button>إرسال</button></form>
    </section></div>}
  </></Localized>;
}
