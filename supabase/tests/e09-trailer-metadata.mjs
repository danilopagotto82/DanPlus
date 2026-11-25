import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";

const runtime = process.argv[2];
if (!runtime) throw new Error("Provide the absolute path to PGlite's dist/index.js");
const { PGlite } = await import(pathToFileURL(runtime));
const db = new PGlite();
const table = "e09_lab.trailer_metadata_cache";
const load = (file) => readFile(new URL("./e09_" + file, import.meta.url), "utf8");

try {
    await db.exec("create role anon; create role authenticated; create role service_role bypassrls");
    await db.exec(await load("trailer_metadata_cache.sql"));
    await db.exec("set role service_role");

    const insert = (id, type, locale, source, selection, video, evidence, checked, expires) =>
        db.query(
            "insert into " + table + " values ($1, $2, $3, $4, $5, $6, $7, now() + $8::interval, now() + $9::interval)",
            [id, type, locale, source, selection, video, evidence, checked, expires]
        );
    const candidates = [
        ["tmdb:101", "movie", "pt-BR", "tmdb_youtube", "pt_br_evidence_v1", "aaaaaaaaaaa", "dubbed_pt_br", "0", "1 hour"],
        ["tmdb:101", "tv", "pt-BR", "tmdb_youtube", "pt_br_evidence_v1", "bbbbbbbbbbb", "original_language", "0", "1 hour"],
        ["tmdb:101", "movie", "en-US", "tmdb_youtube", "provider_default_v1", "ccccccccccc", "fallback", "0", "1 hour"],
        ["tmdb:101", "movie", "pt-BR", "tmdb_youtube", "provider_default_v1", "ddddddddddd", "fallback", "0", "1 hour"],
        ["tmdb:101", "movie", "pt-BR", "imdb", "pt_br_evidence_v1", "vi12345", "fallback", "0", "1 hour"],
        ["imdb:tt202", "movie", "pt-BR", "tmdb_youtube", "pt_br_evidence_v1", null, null, "0", "2 minutes"],
        ["imdb:tt303", "movie", "pt-BR", "tmdb_youtube", "pt_br_evidence_v1", null, null, "-3 minutes", "-1 minute"],
        ["imdb:tt404", "movie", "pt-BR", "tmdb_youtube", "pt_br_evidence_v1", "eeeeeeeeeee", "fallback", "1 minute", "2 minutes"]
    ];
    for (const candidate of candidates) await insert(...candidate);

    await assert.rejects(insert(...candidates[0]), { code: "23505" });
    await assert.rejects(
        insert("tmdb:505", "movie", "pt-BR", "tmdb_youtube", "pt_br_evidence_v1", "https://secret.test/token", "fallback", "0", "1 hour"),
        { code: "23514" }
    );
    await assert.rejects(
        insert("tmdb:606", "movie", "pt-BR", "tmdb_youtube", "pt_br_evidence_v1", null, null, "0", "3 minutes"),
        { code: "23514" }
    );
    await assert.rejects(
        insert("tmdb:707", "movie", "pt-BR", "tmdb_youtube", "pt_br_evidence_v1", "fffffffffff", "fallback", "0", "7 hours"),
        { code: "23514" }
    );
    await db.query("delete from " + table + " where video_id = $1", ["ddddddddddd"]);

    await db.exec("reset role; set role authenticated");
    const { rows } = await db.query("select work_id, video_id from " + table + " order by video_id nulls last");
    assert.deepEqual(rows.map((row) => row.video_id), ["aaaaaaaaaaa", "bbbbbbbbbbb", "ccccccccccc", "vi12345", null]);
    assert.equal(rows.at(-1).work_id, "imdb:tt202");
    await assert.rejects(db.query("delete from " + table), { code: "42501" });
    await assert.rejects(insert(...candidates[0]), { code: "42501" });
    await assert.rejects(db.query("update " + table + " set locale = 'en'"), { code: "42501" });

    await db.exec("reset role; set role anon");
    await assert.rejects(db.query("select * from " + table), { code: "42501" });

    await db.exec("reset role");
    await db.exec(await load("trailer_metadata_cache_rollback.sql"));
    const { rows: remaining } = await db.query("select count(*)::int as count from pg_namespace where nspname = 'e09_lab'");
    assert.equal(remaining[0].count, 0);
    console.log("E09 LAB PASS: five key dimensions, expiry, negative TTL, ID validation, RLS, invalidation and rollback");
} finally {
    await db.close();
}
