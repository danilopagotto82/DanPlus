-- Apply only in the same isolated LAB.
begin;
drop table if exists e09_lab.trailer_metadata_cache;
drop schema if exists e09_lab;
commit;
