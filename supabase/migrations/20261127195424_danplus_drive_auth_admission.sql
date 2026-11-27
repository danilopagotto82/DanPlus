begin;
create schema danplus_drive_private;
revoke all on schema danplus_drive_private from public, anon, authenticated;
grant usage on schema danplus_drive_private to service_role;

create table danplus_drive_private.oauth_budget (
    owner_id uuid not null,
    operation text not null check (operation in ('start', 'poll')),
    window_started timestamptz not null,
    hits integer not null check (hits between 1 and 241),
    primary key (owner_id, operation)
);
alter table danplus_drive_private.oauth_budget enable row level security;
revoke all on table danplus_drive_private.oauth_budget from public, anon, authenticated, service_role;
grant select, insert, update on table danplus_drive_private.oauth_budget to service_role;

-- Atomic admission, bounded counters; at most two rows per authorized owner.
-- No backup contents, OAuth tokens, passwords, or Google identifiers stored.
create function public.danplus_drive_auth_admit(p_owner uuid, p_operation text)
returns boolean
language plpgsql
security invoker
set search_path = ''
as $$
declare
    v_limit integer;
    v_now timestamptz := clock_timestamp();
    v_hits integer;
begin
    if p_owner is null or p_operation is null or p_operation not in ('start', 'poll') then
        raise exception 'Invalid admission request' using errcode = '22023';
    end if;
    v_limit := case p_operation when 'start' then 6 else 240 end;
    insert into danplus_drive_private.oauth_budget as budget
        (owner_id, operation, window_started, hits)
    values (p_owner, p_operation, v_now, 1)
    on conflict (owner_id, operation) do update set
        hits = case when budget.window_started <= v_now - interval '10 minutes'
            then 1 else least(budget.hits + 1, v_limit + 1) end,
        window_started = case when budget.window_started <= v_now - interval '10 minutes'
            then v_now else budget.window_started end
    returning hits into v_hits;
    return v_hits <= v_limit;
end;
$$;
revoke all on function public.danplus_drive_auth_admit(uuid, text) from public, anon, authenticated, service_role;
grant execute on function public.danplus_drive_auth_admit(uuid, text) to service_role;
commit;
