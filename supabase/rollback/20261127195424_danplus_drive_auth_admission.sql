begin;
drop function if exists public.danplus_drive_auth_admit(uuid, text);
drop table if exists danplus_drive_private.oauth_budget;
drop schema if exists danplus_drive_private;
commit;
