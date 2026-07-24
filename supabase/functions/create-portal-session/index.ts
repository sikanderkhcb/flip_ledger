import {
  AuthError,
  createAdminClient,
  createStripeClient,
  requireUser,
  requiredEnv,
} from "../_shared/clients.ts";
import {
  corsHeaders,
  jsonResponse,
  methodNotAllowed,
} from "../_shared/http.ts";

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (request.method !== "POST") return methodNotAllowed();

  try {
    const admin = createAdminClient();
    const user = await requireUser(request, admin);
    const stripe = createStripeClient();

    const { data, error } = await admin
      .from("billing_accounts")
      .select("stripe_customer_id")
      .eq("user_id", user.id)
      .maybeSingle();
    if (error) throw error;
    if (!data?.stripe_customer_id) {
      return jsonResponse({ error: "No Stripe customer exists yet." }, 409);
    }

    const returnUrl =
      `${requiredEnv("SUPABASE_URL")}/functions/v1/stripe-return?result=portal`;
    const session = await stripe.billingPortal.sessions.create({
      customer: data.stripe_customer_id,
      return_url: returnUrl,
    });
    return jsonResponse({ url: session.url });
  } catch (error) {
    if (error instanceof AuthError) {
      return jsonResponse({ error: error.message }, 401);
    }
    console.error("create-portal-session failed", error);
    return jsonResponse({ error: "Unable to open subscription management." }, 500);
  }
});
