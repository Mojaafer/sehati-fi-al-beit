import { lt } from "drizzle-orm";
import {
  ADMIN_SESSION_COOKIE,
  ADMIN_SESSION_MAX_AGE,
  createAdminSessionValue,
  verifyAdminCredentials,
} from "../../../admin-auth";
import { getDb } from "../../../../db";
import { adminSecurity } from "../../../../db/schema";
import { clientRateLimitKey } from "../../../../lib/rate-limit";

/**
 * The lockout below is only as good as this key, so it must not be attacker-chosen. See
 * `clientRateLimitKey` for why the edge header wins and why the *last* `x-forwarded-for` hop is
 * the one that gets read.
 *
 * Requests that yield no usable address share a single bucket rather than getting a free pass,
 * which also means a real admin behind Cloudflare can never be locked out by someone else.
 */
function getRateLimitKey(request: Request): string {
  return `admin-login:${clientRateLimitKey(request)}`;
}

const MAX_ATTEMPTS = 5;
const LOCKOUT_MINUTES = 15;
/** A row is only meaningful for one lockout window; older ones would accumulate forever. */
const ROW_RETENTION_HOURS = 24;

export async function POST(request: Request) {
  try {
    if (!request.headers.get("content-type")?.includes("application/json")) {
      return Response.json({ error: "الطلب غير صالح." }, { status: 415 });
    }

    const payload = await request.json() as { username?: unknown; password?: unknown };
    const username = typeof payload.username === "string" ? payload.username.trim().slice(0, 80) : "";
    const password = typeof payload.password === "string" ? payload.password.slice(0, 200) : "";

    const securityKey = getRateLimitKey(request);

    const db = getDb();
    const security = await db.query.adminSecurity.findFirst({
      where: (table, { eq }) => eq(table.key, securityKey),
    });
    if (security?.lockedUntil && new Date(security.lockedUntil).getTime() > Date.now()) {
      return Response.json(
        { error: "تم إيقاف الدخول مؤقتاً بعد محاولات متكررة. حاول بعد 15 دقيقة." },
        { status: 429, headers: { "Cache-Control": "no-store" } },
      );
    }

    if (!(await verifyAdminCredentials(username, password))) {
      const failedAttempts = (security?.failedLoginAttempts ?? 0) + 1;
      const lockedUntil = failedAttempts >= MAX_ATTEMPTS
        ? new Date(Date.now() + LOCKOUT_MINUTES * 60 * 1000).toISOString()
        : null;
      await db.insert(adminSecurity).values({
        key: securityKey,
        failedLoginAttempts: failedAttempts,
        lockedUntil,
        updatedAt: new Date().toISOString(),
      }).onConflictDoUpdate({
        target: adminSecurity.key,
        set: { failedLoginAttempts: failedAttempts, lockedUntil, updatedAt: new Date().toISOString() },
      });
      return Response.json(
        { error: lockedUntil ? "تم إيقاف الدخول مؤقتاً بعد محاولات متكررة. حاول بعد 15 دقيقة." : "بيانات الدخول غير صحيحة." },
        { status: lockedUntil ? 429 : 401, headers: { "Cache-Control": "no-store" } },
      );
    }

    await db.insert(adminSecurity).values({
      key: securityKey,
      failedLoginAttempts: 0,
      lockedUntil: null,
      updatedAt: new Date().toISOString(),
    }).onConflictDoUpdate({
      target: adminSecurity.key,
      set: { failedLoginAttempts: 0, lockedUntil: null, updatedAt: new Date().toISOString() },
    });

    // One row per source address adds up, and a row outlives its usefulness the moment its
    // lockout window closes. Pruning on the (rare) successful login keeps this off the hot path.
    try {
      const cutoff = new Date(Date.now() - ROW_RETENTION_HOURS * 60 * 60 * 1000).toISOString();
      await db.delete(adminSecurity).where(lt(adminSecurity.updatedAt, cutoff));
    } catch (pruneError) {
      console.error("admin_security prune failed", pruneError);
    }

    const sessionValue = await createAdminSessionValue();
    return Response.json(
      { success: true },
      {
        headers: {
          "Cache-Control": "no-store",
          "Set-Cookie": `${ADMIN_SESSION_COOKIE}=${sessionValue}; Path=/; Max-Age=${ADMIN_SESSION_MAX_AGE}; HttpOnly; Secure; SameSite=Lax`,
        },
      },
    );
  } catch (error) {
    console.error("admin login failed", error);
    return Response.json(
      { error: "تعذر تسجيل الدخول الآن. حاول مرة أخرى." },
      { status: 500, headers: { "Cache-Control": "no-store" } },
    );
  }
}
