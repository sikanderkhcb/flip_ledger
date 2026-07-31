import { requiredEnv } from "../_shared/clients.ts";
import { jsonResponse, methodNotAllowed } from "../_shared/http.ts";
import { sendWelcomeEmail } from "../_shared/email.ts";

/**
 * Sends a welcome email to a newly registered user.
 *
 * Intended to be driven by a Supabase Database Webhook on `auth.users` INSERT. Because that
 * webhook carries no Supabase JWT, this function is deployed with `verify_jwt = false` and instead
 * authenticates the caller with a shared secret header (`x-webhook-secret`), configured on the
 * webhook. This prevents anyone from hitting the public URL to spam welcome emails.
 *
 * Expected payload (Supabase DB webhook shape):
 *   { type: "INSERT", table: "users", record: { email, raw_user_meta_data: {...} }, ... }
 */

interface AuthUserRecord {
  email?: string | null;
  raw_user_meta_data?: Record<string, unknown> | null;
}

interface WebhookPayload {
  type?: string;
  record?: AuthUserRecord | null;
}

function metaString(
  meta: Record<string, unknown> | null | undefined,
  key: string,
): string | null {
  const value = meta?.[key];
  return typeof value === "string" ? value : null;
}

// Constant-time comparison so a caller can't learn the secret by timing responses.
function secretsMatch(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let mismatch = 0;
  for (let i = 0; i < a.length; i++) {
    mismatch |= a.charCodeAt(i) ^ b.charCodeAt(i);
  }
  return mismatch === 0;
}

Deno.serve(async (request) => {
  if (request.method !== "POST") return methodNotAllowed();

  const expectedSecret = requiredEnv("WELCOME_HOOK_SECRET");
  const providedSecret = request.headers.get("x-webhook-secret") ?? "";
  if (!secretsMatch(providedSecret, expectedSecret)) {
    return jsonResponse({ error: "Unauthorized." }, 401);
  }

  let payload: WebhookPayload;
  try {
    payload = await request.json();
  } catch {
    return jsonResponse({ error: "Invalid JSON body." }, 400);
  }

  // Only new inserts should trigger a welcome; ignore anything else the webhook might send.
  if (payload.type && payload.type !== "INSERT") {
    return jsonResponse({ skipped: `ignored ${payload.type}` });
  }

  const email = payload.record?.email?.trim();
  if (!email) {
    return jsonResponse({ error: "No email on record." }, 400);
  }

  const meta = payload.record?.raw_user_meta_data;
  const fullName = metaString(meta, "full_name");
  const businessName = metaString(meta, "business_name");

  try {
    await sendWelcomeEmail(email, fullName, businessName);
  } catch (mailError) {
    // Log and swallow: a mail failure should not make the webhook retry indefinitely.
    console.error("welcome email failed", mailError);
    return jsonResponse({ sent: false, error: "Email send failed." });
  }

  return jsonResponse({ sent: true });
});
