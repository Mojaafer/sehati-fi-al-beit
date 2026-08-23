import { requireAdminSession } from "../../admin-auth";
import AdminProvidersClient from "./providers-client";

export const dynamic = "force-dynamic";

export default async function AdminProvidersPage() {
  await requireAdminSession("/admin/providers");
  return <AdminProvidersClient displayName="مسؤول النظام" />;
}
