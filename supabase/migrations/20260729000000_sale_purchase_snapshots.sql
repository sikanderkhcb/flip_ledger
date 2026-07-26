-- Preserve purchase details after a sale removes the device from active inventory.
alter table public.sales
    add column if not exists purchase_price_cents bigint not null default 0,
    add column if not exists purchase_date date;

create or replace function public.complete_sale(p_sale jsonb, p_device_id text)
returns void language plpgsql security invoker set search_path = public
as $$
declare caller_id uuid := auth.uid();
begin
    if caller_id is null then raise exception 'Authentication required'; end if;
    if exists (select 1 from public.sales where user_id = caller_id and device_id = p_device_id) then return; end if;
    if not exists (select 1 from public.devices where id = p_device_id and user_id = caller_id) then
        raise exception 'Device not found or access denied';
    end if;
    insert into public.sales (
        id, user_id, device_id, model, sold_date, channel, revenue_cents, cost_cents, fees_cents, days_held,
        customer_name, customer_email, customer_phone, customer_address, purchase_price_cents, purchase_date
    ) values (
        p_sale ->> 'id', caller_id, p_device_id, p_sale ->> 'model', (p_sale ->> 'sold_date')::date,
        nullif(p_sale ->> 'channel', ''), (p_sale ->> 'revenue_cents')::bigint, (p_sale ->> 'cost_cents')::bigint,
        coalesce((p_sale ->> 'fees_cents')::bigint, 0), coalesce((p_sale ->> 'days_held')::integer, 0),
        coalesce(p_sale ->> 'customer_name', ''), coalesce(p_sale ->> 'customer_email', ''),
        coalesce(p_sale ->> 'customer_phone', ''), coalesce(p_sale ->> 'customer_address', ''),
        coalesce((p_sale ->> 'purchase_price_cents')::bigint, 0), nullif(p_sale ->> 'purchase_date', '')::date
    );
    delete from public.devices where id = p_device_id and user_id = caller_id;
end;
$$;
