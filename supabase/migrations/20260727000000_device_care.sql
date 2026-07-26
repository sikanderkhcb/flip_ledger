-- Repair and warranty information for each inventory device.
alter table public.devices
    add column if not exists repair_issue text not null default '',
    add column if not exists repair_provider text not null default '',
    add column if not exists repair_started_on date,
    add column if not exists repair_completed_on date,
    add column if not exists warranty_provider text not null default '',
    add column if not exists warranty_expires_on date;
