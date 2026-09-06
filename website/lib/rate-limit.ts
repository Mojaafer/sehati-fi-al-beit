import { lt, sql } from "drizzle-orm";
import { getDb } from "../db";
import { adminSecurity } from "../db/schema";

/**
 * How long a counter row lives before the opportunistic sweep below is allowed to drop it.
 * Comfortably longer than any window used by a caller.
 */
const ROW_RETENTION_HOURS = 24;

/** Chance that a given call also prunes expired rows, so the table cannot grow without bound. */
const SWEEP_PROBABILITY = 0.02;

/**
 * Identifies the caller for rate-limiting purposes.
 *
 * `cf-connecting-ip` is set by Cloudflare itself and cannot be spoofed by the client, so it wins.
 * `x-forwarded-for` is a client-appendable list, and the *first* entry is whatever the caller
 * chose to send — reading it would hand out a fresh bucket per request. The last hop is the one
 * the nearest trusted proxy wrote, so that is what gets used when there is no edge header.
 *
 * A request with neither header shares a single bucket rather than getting a free pass.
 */
export function clientRateLimitKey(request: Request): string {
  const edgeIp = request.headers.get("cf-connecting-ip")?.trim();
  if (edgeIp) return edgeIp.slice(0, 45);

  const hops = request.headers.get("x-forwarded-for")?.split(",") ?? [];
  const lastHop = hops[hops.length - 1]?.trim();
  if (lastHop) return lastHop.slice(0, 45);

  return "unknown";
}

export type RateLimitResult = { allowed: boolean; retryAfterSeconds: number };

/**
 * Fixed-window counter, one row per (bucket, caller, window) in `admin_security`.
 *
 * Reusing that table keeps this migration-free; the keys are namespaced with `rl:` so they cannot
 * collide with the login-lockout rows or the `session:` rows. `failed_login_attempts` doubles as
 * the hit counter.
 *
 * **Fails open.** These endpoints are how a patient books a nurse or a lab visit, so a database
 * hiccup must not turn into a booking outage. The limit exists to blunt scripted abuse, and an
 * attacker cannot induce the failure themselves.
 */
export async function checkRateLimit(
  bucket: string,
  request: Request,
  limit: number,
  windowSeconds: number,
): Promise<RateLimitResult> {
  const windowMs = windowSeconds * 1000;
  const windowIndex = Math.floor(Date.now() / windowMs);
  const key = `rl:${bucket}:${clientRateLimitKey(request)}:${windowIndex}`;
  const retryAfterSeconds = Math.max(1, Math.ceil(((windowIndex + 1) * windowMs - Date.now()) / 1000));

  try {
    const db = getDb();
    const now = new Date().toISOString();
    const [row] = await db.insert(adminSecurity)
      .values({ key, failedLoginAttempts: 1, updatedAt: now })
      .onConflictDoUpdate({
        target: adminSecurity.key,
        set: { failedLoginAttempts: sql`${adminSecurity.failedLoginAttempts} + 1`, updatedAt: now },
      })
      .returning({ hits: adminSecurity.failedLoginAttempts });

    if (Math.random() < SWEEP_PROBABILITY) await sweepExpiredRows(db);

    return { allowed: (row?.hits ?? 1) <= limit, retryAfterSeconds };
  } catch (error) {
    console.error("rate limit check failed, allowing request", bucket, error);
    return { allowed: true, retryAfterSeconds };
  }
}

/** 429 with the Arabic message the public pages already render from `error`. */
export function tooManyRequests(retryAfterSeconds: number): Response {
  return Response.json(
    { error: "عدد المحاولات كبير، انتظر قليلاً ثم حاول مرة أخرى." },
    { status: 429, headers: { "Retry-After": String(retryAfterSeconds) } },
  );
}

async function sweepExpiredRows(db: ReturnType<typeof getDb>): Promise<void> {
  try {
    const cutoff = new Date(Date.now() - ROW_RETENTION_HOURS * 60 * 60 * 1000).toISOString();
    await db.delete(adminSecurity).where(lt(adminSecurity.updatedAt, cutoff));
  } catch (error) {
    console.error("failed to sweep expired rate limit rows", error);
  }
}
