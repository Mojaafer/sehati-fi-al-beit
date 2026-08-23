import { ADMIN_SESSION_COOKIE } from "../../../admin-auth";

export async function POST() {
  return Response.json(
    { success: true },
    {
      headers: {
        "Cache-Control": "no-store",
        "Set-Cookie": `${ADMIN_SESSION_COOKIE}=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Lax`,
      },
    },
  );
}
