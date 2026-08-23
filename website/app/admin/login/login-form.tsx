"use client";

import { FormEvent, useState } from "react";
import { Localized } from "../../language";

export default function LoginForm({ returnTo }: { returnTo: string }) {
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      const response = await fetch("/api/admin/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      const data = await response.json() as { error?: string };
      if (!response.ok) throw new Error(data.error || "تعذر تسجيل الدخول.");
      window.location.assign(returnTo);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "تعذر تسجيل الدخول.");
    } finally {
      setSubmitting(false);
    }
  }

  async function clearDeviceSession() {
    await fetch("/api/admin/logout", { method: "POST" });
    setPassword("");
    setError("");
  }

  return <Localized>{(
    <form className="admin-login-form" onSubmit={submit}>
      <label>
        <span>اسم المستخدم</span>
        <input
          autoComplete="username"
          autoFocus
          maxLength={80}
          onChange={(event) => setUsername(event.target.value)}
          required
          type="text"
          value={username}
        />
      </label>
      <label>
        <span>كلمة المرور</span>
        <input
          autoComplete="current-password"
          maxLength={200}
          onChange={(event) => setPassword(event.target.value)}
          required
          type="password"
          value={password}
        />
      </label>
      {error && <div className="admin-login-error" role="alert">{error}</div>}
      <button disabled={submitting} type="submit">
        {submitting ? "جاري التحقق..." : "دخول لوحة الإدارة"}
      </button>
      <button className="admin-clear-session" type="button" onClick={() => void clearDeviceSession()}>مسح جلسة هذا الجهاز</button>
    </form>
  )}</Localized>;
}
