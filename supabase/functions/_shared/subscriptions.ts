import type { SupabaseClient } from "npm:@supabase/supabase-js@^2.95.0";
import Stripe from "npm:stripe@^22.0.0";

function expandableId(
  value: string | Stripe.Customer | Stripe.DeletedCustomer | null,
): string | null {
  if (!value) return null;
  return typeof value === "string" ? value : value.id;
}

export async function syncSubscription(
  admin: SupabaseClient,
  subscription: Stripe.Subscription,
): Promise<void> {
  const customerId = expandableId(subscription.customer);
  let userId = subscription.metadata.supabase_user_id;

  if (!userId && customerId) {
    const { data, error } = await admin
      .from("billing_accounts")
      .select("user_id")
      .eq("stripe_customer_id", customerId)
      .maybeSingle();
    if (error) throw error;
    userId = data?.user_id ?? "";
  }

  if (!userId) {
    throw new Error(`No FlipLedger user mapping for subscription ${subscription.id}`);
  }

  const periodEnd = subscription.items.data
    .map((item) => item.current_period_end)
    .filter((value): value is number => typeof value === "number")
    .sort((a, b) => b - a)[0];

  const { error } = await admin.from("billing_accounts").upsert(
    {
      user_id: userId,
      stripe_customer_id: customerId,
      stripe_subscription_id: subscription.id,
      subscription_status: subscription.status,
      current_period_end: periodEnd
        ? new Date(periodEnd * 1000).toISOString()
        : null,
      cancel_at_period_end: subscription.cancel_at_period_end,
      updated_at: new Date().toISOString(),
    },
    { onConflict: "user_id" },
  );
  if (error) throw error;
}
