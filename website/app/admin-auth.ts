import { eq } from "drizzle-orm";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getDb } from "../db";
import { adminSecurity } from "../db/schema";

export const ADMIN_SESSION_COOKIE = "sehatak_admin_session";
export const ADMIN_SESSION_MAX_AGE = 8 * 60 * 60;

const encoder = new TextEncoder();

type RuntimeEnv = {
  ADMIN_USERNAME?: string;
  ADMIN_PASSWORD_PBKDF2?: string;
  ADMIN_PASSWORD_SHA256?: string;
  ADMIN_SESSION_SECRET?: string;
};

function runtimeEnv(): RuntimeEnv {
  return {
    ADMIN_USERNAME: process.env.ADMIN_USERNAME,
    ADMIN_PASSWORD_PBKDF2: process.env.ADMIN_PASSWORD_PBKDF2,
    ADMIN_PASSWORD_SHA256: process.env.ADMIN_PASSWORD_SHA256,
    ADMIN_SESSION_SECRET: process.env.ADMIN_SESSION_SECRET,
  };
}

function bytesToHex(bytes: ArrayBuffer): string {
  return Array.from(new Uint8Array(bytes), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function constantTimeEqual(left: string, right: string): boolean {
  const leftBuf = encoder.encode(left);
  const rightBuf = encoder.encode(right);
  const length = Math.max(leftBuf.length, rightBuf.length);
  let difference = leftBuf.length ^ rightBuf.length;
  for (let index = 0; index < length; index += 1) {
    difference |= (leftBuf[index] ?? 0) ^ (rightBuf[index] ?? 0);
  }
  return difference === 0;
}

async function sha256(value: string): Promise<string> {
  return bytesToHex(await crypto.subtle.digest("SHA-256", encoder.encode(value)));
}

/**
 * Verifies a password against `pbkdf2$sha256$<iterations>$<salt-b64>$<hash-hex>`.
 *
 * The iteration count is stored inside the value, so raising it later is a matter of
 * regenerating the env var — no code change and no redeploy of this file. Generate one with
 * `node scripts/hash-admin-password.mjs`.
 */
async function verifyPbkdf2(password: string, stored: string): Promise<boolean> {
  const [scheme, hash, iterationText, saltB64, expectedHex] = stored.trim().split("$");
  if (scheme !== "pbkdf2" || hash !== "sha256" || !saltB64 || !expectedHex) return false;

  const iterations = Number(iterationText);
  if (!Number.isSafeInteger(iterations) || iterations < 1000) return false;

  let decodedSalt: string;
  try {
    decodedSalt = atob(saltB64);
  } catch {
    return false;
  }
  if (decodedSalt.length === 0) return false;

  // Built this way rather than with `Uint8Array.from` so the element type is `ArrayBuffer` and not
  // `ArrayBufferLike`, which WebCrypto's `BufferSource` will not accept.
  const salt = new Uint8Array(decodedSalt.length);
  for (let index = 0; index < decodedSalt.length; index += 1) {
    salt[index] = decodedSalt.charCodeAt(index);
  }

  const key = await crypto.subtle.importKey("raw", encoder.encode(password), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt, iterations, hash: "SHA-256" },
    key,
    256,
  );
  return constantTimeEqual(bytesToHex(bits), expectedHex.toLowerCase());
}

async function sign(value: string, secret: string): Promise<string> {
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  return bytesToHex(await crypto.subtle.sign("HMAC", key, encoder.encode(value)));
}

/**
 * Prefers `ADMIN_PASSWORD_PBKDF2`. Salted and stretched, so the stored value is useless for
 * rainbow-table or GPU lookup even if the environment leaks.
 *
 * `ADMIN_PASSWORD_SHA256` is a single unsalted round — a leaked value is crackable offline in
 * seconds. It is still honoured so that setting the new variable is a deliberate step rather than
 * a lockout, but it warns on every use. Remove it once the PBKDF2 value is in place.
 */
export async function verifyAdminCredentials(username: string, password: string): Promise<boolean> {
  const { ADMIN_USERNAME, ADMIN_PASSWORD_PBKDF2, ADMIN_PASSWORD_SHA256 } = runtimeEnv();
  if (!ADMIN_USERNAME || !username || !password) return false;

  // Always compare the username, even when the password check short-circuits, so the two
  // branches cost the same.
  const usernameMatches = constantTimeEqual(username, ADMIN_USERNAME);

  if (ADMIN_PASSWORD_PBKDF2) {
    return (await verifyPbkdf2(password, ADMIN_PASSWORD_PBKDF2)) && usernameMatches;
  }

  if (ADMIN_PASSWORD_SHA256) {
    console.warn(
      "ADMIN_PASSWORD_SHA256 is unsalted and deprecated; generate ADMIN_PASSWORD_PBKDF2 with scripts/hash-admin-password.mjs",
    );
    const passwordHash = await sha256(password);
    return usernameMatches && constantTimeEqual(passwordHash, ADMIN_PASSWORD_SHA256.toLowerCase());
  }

  return false;
}

export async function createAdminSessionValue(): Promise<string> {
  const { ADMIN_SESSION_SECRET } = runtimeEnv();
  if (!ADMIN_SESSION_SECRET) throw new Error("ADMIN_SESSION_SECRET is not configured");

  const sessionId = crypto.randomUUID();
  const expiresAt = Date.now() + ADMIN_SESSION_MAX_AGE * 1000;
  const payload = `v2.${sessionId}.${expiresAt}`;
  const signature = await sign(payload, ADMIN_SESSION_SECRET);

  try {
    const db = getDb();
    await db.insert(adminSecurity).values({
      key: `session:${sessionId}`,
      failedLoginAttempts: 0,
      lockedUntil: new Date(expiresAt).toISOString(),
      updatedAt: new Date().toISOString(),
    });
  } catch (error) {
    console.error("failed to persist admin session in db", error);
  }

  return `${payload}.${signature}`;
}

async function verifyAdminSessionValue(token: string | undefined): Promise<boolean> {
  const { ADMIN_SESSION_SECRET } = runtimeEnv();
  if (!token || !ADMIN_SESSION_SECRET) return false;

  const parts = token.split(".");
  if (parts.length === 4 && parts[0] === "v2") {
    const sessionId = parts[1];
    const expiresAt = Number(parts[2]);
    if (!Number.isSafeInteger(expiresAt) || expiresAt <= Date.now()) return false;
    if (expiresAt > Date.now() + ADMIN_SESSION_MAX_AGE * 1000) return false;

    const payload = `${parts[0]}.${parts[1]}.${parts[2]}`;
    const expectedSignature = await sign(payload, ADMIN_SESSION_SECRET);
    if (!constantTimeEqual(parts[3], expectedSignature)) return false;

    try {
      const db = getDb();
      const session = await db.query.adminSecurity.findFirst({
        where: (table, { eq }) => eq(table.key, `session:${sessionId}`),
      });
      return Boolean(session && (!session.lockedUntil || new Date(session.lockedUntil).getTime() > Date.now()));
    } catch {
      return false;
    }
  }

  return false;
}

export async function revokeAdminSession(token: string | undefined): Promise<void> {
  if (!token) return;
  const parts = token.split(".");
  if (parts.length === 4 && parts[0] === "v2") {
    const sessionId = parts[1];
    try {
      const db = getDb();
      await db.delete(adminSecurity).where(eq(adminSecurity.key, `session:${sessionId}`));
    } catch (error) {
      console.error("failed to revoke admin session in db", error);
    }
  }
}

export async function hasAdminSession(): Promise<boolean> {
  const cookieStore = await cookies();
  return verifyAdminSessionValue(cookieStore.get(ADMIN_SESSION_COOKIE)?.value);
}

export async function requireAdminSession(returnTo: string): Promise<void> {
  if (await hasAdminSession()) return;
  redirect(`/admin/login?return_to=${encodeURIComponent(safeAdminReturnPath(returnTo))}`);
}

export function safeAdminReturnPath(value: string | null | undefined): string {
  if (!value || !value.startsWith("/admin/") || value.startsWith("//")) return "/admin/providers";
  if (value.startsWith("/admin/login")) return "/admin/providers";

  try {
    const url = new URL(value, "https://app.local");
    if (url.origin !== "https://app.local") return "/admin/providers";
    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return "/admin/providers";
  }
}
