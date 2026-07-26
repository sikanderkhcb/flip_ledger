-- Reduce the Free plan from 10 to 5 lifetime device records.
-- Existing devices and sales remain accessible; only future device creation is gated.

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

    if billing_status not in ('active', 'trialing') and devices_created >= 5 then
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

    for cost_row in
        select value
        from jsonb_array_elements(coalesce(p_costs, '[]'::jsonb))
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
