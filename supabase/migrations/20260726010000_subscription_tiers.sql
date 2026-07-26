-- Persist the Stripe tier so the app can distinguish Solo from Partner.

alter table public.billing_accounts
    add column if not exists plan_tier text not null default 'free';

alter table public.billing_accounts
    drop constraint if exists billing_accounts_plan_tier_check;

alter table public.billing_accounts
    add constraint billing_accounts_plan_tier_check
    check (plan_tier in ('free', 'solo', 'partner'));

-- Subscriptions created before tier support used the $10 Solo price.
update public.billing_accounts
set plan_tier = 'solo'
where stripe_subscription_id is not null
  and plan_tier = 'free';
