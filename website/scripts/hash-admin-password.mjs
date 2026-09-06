#!/usr/bin/env node
/**
 * Generates the ADMIN_PASSWORD_PBKDF2 value for the admin login.
 *
 *   node scripts/hash-admin-password.mjs
 *   node scripts/hash-admin-password.mjs --iterations 250000
 *
 * The password is read from stdin so it never lands in shell history. Output format:
 *   pbkdf2$sha256$<iterations>$<salt-base64>$<hash-hex>
 *
 * Paste the whole line into ADMIN_PASSWORD_PBKDF2 (Cloudflare secret / Vercel env var), then
 * delete the old ADMIN_PASSWORD_SHA256.
 *
 * Iterations default to 100_000: Cloudflare Workers has historically rejected larger PBKDF2
 * counts, and the value is embedded in the output, so raising it later needs no code change —
 * just regenerate. Verification happens once per login, so the cost is not on any hot path.
 */
import { webcrypto } from "node:crypto";
import { createInterface } from "node:readline";

const DEFAULT_ITERATIONS = 100_000;

function parseIterations(argv) {
  const flagIndex = argv.indexOf("--iterations");
  if (flagIndex === -1) return DEFAULT_ITERATIONS;

  const value = Number(argv[flagIndex + 1]);
  if (!Number.isSafeInteger(value) || value < 1000) {
    console.error("--iterations must be an integer of at least 1000");
    process.exit(1);
  }
  return value;
}

function readPassword() {
  return new Promise((resolve) => {
    const rl = createInterface({ input: process.stdin, output: process.stderr, terminal: false });
    process.stderr.write("Password: ");
    rl.once("line", (line) => {
      rl.close();
      resolve(line);
    });
  });
}

const iterations = parseIterations(process.argv.slice(2));
const password = (await readPassword()).trim();

if (password.length < 12) {
  console.error("\nRefusing to hash a password shorter than 12 characters.");
  process.exit(1);
}

const salt = webcrypto.getRandomValues(new Uint8Array(16));
const key = await webcrypto.subtle.importKey(
  "raw",
  new TextEncoder().encode(password),
  "PBKDF2",
  false,
  ["deriveBits"],
);
const bits = await webcrypto.subtle.deriveBits(
  { name: "PBKDF2", salt, iterations, hash: "SHA-256" },
  key,
  256,
);

const saltB64 = Buffer.from(salt).toString("base64");
const hashHex = Buffer.from(bits).toString("hex");

process.stderr.write("\n\nSet this as ADMIN_PASSWORD_PBKDF2:\n\n");
console.log(`pbkdf2$sha256$${iterations}$${saltB64}$${hashHex}`);
