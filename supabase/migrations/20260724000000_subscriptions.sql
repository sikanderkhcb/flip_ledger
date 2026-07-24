-- Stripe-backed subscriptions and the lifetime 10-device free allowance.

create table if not exists public.billing_accounts (
    user_id uuid primary key references auth.users(id) on delete cascade,
    lifetime_devices_created integer not null default 0
        check (lifetime_devices_created >= 0),
    stripe_customer_id text unique,
    stripe_subscription_id text unique,
    subscription_status text not null default 'free'
        check (
            subscription_status in (
                'free',
                'incomplete',
                'incomplete_expired',
                'trialing',
                'active',
                'past_due',
                'canceled',
                'unpaid',
                'paused'
            )
        ),
    current_period_end timestamptz,
    cancel_at_period_end boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.billing_accounts enable row level security;

drop policy if exists "billing_accounts_select_own" on public.billing_accounts;
create policy "billing_accounts_select_own" on public.billing_accounts
    for select to authenticated using (user_id = auth.uid());

revoke all on table public.billing_accounts from anon, authenticated;
grant select on table public.billing_accounts to authenticated;

-- Existing accounts receive credit for every device that is still in inventory or has a sale.
-- Previously deleted, unsold records cannot be recovered because no historical row exists.
insert into public.billing_accounts (user_id, lifetime_devices_created)
select
    u.id,
    coalesce(d.device_count, 0) + coalesce(s.sale_count, 0)
from auth.users u
left join (
    select user_id, count(*)::integer as device_count
    from public.devices
    group by user_id
) d on d.user_id = u.id
left join (
    select user_id, count(*)::integer as sale_count
    from public.sales
    group by user_id
) s on s.user_id = u.id
on conflict (user_id) do update
set lifetime_devices_created = greatest(
    public.billing_accounts.lifetime_devices_created,
    excluded.lifetime_devices_created
);

-- Keep profile and billing rows in sync with newly-created Auth users.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, owner_name)
    values (new.id, coalesce(new.raw_user_meta_data ->> 'full_name', ''))
    on conflict (id) do nothing;

    insert into public.billing_accounts (user_id)
    values (new.id)
    on conflict (user_id) do nothing;

    return new;
end;
$$;

-- Device creation must go through this RPC. Removing the direct INSERT policy prevents a
-- modified client from bypassing the lifetime free allowance.
drop policy if exists "devices_insert_own" on public.devices;

create or replace function public.add_device(p_device jsonb, p_costs jsonb)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    cost_row jsonb;
    devices_created integer;
    billing_status text;
begin
    if caller_id is null then
        raise exception 'Authentication required';
    end if;

    insert into public.billing_accounts (user_id)
    values (caller_id)
    on conflict (user_id) do nothing;

    select lifetime_devices_created, subscription_status
    into devices_created, billing_status
    from public.billing_accounts
    where user_id = caller_id
    for update;

    if billing_status not in ('active', 'trialing') and devices_created >= 10 then
        raise exception using
            errcode = 'P0001',
            message = 'FREE_DEVICE_LIMIT_REACHED';
    end if;

    insert into public.devices (
        id, user_id, category, model, identifier, condition, storage, lock,
        purchase_price_cents, source, purchase_date, status, days_held
    ) values (
        p_device ->> 'id',
        caller_id,
        p_device ->> 'category',
        p_device ->> 'model',
        coalesce(p_device ->> 'identifier', ''),
        nullif(p_device ->> 'condition', ''),
        coalesce(p_device ->> 'storage', ''),
        coalesce(p_device ->> 'lock', '—'),
        (p_device ->> 'purchase_price_cents')::bigint,
        nullif(p_device ->> 'source', ''),
        (p_device ->> 'purchase_date')::date,
        coalesce(p_device ->> 'status', 'Purchased'),
        coalesce((p_device ->> 'days_held')::integer, 0)
    );

    for cost_row in select value from jsonb_array_elements(coalesce(p_costs, '[]'::jsonb))
    loop
        insert into public.costs (
            id, user_id, device_id, type, amount_cents, paid_by, date, note
        ) values (
            cost_row ->> 'id',
            caller_id,
            p_device ->> 'id',
            cost_row ->> 'type',
            (cost_row ->> 'amount_cents')::bigint,
            coalesce(cost_row ->> 'paid_by', 'You'),
            (cost_row ->> 'date')::date,
            coalesce(cost_row ->> 'note', '')
        );
    end loop;

    update public.billing_accounts
    set
        lifetime_devices_created = lifetime_devices_created + 1,
        updated_at = now()
    where user_id = caller_id;
end;
$$;

revoke all on function public.add_device(jsonb, jsonb) from public, anon;
grant execute on function public.add_device(jsonb, jsonb) to authenticated;
