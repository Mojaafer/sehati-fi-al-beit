import { redirect } from "next/navigation";
import Link from "next/link";
import { hasAdminSession, safeAdminReturnPath } from "../../admin-auth";
import LoginForm from "./login-form";
import { Localized } from "../../language";

export const dynamic = "force-dynamic";

export default async function AdminLoginPage({
  searchParams,
}: {
  searchParams: Promise<{ return_to?: string }>;
}) {
  const params = await searchParams;
  const returnTo = safeAdminReturnPath(params.return_to);
  if (await hasAdminSession()) redirect(returnTo);

  return <Localized>{(
    <main className="admin-login-page" dir="rtl">
      <section className="admin-login-card">
        <Link href="/" className="admin-login-brand" aria-label="العودة إلى منصة صحتك">
          <img src="/sehatak-logo.webp" alt="صحتك — Sehatak" />
          <small>الرعاية الصحية لحد باب بيتك</small>
        </Link>
        <div className="admin-login-icon" aria-hidden="true">🔐</div>
        <span className="eyebrow">دخول آمن</span>
        <h1>لوحة إدارة مقدمي الخدمة</h1>
        <p>أدخل اسم المستخدم وكلمة المرور المخصصة للإدارة.</p>
        <LoginForm returnTo={returnTo} />
        <small className="admin-login-note">الجلسة مشفّرة وتنتهي تلقائياً بعد 8 ساعات.</small>
      </section>
    </main>
  )}</Localized>;
}
