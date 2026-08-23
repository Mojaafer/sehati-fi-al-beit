import { createClient } from "@supabase/supabase-js";

const bucket = process.env.SUPABASE_STORAGE_BUCKET || "private-files";

function storage() {
  const url = process.env.SUPABASE_URL;
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!url || !serviceRoleKey) {
    throw new Error("SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY must be configured");
  }

  return createClient(url, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  }).storage.from(bucket);
}

export async function uploadPrivateFile(path: string, data: ArrayBuffer, contentType: string) {
  const { error } = await storage().upload(path, data, { contentType, upsert: false });
  if (error) throw error;
}

export async function downloadPrivateFile(path: string) {
  const { data, error } = await storage().download(path);
  if (error) {
    if (error.message.toLowerCase().includes("not found")) return null;
    throw error;
  }
  return data;
}

export async function removePrivateFiles(paths: string[]) {
  if (paths.length === 0) return;
  const { error } = await storage().remove(paths);
  if (error) throw error;
}
