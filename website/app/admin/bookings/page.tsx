import { requireAdminSession } from "../../admin-auth";
import AdminBookingsClient from "./bookings-client";

export const dynamic = "force-dynamic";

export default async function AdminBookingsPage() {
  await requireAdminSession("/admin/bookings");
  return <AdminBookingsClient />;
}
