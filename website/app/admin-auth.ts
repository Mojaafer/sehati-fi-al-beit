import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export const ADMIN_SESSION_COOKIE = "sehatak_admin_session";
export const ADMIN_SESSION_MAX_AGE = 8 * 60 * 60;

const encoder = new TextEncoder();

type RuntimeEnv = {
  ADMIN_USERNAME?: string;
  ADMIN_PASSWORD_SHA256?: string;
  ADMIN_SESSION_SECRET?: string;
};

function runtimeEnv(): RuntimeEnv {
  return {
    ADMIN_USERNAME: process.env.ADMIN_USERNAME,
    ADMIN_PASSWORD_SHA256: process.env.ADMIN_PASSWORD_SHA256,
    ADMIN_SESSION_SECRET: process.env.ADMIN_SESSION_SECRET,
  };
}

function bytesToHex(bytes: ArrayBuffer): string {
  return Array.from(new Uint8Array(bytes), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

function constantTimeEqual(left: string, right: string): boolean {
  const length = Math.max(left.length, right.length);
  let difference = left.length ^ right.length;
  for (let index = 0; index < length; index += 1) {
    difference |= (left.charCodeAt(index) || 0) ^ (right.charCodeAt(index) || 0);
  }
  return difference === 0;
}

async function sha256(value: string): Promise<string> {
  return bytesToHex(await crypto.subtle.digest("SHA-256", encoder.encode(value)));
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

export async function verifyAdminCredentials(username: string, password: string): Promise<boolean> {
  const { ADMIN_USERNAME, ADMIN_PASSWORD_SHA256 } = runtimeEnv();
  if (!ADMIN_USERNAME || !ADMIN_PASSWORD_SHA256 || !username || !password) return false;

  const passwordHash = await sha256(password);
  return constantTimeEqual(username, ADMIN_USERNAME)
    && constantTimeEqual(passwordHash, ADMIN_PASSWORD_SHA256.toLowerCase());
}

export async function createAdminSessionValue(): Promise<string> {
  const { ADMIN_SESSION_SECRET } = runtimeEnv();
  if (!ADMIN_SESSION_SECRET) throw new Error("ADMIN_SESSION_SECRET is not configured");

  const expiresAt = Date.now() + ADMIN_SESSION_MAX_AGE * 1000;
  const payload = `v1.${expiresAt}`;
  return `${payload}.${await sign(payload, ADMIN_SESSION_SECRET)}`;
}

async function verifyAdminSessionValue(token: string | undefined): Promise<boolean> {
  const { ADMIN_SESSION_SECRET } = runtimeEnv();
  if (!token || !ADMIN_SESSION_SECRET) return false;

  const parts = token.split(".");
  if (parts.length !== 3 || parts[0] !== "v1") return false;

  const expiresAt = Number(parts[1]);
  if (!Number.isSafeInteger(expiresAt) || expiresAt <= Date.now()) return false;
  if (expiresAt > Date.now() + ADMIN_SESSION_MAX_AGE * 1000) return false;

  const payload = `${parts[0]}.${parts[1]}`;
  const expectedSignature = await sign(payload, ADMIN_SESSION_SECRET);
  return constantTimeEqual(parts[2], expectedSignature);
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
