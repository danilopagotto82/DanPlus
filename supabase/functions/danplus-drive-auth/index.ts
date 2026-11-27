// ML08: isolated Google device-code broker. No deployment until the release gate.
const GOOGLE = "https://oauth2.googleapis.com/";
const NUVIO = "https://api.nuvio.tv";
const SCOPE = "https://www.googleapis.com/auth/drive.file";
const UUID = /^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i;
const encoder = new TextEncoder();
type Json = Record<string, unknown>;
type Config = { clientId: string; clientSecret: string; ticketSecret: string; owners: string[] };
type Operation = "start" | "poll";
type Dependencies = {
    config: Config;
    fetch: typeof fetch;
    now: () => number;
    admit: (owner: string, operation: Operation) => Promise<boolean>;
};
class Refusal extends Error {
    readonly status: number;
    readonly code: string;
    constructor(status: number, code: string) { super(code); this.status = status; this.code = code; }
}
function object(value: unknown): value is Json {
    return !!value && typeof value === "object" && !Array.isArray(value);
}
function str(value: unknown, max = 4096): string {
    if (typeof value !== "string" || !value.length || value.length > max) throw new Refusal(400, "invalid_request");
    return value;
}
function reply(body: Json, status = 200) {
    return new Response(JSON.stringify(body), { status, headers: {
        "Content-Type": "application/json", "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff"
    } });
}
async function limitedJson(message: Request | Response, limit = 16384): Promise<Json> {
    const reader = message.body?.getReader();
    if (!reader) throw new Refusal(400, "invalid_request");
    const decoder = new TextDecoder("utf-8", { fatal: true });
    let bytes = 0, result = "";
    for (;;) {
        const next = await reader.read();
        if (next.done) break;
        bytes += next.value.byteLength;
        if (bytes > limit) { await reader.cancel(); throw new Refusal(413, "oversized"); }
        result += decoder.decode(next.value, { stream: true });
    }
    const parsed: unknown = JSON.parse(result + decoder.decode());
    if (!object(parsed)) throw new Refusal(400, "invalid_request");
    return parsed;
}
async function owner(req: Request, deps: Dependencies) {
    const bearer = req.headers.get("Authorization") ?? "";
    const key = req.headers.get("X-Nuvio-Publishable-Key") ?? "";
    if (!/^Bearer [\w.-]{1,4096}$/.test(bearer) || !/^[\w.-]{20,4096}$/.test(key)) throw new Refusal(401, "sign_in");
    const headers = { Authorization: bearer, apikey: key, "Content-Type": "application/json" };
    const response = await deps.fetch(NUVIO + "/auth/v1/user", { headers, redirect: "error", signal: AbortSignal.timeout(6000) });
    if (!response.ok) throw new Refusal(401, "sign_in");
    const user = await limitedJson(response);
    if (typeof user.id !== "string" || !UUID.test(user.id) || user.is_anonymous !== false) throw new Refusal(401, "sign_in");
    const account = await deps.fetch(NUVIO + "/rest/v1/rpc/get_sync_owner", {
        method: "POST", body: "{}", headers, redirect: "error", signal: AbortSignal.timeout(6000)
    });
    if (!account.ok) throw new Refusal(401, "sign_in");
    const id: unknown = await account.json();
    if (typeof id !== "string" || !UUID.test(id) || !deps.config.owners.includes(id.toLowerCase())) throw new Refusal(403, "not_enabled");
    return { owner: id.toLowerCase(), actor: user.id.toLowerCase() };
}
function b64(bytes: Uint8Array) {
    return btoa(String.fromCharCode(...bytes)).replaceAll("+", "-").replaceAll("/", "_").replace(/=+$/, "");
}
function unb64(value: string) {
    if (!/^[A-Za-z0-9_-]+$/.test(value)) throw new Refusal(400, "invalid_ticket");
    return Uint8Array.from(atob(value.replaceAll("-", "+").replaceAll("_", "/")), char => char.charCodeAt(0));
}
async function signingKey(config: Config) {
    return crypto.subtle.importKey("raw", encoder.encode(config.ticketSecret), { name: "HMAC", hash: "SHA-256" }, false, ["sign", "verify"]);
}
async function issueTicket(config: Config, payload: Json) {
    const data = b64(encoder.encode(JSON.stringify(payload)));
    const mac = await crypto.subtle.sign("HMAC", await signingKey(config), encoder.encode(data));
    return data + "." + b64(new Uint8Array(mac));
}
async function verifyTicket(config: Config, ticket: string, identity: { owner: string; actor: string }, now: number) {
    const parts = ticket.split(".");
    if (parts.length !== 2 || !await crypto.subtle.verify("HMAC", await signingKey(config), unb64(parts[1]), encoder.encode(parts[0]))) throw new Refusal(400, "invalid_ticket");
    const payload: unknown = JSON.parse(new TextDecoder().decode(unb64(parts[0])));
    if (!object(payload) || payload.v !== 1 || payload.owner !== identity.owner || payload.actor !== identity.actor ||
        typeof payload.exp !== "number" || payload.exp <= now || payload.exp > now + 86400_000) throw new Refusal(403, "expired_or_wrong_account");
    return str(payload.code);
}
async function requireAdmission(deps: Dependencies, ownerId: string, operation: Operation) {
    let allowed: boolean;
    try {
        allowed = await deps.admit(ownerId, operation);
    } catch {
        throw new Refusal(503, "admission_unavailable");
    }
    if (allowed !== true) throw new Refusal(429, "rate_limited");
}

