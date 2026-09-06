import type { NextConfig } from "next";

/**
 * Response headers. The site loads no third-party scripts, styles, fonts or images — the only
 * external links are `wa.me` navigations — so `'self'` is enough for every fetch directive.
 *
 * `script-src` still needs `'unsafe-inline'`: the App Router streams its RSC payload through
 * inline `<script>` tags and Next does not emit a nonce for them by default. The directives that
 * do the real work here are `frame-ancestors`, `object-src`, `base-uri` and `form-action`, which
 * cost nothing and shut down clickjacking of the admin panel plus base-tag and form hijacking.
 */
const contentSecurityPolicy = [
  "default-src 'self'",
  "script-src 'self' 'unsafe-inline'",
  "style-src 'self' 'unsafe-inline'",
  "img-src 'self' data: blob:",
  "font-src 'self' data:",
  "connect-src 'self'",
  "form-action 'self'",
  "base-uri 'self'",
  "frame-ancestors 'none'",
  "object-src 'none'",
].join("; ");

const nextConfig: NextConfig = {
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "Content-Security-Policy", value: contentSecurityPolicy },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          // No feature on this site asks for a camera, a microphone or a location.
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
          // Cloudflare terminates TLS in front of the worker; the site is HTTPS-only.
          { key: "Strict-Transport-Security", value: "max-age=31536000; includeSubDomains" },
        ],
      },
    ];
  },
};

export default nextConfig;
