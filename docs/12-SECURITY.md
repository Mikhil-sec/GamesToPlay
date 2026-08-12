# CONTINUE? — Security

> **Read this before touching the Worker, `local.properties`, `.gitignore`, or anything that
> makes a network call.** This repo is **public** (Next Gen requires an OSI licence and an open
> repo), and the backend is an **unauthenticated public endpoint on free-tier quotas**. Those
> two facts drive every decision below.
>
> Last full audit: **2026-08-12**. Every claim here was verified against the live deployment,
> not just read in source — the commands are included so they can be re-run.

---

## 1. Threat model in one paragraph

There are no user accounts and no server-issued credentials, so **there is no secret we can
ship in the app**: anything embedded in a public open-source Android APK is extractable in
minutes. Authentication is therefore not available to us. The realistic goals are (a) keep
genuinely-secret credentials out of the repo and out of the client entirely, and (b) make
abuse *bounded and unprofitable* rather than impossible. The assets actually worth protecting
are the Twitch/IGDB credentials, the release keystore, and — most exposed of all — the
**free-tier quotas**, which a single script can exhaust in under a minute.

## 2. What is secret, what only looks secret

Getting this wrong in either direction wastes time, so it's written down explicitly.

| Value | Secret? | Where it lives |
|---|---|---|
| Twitch **Client Secret** | 🔴 **Yes** | Cloudflare Worker secret (`wrangler secret put`). Never in a file, never in the app. |
| RevenueCat **secret** key (`sk_…`) | 🔴 **Yes** | Worker secret only. Not yet set. **Never** put this in the app — it can grant currency and entitlements. |
| Steam API key | 🔴 **Yes** | Worker secret only. |
| Release keystore + passwords | 🔴 **Yes** | `app/keystore/*.jks` + `key.properties`, both gitignored. Backed up externally (confirmed 2026-08-12). |
| RevenueCat **public** SDK key (`goog_…`) | 🟢 No | Designed to ship in clients. In `local.properties` only so rotation is easy. |
| Twitch **Client ID** | 🟢 No | Public identifier. |
| AdMob App ID + ad unit IDs | 🟢 No | Ship in the APK manifest by design. |
| Cloudflare KV namespace id | 🟢 No | Not a credential; useless without account access. |
| Worker URL | 🟢 No | Public endpoint by definition — which is why §4 exists. |

**The rule:** if a value can spend money, grant entitlements, or read another user's data, it
belongs in `wrangler secret put` and the app must reach it only through a Worker endpoint that
validates the request. No exceptions, no "just for testing".

## 3. Repo hygiene — audited 2026-08-12 ✅

| Check | Result |
|---|---|
| `.gitignore` covers `local.properties`, `key.properties`, `*.jks`, `*.keystore`, `.env*`, `worker/.dev.vars`, `.wrangler/` | ✅ |
| Live secret files confirmed ignored via `git check-ignore` | ✅ `local.properties`, `key.properties`, `continue-release.jks` |
| Any secret file **ever committed**, across all history | ✅ None — `git log --all --diff-filter=A --name-only` is clean |
| Secret-shaped strings across **all** history (`sk_`, `AIza`, private keys, literal passwords) | ✅ No true positives. Only the RevenueCat *public* key and `getProperty("storePassword")` call sites |
| Only `.example` templates tracked | ✅ `local.properties.example`, `key.properties.example` |

Re-run the history scan before any push that worries you:

```bash
git log --all --pretty=format: --name-only --diff-filter=A | sort -u \
  | grep -iE "local\.properties$|key\.properties$|\.jks$|\.env$|\.dev\.vars"

git grep -nIE "(sk_[A-Za-z0-9]{20,}|AIza[A-Za-z0-9_-]{30,}|-----BEGIN .*PRIVATE KEY)" $(git rev-list --all)
```

> ⚠️ `.gitignore` only protects files that were **never** committed. If a secret ever does land
> in a commit, adding it to `.gitignore` does nothing — the value must be **rotated**, because
> it is permanently in the history (and, on a public repo, scraped within minutes).

## 4. API abuse hardening — the main event

The Worker is public and unauthenticated. Before this pass it had **no limits of any kind**:
anyone could loop `curl` and drain everything. What's actually at stake:

| Resource | Free-tier limit | Why it hurts |
|---|---|---|
| **Workers KV writes** | **1,000/day** | 🔥 **Tightest by far.** One uncached search = one write. ~1,000 unique queries and caching dies for the day, so every later request falls through to IGDB. |
| IGDB | 4 requests/sec | Sustained abuse gets the Twitch app throttled or **banned**. Losing IGDB inside the deadline is unrecoverable. |
| Workers requests | 100,000/day | Whole API goes dark. |
| KV reads | 100,000/day | Cache stops serving. |

### Controls now in place

**Three rate limiters** (`[[ratelimits]]` in `wrangler.toml`, `src/security.ts`). Chosen over a
KV-based counter deliberately: KV counters would consume the very write quota they protect.

| Limiter | Budget | Rationale |
|---|---|---|
| `API_LIMITER` | 120/min per IP | Comfortable for a human typing into a 300ms-debounced search box; useless to a script. |
| `RESOLVE_LIMITER` | 20/min per IP | `/resolve` fans one request out to up to 6 provider searches, so it needs a much tighter budget than a plain read. |
| `GLOBAL_LIMITER` | 600/min account-wide | **Per-IP limits do nothing against a distributed attack.** Keyed on a constant so all traffic shares one counter, sized under IGDB's 240/min so we throttle ourselves before IGDB bans us. |

Limits are applied **before routing**, so probing unknown paths costs an attacker the same
budget as a real request. Shallow `/health` is exempt (it touches nothing); `/health?deep=1`
is not, because it spends a real IGDB call. All three **fail open** if a binding is missing, so
a misconfigured deploy degrades rather than taking the API down.

