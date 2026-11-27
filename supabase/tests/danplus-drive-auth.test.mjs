import { test } from "node:test";
import assert from "node:assert/strict";
import { handleDriveAuthorization } from "../functions/danplus-drive-auth/index.ts";
const uid = "11111111-1111-4111-8111-111111111111";
const other = "22222222-2222-4222-8222-222222222222";
const scope = "https://www.googleapis.com/auth/drive.file";
function setup() {
    let actor = uid, owner = uid, now = 1000, polls = 0, startCalls = 0, tokenScope = scope;
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
            polls++;
            assert.equal(options.body.get("client_secret"),"fixture-secret");
            assert.equal(options.body.get("device_code"),"fixture-code");
            return Response.json({access_token:"fixture-access",refresh_token:"fixture-refresh",expires_in:3600,token_type:"Bearer",scope:tokenScope});
        }
        throw new Error("unexpected upstream");
    };
    const deps = {config,fetch:fake,now:()=>now};
    const call = (body, auth=true)=>handleDriveAuthorization(new Request("https://example.test",{method:"POST",headers:auth?{Authorization:"Bearer fixture.jwt.token","X-Nuvio-Publishable-Key":"fixture-publishable-key-123"}:{},body:JSON.stringify(body)}),deps);
    return {call,config, actor:x=>actor=x, owner:x=>owner=x, time:x=>now=x, scope:x=>tokenScope=x, polls:()=>polls, starts:()=>startCalls};
}
test("authorized start and poll do not return secret",async()=>{
    const s=setup(); const start=await s.call({operation:"start"}); assert.equal(start.status,200);
    const body=await start.json(); assert.equal(body.user_code,"TEST-CODE"); assert.ok(!JSON.stringify(body).includes("fixture-secret"));
    const response=await s.call({operation:"poll",device_code:body.device_code}); assert.equal(response.status,200);
    assert.equal(response.headers.get("Cache-Control"),"no-store"); const token=await response.json();
    assert.equal(token.refresh_token,"fixture-refresh"); assert.ok(!JSON.stringify(token).includes("fixture-secret"));
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
