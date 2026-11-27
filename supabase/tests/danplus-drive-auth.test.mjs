import { test } from "node:test";
import assert from "node:assert/strict";
import { handleDriveAuthorization, persistentAdmission } from "../functions/danplus-drive-auth/index.ts";
const uid = "11111111-1111-4111-8111-111111111111";
const other = "22222222-2222-4222-8222-222222222222";
const scope = "https://www.googleapis.com/auth/drive.file";
function setup() {
    let actor = uid, owner = uid, now = 1000, polls = 0, startCalls = 0, tokenScope = scope;
    let refreshCalls = 0;
    let renewal = {access_token:"fixture-renewed",expires_in:900,token_type:"Bearer"};
    let renewalStatus = 200;
    const config = {clientId:"fixture-client",clientSecret:"fixture-secret",ticketSecret:"test-ticket-secret-0123456789012345",owners:[uid]};
    const fake = async (url, options) => {
        if (url.endsWith("/auth/v1/user")) return Response.json({id:actor,is_anonymous:false});
        if (url.endsWith("/get_sync_owner")) return Response.json(owner);
        if (url.endsWith("/device/code")) {
            startCalls++;
            assert.equal(options.body.get("scope"),scope);
            assert.equal(options.body.has("client_secret"),false);
            return Response.json({device_code:"fixture-code",user_code:"TEST-CODE",verification_url:"https://www.google.com/device",expires_in:600,interval:5});
        }
        if (url.endsWith("/token")) {
            assert.equal(options.body.get("client_secret"),"fixture-secret");
            if (options.body.get("grant_type") === "refresh_token") {
                refreshCalls++;
                assert.equal(options.body.get("client_id"),"fixture-client");
                assert.ok(["fixture-refresh","rotated-refresh"].includes(options.body.get("refresh_token")));
                assert.equal(options.body.has("scope"),false);
                return Response.json(renewal, {status:renewalStatus});
            }
            polls++;
            assert.equal(options.body.get("device_code"),"fixture-code");
            return Response.json({access_token:"fixture-access",refresh_token:"fixture-refresh",expires_in:3600,token_type:"Bearer",scope:tokenScope});
        }
        throw new Error("unexpected upstream");
    };
    let admission = true, unavailable = false;
    const attempts = [];
    const deps = {config,fetch:fake,now:()=>now,admit:async (owner, operation)=>{
        attempts.push({owner,operation});
        if (unavailable) throw new Error("fixture outage");
        return admission;
    }};
    const call = (body, auth=true)=>handleDriveAuthorization(new Request("https://example.test",{method:"POST",headers:auth?{Authorization:"Bearer fixture.jwt.token","X-Nuvio-Publishable-Key":"fixture-publishable-key-123"}:{},body:JSON.stringify(body)}),deps);
    return {call,config, actor:x=>actor=x, owner:x=>owner=x, time:x=>now=x, scope:x=>tokenScope=x, polls:()=>polls, starts:()=>startCalls, attempts, admit:x=>admission=x, unavailable:x=>unavailable=x,
        refreshes:()=>refreshCalls, renewal:(body,status=200)=>{renewal=body; renewalStatus=status;}};
}
test("authorized start and poll do not return secret",async()=>{
    const s=setup(); const start=await s.call({operation:"start"}); assert.equal(start.status,200);
    const body=await start.json(); assert.equal(body.user_code,"TEST-CODE"); assert.ok(!JSON.stringify(body).includes("fixture-secret"));
    const response=await s.call({operation:"poll",device_code:body.device_code}); assert.equal(response.status,200);
    assert.equal(response.headers.get("Cache-Control"),"no-store"); const token=await response.json();
    assert.equal(token.refresh_token,"fixture-refresh"); assert.ok(!JSON.stringify(token).includes("fixture-secret"));
    assert.match(token.refresh_proof,/^[A-Za-z0-9_-]{43}$/);
});

async function enroll(s) {
    const start = await (await s.call({operation:"start"})).json();
    return (await s.call({operation:"poll", device_code:start.device_code})).json();
}

function renew(s, grant, extra={}) {
    return s.call({operation:"refresh", refresh_token:grant.refresh_token, refresh_proof:grant.refresh_proof, ...extra});
}

test("refresh uses the server secret, same scope and original owner admission",async()=>{
    const s=setup(); const grant=await enroll(s); const response=await renew(s,grant);
    assert.equal(response.status,200);
    assert.equal(response.headers.get("Cache-Control"),"no-store");
    const renewed=await response.json();
    assert.equal(renewed.access_token,"fixture-renewed");
    assert.equal(renewed.scope,scope);
    assert.equal(renewed.refresh_token,grant.refresh_token);
    assert.equal(renewed.refresh_proof,grant.refresh_proof);
    assert.ok(!JSON.stringify(renewed).includes("fixture-secret"));
    assert.deepEqual(s.attempts.at(-1),{owner:uid,operation:"start"});
});

