import { createAdminClient, requireUser } from "../_shared/clients.ts";
import { jsonResponse, methodNotAllowed } from "../_shared/http.ts";

/**
 * Deletes the calling user's account and all their data. Required by Google Play for any app
 * with user accounts. The caller is identified from their Supabase JWT (supabase-kt attaches it
 * on functions.invoke), so a user can only ever delete themselves. Deleting the auth user
 * cascades to profiles/devices/costs/sales/billing via each table's ON DELETE CASCADE FK.
 */
Deno.serve(async (request) => {
  if (request.method !== "POST") return methodNotAllowed();

  const admin = createAdminClient();

  let userId: string;
  try {
    const user = await requireUser(request, admin);
    userId = user.id;
  } catch {
    return jsonResponse({ error: "Authentication required." }, 401);
  }

  const { error } = await admin.auth.admin.deleteUser(userId);
  if (error) {
    console.error("delete-account failed", error);
    return jsonResponse({ error: "Unable to delete account." }, 500);
  }

  return jsonResponse({ deleted: true });
});