export function persistentAdmission(fetcher: typeof fetch, base: string, serviceKey: string) {
    return async (ownerId: string, operation: Operation): Promise<boolean> => {
        if (!/^https:\/\/[a-z0-9-]+\.supabase\.co$/.test(base) || !serviceKey) throw new Error("Admission unavailable");
        const result = await fetcher(base + "/rest/v1/rpc/danplus_drive_auth_admit", {
            method: "POST",
            headers: { apikey: serviceKey, Authorization: "Bearer " + serviceKey, "Content-Type": "application/json" },
            body: JSON.stringify({ p_owner: ownerId, p_operation: operation }),
            redirect: "error",
            signal: AbortSignal.timeout(6000),
        });
        if (!result.ok) throw new Error("Admission unavailable");
        const value: unknown = await result.json();
        if (typeof value !== "boolean") throw new Error("Invalid admission result");
        return value;
    };
}

async function google(deps: Dependencies, path: "device/code" | "token", fields: Record<string, string>) {
    const result = await deps.fetch(GOOGLE + path, {
        method: "POST", body: new URLSearchParams(fields), redirect: "error", signal: AbortSignal.timeout(12000),
        headers: { "Content-Type": "application/x-www-form-urlencoded" }
    });
    return { status: result.status, ok: result.ok, data: await limitedJson(result) };
}
function seconds(value: unknown): number {
    if (typeof value !== "number" || !Number.isSafeInteger(value) || value < 1 || value > 86400) throw new Refusal(502, "invalid_google_response");
    return value;
}
export async function handleDriveAuthorization(req: Request, deps: Dependencies): Promise<Response> {
    try {
        if (req.method !== "POST") throw new Refusal(405, "method");
        const c = deps.config;
        if (!c.clientId || !c.clientSecret || c.ticketSecret.length < 32 || !c.owners.length) throw new Refusal(503, "not_configured");
        const body = await limitedJson(req, 16384);
        const identity = await owner(req, deps);
        if (body.operation === "start") {
            await requireAdmission(deps, identity.owner, "start");
            const r = await google(deps, "device/code", { client_id: c.clientId, scope: SCOPE });
            if (!r.ok) throw new Refusal(502, "google_unavailable");
            const expires = seconds(r.data.expires_in);
            const interval = Math.max(5, seconds(r.data.interval));
            const url = new URL(str(r.data.verification_url, 1024));
            if (url.protocol !== "https:" || !["google.com", "www.google.com", "accounts.google.com"].includes(url.hostname) || url.username || url.password || url.port) throw new Refusal(502, "invalid_google_response");
            return reply({ device_code: await issueTicket(c, { v: 1, ...identity, exp: deps.now() + expires * 1000, code: str(r.data.device_code) }),
                user_code: str(r.data.user_code, 128), verification_url: url.toString(), expires_in: expires, interval });
        }
        if (body.operation === "poll") {
            const code = await verifyTicket(c, str(body.device_code, 12000), identity, deps.now());
            await requireAdmission(deps, identity.owner, "poll");
            const r = await google(deps, "token", { client_id: c.clientId, client_secret: c.clientSecret, device_code: code,
                grant_type: "urn:ietf:params:oauth:grant-type:device_code" });
            if (!r.ok) {
                const error = r.data.error;
                if (typeof error === "string" && ["authorization_pending", "slow_down", "expired_token", "access_denied"].includes(error)) return reply({ error }, 400);
                throw new Refusal(502, "google_unavailable");
            }
            if (r.data.scope !== SCOPE || typeof r.data.token_type !== "string" || r.data.token_type.toLowerCase() !== "bearer") throw new Refusal(502, "invalid_scope");
            return reply({ access_token: str(r.data.access_token), refresh_token: str(r.data.refresh_token),
                expires_in: seconds(r.data.expires_in), token_type: "Bearer", scope: SCOPE, client_id: c.clientId });
        }
        throw new Refusal(400, "invalid_request");
    } catch (error) {
        if (error instanceof Refusal) return reply({ error: error.code }, error.status);
        return reply({ error: "unavailable" }, 502);
    }
}
if (import.meta.main) {
    Deno.serve(req => handleDriveAuthorization(req, {
        config: {
            clientId: Deno.env.get("DANPLUS_DRIVE_CLIENT_ID") ?? "",
            clientSecret: Deno.env.get("DANPLUS_DRIVE_CLIENT_SECRET") ?? "",
            ticketSecret: Deno.env.get("DANPLUS_DRIVE_TICKET_SECRET") ?? "",
            owners: (Deno.env.get("DANPLUS_DRIVE_ALLOWED_OWNERS") ?? "").split(",").map(x => x.trim().toLowerCase()).filter(x => UUID.test(x)),
        },
        fetch, now: Date.now,
        admit: persistentAdmission(fetch, Deno.env.get("SUPABASE_URL") ?? "", Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""),
    }));
}