test("an unbound token, proof, actor, owner or client cannot use Google or consume the budget",async()=>{
    const s=setup(); const grant=await enroll(s); const count=s.attempts.length;
    for (const extra of [{refresh_token:"arbitrary"}, {refresh_proof:null}, {refresh_proof:"Z".repeat(43)}]) {
        assert.equal((await renew(s,grant,extra)).status,403);
    }
    s.actor(other); assert.equal((await renew(s,grant)).status,403);
    s.actor(uid); s.config.owners.push(other); s.owner(other);
    assert.equal((await renew(s,grant)).status,403);
    s.owner(uid); s.config.clientId="other-client"; assert.equal((await renew(s,grant)).status,403);
    assert.equal(s.refreshes(),0); assert.equal(s.attempts.length,count);
});

test("refresh rate limits and admission outages stop before Google",async()=>{
    const s=setup(); const grant=await enroll(s);
    s.admit(false); assert.equal((await renew(s,grant)).status,429);
    s.unavailable(true); assert.equal((await renew(s,grant)).status,503);
    assert.equal(s.refreshes(),0);
});

test("rotation binds a new proof; OAuth errors and foreign scope never leak provider details",async()=>{
    const s=setup(); const grant=await enroll(s);
    s.renewal({access_token:"renewed",expires_in:900,token_type:"Bearer",scope,refresh_token:"rotated-refresh"});
    const rotated=await (await renew(s,grant)).json();
    assert.notEqual(rotated.refresh_proof,grant.refresh_proof);
    assert.equal((await renew(s,rotated,{refresh_proof:grant.refresh_proof})).status,403);
    assert.equal((await renew(s,rotated)).status,200);
    s.renewal({access_token:"never-returned",expires_in:900,token_type:"Bearer",scope:scope+" extra"});
    const invalid=await renew(s,grant);
    assert.equal(invalid.status,502);
    assert.deepEqual(await invalid.json(),{error:"invalid_scope"});
    s.renewal({error:"invalid_grant",error_description:"must not be forwarded"},400);
    const revoked=await renew(s,grant);
    assert.equal(revoked.status,410);
    assert.deepEqual(await revoked.json(),{error:"reauthorization_required"});
});
test("unauthorized and unconfigured requests never start Google authorization",async()=>{
    const s=setup(); assert.equal((await s.call({operation:"start"},false)).status,401);
    s.owner(other); assert.equal((await s.call({operation:"start"})).status,403);
    s.config.ticketSecret="short"; assert.equal((await s.call({operation:"start"})).status,503); assert.equal(s.starts(),0);
});
test("tampered, expired and cross-actor tickets never poll",async()=>{
    const s=setup(); const body=await (await s.call({operation:"start"})).json();
    const ticket=body.device_code;
    assert.notEqual((await s.call({operation:"poll",device_code:"X"+ticket})).status,200);
    s.actor(other); assert.equal((await s.call({operation:"poll",device_code:ticket})).status,403);
    s.actor(uid); s.time(602000); assert.equal((await s.call({operation:"poll",device_code:ticket})).status,403); assert.equal(s.polls(),0);
});
test("unexpected Google scope is refused",async()=>{
    const s=setup(); const body=await (await s.call({operation:"start"})).json(); s.scope(scope+" extra");
    const r=await s.call({operation:"poll",device_code:body.device_code}); assert.equal(r.status,502); assert.equal((await r.json()).error,"invalid_scope");
});

test("persistent admission refuses before reaching Google",async()=>{
    const s=setup();
    s.admit(false);
    const denied=await s.call({operation:"start"});
    assert.equal(denied.status,429); assert.equal(s.starts(),0);
    assert.deepEqual(s.attempts,[{owner:uid,operation:"start"}]);
    s.unavailable(true);
    assert.equal((await s.call({operation:"start"})).status,503); assert.equal(s.starts(),0);
});
test("poll limit and invalid ticket never reach token endpoint",async()=>{
    const s=setup(); const body=await (await s.call({operation:"start"})).json();
    s.admit(false);
    assert.equal((await s.call({operation:"poll",device_code:body.device_code})).status,429);
    assert.equal(s.polls(),0);
    const count=s.attempts.length;
    assert.notEqual((await s.call({operation:"poll",device_code:"invalid"})).status,200);
    assert.equal(s.attempts.length,count);
});
test("RPC admission only accepts an exact boolean from the configured host",async()=>{
    let response=Response.json(true), calls=0;
    const fake=async (url, options)=>{
        calls++;
        assert.equal(url,"https://fixture.supabase.co/rest/v1/rpc/danplus_drive_auth_admit");
        assert.deepEqual(JSON.parse(options.body),{p_owner:uid,p_operation:"start"});
        assert.equal(options.redirect,"error");
        return response;
    };
    const admit=persistentAdmission(fake,"https://fixture.supabase.co","fixture-service-key");
    assert.equal(await admit(uid,"start"),true);
    response=Response.json(false); assert.equal(await admit(uid,"start"),false);
    response=Response.json({allowed:true}); await assert.rejects(admit(uid,"start"));
    response=Response.json({}, {status:503}); await assert.rejects(admit(uid,"start"));
    await assert.rejects(persistentAdmission(fake,"https://fixture.supabase.co.evil.test","fixture-service-key")(uid,"start"));
    assert.equal(calls,4);
});
