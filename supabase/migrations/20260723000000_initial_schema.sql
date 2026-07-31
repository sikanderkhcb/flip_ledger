-- BlackInk's reproducible Supabase schema.
-- Apply through the Supabase CLI after reviewing it against the target environment.

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    business_name text not null default '',
    owner_name text not null default '',
    partner_name text not null default 'Partner',
    workspace_type text not null default 'solo'
        check (workspace_type in ('solo', 'partner')),
    split_you integer not null default 60
        check (split_you between 0 and 100),
    currency text not null default 'USD'
        check (currency in ('USD', 'CAD')),
    category_pref text not null default 'mixed',
    onboarded boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.devices (
    id text primary key,
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    category text not null,
    model text not null check (length(trim(model)) > 0),
    identifier text not null default '',
    condition text,
    storage text not null default '',
    lock text not null default '—',
    purchase_price_cents bigint not null check (purchase_price_cents > 0),
    source text,
    purchase_date date not null,
    status text not null default 'Purchased'
        check (status in ('Purchased', 'Repair', 'Ready', 'Listed', 'Sold')),
    days_held integer not null default 0 check (days_held >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.costs (
    id text primary key,
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    device_id text not null references public.devices(id) on delete cascade,
    type text not null,
    amount_cents bigint not null check (amount_cents > 0),
    paid_by text not null default 'You' check (paid_by in ('You', 'Partner')),
    date date not null,
    note text not null default '',
    created_at timestamptz not null default now()
);

create table if not exists public.sales (
    id text primary key,
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    device_id text not null,
    model text not null,
    sold_date date not null,
    channel text,
    revenue_cents bigint not null check (revenue_cents > 0),
    cost_cents bigint not null check (cost_cents >= 0),
    fees_cents bigint not null default 0 check (fees_cents >= 0),
    days_held integer not null default 0 check (days_held >= 0),
    created_at timestamptz not null default now(),
    unique (user_id, device_id)
);

-- Existing early-development databases may predate the idempotency column.
alter table public.sales add column if not exists device_id text;
create unique index if not exists sales_user_device_unique_idx
    on public.sales(user_id, device_id) where device_id is not null;

create index if not exists devices_user_created_idx
    on public.devices(user_id, created_at desc);
create index if not exists costs_user_device_idx
    on public.costs(user_id, device_id);
create index if not exists sales_user_created_idx
    on public.sales(user_id, created_at desc);

alter table public.profiles enable row level security;
alter table public.devices enable row level security;
alter table public.costs enable row level security;
alter table public.sales enable row level security;

drop policy if exists "profiles_select_own" on public.profiles;
create policy "profiles_select_own" on public.profiles
    for select to authenticated using (id = auth.uid());
drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own" on public.profiles
    for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

drop policy if exists "devices_select_own" on public.devices;
create policy "devices_select_own" on public.devices
    for select to authenticated using (user_id = auth.uid());
drop policy if exists "devices_insert_own" on public.devices;
create policy "devices_insert_own" on public.devices
    for insert to authenticated with check (user_id = auth.uid());
drop policy if exists "devices_update_own" on public.devices;
create policy "devices_update_own" on public.devices
    for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists "devices_delete_own" on public.devices;
create policy "devices_delete_own" on public.devices
    for delete to authenticated using (user_id = auth.uid());

drop policy if exists "costs_select_own" on public.costs;
create policy "costs_select_own" on public.costs
    for select to authenticated using (user_id = auth.uid());
drop policy if exists "costs_insert_own" on public.costs;
create policy "costs_insert_own" on public.costs
    for insert to authenticated with check (
        user_id = auth.uid()
        and exists (
            select 1 from public.devices d
            where d.id = device_id and d.user_id = auth.uid()
        )
    );
drop policy if exists "costs_update_own" on public.costs;
create policy "costs_update_own" on public.costs
    for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists "costs_delete_own" on public.costs;
create policy "costs_delete_own" on public.costs
    for delete to authenticated using (user_id = auth.uid());

drop policy if exists "sales_select_own" on public.sales;
create policy "sales_select_own" on public.sales
    for select to authenticated using (user_id = auth.uid());
drop policy if exists "sales_insert_own" on public.sales;
create policy "sales_insert_own" on public.sales
    for insert to authenticated with check (user_id = auth.uid());

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
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute procedure public.handle_new_user();

-- Backfill profiles for accounts created before the trigger existed.
insert into public.profiles (id, owner_name)
select
    u.id,
    coalesce(u.raw_user_meta_data ->> 'full_name', '')
from auth.users u
on conflict (id) do nothing;

-- Adds a device and any initial costs in one transaction.
create or replace function public.add_device(p_device jsonb, p_costs jsonb)
returns void
language plpgsql
security invoker
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
    cost_row jsonb;
begin
    if caller_id is null then
        raise exception 'Authentication required';
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
end;
$$;

revoke all on function public.add_device(jsonb, jsonb) from public, anon;
grant execute on function public.add_device(jsonb, jsonb) to authenticated;

-- Atomically records a sale and removes its device. PostgreSQL rolls the whole function back
-- if either operation fails. device_id makes retries idempotent after a lost network response.
create or replace function public.complete_sale(p_sale jsonb, p_device_id text)
returns void
language plpgsql
security invoker
set search_path = public
as $$
declare
    caller_id uuid := auth.uid();
begin
    if caller_id is null then
        raise exception 'Authentication required';
    end if;

    if exists (
        select 1 from public.sales
        where user_id = caller_id and device_id = p_device_id
    ) then
        return;
    end if;

    if not exists (
        select 1 from public.devices
        where id = p_device_id and user_id = caller_id
    ) then
        raise exception 'Device not found or access denied';
    end if;

    insert into public.sales (
        id, user_id, device_id, model, sold_date, channel,
        revenue_cents, cost_cents, fees_cents, days_held
    ) values (
        p_sale ->> 'id',
        caller_id,
        p_device_id,
        p_sale ->> 'model',
        (p_sale ->> 'sold_date')::date,
        nullif(p_sale ->> 'channel', ''),
        (p_sale ->> 'revenue_cents')::bigint,
        (p_sale ->> 'cost_cents')::bigint,
        coalesce((p_sale ->> 'fees_cents')::bigint, 0),
        coalesce((p_sale ->> 'days_held')::integer, 0)
    );

    delete from public.devices
    where id = p_device_id and user_id = caller_id;
end;
$$;

revoke all on function public.complete_sale(jsonb, text) from public, anon;
grant execute on function public.complete_sale(jsonb, text) to authenticated;
