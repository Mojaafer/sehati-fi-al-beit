import type { Metadata } from "next";
import DoctorsClient from "./doctors-client";

export const metadata: Metadata = {
  title: "دليل الأطباء | منصة صحتك",
  description: "اختر طبيباً معتمداً وأرسل طلب الحضور للعيادة عبر منصة صحتك.",
  openGraph: { title: "دليل الأطباء | منصة صحتك", description: "حجز موعد حضور في العيادة مع أطباء معتمدين." },
  twitter: { title: "دليل الأطباء | منصة صحتك", description: "حجز موعد حضور في العيادة مع أطباء معتمدين." },
};

export default function DoctorsPage() {
  return <DoctorsClient />;
}
