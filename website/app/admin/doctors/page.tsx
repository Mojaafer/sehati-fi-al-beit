import { requireAdminSession } from "../../admin-auth";
import AdminDoctorsClient from "./doctors-client";

export const dynamic = "force-dynamic";

export default async function AdminDoctorsPage() {
  await requireAdminSession("/admin/doctors");
  return <AdminDoctorsClient />;
}
