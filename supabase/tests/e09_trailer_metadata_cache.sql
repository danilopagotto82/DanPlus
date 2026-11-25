-- E09 candidate contract for an isolated PostgreSQL/Supabase LAB.
begin;

create schema e09_lab;
revoke all on schema e09_lab from public, anon, authenticated;
grant usage on schema e09_lab to authenticated, service_role;

create table e09_lab.trailer_metadata_cache (
    work_id text not null check (work_id ~ '^(imdb:tt|tmdb:)[0-9]{1,12}$'),
    media_type text not null check (media_type in ('movie', 'tv')),
    locale text not null check (locale ~ '^[a-z]{2}(-[A-Z]{2})?$'),
    source text not null check (source in ('tmdb_youtube', 'imdb')),
    selection text not null check (selection in ('pt_br_evidence_v1', 'provider_default_v1')),
    video_id text,
    evidence text check (evidence in ('dubbed_pt_br', 'subtitled_pt_br', 'original_language', 'fallback')),
    checked_at timestamptz not null default now(),
    expires_at timestamptz not null,
    primary key (work_id, media_type, locale, source, selection),
    check (
        (video_id is null and evidence is null) or
        (video_id is not null and evidence is not null and (
            (source = 'tmdb_youtube' and video_id ~ '^[A-Za-z0-9_-]{11}$') or
            (source = 'imdb' and video_id ~ '^vi[0-9]{1,12}$')
        ))
    ),
    check (expires_at > checked_at and expires_at <= checked_at +
        case when video_id is null then interval '2 minutes' else interval '6 hours' end)
);

alter table e09_lab.trailer_metadata_cache enable row level security;
revoke all on e09_lab.trailer_metadata_cache from public, anon, authenticated;
grant select on e09_lab.trailer_metadata_cache to authenticated;
grant select, insert, update, delete on e09_lab.trailer_metadata_cache to service_role;

create policy trailer_metadata_live_read
on e09_lab.trailer_metadata_cache for select to authenticated
using (checked_at <= now() and expires_at > now());

commit;