**Amplification caps.** The worst offender was `/steam/owned`: an unbounded `Promise.all` over a
Steam library turned **one** request into thousands of simultaneous IGDB searches. Now capped
at `MAX_STEAM_GAMES = 100`, most-played first, and matched sequentially.

**Input validation.** Query ≤ 100 chars (also keeps KV keys under KV's 512-byte limit — an
over-long query used to *throw* inside `cached()`), shared text ≤ 2,000 chars (cost of
`rankedCandidates` grows with input size, so an unbounded body was cheap CPU exhaustion),
request bodies ≤ 8 KB, game ids `\d{1,9}`, and `/coins/spend` now type-checks every field
instead of trusting a TypeScript `as` cast.

**Empty responses are never cached** (`kv.ts`) — this both prevents a bad upstream answer from
being pinned for 24h and stops an attacker cheaply filling the cache with junk keys.

**CORS removed.** It was `Access-Control-Allow-Origin: *`, which let *any web page* use this
Worker as a free games API on our IGDB quota. The only intended client is a native Android app,
which neither sends `Origin` nor enforces CORS, so the header bought nothing. (This stops
browser freeloading; it is not a boundary against scripted clients — rate limiting is.)

**Internal errors no longer leak.** The 500 handler returned `error.message`, which had
included provider URLs and query syntax. It now logs server-side (`wrangler tail`) and returns
a bare `INTERNAL_ERROR`.

### Verified live, 2026-08-12

```
150 requests → /games/trending        123× 200, 27× 429   ✅ enforced
POST /resolve  8KB+ body              400 BAD_REQUEST     ✅
POST /resolve  malformed JSON         400 BAD_REQUEST     ✅
/games/search?q=hollow knight         4 results, playtimes intact  ✅ no regression
```

> 🚨 **The bug this pass nearly shipped with.** The first deploy of the rate limiters *silently
> did nothing* — wrangler v3 ignores the `[[ratelimits]]` key without warning, and the deploy
> output listed only the KV binding. 150/150 requests returned `200`. Fixed by upgrading to
> **wrangler v4** (which required `@cloudflare/workers-types@5` in the same install). This is
> the same failure shape as the IGDB `category` bug: **a config that looks applied but isn't.**
> After any change to bindings, check the deploy output actually lists them, then prove it with
> traffic. Never assume a limit is live because the config file says so.

## 5. SSRF — found and fixed

`/resolve` performs **server-side fetches of URLs supplied by the caller**, so its host
allowlist is a real security boundary. It used `hostname.includes("tiktok.com")`, a substring
check — meaning `vm.tiktok.com.attacker.example` passed, and an attacker could have aimed the
Worker's fetch at any host they controlled (internal services, cloud metadata endpoints).

Fixed with `hostMatches()` in `src/security.ts`: exact host or dot-anchored subdomain only,
plus an **https-only** requirement. Covered by tests in `test/security.test.ts`, including the
lookalike host, `localhost`, and `169.254.169.254`. Verified live — both are refused without
any outbound fetch.

## 6. Android client

| Check | State |
|---|---|
| `usesCleartextTraffic="false"` | ✅ Added 2026-08-12 (explicit, though targetSdk 36 defaults this off) |
| R8 minify + resource shrink on release | ✅ Enabled |
| `exported="true"` activities | ✅ Only MainActivity (launcher) and ShareTargetActivity (share target) — both require it |
| `FileProvider` | ✅ `exported="false"`, scoped paths |
| Backup rules | ✅ Only the pile DB. DataStore is **not** backed up, so backup/restore can't duplicate coins |
| Secrets in the APK | ✅ None beyond values that are public by design (§2) |

**Coins are client-side and deliberately not fraud-proof.** `CoinLedger` holds the balance on
device because coins gate DRAW and CLAUDE.md constraint #5 requires that to work offline — and
because RevenueCat virtual currency *cannot* be credited from a client (granting needs the
secret key). Clearing app data resets the balance. This is an accepted, documented trade-off,
not an oversight; the fix is AdMob **server-side verification**, which needs real ad units,
which needs a production Play listing. Entitlements (`pro`) are unaffected — those are
RevenueCat-authoritative and verified server-side.

## 7. Known gaps — accepted, with reasons

These are deliberate, not missed. Revisit if the threat picture changes.

- **No client attestation.** Play Integrity API could prove requests come from a genuine
  install, and is the correct long-term answer to "how do we know this is our app?" Not worth
  the integration cost before launch; rate limiting bounds the damage meanwhile.
- **Rate limiting is per-IP**, so it's weak against a botnet with many addresses. That's
  precisely what `GLOBAL_LIMITER` backstops.
- **Coin balance is client-side** (§6).
- **`/coins/spend` and `/steam/owned` are 501** pending secrets — they fail closed, which is
  the safe direction.
- **No alerting on quota burn.** Worth adding a Cloudflare notification for Workers/KV usage
  before judging, so an attack or a viral spike is noticed rather than discovered.

## 8. Checklist for future sessions

Run before any push to the public repo, and after any change to bindings or endpoints:

- [ ] `git check-ignore -q local.properties key.properties app/keystore/*.jks` — all ignored
- [ ] History scan (§3) still clean
- [ ] No new secret read directly by app code — must go through the Worker
- [ ] New endpoints: rate-limited before routing, inputs length-capped, bodies size-capped
- [ ] New outbound fetch of a user-supplied URL: uses `hostMatches()`, https-only
- [ ] No new unbounded `Promise.all` over user-controlled collections
- [ ] `npx wrangler deploy` output **lists every expected binding**
- [ ] `cd worker && npm test` (28 tests) and `npx tsc --noEmit` clean
