"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { Localized, useLanguage } from "../language";

type Notification = {
  id: number;
  title: string;
  body: string;
  isRead: number;
  createdAt: string;
};

const links = [
  ["/admin/doctors", "الأطباء"],
  ["/admin/bookings", "الحجوزات والمدفوعات"],
  ["/admin/providers", "مقدمو الخدمة"],
  ["/admin/service-requests", "الخدمات المنزلية"],
] as const;

export default function AdminNav() {
  const { direction, locale } = useLanguage();
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const loadNotifications = useCallback(async () => {
    const response = await fetch("/api/admin/notifications", { cache: "no-store" });
    if (!response.ok) return;
    const data = await response.json() as { notifications?: Notification[]; unreadCount?: number };
    setNotifications(data.notifications || []);
    setUnreadCount(data.unreadCount || 0);
  }, []);

  useEffect(() => {
    const initialId = window.setTimeout(() => void loadNotifications(), 0);
    const intervalId = window.setInterval(() => void loadNotifications(), 15000);
    return () => { window.clearTimeout(initialId); window.clearInterval(intervalId); };
  }, [loadNotifications]);

  async function markRead(id?: number) {
    await fetch("/api/admin/notifications", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(id ? { id } : { all: true }),
    });
    await loadNotifications();
  }

  async function logout() {
    await fetch("/api/admin/logout", { method: "POST" });
    window.location.assign("/admin/login");
  }

  return (
    <Localized><nav className="admin-nav" dir={direction}>
      <div className="admin-nav-inner">
        <Link className="admin-nav-brand" href="/" aria-label="منصة صحتك"><img src="/sehatak-logo.webp" alt="" /><span>صحتك <small>لوحة الإدارة</small></span></Link>
        <button className="admin-nav-menu" onClick={() => setMenuOpen((current) => !current)} aria-expanded={menuOpen}>القائمة</button>
        <div className={menuOpen ? "admin-nav-links open" : "admin-nav-links"}>
          {links.map(([href, label]) => (
            <Link key={href} href={href} className={pathname === href ? "active" : ""} onClick={() => setMenuOpen(false)}>{label}</Link>
          ))}
        </div>
        <div className="admin-nav-actions">
          <button className="admin-notification-button" onClick={() => setNotificationsOpen((current) => !current)} aria-label="إشعارات الإدارة">
            🔔 {unreadCount > 0 && <b>{unreadCount > 99 ? "99+" : unreadCount}</b>}
          </button>
          <button className="admin-nav-logout" onClick={() => void logout()}>تسجيل الخروج</button>
        </div>
        {notificationsOpen && (
          <section className="admin-notification-panel">
            <header><strong>إشعارات الإدارة</strong><button disabled={!unreadCount} onClick={() => void markRead()}>تحديد الكل كمقروء</button></header>
            <div>
              {notifications.length ? notifications.map((notification) => (
                <Link
                  href="/admin/service-requests"
                  key={notification.id}
                  className={notification.isRead ? "read" : ""}
                  onClick={() => { setNotificationsOpen(false); if (!notification.isRead) void markRead(notification.id); }}
                >
                  <strong>{notification.title}</strong>
                  <span>{notification.body}</span>
                   <small>{new Date(notification.createdAt).toLocaleString(locale)}</small>
                </Link>
              )) : <p>لا توجد إشعارات حالياً.</p>}
            </div>
          </section>
        )}
      </div>
    </nav></Localized>
  );
}
