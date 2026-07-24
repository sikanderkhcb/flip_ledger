import Stripe from "npm:stripe@^22.0.0";
import {
  createAdminClient,
  createStripeClient,
  requiredEnv,
} from "../_shared/clients.ts";
import { jsonResponse, methodNotAllowed } from "../_shared/http.ts";
import { syncSubscription } from "../_shared/subscriptions.ts";

const cryptoProvider = Stripe.createSubtleCryptoProvider();

Deno.serve(async (request) => {
  if (request.method !== "POST") return methodNotAllowed();

  const signature = request.headers.get("Stripe-Signature");
  if (!signature) return jsonResponse({ error: "Missing Stripe signature." }, 400);

  const stripe = createStripeClient();
  const body = await request.text();
  let event: Stripe.Event;
  try {
    event = await stripe.webhooks.constructEventAsync(
      body,
      signature,
      requiredEnv("STRIPE_WEBHOOK_SECRET"),
      undefined,
      cryptoProvider,
    );
  } catch (error) {
    console.error("stripe-webhook signature verification failed", error);
    return jsonResponse({ error: "Invalid webhook signature." }, 400);
  }

  try {
    const admin = createAdminClient();

    switch (event.type) {
      case "checkout.session.completed": {
        const session = event.data.object;
        if (session.mode !== "subscription" || !session.subscription) break;
        const subscriptionId = typeof session.subscription === "string"
          ? session.subscription
          : session.subscription.id;
        const subscription = await stripe.subscriptions.retrieve(subscriptionId);
        await syncSubscription(admin, subscription);
        break;
      }
      case "customer.subscription.created":
      case "customer.subscription.updated":
      case "customer.subscription.deleted":
        await syncSubscription(admin, event.data.object);
        break;
      default:
        break;
    }

    return jsonResponse({ received: true });
  } catch (error) {
    // A 5xx response tells Stripe to retry a genuine event that could not be persisted.
    console.error("stripe-webhook processing failed", error);
    return jsonResponse({ error: "Unable to process webhook." }, 500);
  }
});
