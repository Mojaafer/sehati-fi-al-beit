import type { Metadata } from "next";
import { IBM_Plex_Sans_Arabic } from "next/font/google";
import "./globals.css";
import { LanguageProvider, LanguageSwitch, Localized } from "./language";

/**
 * Arabic-first typeface. Plex Sans Arabic carries the headings and UI text;
 * the system stack underneath covers anything it doesn't (emoji, symbols).
 * Loaded with display=swap so text never blocks on the font download.
 */
const arabicSans = IBM_Plex_Sans_Arabic({
  subsets: ["arabic", "latin"],
  weight: ["400", "500", "600", "700"],
  display: "swap",
  variable: "--font-sans-ar",
});

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
    <html lang="ar" dir="rtl" className={arabicSans.variable}>
      <body><LanguageProvider><Localized>{children}</Localized><LanguageSwitch /></LanguageProvider></body>
    </html>
  );
}
