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

    const { data: existing, error: accountError } = await admin
      .from("billing_accounts")
      .select("stripe_customer_id, subscription_status")
      .eq("user_id", user.id)
      .maybeSingle();
    if (accountError) throw accountError;

    if (
      existing?.subscription_status === "active" ||
      existing?.subscription_status === "trialing"
    ) {
      return jsonResponse({ error: "Subscription is already active." }, 409);
    }

    let customerId = existing?.stripe_customer_id as string | null;
    if (!customerId) {
      const customer = await stripe.customers.create({
        email: user.email,
        metadata: { supabase_user_id: user.id },
      });
      customerId = customer.id;

      const { error } = await admin.from("billing_accounts").upsert(
        {
          user_id: user.id,
          stripe_customer_id: customerId,
          updated_at: new Date().toISOString(),
        },
        { onConflict: "user_id" },
      );
      if (error) throw error;
    }

    const returnBase =
      `${requiredEnv("SUPABASE_URL")}/functions/v1/stripe-return`;
    const session = await stripe.checkout.sessions.create({
      mode: "subscription",
      customer: customerId,
      client_reference_id: user.id,
      line_items: [
        {
          price: requiredEnv("STRIPE_PRICE_ID"),
          quantity: 1,
        },
      ],
      metadata: { supabase_user_id: user.id },
      subscription_data: {
        metadata: { supabase_user_id: user.id },
      },
      success_url: `${returnBase}?result=success`,
      cancel_url: `${returnBase}?result=cancelled`,
    });

    if (!session.url) throw new Error("Stripe did not return a Checkout URL.");
    return jsonResponse({ url: session.url });
  } catch (error) {
    if (error instanceof AuthError) {
      return jsonResponse({ error: error.message }, 401);
    }
    console.error("create-checkout-session failed", error);
    return jsonResponse({ error: "Unable to start checkout." }, 500);
  }
});
