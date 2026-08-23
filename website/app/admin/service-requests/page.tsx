import { requireAdminSession } from "../../admin-auth";
import AdminServiceRequestsClient from "./service-requests-client";

export const dynamic = "force-dynamic";

export default async function AdminServiceRequestsPage() {
  await requireAdminSession("/admin/service-requests");
  return <AdminServiceRequestsClient />;
}
