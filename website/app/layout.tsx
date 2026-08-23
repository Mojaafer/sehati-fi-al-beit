import type { Metadata } from "next";
import "./globals.css";
import { LanguageProvider, LanguageSwitch, Localized } from "./language";

export const metadata: Metadata = {
  metadataBase: new URL("https://sehatak-home-health.vercel.app"),
  title: "منصة صحتك",
  description: "منصة تربطك بخدمات الرعاية الصحية المنزلية الموثوقة في السودان.",
  openGraph: {
    title: "منصة صحتك",
    description: "الرعاية الصحية لحد باب بيتك",
    url: "https://sehatak-home-health.vercel.app",
    siteName: "منصة صحتك",
    locale: "ar_SD",
    type: "website",
    images: [{
      url: "/og.png",
      width: 1200,
      height: 630,
      alt: "منصة صحتك — الرعاية الصحية لحد باب بيتك",
    }],
  },
  twitter: {
    card: "summary_large_image",
    title: "منصة صحتك",
    description: "الرعاية الصحية لحد باب بيتك",
    images: ["/og.png"],
  },
  icons: {
    icon: "/favicon.svg",
    shortcut: "/favicon.svg",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ar" dir="rtl">
      <body><LanguageProvider><Localized>{children}</Localized><LanguageSwitch /></LanguageProvider></body>
    </html>
  );
}
