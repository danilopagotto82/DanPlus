import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { pathToFileURL } from 'node:url'

const { PGlite } = await import(pathToFileURL(process.argv[2]))
const db = new PGlite()
const ownerA = '10000000-0000-4000-8000-000000000001'
const ownerB = '10000000-0000-4000-8000-000000000002'
const call = (id, op) => db.query('select public.danplus_drive_auth_admit($1,$2) as admitted', [id,op])
const sql = async (file) => db.exec(await readFile(new URL(file, import.meta.url), 'utf8'))
try {
    await db.exec('create role anon; create role authenticated; create role service_role bypassrls')
    await sql('../migrations/20261127195424_danplus_drive_auth_admission.sql')
    for (const role of ['anon','authenticated']) {
        await db.exec('set role ' + role)
        await assert.rejects(call(ownerA, 'start'), /permission denied/)
        await assert.rejects(db.query('select * from danplus_drive_private.oauth_budget'), /permission denied/)
    }
    await db.exec('set role service_role')
    const results = await Promise.all(Array.from({length: 12}, () => call(ownerA, 'start')))
    assert.equal(results.filter(result => result.rows[0].admitted).length, 6)
    assert.equal((await call(ownerB,'start')).rows[0].admitted, true)
    assert.equal((await call(ownerA,'poll')).rows[0].admitted, true)
    await assert.rejects(call(ownerA,'other'), /Invalid admission/)
    await db.query("update danplus_drive_private.oauth_budget set window_started = now() - interval '11 minutes' where owner_id = $1", [ownerA])
    assert.equal((await call(ownerA,'start')).rows[0].admitted, true)
    await db.exec('reset role')
    await sql('../rollback/20261127195424_danplus_drive_auth_admission.sql')
    await assert.rejects(call(ownerA,'start'), /does not exist/)
    console.log('PASS role isolation, concurrent submissions, independent budgets, window reset and rollback')
} finally { await db.close() }
