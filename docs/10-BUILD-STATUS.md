# CONTINUE? — Build Status

> **Read this first in any fresh session**, after `CLAUDE.md` and before `09-PENDING-INPUTS.md`.
> This is the living ledger of what's actually built, what's verified, what's broken, and
> what's next. Update it whenever you finish a chunk of work or discover something that
> changes this picture — don't let it go stale like a comment nobody re-reads.
>
> Last updated: **2026-09-19** — ✅ **`versionCode 11` is live in production and tested on a
> device: the FRIENDS share loop works end to end.** ✅ **AdMob is sorted**: app-ads.txt verified,
> and the Play listing is linked to the AdMob app that v11 actually uses. ✅ Privacy policy §3b
> (FRIENDS) is live. What's left before Sept 30 is the **submission**, not the app; see the
> 2026-09-19 entry for the open list.
>
> Previous: **2026-09-18** — **Judge access is now in place store-side**: the 7-day trial
> offer (`trial-feature-monthly`) is live in all 173 regions, and a promo code exists; both still
> need a device check. The trial had never existed before today; the app needed no change.
> 🔴 **New finding: bought coins never reach the in-app balance.** Coin packs can't be bought in
> v11, nothing reads RevenueCat's COIN balance, and testers hold 600–4,250 COIN in RevenueCat that
> the app never shows, so the paywall's **"A MONTHLY COIN DROP"** perk is false in v11. The store
> listing line can be fixed without a build; the paywall line can't. See the 2026-09-18 entry.
>
> Previous entry: **2026-09-17** — **FRIENDS ships in `versionCode 11` / `0.11.0`**, the intended
> production release: share your pile as a signed link, and follow friends' piles from a new
> FRIENDS tab — no accounts, nothing stored server-side. Also: the **consent flow is verified on a
> device** (after fixing why the debug build never showed it). **203 app tests + 75 Worker tests,
> 0 failures.** Worker **deployed and verified**. 🔴 Not yet rendered on a device; privacy policy
> not yet pushed — see the 2026-09-17 entry.
>
> Earlier: **2026-09-12** (third entry same day) — **the EEA/UK/CH exclusion is REVERSED**;
> a real UMP consent flow ships instead, so the app goes to **all countries**. `versionCode 10` /
> `0.10.0` built. 🔴 Inert until a **GDPR message is published in the AdMob console**.
>
> Earlier: **2026-09-12** (second entry same day) — **`versionCode 9` / `0.9.0` is built
> and signed** with the real Play App Signing fingerprint in place; clear reward cut +5→+1; a live
> privacy-policy inaccuracy about IGDB's image CDN fixed; IGDB credits now on six screens.
> 🔴 **The Worker still needs `npx wrangler deploy` before that build is installed.**
>
> Earlier that day — **production access is GRANTED**, and **SHARE was rebuilt around
> a friend loop**: a shared game is now a real Android App Link that unfurls with key art in
> WhatsApp/Discord and opens straight into the app. Three properly designed cards replace the one
> Roboto-on-a-rectangle card, the Credits Roll finally has a share button, and the fake
> `continue.app/pile` URL is gone. **166 app tests + 65 worker tests, 0 failures.** 🔴 One
> blocking input: the **Play App Signing SHA-256**, needed before the Worker is deployed — see
> the 2026-09-12 entry and `docs/09-PENDING-INPUTS.md`.
>
> Previous entry: **2026-09-07** — see the 2026-09-07 entry immediately below. No code changed;
> what changed is that **`versionCode 8` reached the Closed track and came back clean**, and the
> production-access application is drafted.
>
> Previous entry: 2026-08-28 — **the fourth closed-test feedback round, and the biggest one**:
> ten tester items plus one of Mikhil's own, all eleven addressed. Headlines: the **HAPTICS
> setting had never been wired to anything** (five screens each built their own `Haptics`); the
> **clear-a-game coin reward was an unbounded faucet** (clear → back to THE PILE → clear again);
> **filters were rebuilt on one shared vocabulary** (`GameTaxonomy`) now used by PILE, DRAW's new
> GENRE dial and a new **STATS** screen; **backdating** landed, so games cleared before the app
> existed can be logged with real dates; search learned to **respell** a query with its
> separators missing (`spiderman` → `spider man`), to **re-rank** IGDB's answer and to **dedupe**
> it; DISCOVER search rows gained **cover art, year and length**; rails gained **SHOW MORE**; and
> RANK gained **manual reordering and removal**. "Ghost of Tsushima has no banner" root-caused to
> the **share target writing a deliberately incomplete `games` row** — fixed, plus a
> once-per-launch batch refresh that repairs every already-cached row on every device. The Worker
> was redeployed (game modes in `tags`, `/games/batch`, rail paging, `CACHE_VERSION` v5) and
> verified against production. **131 unit tests, 0 failures** (was 84). See the 2026-08-28 entry
> immediately below.
>
> Same day, second pass: **the two remaining dead controls and the Room migration lane**. The
> CLIPBOARD DETECTION switch and the hours-per-week input were the same bug as HAPTICS — controls
> a user can touch that were wired to nothing — and `AppDatabase` was one entity change away from
> crashing every existing install on launch. All three fixed; **150 unit tests, 0 failures**.
>
> ⚠️ **Nothing in this round has been on a device.** `adb devices` was empty for the whole
> session, so every item below is "compiles, tested where testable, unverified on glass".
>
> Previous entry: 2026-08-26 — **the third closed-test feedback round**, and the first one to
> contain a *crash*. Four items: the STACK crash reported by three testers and carried in two
> Play Console issues (**root-caused and fixed** — one missing `remember` key), no way to undo an
> accidental add (**REMOVE FROM PILE added**), the paywall's lifetime CTA reading "INSERT COIN"
> (**now "PURCHASE"**), and TikTok shares dead-ending on a chooser full of wrong games (**they now
> go straight to a blank field with an explanation**). Also confirmed closed: the DISCOVER →
> pile bug left open on 2026-08-23 — it was the **RETIRED** category all along, not a lost write.
> See the 2026-08-26 entry immediately below. **`versionCode 7` / `0.7.0` is built and signed**,
> carrying all of it — `app/build/outputs/bundle/release/app-release.aab` (32.4 MB, signer valid
> to 2056, `jarsigner -verify` clean). **Not yet uploaded to any track** — testers are still on
> `versionCode 6`; getting this onto Closed is next.
>
> Previous entry: 2026-08-23 — **the second closed-test feedback round**: five items reported,
> four fixed in code (playtime accuracy, share auto-match accuracy, DISCOVER's clear button
> and add-confirmation, STACK's swipe hint). The fifth — "games added from DISCOVER never
> reached the pile" — **has no reproducible root cause yet**; what shipped is the set of
> changes that make it visible and make the write robust. See the 2026-08-23 entry
> immediately below, and **the open question at the end of it**. `versionCode 5`/`0.5.0` was
> superseded by **`versionCode 6` / `0.6.0`** (5 was already used on Internal testing), built
> 2026-08-24 08:51 and carrying all of it; it goes to the Closed track on 2026-08-24.
>
> Previous entry: 2026-08-19 (latest) — **the share target's accuracy bugs are fixed** (four of
> them, found by reproducing a tester report against production), and **`versionCode` 2/3/4 were
> all confirmed uploaded to Internal testing** (earlier entries wrongly said only 1 was ever
> uploaded — see §RevenueCat below and the 🔴 table in `docs/09-PENDING-INPUTS.md`). Earlier the
> same day: **closed testing is live**, `versionCode 4` is the build on the *Closed* track, and
> the clock started **2026-08-19**, so the earliest production-access application is
> **2026-09-02**. Built this day: the **STACK view**, **three new DISCOVER rails**
> (deployed and verified in production), and the first copy anywhere in the app that tells a user
> the **share target exists**. **`versionCode 5`/`0.5.0` is signed and built, not yet uploaded to
> either track** — that's Mikhil's next action, not blocked on anything. The 14 days of waiting
> are build time, not dead time.
>
> Previous entry: 2026-08-15 — a **second phone-test feedback round** (the first build Mikhil has
> run on both a phone and the tablet). Five items reported, all layout/ergonomics; four needed
> work, all four are fixed below. The paywall and the new dispenser deal animation were both
> called out as working well on glass — **those two are now device-verified**, which closes the
> single biggest gap from the previous entry.
>
> **A signed `versionCode 5` / `0.5.0` `app-release.aab` is built and current** at
> `app/build/outputs/bundle/release/` (32.4 MB, built 2026-08-19 19:22, signer cert valid to
> 2056), carrying everything below plus the STACK view, DISCOVER rails, and share-target fixes
> — **not yet uploaded to any track**, that's next.
> Rebuild after any further app change: `JAVA_HOME=/c/Android/jdk21/jdk-21.0.12+8 ./gradlew bundleRelease`.
>
> **Correction, 2026-08-19: `versionCode` 2, 3, and 4 were all uploaded to *Internal* testing**
> (Mikhil confirms), not just 1 as earlier entries here claimed — confirmed independently via
> RevenueCat, which shows real customer records tagged `0.2.0` and `0.3.0`. Only *Closed*
> testing has stayed on `versionCode 4` throughout; Internal testing has been iterated on the
> whole time. "Never uploaded" below refers only to the Closed track.

---

## 2026-09-19 — v11 live and tested; AdMob resolved; the rest is submission work

**Done (confirmed by Mikhil unless noted):**
- `versionCode 11` / `0.11.0` is **live on the production track**.
- **On-device tap-through done: the FRIENDS share loop works** (share pile → friend opens the link →
  pile appears in FRIENDS). This closes the 2026-09-17 "not yet rendered on a device" gap.
- **AdMob fully resolved.** Two separate problems:
  1. **No app-ads.txt at the address AdMob checks.** AdMob reads the Play listing's developer
     website (`https://mikhil-sec.github.io/GamesToPlay/`) and checks the **host root**, which a
     project Pages site can't serve. Fixed with a separate user-site repo
     **`Mikhil-sec/Mikhil-sec.github.io`** containing only `app-ads.txt`
     (`google.com, pub-5970073296518593, DIRECT, f08c47fec0942fa0`); verified serving 200
     text/plain. **Never delete that repo**; AdMob re-crawls it. It does not affect
     `/GamesToPlay/…` (Pages serves each repo under its own path).
  2. **A duplicate AdMob app.** Using "Add app" to attach the Play listing created a *second*
     AdMob app (`…~7461596117`, 0 ad units) and linked the listing to it. v11 is compiled
     against the original `…~4180553585`, which owns both rewarded units. The listing was cleared
     from the duplicate and linked to the original. AdMob apps can't be deleted, only hidden;
     **never add ad units to `…7461596117`**.
- **Privacy policy §3b (FRIENDS) is live**: checked on the public URL 2026-09-19 (by Claude).
- **Consent (UMP) messages are published in AdMob** (Mikhil, 2026-09-18).

**Still open, in priority order:**
1. **Judge access on a device**: see the 7-day trial badge on GO PRO from a never-subscribed
   account, and redeem one of the 200 one-time promo codes to confirm PRO unlocks. Store side is
   verified (2026-09-18). "App has been tested" did not explicitly cover these two.
2. **Store-listing sentence "a monthly coin drop"** is false in v11 (2026-09-18 entry below).
   Re-paste the corrected full description from `docs/13-STORE-LISTING.md`. No build needed.
3. **Decide on a v12 or not.** Only a build can fix the paywall's "A MONTHLY COIN DROP" perk:
   either drop the line, or bridge RevenueCat's COIN balance into `CoinLedger` via
   `creditPurchased`. With v11 live, a v12 is an ordinary update, not a restarted production
   review.
4. **Devpost submission** (deadline **2026-09-30 11:45pm PDT**): start from
   `docs/07-SUBMISSION-KIT.md` §"READ FIRST". The old drafts oversell (no FREE PLAY, no SSV, no
   share themes, no Steam import, and RevenueCat Virtual Currency isn't wired). Still to make:
   video, screenshots, the three write-ups, judge promo codes in a judges-only field.

---

## 2026-09-18 — the free trial was never created; judge access is unmet (no code change needed)

`versionCode 11` went to production review on 2026-09-17. Every Shipaton category except Next Gen
requires: *"the app must either offer a free trial or the Entrant must include a promo code for
judges to unlock the in-app purchase and test all premium features."* The docs had this down as
covered by "Pro Monthly — 7-day free trial". **It wasn't.**

**What the live store says** (RevenueCat `get-product-store-state` on `continue_pro_monthly` /
`prodb3fdae1092`, fetched 2026-09-18 05:35 UTC): base plan `monthly` is `ACTIVE`, P1M, $3.99 —
and `offers: {}`. On Play a free trial is **not a property of the product or the base plan**; it
is a separate *offer* attached to the base plan. The ✅ in `docs/09-PENDING-INPUTS.md`'s product
table only ever verified that the product existed. The Play promo-code backup was also never
generated. So as of today neither half of the rule is satisfied.

**Why this needs no new build.** The app was written to read terms from the store, not to
hardcode them:
- `RealBillingRepository.toProTier()` reads the trial from
  `product.defaultOption.freePhase` — RevenueCat's `defaultOption` is the eligible offer with the
  longest free phase (offers tagged `rc-ignore-offer` excluded), falling back to the base plan.
- `purchase()` passes the `Package`, so the purchase uses that same default option.
- `PaywallScreen` already renders the `N-DAY FREE TRIAL` badge, the `START N FREE DAYS ▸` CTA and
  the "Free for N days, then …" renewal terms whenever `freeTrialDays != null`.

So creating the offer in Play Console lights up the whole trial path on the `versionCode 11`
already in review. Offers are store configuration and do not touch the release under review.

**What Mikhil does (Play Console, not code):**
1. Monetize → Subscriptions → `continue_pro_monthly` → base plan `monthly` → **Add offer**:
   eligibility *New customer acquisition*, one phase *Free trial, 7 days*, **no** `rc-ignore-offer`
   tag → Activate.
2. Monetize → **Promo codes** → a code for `continue_pro_lifetime`, for the Devpost submission —
   a trial still demands a payment method, and some judges won't enter one. Redeemed in the Play
   Store app (*Payments & subscriptions → Redeem code*); the RevenueCat SDK syncs out-of-app
   purchases on the next foreground, v11 already listens via `updatedCustomerInfoListener`, and
   RESTORE PURCHASE is the manual fallback. **Test one code end-to-end on a device before putting
   it in the submission.**

**How to verify, and what counts as done:** re-run `get-product-store-state` and confirm the offer
appears under `base_plans.monthly.offers`; then open GO PRO on a device signed into an account that
has **never** subscribed and see the 7-day badge (a fresh offer can take a while to propagate, and
offerings are cached until app restart). Only the device check proves the path.

**The lesson**, same shape as the earlier config-looked-applied-but-was-inert bugs: "product exists" was
recorded as "trial exists". A requirement that lives in store config has to be checked against
the store's live state, not against the doc that planned it.

### Later the same day — trial verified live, promo code created

Mikhil created both. Re-checked with `get-product-store-state` (fetched 2026-09-18 08:56 UTC):
base plan `monthly` now carries offer **`trial-feature-monthly`**, state `ACTIVE`, one phase
`duration P7D`, `recurrence_count 1`, `free: true` in **all 173 regions** the base plan is sold
in, and no offer tags (so nothing hides it from RevenueCat's `defaultOption`). **Store side: done.**
Still open: see the 7-day badge on GO PRO from a never-subscribed account (a tester who already
had a monthly sub will correctly *not* be offered it).

**Play Console's "make sure Play Billing Library is integrated" warning on the promo code.** It
is: RevenueCat's SDK *is* a Play Billing Library wrapper — `purchases` 10.12.0 pulls in
`com.android.billingclient:billing` 8.3.0 (the version in the Gradle cache for this build), and
every purchase already goes through it. The warning is shown to everyone creating a code; it
matters for apps that sell with no billing library at all. What does matter is **which kind of
code** was made, because they redeem differently (RevenueCat docs, Google Play Offers → Promo
codes):
- **One-time codes** can be redeemed in the Play Store app (*Payments & subscriptions → Redeem
  code*, or `https://play.google.com/redeem?code=…`). The purchase happens outside the app;
  RevenueCat picks it up when the app next talks to Play, and RESTORE PURCHASE forces it.
- **Custom codes** (one code, many uses — the convenient kind for a judge panel) can only be
  redeemed **inside the purchase flow**: GO PRO → LIFETIME → PURCHASE → in Google's sheet, change
  the payment method to *Redeem code*. That is an ordinary purchase through the SDK, so v11
  handles it with no special code.
Either way: **redeem one on a device and confirm the PRO badge appears before the code goes on
Devpost**, and write the redemption steps for whichever type it is.

### And a second unverified claim: bought coins never reach the balance

Raised from another session: the Devpost drafts say coins are RevenueCat Virtual Currency. Checked
against the code and against RevenueCat's live customer data:

- **Coin packs cannot be bought in v11.** Nothing in the app references `coins_50/150/500` or the
  `coins` offering; `RealBillingRepository` only ever reads `offerings.current` (Monthly +
  Lifetime). There is no buy-coins UI.
- **Nothing reads RevenueCat's COIN balance.** No `virtualCurrencies()` call anywhere.
  `CoinLedger.creditPurchased()` — the documented bridge ("coins purchased are granted by
  RevenueCat server-side and folded in via `creditPurchased`") — **has zero callers.** The
  visible balance is purely the on-device ledger: 3 to start, +1 per rewarded ad, +1 per first
  clear of a game.
- **RevenueCat is granting — into a balance nobody sees.** The dashboard auto-grants
  `continue_pro_monthly` → 50 COIN per cycle and `continue_pro_lifetime` → 600 once. Four closed
  testers sampled via `list-virtual-currencies-balances` hold **600, 1,150, 1,450 and 4,250 COIN**
  in RevenueCat (sandbox renewals run every few minutes, hence the big numbers). None of that has
  ever appeared in their app.
- **So a live claim in v11 is false:** the paywall perk **"A MONTHLY COIN DROP — Coins land in your
  cabinet automatically"** (`PaywallScreen.kt` `PERKS`), and the store listing's "PRO adds …
  a monthly coin drop" (`docs/13-STORE-LISTING.md`). The PaywallScreen comment says every perk "is
  a capability that exists in the shipped build"; this one isn't.

**What can be fixed without a build:** the store-listing sentence (remove "a monthly coin drop").
**What can't:** the paywall perk and the missing bridge. The fix, if a v12 happens: on each
`CustomerInfo` update / app foreground, read `Purchases.sharedInstance.virtualCurrencies()["COIN"]`,
credit the increase over a persisted last-seen value via `creditPurchased`, then store the new
value — offline-first stays intact, the ledger stays the source of truth for spending. (Or, the
smaller change: drop the perk line.) If no v12 happens, **don't claim RevenueCat Virtual Currency
as a working feature on Devpost**; the honest framing is "COIN is configured in RevenueCat with
purchase auto-grants; the in-app balance is an offline ledger, and bridging the two is the next
step".

---

## 2026-09-17 — FRIENDS: follow a friend's pile, with no accounts

**`versionCode 11` / `0.11.0`, signed** (`jarsigner` clean; `versionCode 11`, `FriendImportActivity`
and the `/p` filter all read back out of the bundle's own manifest). Mikhil will send this build
straight to the production track.

### First: the consent flow never actually appeared in debug — fixed

Mikhil installed a debug build to check the 09-12 UMP flow and got no form. Cause:
`ConsentManager` set `DEBUG_GEOGRAPHY_EEA` but never called `addTestDeviceHashedId`, and Google's
docs are explicit that *"debug settings only work on test devices"*. So on real hardware in
Mauritius the SDK correctly concluded no form was needed — only an emulator would ever have shown
it. Debug builds now register the running device by the upper-case MD5 of `ANDROID_ID` (the id UMP
itself logs). **Verified on Mikhil's device: the form appears.** Release builds are unaffected.

Also: the debug build installs beside the Play build (`.debug` suffix) with an identical icon and
name, so there was no way to tell which one had been opened. It's now labelled
**CONTINUE? DEBUG** (`app/src/debug/res/values/strings.xml`).

### The feature

Mikhil's ask: share your pile, a friend taps the link, the app opens with "add friend's pile",
you name them, and a tab lists every friend's Pile / Now Playing / Cleared / Retired —
**only if it fits the current privacy promises** (no accounts, no tracking, no new Data Safety
entries). It does. Design in `docs/02-PRODUCT-SPEC.md` §6 FRIENDS; threat model in
`docs/12-SECURITY.md` §6b. The decisions worth not re-deriving:

- **The pile rides in the URL fragment** (`/p#<payload>`). Browsers never send fragments, so the
  Worker's `/p` is a static page, byte-identical for every visitor. The server learns nothing.
- **Signed with a per-phone random ECDSA P-256 key** (`PileIdentity`, `noBackupFilesDir`, software
  key — AndroidKeyStore would add device-specific failure modes to protect a read-only game list).
  Only the same key may update a friend, only with a higher `sequence` — a counter, not the
  sender's clock, because a phone set a year ahead would otherwise lock itself out forever.
- **No compression**, on purpose: sorted ids delta-encode to ~2 bytes each anyway, and no inflate
  step means no decompression bomb. A 12-game pile is a 306-char link; the 400-game cap is ~2,000.
- **Not live.** Updates arrive when the friend shares again and are applied on tap, with a diff
  ("3 new · 1 newly cleared · 2 gone"). A new key is never saved without the user naming them.
- **HIGH SCORES and CLEARED shares deliberately don't carry the pile link.** Only SHARE YOUR PILE,
  which says on screen that the link lists every game, may send the whole list. (It briefly did
  carry it from HIGH SCORES during the build; reverted for exactly that reason.)
- **Friend games get names offline first** — `OfflineGameIndex.lookup(ids)` writes placeholder
  rows (`cachedAt = 0`), then `/games/batch` fills them in (zero KV writes, ≤10 batches, 750 ms
  apart, each id at most once per 10 minutes so a link full of bogus ids can't make every screen
  visit re-request them). The launch-time stale sweep now covers friends' games too, your own
  pile first.
- **Room schema 1 → 2** — the first real migration through the lane built on 2026-08-28. Three
  additive tables (`friends`, `friend_games`, `friend_ranks`), no foreign keys (deletes are one
  explicit transaction). `AppMigrationsTest` now compares the migration SQL against Room's own
  exported `2.json`, so the two can't drift.
- **Bottom bar is now PILE · DISCOVER · (DRAW) · FRIENDS · YOU** — symmetric for the first time.
  ⚠️ Five columns on a 360dp phone: check the labels on glass (history says the tablet hides this).

New files: `core/friends/{PileSnapshot, PileSnapshotCodec, PileKeys, PileIdentity, FriendRepository}.kt`,
`core/data/entity/FriendEntity.kt`, `core/data/dao/FriendDao.kt`, `core/util/RelativeTime.kt`,
`feature/friends/*` (import sheet Activity, FRIENDS tab, friend pile screen),
`worker/src/routes/pileLink.ts`, `tools/make_pile_link.py`.

### Security review, done as part of the build

Every item is in `docs/12-SECURITY.md` §6b. The two found and fixed *during* review rather than
designed in up front: the id-retry window above, and a **double Back** on removing a friend from
their own screen (the remove callback *and* the "friend is gone" effect both navigated, popping
FRIENDS too).

### Verification

- **203 app unit tests, 0 failures** (was 166): 23 codec tests (round trip, every-byte tamper,
  key swap, every truncation, crafted duplicate/oversize/over-long-varint payloads, wrong-domain
  signature, off-curve points), a fixture minted by an **independent Python encoder**
  (`tools/make_pile_link.py`, `cryptography` library) that must decode exactly, pile-link parsing,
  name cleaning (bidi overrides, emoji-safe truncation), diffs, relative time, migration-vs-schema.
- **75 Worker tests, 0 failures** (was 66), `tsc --noEmit` clean — including running the landing
  page's script against hostile fragments and checking the CSP hash matches the embedded script.
- ⚠️ **Nothing new has been rendered.** `adb devices` was empty all session.

### Play Console's two edge-to-edge recommendations (flagged on `versionCode 8`)

1. *"Edge-to-edge may not display for all users"*: only `MainActivity` opted in. The two
   transparent sheet activities (share target, friend import) now call it too, and pad for the
   status bar so a tall sheet stops below the clock.
2. *"Deprecated APIs or parameters for edge-to-edge"*: `themes.xml` set
   `android:statusBarColor` / `android:navigationBarColor`, both deprecated (and ignored) on
   Android 15. Removed. Bars are now styled only by `core/design/EdgeToEdge.kt`.

**Real bug found along the way:** the no-argument `enableEdgeToEdge()` chooses icon colour from
the *system* theme, so on a phone in light mode the status-bar icons were dark on this dark-only
app's dark chrome, i.e. invisible. `enableArcadeEdgeToEdge()` forces the dark style everywhere.

⚠️ **Warning 2 may not fully clear.** `androidx.activity`'s own `enableEdgeToEdge()` still calls
`Window.setStatusBarColor` for backward compatibility on older APIs, and the ads/billing SDKs
may call deprecated window APIs too. Play attributes those calls to the app. They're
recommendations, not release blockers. Upgrading `activity` (1.9.3 → 1.13) would need a newer
compileSdk/AGP, which isn't worth the risk two weeks before the deadline.

### 🔴 Before this goes to production

1. ✅ **Worker deployed 2026-09-17** (version `7e2fc6f0`) and verified against production — `/p`
   serves the page with a CSP hash matching the served script; existing routes unchanged.
2. Push `docs/privacy.html` (new §3b).
3. Tap-through on a phone — list in `docs/09-PENDING-INPUTS.md`. `python tools/make_pile_link.py`
   then `adb shell am start -a android.intent.action.VIEW -d "<link>"` exercises the import sheet
   without a second device; `--key k.pem --seq 2` re-run exercises "updated".

---

## 2026-09-12 (third) — the EEA exclusion is reversed; a real consent flow ships instead

**`versionCode 10` / `0.10.0`.** The plan of record since 2026-08-24 was to exclude the EEA, UK
and Switzerland — 32 countries — from Play availability rather than build a consent flow. That
is now **reversed**. Ship to everywhere.

**Why the August decision no longer holds.** It was made when production access was a distant
maybe and the only thing that mattered was not disturbing the closed test; exclusion was the
cheap way to sidestep the work. Two things changed:

1. **It can lock a judge out.** Three of the four target categories are judged by someone
   opening the Play listing. A judge anywhere in those 32 countries sees *"not available in
   your country"* — not a weak app, **no app**.
2. **It contradicts the Catvertising thesis.** The entire advertising pitch is that CONTINUE?
   treats ads more respectfully than everyone else — no interstitials, every ad user-initiated
   with a stated exchange. "We removed a third of the developed world rather than show a
   dialog" is the opposite of that, and it's the category's own judge who would notice.

**What shipped** — `core/ads/ConsentManager.kt`, on `com.google.android.ump:user-messaging-platform:4.0.0`:

- `requestConsentInfoUpdate()` on **every** launch from `MainActivity`, not once — consent
  status changes server-side when vendor lists or policies do, so a one-time check drifts out
  of compliance silently. Fired before `setContent` and never awaited: a compliance check has
  no business on the cold-start path.
- **The gate lives inside `RealAdRepository.loadRewarded`**, not at the two call sites. Fourth
  time this codebase has applied that rule and the reasoning is identical to the HAPTICS bug: a
  rule enforced at call sites is enforced only where somebody remembered. A third ad surface
  added later inherits the check because there is no other way to obtain an ad. Refusal returns
  `null`, which every caller already handles — it is what a no-fill looks like, and an
  un-consented request *is* a no-fill from the user's side.
- **Fails closed.** `canRequestAds()` is false until the check completes, so an ad requested too
  early is simply not loaded rather than loaded on the assumption consent probably isn't needed.
- **"AD PRIVACY CHOICES" in YOU**, shown only where `privacyOptionsRequirementStatus` is
  `REQUIRED`. Google's policy requires a persistent way to withdraw consent; a row that opens an
  empty form for a user in Mauritius would be worse than no row.
- **Debug builds force `DEBUG_GEOGRAPHY_EEA`.** Without it the flow is untestable from Mauritius
  — every local run would exercise the one path that does nothing and the branch that matters
  would ship having never executed once.

**Declining costs a European user nothing they can't get elsewhere.** Pile, DRAW, RANK, sharing
and offline are untouched; they simply aren't offered the watch-an-ad-for-a-coin exchange, and
coins are still earned by clearing games or bought outright. Privacy policy §5 updated to say so
— it had been written deliberately *not* to claim a consent flow existed, and now one does.

🔴 **The code is inert until a GDPR message is published in the AdMob console.** If no message
exists, no form can be shown, consent is never obtained and `canRequestAds()` stays false — so
European users get no rewarded ads. Safe direction, still broken. See `docs/09-PENDING-INPUTS.md`.
Publishing is server-side and fixes the shipped build with no new release.

**Verified:** `jarsigner -verify` clean; release manifest `versionCode="10"` / `0.10.0`; UMP
present in the R8 mapping (11 classes) and `ConsentManager` survived minification as `v5.i`.
**166 app tests + 66 worker tests, 0 failures.** ⚠️ The consent form itself has **not** been seen
on a device — that needs a debug build and the AdMob message published.

---

## 2026-09-12 (later) — `versionCode 9` / `0.9.0` is built and signed

The blocking input landed. Mikhil read the **Play App Signing SHA-256** out of Play Console
(`F1:A8:69:…:6B:B4`) and it is now in `worker/src/routes/gameLink.ts`. It is correctly
*different* from the upload key — there is a test asserting exactly that, because pasting the
upload certificate into the Play App Signing slot produces 32 valid hex bytes that look entirely
right and break links for every real user while working perfectly on a sideloaded build.

Also this pass, following Mikhil's review:

- **The clear reward dropped +5 → +1** (`COMPLETION_COIN_REWARD`). A DRAW re-roll costs 1, so at
  +5 clearing two games bought ten re-rolls and nobody ever ran out — which removes every reason
  to watch a rewarded ad or buy Pro. The 5 was a closed-testing convenience.
- **The privacy policy had a false statement, unrelated to sharing, and it was live.** §6 claimed
  *"Requests to IGDB are made by our server, never by your device directly, so IGDB never sees
  your IP address."* True of IGDB's **data API** (`api.igdb.com`, which only the Worker touches);
  **false of the image CDN** (`images.igdb.com`), which every device hits directly via Coil at
  ~20 call sites. Rewritten to separate the two honestly, plus a new **§3a** covering sharing.
  No Data Safety change needed — the share feature collects nothing new, which was checked
  deliberately rather than assumed (see `docs/13-STORE-LISTING.md` §8).
- **`X-Robots-Tag: noindex, nofollow` on `/g/<id>`.** A quota defence, not SEO: it is the first
  endpoint here whose URLs are meant to be posted publicly, and a cache miss costs one IGDB call
  plus one KV write against 1,000/day. A crawler walking the id space would drain that with no
  malice at all. Deliberate enumeration was already covered by the rate limiters.
- **One IGDB attribution component, on six screens.** There were three hand-rolled copies with
  different styling and none of them linked anywhere. `core/ui/IgdbAttribution.kt` is now shared
  and tappable through to igdb.com, and it was added to **PILE** (grid + list), **STACKS** and
  **DRAW**'s dealt-cards phase, which showed IGDB data and credited nobody. Deliberately *not*
  on DRAW's dials (no IGDB data there, and that layout is measured to the pixel around the lever)
  or the Credits Roll (a full-screen cinematic).
- **`BillingModule`'s doc comment was badly stale** — it claimed no RevenueCat products existed
  and told a reader to flip debug to Real "once products exist". All five have existed since
  2026-08-12 and testers have transacted through this class. Corrected.

**Verified in the artifact, not the source:** `jarsigner -verify` → *jar verified*; the release
merged manifest reads `versionCode="9"`, `versionName="0.9.0"`, `autoVerify="true"`,
`host="continue-worker.gamestoplay.workers.dev"`, `pathPrefix="/g/"`; and the strings
`continue-worker.gamestoplay.workers.dev`, `/g/` and `autoVerify` are present inside the bundle's
own proto manifest. 32.8 MB at `app/build/outputs/bundle/release/app-release.aab`.

**166 app tests + 66 worker tests, 0 failures.**

🔴 **The Worker is still not deployed.** `npx wrangler deploy` from `worker/` — this environment
blocks production deploys. **It must run before `versionCode 9` is installed anywhere**, because
Android fetches `/.well-known/assetlinks.json` at *install* time; a build installed while that
path still 404s stays unverified until the next reinstall or app update, and nothing reports it.

---

## 2026-09-12 — production access granted, and SHARE was rebuilt around a friend loop

**Production track access is granted.** The Play gate that has been the project's top schedule
risk since day one is behind us, and attention moves from *can we ship* to *is it good*.

The first thing looked at with that freedom was **SHARE**, which Mikhil correctly named as the
weakest feature in the app. It was worse than "unimpressive" — most of it had never been built,
and the part that had been built was broken in a way nobody had noticed.

### What SHARE actually was, before today

`docs/02-PRODUCT-SPEC.md` §6 specifies **five** cards (CLEARED, HIGH SCORE, THE PILE, THE STACK,
YEAR IN GAMES) plus card themes as the primary cosmetic coin sink. What existed:

- **One card.** THE PILE. Reachable from exactly one button, on the pile screen.
- **It rendered in Roboto.** `ShareCardRenderer` was an `object`, so it had no `Context`, so it
  could not reach `R.font.chakrapetch_bold` and could not decode a single piece of key art. The
  most-screenshotted artifact the app produces was flat text on a flat rectangle, in a typeface
  used nowhere else in the app. For a Design Award entry that is the wrong thing to get wrong.
- **The link printed on it was fake.** `continue.app/pile` is a domain nobody has ever
  registered. Every card ever shared from this app carried a dead URL.
- **The intent carried no text.** `ACTION_SEND` with `EXTRA_STREAM` and nothing else — no
  `EXTRA_TEXT`, no Play link. So the "growth loop" in the spec was a PNG with a fake link
  painted into the pixels and no way for a recipient to act on it.
- **The Credits Roll had no share button.** The single moment a person most wants to tell
  someone — credits rolling on a game they've carried for two years — offered nothing.
- **Nothing was actionable between friends.** Every card was a one-way broadcast image.

The asymmetry worth naming: `ShareTargetActivity` already did the *hard* half — catch a shared
link, resolve it to a game, add it to the pile. The app could receive from TikTok but could not
send to another CONTINUE? user. There was no `VIEW` intent filter in the manifest at all.

### The friend loop

A game shared from CONTINUE? is now a real link that a friend's phone knows what to do with.

**Worker — `worker/src/routes/gameLink.ts`, two new routes:**

- **`GET /g/<igdbId>`**, optionally `?c=pick|dare|cleared`. A self-contained HTML landing page
  with Open Graph tags, so the link **unfurls in WhatsApp/Discord/iMessage with the game's real
  key art** — the artwork arrives without us drawing, uploading or hosting anything. No external
  CSS, no JS, no web fonts: an unfurl is fetched by a bot on someone else's infrastructure with
  its own timeout, and a page needing a second round trip is a page that sometimes unfurls blank.
- **`GET /.well-known/assetlinks.json`** — Digital Asset Links, so Android will hand `/g/*` URLs
  straight to the app. Answered **before rate limiting**, deliberately: the clients are Android's
  install-time verifier and Google's crawler, and 429-ing *them* doesn't slow an attacker down,
  it silently un-verifies App Links for whoever was installing.

**Cost: effectively zero.** `/g/<id>` reuses the **same `detail:<id>` KV key** as `GET
/games/<id>`, so a share link for a game anyone has already opened costs **no KV write at all** —
which matters because KV writes (1,000/day) are the binding quota in the whole stack.

**App:**

- `ShareTargetActivity` gained an `autoVerify` App Links filter for `https://<worker>/g/*` and a
  `continueapp://g/<id>` fallback. It lands on the **share sheet**, not MainActivity, so the
  friend loop inherits the entire existing degradation ladder instead of growing a second one.
- `ShareTargetViewModel.resolveGameId()` skips resolution entirely — a link minted by CONTINUE?
  already names its game. It checks **Room first and the network second**, the reverse of the
  obvious order, because the most likely place to tap a friend's link is a group chat on the bus.
- `core/share/ShareLinks.kt` is the single place that builds *and* parses these links. That is a
  security boundary, not a formatting helper: the https form is exported to the whole internet
  and the custom scheme can be fired by any app on the device. **15 tests**, mostly about what it
  refuses — other hosts, `http`, suffix hosts like `<host>.evil.example`, ids past the Worker's
  own `\d{1,9}` bound.

### The cards are now actually designed

`ShareCardRenderer` is a `@Singleton` with a `Context` and Coil's **shared** `ImageLoader` — the
app's existing one, from `ContinueApplication`'s 192 MB disk cache, so a cover the pile just
displayed renders into a card **instantly and offline**. Three cards at 1080×1350:

- **CLEARED** — key art bleeding from the top edge and dissolving into the background, cover
  thumbnail, title (shrunk a step at a time before it's allowed to wrap), hours, all-time rank.
  **Shared from the Credits Roll**, where a share button always belonged.
- **THE PILE** — a 4×4 **wall of box art** from the backlog, longest games first, washed down
  under the headline. The count is abstract; a wall of games you own and haven't finished is the
  joke landing.
- **HIGH SCORES** — an actual arcade high-score table in JetBrains Mono, `1ST`/`2ND`/`3RD` in
  coin/white/hot. Shared from the HIGH SCORES header, hidden while editing.

All three carry Chakra Petch, arcade corner brackets, and a CRT scanline pass drawn **last, over
everything** so the card reads as one screen rather than a texture on part of one.

### One owner for every outbound intent

New `core/share/ShareLauncher.kt` owns every `ACTION_SEND` in the app. Same lesson as the HAPTICS
bug: *a rule enforced at the call sites holds only at the call sites somebody remembered.* The
rule here is **every share carries a real, tappable link**, and routing all of it through one
class makes a link-less share something you'd have to go out of your way to write. It also fixes
a silent bug — `ClipData` is now set alongside `EXTRA_STREAM`, without which a handful of target
apps receive a share with no image at all and no error.

Recommending a game from the pile's long-press menu (**RECOMMEND IT** / **DARE THEM TO FINISH
IT**) is deliberately a **text** share, not a rendered card: the link's own unfurl supplies the
artwork, it fires instantly with no render, and the recipient gets something *tappable* rather
than a JPEG of a recommendation.

### Also fixed along the way

The share sheet used to say **"added to your pile" for a game that was already there** — the
duplicate check existed and correctly skipped the insert, it just never told anyone. There's now
an `AlreadyInPile` rung that says so and, unlike the "added" confirmation, doesn't auto-dismiss:
"added" confirms something you asked for, "you already have this" is new information.

### Verification

**166 app unit tests, 0 failures** (was 150). **65 Worker tests, 0 failures** (was 50).
`tsc --noEmit` clean. The landing page was rendered and inspected as real HTML, not just tested.

⚠️ **Nothing in this round has been on a device.** Same standing caveat as 2026-08-28.

### 🔴 The one blocking input: the Play App Signing SHA-256

`assetlinks.json` ships the **upload key** and the **debug key** fingerprints, both read locally
from the keystores. The third — **Play App Signing** — cannot be read from this machine, because
Google re-signs every upload with its own key, and *that* is the certificate real users' installs
carry. Read it from **Play Console → Test and release → Setup → App integrity → App signing key
certificate → SHA-256**.

Until it's filled in, shared links still *work* — they open the landing page and the Play listing
— they just don't open the app for anyone who installed from Play.

Two things about this that are easy to get wrong:

1. **The failure is silent.** Wrong fingerprint → Android opens a browser → nothing logs
   anything, anywhere. That is why `worker/test/gameLink.test.ts` carries a `PENDING:` test that
   currently asserts the placeholder is still there: when the real value lands the test goes red
   and you flip `false` to `true`. A red test is a far more reliable reminder than a TODO.
2. **Order matters.** The Worker must be deployed with the correct fingerprint *before* an app
   build carrying `autoVerify` is installed, because Android verifies at **install time**.

### Not done, and deliberately

- **The Worker is not deployed.** Held so it goes out once, with the real fingerprint, in the
  right order. Deploy with `npx wrangler deploy` from `worker/`.
- **Card themes** — the specced cosmetic coin sink (`HOLOGRAPHIC FOIL`, `ARCADE MARQUEE`, `NEON
  NOIR`) — still don't exist, so coins still have no cosmetic sink. Now much cheaper to build
  than it was this morning: the renderer has a palette and a chrome pass to vary.
- **THE STACK** and **YEAR IN GAMES** cards remain unbuilt.

---

## 2026-09-07 — the gate is passed, and the fourth round came back clean

No code changed this session. What changed is the state of the world, and it resolves the
largest open warning in this document.

**`versionCode 8` is on the Closed track and has run there roughly five days.** The 2026-08-28
entry below is stamped, correctly, with "nothing in this round has been on a device" — `adb
devices` was empty for that whole session. That warning is now **discharged, by testers rather
than by a cable**. The feedback that came back contained **no bugs and no defects**: only
suggestions for features that were already on the later-work list.

That is a meaningful result and worth stating plainly, because the two rounds before it each
hid a phone-only layout break the tablet absorbed. The specific things flagged as highest-risk
on 2026-08-28 — the brand-new **STATS** screen, the first use of Material 3's `DatePicker` in
this app, **PILE's rebuilt three-group filter section** (the exact shape of both previous
breaks), **DRAW's GENRE dial** and **HIGH SCORES edit mode** — have all now rendered on real
phones without a report. The haptics toggle likewise: it was the loudest complaint of the
previous round and nobody raised it again.

**What this does not cover:** no tester completed a paid transaction, so the purchase flow is
still unexercised end to end on a real Play account. Everything else about the paywall — its
layout, its copy, the CTA fixed on 2026-08-26 — has been seen.

**Track state — and a correction.** The Closed track ran **`4` → `6` → `7` → `8`**. Entries
above this one say `versionCode 7` was "never uploaded to any track"; **that is wrong** and has
been corrected here and in `docs/09-PENDING-INPUTS.md`. Mikhil confirms it went to Closed, and
RevenueCat corroborates it independently: Google's pre-launch device farm minted `0.7.0`
installs on 2026-08-26 (06:31–07:10 UTC) and `0.8.0` installs on 2026-08-28 (18:54–19:27 UTC),
and the farm only runs on upload. **Four builds reached testers during the closed test, one per
feedback round** — the single most useful fact for the production-access form.

**The 14-day gate is passed.** Clock started 2026-08-19; earliest apply date was 2026-09-02;
today is 2026-09-07, 19 days in.

### Reading the tester population out of RevenueCat

Mikhil's estimate was ~15 (Play Console exposes no live opted-in count, only the met/not-met
bar). The RevenueCat records support something firmer. Applying the device-farm signature from
the 2026-08-19 entry — Android API **30** exactly, `first_seen_at` equal to `last_seen_at` to the
millisecond, US/BR, clustered on upload days — removes **48 of the 78** customer records and
leaves 30 real installs. Of those:

- **21 are on `0.8.0` and were last seen between 2026-09-03 and 2026-09-07**, one of them within
  the hour.
- **No non-farm install has a `first_seen_at` later than 2026-08-21.** This is the load-bearing
  observation: a reinstall mints a *fresh* anonymous id, so churn would show up as September
  first-seens. There are none. Every currently-active install has been continuously present
  since 19–21 August, ~18 days.
- One of the 21 (`first_seen` 2026-08-12, pre-dating the closed test) is Mikhil's own device.

This is install-level evidence, not Play's opt-in metric, so it doesn't *prove* the opt-in count
— but it corroborates "comfortably above 12, continuously, for well over 14 days" far better
than an estimate does.

### Testers did complete purchases — and the billing integration is proven

The 2026-08-28 warning that the purchase flow was unexercised is **wrong as of this session**.
Sampling 6 active testers found **5 with completed transactions**, all `store: play_store`,
all `environment: sandbox` (Play licence-tester cards — no real money):

- **Four bought the one-time lifetime upgrade** (`prodbf1ac2c0d6`) on 19–20 Aug — three MU, one
  MY. Each shows the `pro` entitlement **active**, so the entitlement gate is confirmed working
  end to end against a genuine Play transaction, not a fake.
- **One bought the monthly subscription** (`prodb3fdae1092`) **five separate times across 20–25
  Aug**. Sandbox subscriptions expire in minutes, so that's a tester deliberately re-running the
  flow on five different days.

⚠️ **The sandbox revenue figures are artefacts** ($9.99, $19.95, $27.93 — the last one for a
"monthly" sub). Never quote them, in the Play form or on Devpost. Same standing rule as the
customer count.

### The Play service account was already configured — the docs were stale

`docs/09-PENDING-INPUTS.md` listed "Play Console → RevenueCat service account" as **Not
started**. It is done: `validate-app-credentials` on `app28ef647c38` returns `status: valid`
with all three checks passing (validate subscription purchases · read the in-app product
catalogue · read the subscription catalogue and base plans). The traffic said so first — those
tester purchases carry real `GPA.…` order ids and correct expiry tracking, which RevenueCat
cannot produce without the credential. Another instance of the standing lesson: **verify config
with traffic, not with what a doc claims about it.**

### The production-access application is drafted

**`docs/14-PRODUCTION-ACCESS.md`** — paste-ready answers to every question Google's own support
page says the form asks, plus a pre-submit checklist, an evidence table of all ~21 tester
reports and what shipped for each, and the ordered list of what must happen before the
production *release* (as distinct from the application). **Not yet submitted.**

Three things worth carrying out of writing it:

1. **Applying is not publishing.** The form unlocks the production track; it releases nothing.
   So the outstanding EEA/UK/CH exclusion — decided 2026-08-24, still not done — blocks the
   production rollout, not the application. Doing it *now* would be actively harmful: country
   availability is per track, and excluding a country a current tester lives in drops them and
   restarts their 14 days, mid-review.
2. **The engagement question has a truthful answer that is also a strong one.** The worry
   recorded on 2026-08-24 was that 2-3 minutes a day reads as thin usage. It only reads that way
   if you assume session length is the metric. For a backlog manager it inverts: the app exists
   to stop people scrolling lists instead of playing, so a user who stays inside it for forty
   minutes is a *failure* case. That argument is in §2 of the new doc and it needs no spin.
3. **Do not put RevenueCat's numbers anywhere near this form.** It currently reports 77 new
   customers and 77 active users over 28 days. The app configures `Purchases` with no
   `appUserID`, so a "customer" is an anonymous install — every reinstall mints another — and
   the figure also absorbs Google's pre-launch device farm. Quoting 77 alongside "~15 testers"
   invites the one question you don't want a reviewer asking. Same standing rule as the Devpost
   writeup; see the 2026-08-19 entry on reading RevenueCat.

**Timeline.** Review is "usually 7 days or less". Submitting 2026-09-07 puts a decision around
2026-09-14, leaving ~2 weeks for the production release and its own separate review before the
2026-09-30 deadline. Workable, with no room for a second attempt. The hedge in
`docs/01-PLAY-STORE-CRITICAL-PATH.md` still stands: an **open testing** track produces a public
Play URL without production access, and is worth switching to if the review is still pending
around 2026-09-15.

---

## 2026-08-28 — fourth closed-test feedback round: eleven items

Ten from testers, one from Mikhil. All eleven are in code; **none has been on a device** (see the
warning at the top).

### 1. Haptics didn't turn off ✅

Reported against both the STACK flick and the DRAW lever, and true of every haptic in the app.
`UserPreferencesRepository` stored the switch, `ProfileScreen` drew it, `Haptics` carried a
comment saying "global on/off is a settings toggle at the call site" — and **not one of the five
call sites checked it**. Every screen did `remember { Haptics(context) }`, so each had its own
engine and none had ever seen the preference.

`Haptics` is now an injected `@Singleton` that collects `isHapticsEnabled` into a `@Volatile`
mirror and short-circuits every effect itself, reached through a `LocalHaptics` composition
local provided in `MainActivity`. The toggle is unfalsifiable now: there is no way to fire an
effect that skips the check, because there is no other way to obtain a `Haptics`.

**Worth generalising:** a setting enforced "at the call site" is a setting enforced nowhere. Put
the check inside the thing being switched off.

### 2. The clear-a-game coin reward could be farmed ✅

Clear a game (+5), move it back to THE PILE, clear it again. Forever. The existing guard
(`if (entry.state != PileState.COMPLETED)`) only ever stopped the *same* Credits Roll paying
twice, which was never the problem.

The reward is a property of the **game**, not of the transition, so it's now claimed against
`clear:<gameId>` in a new `CoinLedger.earnOnce` — check and write in one `edit` transaction, so a
double-tap can't get through either. A genuine replay still gets the whole cinematic; it just
reads `ALREADY PAID FOR THIS ONE` rather than promising coins the balance won't show, because
the Credits Roll now prints `state.coinsAwarded` instead of a hardcoded "+5 COINS".

### 3. Backdating — logging games cleared before the app existed ✅

Two entry points, one dialog (`core/ui/GameDatesDialog.kt`):

- **DISCOVER → long-press a result → ALREADY CLEARED** → pick started/cleared dates → the game
  goes straight into CLEARED. `addedAt` is backdated too, so "time in the pile" and the RECENT
  sort read as history rather than as something added today.
- **PILE → a game → EDIT DATES** → for one already in the pile.

Future dates are blocked *in the picker* rather than validated after; a cleared date before a
started date disables SAVE with the reason spelled out.

One real trap handled in `CalendarDates`: Material's `DatePickerState.selectedDateMillis` is
**UTC midnight** of the picked day, while every timestamp in `pile_entries` is a local instant
formatted in the device's zone. Feeding one straight into the other is an off-by-one-day bug that
only appears for users in the wrong half of the world. A picked day becomes **local noon**, which
no zone or DST transition can round onto a neighbouring date.

**Backdated clears pay no coins and burn the reward key** — see item 2. Logging a hundred old
games in a minute would have been a faster faucet than the loop just closed.

### 4. Filters overhauled, and DRAW now uses the same ones ✅

Reported as "filters not overhauled — better filters, like having genres (horror, thriller, fps,
multiplayer); not consistent across categories and the draw button". Three separate bugs under
one complaint:

1. **The vocabulary was IGDB's raw genre strings**, which contain no "horror" (that's a *theme*),
   no "FPS" (that's "Shooter") and no "multiplayer" at all — because the Worker was **dropping
   `game_modes` on the floor** (item 8). New `core/util/GameTaxonomy.kt` defines 22 facets matched
   across genres, themes **and** game modes at once, and PILE's chips, **DRAW's new GENRE dial**
   and the new STATS screen all read the same function. That is what "consistent across
   categories and the draw button" actually required.
2. **The chip set was built from the current tab**, so it changed every time you switched tab.
   It's now built from the whole pile and is therefore stable; the *counts* on each chip describe
   the tab you're looking at.
3. **A chip matching nothing vanished**, taking with it the only control that could turn off a
   filter that was silently emptying the tab. Zero-count chips now render disabled with a `0` —
   and stay tappable if they're the one that's on.

Also: multi-select with **OR inside a group, AND between groups**, a CLEAR FILTERS button, and
`PileSort`'s chip row generated from the enum — which surfaced that **`RATING` was implemented as
the identity function** ("rating not denormalized onto PileEntryWithGame yet") and
**`RELEASE_DATE` sorted by date *added***. Both columns were already on `games`; the projection
just never selected them. Fixed, and both are now offered.

DRAW's relaxation ladder is now platform → genre → **time last**, because time is the machine's
premise: a draw that quietly ignores "30 minutes" hands you a 60-hour RPG, which is the one
failure the user can't work around.

### 5. STATS ✅

New screen, reached from PILE's header (`feature/stats/`). Scope selector (EVERYTHING or any one
state) over: THE SPREAD (every state as one proportional bar), GAMES/HOURS/SPAN tiles, a
**DIVERSITY 0–100** meter, and bars for genre & mood, length, platform and release decade.

Diversity is **normalised Shannon entropy over the facet mix**, not a category count —
19 shooters and 1 puzzle game is not a varied pile, and a count says it is. Pinned by
`PileStatsTest`.

Drawn entirely with layout (`Box` width fractions, a `Row` of weights) rather than a chart
library: no new dependency in a public repo, and it reads to TalkBack as ordinary text.

### 6. "Ghost of Tsushima has no banner" — the share target was writing a stub ✅

Not an IGDB gap. `/games/75235` returns full 1920x1080 key art, three genres, five themes, two
platforms and real playtimes — verified against production this session. The app had never asked.

`ShareTargetViewModel.addCandidate` built a `GameEntity` inline from a resolve candidate, which
carries only id/name/cover, and wrote `null` for the background, the release date and all three
playtimes plus empty arrays for genres, themes and platforms — **as an unconditional REPLACE**,
so sharing in a game you already had *downgraded* a complete row to the stub. That single line is
also why a shared-in game showed "ENDLESS" for its length and matched no filter anywhere.

New `core/data/GameCacheRepository.kt` owns every write to `games`:

- `cacheMinimal` never overwrites an existing row, and stamps the stub `cachedAt = 0` so it is
  first in line for the next refresh. It does **no** network work — the share sheet says "added"
  the moment it returns, and a detail fetch there would put a round trip between the tap and the
  confirmation. `hydrateInBackground` is the non-blocking half, on a process-scoped coroutine
  because the share Activity finishes and takes `viewModelScope` with it.
- `refreshStaleGamesOnce()` runs once per launch and repairs up to **50 games in one request**
  via the new `/games/batch`. Rows cached before `CACHE_EPOCH_MILLIS` are refreshed on sight —
  the client-side twin of the Worker's `CACHE_VERSION`, because a shape change that only affects
  *new* rows leaves every existing user on the old shape forever.

### 7. Search: "spiderman vs spider-man" ✅

`search "spiderman"` returns **4** results led by *Questprobe featuring Spider-Man* (1984);
`search "spider-man"` returns **20** led by the ones anybody means. `streetfighter` returns
**zero**. IGDB tokenises on punctuation, so a query with its separators left out is one unknown
token.

Two fixes:

- **Respelling** (`OfflineGameIndex.respell`). The dictionary that fixes this was already on the
  device: the offline index is keyed by *normalized* name — spaces kept, punctuation gone — so
  `Spider-Man` is stored as `spider man`, and squashing that key's spaces out gives back exactly
  what the user typed. No word list and no heuristic segmentation; the only respellings it can
  produce are real IGDB titles. Fires only for a single-token query of 6+ characters, and the
  respelled search is run *alongside* the original, never instead of it, with a note on screen.
- **Re-ranking** (`core/util/SearchRanking.kt`). Coarse tiers (exact / prefix / word-boundary /
  contains) with a tiebreak on how much of the title the query covers, and stable sorting so
  IGDB's own order survives inside a tier.

### 8. "Blasphemous appears 2 times, and each can be added twice" ✅

Two different things, so two fixes. `SearchRanking.dedupe` removes real duplicates — the same id
twice (which would also *throw* in a keyed `LazyColumn`, and is guaranteed once two queries are
merged) and the same squashed name released the same year. And search rows now show the **release
year**, because IGDB carries four genuinely different games called *Spider-Man* and merging those
would be the worse bug; without a date they were four identical rows and the app looked broken.

### 9. Covers in search results ✅

Asked for directly. Each row now carries 40dp box art, and a `year · length · platform` metadata
line — the same initial-underneath trick `GameCard` uses, so a row whose art hasn't arrived still
reads as a game rather than an empty slab.

### 10. DISCOVER SHOW MORE ✅ (it doesn't strain the API)

The tester's caveat was "if it would strain the api, drop the idea". It doesn't, and that's a
property of the design rather than luck: **a rail's contents don't depend on who is asking**, so
page 2 of TRENDING is one KV entry shared by every user, not one per user. Three pages (60 games)
per rail, clamped in `security.ts`, after which the button removes itself. Search paging was
deliberately *not* added — that key **would** be per-user.

### 11. (Mikhil's) Rankings can be edited ✅

YOU → HIGH SCORES → **EDIT** turns on per-row up/down/drop. Up-and-down buttons rather than
drag-to-reorder: a drag handle inside a scrolling `LazyColumn` fights the scroll on a phone, and
a leaderboard gets corrected by a slot or two, not rearranged wholesale.

The subtle part is buckets. `PairwiseRanker` binary-searches *within* a bucket and assumes each
bucket is one contiguous run of positions, so a game dragged past a boundary **adopts the bucket
it lands in** — otherwise every later automatic placement would be quietly wrong. Positions are
renumbered densely in one transaction rather than swapped, because they genuinely arrive with
gaps (`deleteByGameId` leaves one, `shiftDown` only pushes downward), so "swap the two numbers"
would be right only by luck.

Dropping a game from the leaderboard **leaves it in CLEARED** — it is not REMOVE FROM PILE, and
the confirmation says so, because the app already has a destructive action that this could be
mistaken for.

### 12. The two remaining dead controls, and the migration landmine

Found by auditing for the *shape* of the haptics bug rather than by a report. All three are
things that looked implemented from every angle except the one that counts.

**CLIPBOARD DETECTION was a switch wired to nothing.** `UserPreferencesRepository` stored it and
`ProfileScreen` drew it; **no other file in the app read it**, and there was no `ClipboardManager`
anywhere. Now built (docs/02-PRODUCT-SPEC.md §2d): on every foreground, if the setting is on, the
clipboard text is checked against the IGDB name index **already in the APK** — offline, no Worker
call — and a confident hit becomes a dismissible banner.

The gate is deliberately mean, because this is the one surface that interrupts unprompted:
`isPlausibleGameName` rejects links (those belong to the share target, which resolves them
properly), emails and handles, one-time codes and phone numbers, multi-line text, anything over
60 characters or 8 words, and anything with no letters in it. Then it needs an offline match at
`CONFIDENT_ENOUGH` — the same bar the share target uses to skip its chooser entirely. A game
already in the pile is silently swallowed rather than offered. `ClipboardNudgeTest` pins every
rejection.

Two details that aren't obvious:
- **Off by default, and that's not just a preference.** From Android 12 the system toasts
  *"CONTINUE? pasted from your clipboard"* whenever an app reads clipboard content it didn't
  write. Reading unprompted would put that toast on every single app open. Behind an off-by-
  default switch, only people who asked for the feature ever see it — and the toggle's new
  description says so, in as many words, rather than letting the toast be the user's first hint.
- **A dismissal is persisted**, not held in memory, so "no" survives a relaunch. Re-asking on
  every foreground is exactly the "irritation when it's wrong" the spec warns about.

**Hours-per-week was a knob with no handle.** `PileViewModel.setHoursPerWeek` existed and had
**no callers**, so "FINISHED BY 2029" — the headline of the app's signature bar — was computed
from a hardcoded 6 h/week for everybody. `TimeBudgetBar` had carried an unused `onExpand` hook
since it was written, so its chevron had never even rendered. It now opens a slider, the value is
persisted, and the projection updates **as you drag** through the same `finishByCopy` the bar
itself calls — one copy of the arithmetic, not two.

Extracting that function immediately surfaced a real bug it had been hiding: months were passed
to `Calendar.add` as an `Int`, and a large enough pile at one hour a week **overflows and wraps
the date backwards**, printing a finish year that has already happened for the most hopeless pile
in the app. Now capped with an explicit "FINISHED BY NEVER". `FinishByCopyTest` covers it.

**Room had no migration path and no fallback.** `AppDatabase` was `version = 1` built with a bare
`.build()`. Room stores a version in every device's database file; open one whose stored version
is older than the code expects with no `Migration` covering the gap and it throws
`IllegalStateException` — **on launch, for every user who upgrades**. The reason that ships so
easily is that a *fresh install* works perfectly: only an upgrade, which is what every tester gets
from Play, hits it. This release routed around the landmine deliberately (every change went into
DAO projections and queries, never the schema), but the next added column would have found it.

Now: `DatabaseSchema.VERSION` is the single declaration, `AppMigrations.ALL` is the (currently
empty) chain, `DataModule` calls `.addMigrations(...)`, and **`AppMigrationsTest` fails the build**
if the version moves without a migration to service it, or if an old schema JSON is deleted. The
guard was verified by temporarily bumping the version to 2 and confirming the suite goes red —
a test that has never failed is a test you don't know works.

`fallbackToDestructiveMigration()` is banned in writing in `DatabaseSchema`. It makes the crash
go away by deleting the database — that is, by erasing the user's entire pile, every clear date
and every ranking, which is the one thing in this app that can't be re-fetched.

One thing the first cut of the clipboard nudge got wrong, caught before it shipped: the read was
gated in the **ViewModel**, which meant the clipboard was already read by the time anything
checked the setting — and on Android 12+ the system paste toast fires on the *read*. Every user
would have seen "CONTINUE? pasted from your clipboard" on every app open regardless of the
switch. The gate now sits in the composable (`DisposableEffect` keyed on the setting, so with it
off no observer is even registered) with the ViewModel check kept as defence in depth. **A
privacy gate has to sit at the point of access, not at the point of use** — the same lesson as
the haptics bug, one layer further out.

**Left alone on purpose:** `loadFreePlayAd()` is implemented against a real ad unit and called by
nothing. That's dead *code* awaiting a feature, not a dead *control* — no user can touch it and
be lied to — and the real AdMob units can't fill until the app is live on Play anyway, so FREE
PLAY mode is correctly a post-launch item.

### Worker changes (deployed and verified against production)

- `toDto` folds **`game_modes` into `tags`** alongside themes. It had been requested from IGDB in
  `GAME_FIELDS` since day one and then discarded — which is why nothing could tell a multiplayer
  game from a single-player one, and why `MoodMapper`'s CHAOS rule (which looks for "multiplayer"
  in exactly that list) had never once matched.
- **`/games/batch?ids=`** — up to 50 games in one request, **uncached on purpose**: the id set is
  user-shaped, so a cache key would burn the 1,000 writes/day budget. Ids are parsed to `Number`
  before the query is built; malformed ones are dropped rather than failing the request.
- **`?page=0..2`** on every rail, clamped in `security.ts` (`sanitizePage`) — an uncapped page is
  an unbounded-KV-key hole.
- `CACHE_VERSION` **v4 → v5**, because `tags` changed shape.

Verified live, not just deployed: `/games/75235` now returns `"Single player"` in `tags`,
`/games/batch?ids=75235,26820,1020,abc,-5` returns exactly the three real games with full
metadata, and `trending?page=9` clamps to page 2 rather than erroring or paging forever.

### Verified

- `worker`: `npm test` — **50 tests, 0 failures** (5 new, covering page/id sanitisation).
- `app`: `./gradlew testDebugUnitTest` — **150 tests, 0 failures** (was 84). New:
  `GameTaxonomyTest`, `SearchRankingTest`, `PileFilteringTest`, `PileStatsTest`,
  `ClipboardNudgeTest`, `FinishByCopyTest`, `AppMigrationsTest`, plus respell cases in
  `OfflineGameIndexTest` (run against the **real shipped index**) and reorder cases in
  `PairwiseRankerTest`.
- `./gradlew bundleRelease` — **`versionCode 8` / `0.8.0` built and signed** at
  `app/build/outputs/bundle/release/app-release.aab` (32.8 MB, `jarsigner -verify` clean, signer
  cert valid to 2056). `versionName` read back **out of the bundle's own manifest**, not trusted
  from the Gradle file. Supersedes `versionCode 7`, built 2026-08-26; the Closed track is still
  on `versionCode 6` **at the time of writing**.
  > **Corrected 2026-09-07:** both halves of that last sentence went out of date immediately.
  > `versionCode 7` *was* uploaded to the Closed track, and `8` followed it the same evening.
  > The Closed track ran `4` → `6` → `7` → `8`. See the 2026-09-07 entry.

### NOT verified — read this before assuming anything works

**`adb devices` was empty for this entire session, so nothing below has been seen on a screen.**
The highest-risk items, in the order worth tapping through:

1. **STATS** — a brand-new screen. Never rendered.
2. **`GameDatesDialog`** — the first use of Material 3's `DatePicker` anywhere in the app;
   its colours are themed by hand and have never been looked at.
3. **PILE's filter section** — three chip groups where there was one row. On a phone this is the
   exact shape of the 2026-08-14 and 2026-08-15 layout breaks, both of which a tablet hid.
4. **DRAW's new GENRE dial** — one more `DialSection` in a region that already scrolls, on a
   screen whose lever must never be compressed.
5. **HIGH SCORES edit mode** — three icon buttons appended to a row that already holds a rank, a
   cover and two lines of text.
6. **Haptics actually going quiet** with the toggle off — the whole point of item 1, and only a
   thumb can confirm it.
7. **The clipboard nudge** — turn CLIPBOARD DETECTION on, copy `Hollow Knight Silksong`, background
   and reopen the app. Check the banner appears, that ADD works, that dismissing it stops it
   coming back, and what Android's paste toast actually looks like on the test device.
8. **The hours-per-week slider** — a Material 3 `Slider` inside an `AlertDialog`, hand-themed.

One layout risk was caught by arithmetic rather than by a screen, and is worth recording because
it is the *third* instance of the same shape: STATS was first put in PILE's header as a fourth
icon button, which on a 360dp phone is ~350dp of non-weighted children inside ~328dp of usable
width. `Row` measures non-weighted children first and hands the leftovers whatever is left, so
the last button would have been squeezed — exactly the 2026-08-14 tabs break and the 2026-08-15
DRAW-button break, both of which a tablet absorbed. STATS is reached from the filter panel and
from **YOU → SEE THE FULL BREAKDOWN** instead, and the header row is byte-for-byte as wide as the
one testers have already used. **Count the widths before adding anything to a shared chrome row.**

---

## 2026-08-26 — third closed-test feedback round: the STACK crash, and three fixes

The first round to contain a crash, and the first one where a tester handed over a reliable
reproduction. Four items, all fixed. One item from the previous round is also closed.

### 0. Closed: "games added from DISCOVER never reached the pile"

Left open on 2026-08-23 with no reproducible root cause. There wasn't one — the games *were*
written, into the **RETIRED** tab, and the tester was looking at THE PILE. Not a lost write, and
nothing to fix in the write path. The robustness and visibility changes made on 2026-08-23 were
worth keeping regardless, but they were not the fix, because there was nothing to fix.

### 1. The STACK crash — one missing `remember` key

Three testers hit it; Play Console carries it as two issues (3 users affected and 1), which are
the same bug landing on differently-sized lists:

```
java.lang.IndexOutOfBoundsException: Index: 7, Size: 1
java.lang.ArrayIndexOutOfBoundsException: length=2; index=2
  at ...PileStackView$6.invoke (PileStackView.kt:251)
```

Line 251 was the caption's `entries[anchor]`. `anchor` came from

```kotlin
val anchor by remember { derivedStateOf { position.value.roundToInt().coerceIn(0, lastIndex) } }
```

`remember` with **no key**, so the lambda closed over `lastIndex` from the *first* composition
and kept clamping to that number for the life of the composable. Everything else follows:

- **List shrinks** (switch to a shorter tab, a filter cutting the results, a game moving out of
  the current state): `anchor` stays clamped to the old, larger ceiling, and `entries[anchor]`
  indexes past the end of the new list. `Index: 7, Size: 1` is exactly "scrolled to card 8 of 8,
  then switched to a tab holding one game" — the tester's RETIRED → WANTED reproduction.
- **List grows** (the pile filling in after the first frame): `anchor` is capped *below* the real
  end, so the cards freeze while `position` — which the drag handler correctly clamps against the
  *live* `lastIndex`, and which the per-card haptic reads unclamped — keeps running underneath.
  That is the other half of the tester's report verbatim: the last card leaves, nothing is drawn,
  and the phone still buzzes once per card as you keep flicking at an empty stack. The cards
  weren't gone, they were transformed to a depth well past the exit and therefore invisible,
  which is why flicking back down brought them all back.

The two `LaunchedEffect`s that pull `position` back into range (`resetKey` and `lastIndex`) can't
save it: they run *after* the composition that swapped the list, so composition has to survive a
position belonging to the previous list on its own.

Fixed by making every read of the position-as-an-index clamp against the list being indexed right
now — `remember(lastIndex)`, a clamped `positionProvider` so the visuals can't spend a frame past
the end either, and the tap handler clamping against `currentEntries` rather than the list its
`pointerInput` was keyed on. The clamp is now a named pure function, `stackAnchor(position,
lastIndex)`, precisely so it can be tested without Compose: `StackAnchorTest` restates both Play
Console stack traces as arithmetic.

**Worth generalising:** `remember { derivedStateOf { … } }` silently freezes every non-`State`
value the lambda touches. It is only safe when the lambda reads *nothing* but snapshot state.
`lastIndex` is a plain `Int` derived from a parameter, and that was enough.

### 2. No way to remove an accidentally added game

RETIRED was the only exit, and it is not the same thing — it's a *decision* ("letting this one
go") that PROFILE counts toward the "retire 10 games" trophy. A game shared in by mistake was
never in the pile in any meaningful sense.

`REMOVE FROM PILE` now sits at the bottom of the pile action menu under its own `OR` heading, in
`AccentHot`, deliberately outside the `MOVE TO` list so it can't be mis-tapped as a sixth
destination. It confirms in a dialog that names the game and points at RETIRED as the thing they
probably meant, with the safe choice in the default position.

It sweeps `stack_members` and `rankings` as well as `pile_entries`: neither has a foreign key
back to the pile entry, so without that the game keeps appearing inside its stacks and holding
its slot in RANK with no row behind it. The cached `games` row is deliberately **kept** — that's
shared cache, not user data, and dropping it would force a network round trip if the same game is
added back or shows up in DISCOVER.

### 3. The paywall's lifetime CTA said "INSERT COIN"

Next to the monthly tier's "SUBSCRIBE". Worse than inconsistent: "INSERT COIN" is the app's label
for *earning* a coin by watching a rewarded ad (`DrawGateScreen`), so on the one screen that takes
real money it pointed at the wrong mental model entirely. Now `PURCHASE ▸`, keeping the caps and
the chevron the rest of the paywall uses.

### 4. TikTok shares dead-ended on a chooser full of wrong games

TikTok's oEmbed *does* return something — but it returns the video's **caption**, which is written
for the algorithm, not a title. Read off production on 2026-08-26:

| shared text | resolvedTitle | what the app showed |
|---|---|---|
| `https://vm.tiktok.com/ZMSkFqPxY/` | `Пользуйтесь на здоровье👍 #юмор` | blank field (nothing matched) |
| `Check this out https://www.tiktok.com/@…/video/…` | `Check this out` | *Check-In*, *Check Inn*, *Wai-wai Check!* — all 0.425 |

The second row is the failure mode: a caption with no game in it still scores just high enough to
fill the chooser, so the tester has to notice the list is wrong, back out, and type the name
anyway. TikTok is the one source where a near miss is worse than no guess.

So: a share carrying a tiktok.com link now skips the middle rung. A **confident** hit is still
offered as one tap — that's strictly better than a blank field and it does happen — but anything
short of confident goes straight to an empty manual-entry field carrying its own copy: *"TikTok
doesn't hand over a video's title, so CONTINUE? can't match this one for you — type the game's
name."* That replaces "Couldn't match that automatically", which reads as a fault the app might
fix on a retry rather than as how that source works.

Detection is a **host-label** match, not `contains("tiktok.com")` — the same distinction
`worker/src/security.ts` already makes, for the same reason it's easy to get wrong. `vm.tiktok.com`
and `vt.tiktok.com` count; `tiktok.com.example.org` and `eviltiktok.com` don't. `TikTokLinkTest`
covers both directions. Screenshot shares deliberately don't take this path — OCR text stands on
its own merits whatever app it was captured in.

Separately, the Worker's `suggestion` now goes back through `TitleParser` on the app side before
it reaches the field. The Worker documents it as never containing a URL and it never has in
testing, but this field is the one place in the app where being wrong costs the user a
select-all-and-delete before they can type, so the guarantee is now enforced on the side that
suffers if it breaks. The re-clean is a no-op on already-cleaned text (every rule in the table is
a removal, so it's idempotent).

### Verified

`./gradlew testDebugUnitTest` — **84 tests, 0 failures**, including 12 new ones across
`StackAnchorTest` (5), `ShareResolutionStateTest` (5) and `TikTokLinkTest` (2).

**Not device-verified.** `versionCode 7` / `0.7.0` **is built and signed** —
`./gradlew bundleRelease` succeeded clean, `jarsigner -verify` reports "jar verified" with the
same signer (valid to 2056), output is `app/build/outputs/bundle/release/app-release.aab`
(32.4 MB). **Not yet uploaded to any track** — testers are still on `versionCode 6`. The STACK
crash fix is the reason to make the upload the next action rather than batching more work onto
this bundle — it is the only known crash in the app, three testers have hit it, and it is in
PILE's *default* view.

### Also worth having on the record

Confirmed good on glass this round, from tester feedback: STACK's swipe hint, DISCOVER's
add-confirmation banner, and the new playtime labels. All three were 2026-08-23 changes shipped
blind in `versionCode 6`; all three landed.

---

## 2026-08-23 — second closed-test feedback round

Five items from testers. Four are fixed and covered by tests; the fifth is still open and is
the most important thing in this entry.

### 1. "Minecraft says 900 hours" — IGDB's playtime data, and what the app prints

Not a mapping bug. IGDB's `game_time_to_beats` genuinely returns
`hastily 98 / normally 956 / completely 20417` for *Minecraft: Java Edition*, and the app was
printing `normally` verbatim everywhere. Read off the production Worker on 2026-08-23:

| game | hastily | normally | completely |
|---|---|---|---|
| Portal 2 | 4 | 9 | 28 |
| Stardew Valley | 50 | 90 | 208 |
| Skyrim | 25 | 109 | 201 |
| Baldur's Gate III | 128 | **132** | 5650 |
| Counter-Strike | 9 | **15** | 761 |
| Minecraft: Java Ed. | 98 | **956** | 20417 |

Two things this makes clear. The three fields track HowLongToBeat's Main / Main+Extras /
Completionist, so `normally` running *above* a Google search's headline number is normal and
expected — but on a sandbox or live-service game the later fields collect *lifetime playtime*
rather than time-to-an-ending. And the pollution is **per field, not per game**: Baldur's Gate
3's `completely` is nonsense while its `normally` is spot on, so "distrust this game" would be
the wrong shape of fix.

`core/util/Playtime.kt` is a plausibility ladder over the three values — `normally`, then
`hastily`, then `completely`, taking the first that is `0 < h <= 300`, and reading "ENDLESS"
when none is. Minecraft falls through to 98 HRS, which is both defensible and inside the
50-200 range the tester expected; every game in the table above is unchanged. 300 sits clear
above the longest believable campaign in the sample (132) and well below 956.

Deliberately computed **at read time from the three columns already in Room**, not stamped in
by the Worker: no schema migration, it retroactively fixes every row already cached on every
tester's device, and it still works offline. Applied to PILE (all three views), the pile-hours
total and its `FINISHED BY` estimate, PROFILE's stats, the shared pile card, and DRAW's time
budget — DRAW keeps its own `hastily`-first preference, just with the same ceiling applied.
`PlaytimeTest` asserts against the real figures in that table.

### 2. "Resident Evil Requiem matched to Resident Evil OG"

The Worker resolves that caption correctly today (verified against production, 1.00
confidence, four phrasings), so this is a *scoring* bug reachable from either side rather than
a missing-data one — and the arithmetic shows exactly how:

`verifyAgainstText` scores a name by whole-word containment in the caption. `"Resident Evil"`
inside `"Resident Evil Requiem"` scores **0.925** — over the 0.9 bar that stops the search
early, and over the 0.85 bar that presents a result as *confident*. A strict prefix of a title
beats the bar for being the whole answer, and nothing in the score ever noticed the leftover
word. Any caption where the fuller title isn't returned first — a stale offline index, an IGDB
`search` miss on a noisy string — lands on the older game with full confidence.

Both ports now demote a match the caption itself says is incomplete: if the very next token in
the **original** text is capitalised, isn't a stopword, and isn't video-title boilerplate
(`official`, `trailer`, `reveal`, …), the score is capped at `FRAGMENT_CEILING = 0.84`. Under
0.85 so it can't be confident, under 0.9 so it can't end the search, still high enough to lead
the chooser when nothing better exists — which is what an offline-only device gets.

Capitalisation in the *raw* text is the signal, so it can't be computed from the normalized
string: a title carries on in caps ("Resident Evil **Requiem**"), prose does not ("Elden Ring
**is** brutal"). Only the *following* word is examined — a capital at the start of a sentence
is indistinguishable from a title word, so checking backwards would demote "Playing Hades
tonight". A punctuation-only token between the two ("REQUIEM **-** Announcement Trailer") ends
the title.

Five assertions each in `worker/test/candidates.test.ts` and `GameNameCandidatesTest`, kept
identical because the offline and online paths have to agree. **Consequence worth knowing:**
"Elden Ring Shadow of the Erdtree is brutal" now prefers the DLC and, if the DLC isn't found,
shows a chooser instead of confidently offering the base game. That is the intended trade —
an extra tap beats a wrong one-tap add.

**Correction, same session — the report was about "Resident Evil 9 Requiem", not
"Resident Evil Requiem", and the number changes everything.** The first fix above is real but
did not touch this case, and two further faults did.

**Fault A — a digit is not "capitalized", so every numbered sequel broke its own title in
half.** `properNounRuns` builds runs out of capitalised words; `'9'.isUpperCase()` is false, so
"Resident Evil 9 Requiem" produced the runs `Resident Evil` + `Requiem`, and "Resident Evil 4"
produced `Resident Evil`. The prefix was therefore tried *first* and won. Measured on the live
Worker before the fix:

| shared text | before |
|---|---|
| `Resident Evil 9 Requiem` | **Resident Evil @0.925** |
| `Resident Evil 4 Remake is amazing` | **Resident Evil @0.925** beat Resident Evil 4 @0.840 |
| `Final Fantasy 7 Rebirth` | **Final Fantasy @0.925** |

This was never specific to RE9 — it affected every numbered sequel in the catalogue. Numbers
now continue a run (never start one), a *following* sequel number counts as the title carrying
on, and one or two digits only, so `Elden Ring 2024 gameplay` is still a year and not a sequel.

The flat `FRAGMENT_CEILING` from the first fix also had to become a **multiplier**
(`FRAGMENT_PENALTY`, ×0.84): in "Resident Evil 4 Remake" *both* candidates are incomplete
matches, and clamping both to 0.84 threw away the only thing separating them. `scoreOf` tops
out at 1.0, so a multiplier keeps the same guarantees (< 0.85 confident, < 0.9 early-stop) while
preserving order.

**Fault B — the entire `alternative_names` half of the offline index had never worked.**
`verifyAgainstText` asks whether a name appears whole-word in the caption, and
`OfflineGameIndex.match` asked that about the record's **canonical** name. An alternative name
never shares its spelling with the canonical one — that is what makes it an alternative — so
every alt-name hit scored 0 and was thrown away. Verified on the real shipped asset:

```
Resident Evil 9 Requiem -> Resident Evil Requiem : SCORES 0 -> DISCARDED
BG3 is amazing          -> Baldur's Gate III     : SCORES 0 -> DISCARDED
FF7 Rebirth             -> Final Fantasy VII Rebirth : SCORES 0 -> DISCARDED
```

The index has held the answer all along — id 347668 carries `RE9`, `Resident Evil 9`,
`Biohazard 9`, `Resident Evil 9: Requiem`. Abbreviations and regional titles are precisely what
shipping a 610 KB dump index was *for*, and none of them could ever win. Each hit is now scored
against both the canonical name **and the key that found it**, best wins. That is safe because
the key is not a free-text guess: it is a string taken from the user's own caption that turned
out to be an exact IGDB name for that game, and both halves still verify against the caption.

`OfflineGameIndex.matchIn` was split out of `match` so `OfflineGameIndexTest` can run the real
scoring loop over the real shipped index from a plain JVM test — the RE9 and BG3 cases are both
asserted there, not just their ingredients.

**Third fix — matching through a number the official title omits.** IGDB says "Resident Evil
Requiem"; the internet says "Resident Evil 9 Requiem". Whole-word containment fails on the
interior digit, so `verifyAgainstText` now retries against a haystack with standalone sequel
numbers dropped, scoring ×0.84. Deliberately not confident on its own: "Mass Effect 2 Legendary
Edition" is structurally identical and there the user probably means Mass Effect 2.

**Deployed 2026-08-23** (version `51e24d40`, all four bindings confirmed). Verified on live
traffic after propagation — note a batch run seconds after `wrangler deploy` returned still hit
a stale edge on one row, so **wait before verifying**:

| shared text | after |
|---|---|
| `Resident Evil 9 Requiem` | **Resident Evil Requiem @0.840** → Resident Evil @0.777 |
| `Resident Evil 4 Remake is amazing` | **Resident Evil 4 @0.819** → Resident Evil @0.777 |
| `Final Fantasy 7 Rebirth` | **Final Fantasy VII @0.819** → Final Fantasy @0.777 |
| `Resident Evil Requiem` | Resident Evil Requiem @1.000 |
| `Elden Ring is brutal` | Elden Ring @0.900 — unchanged |
| `Elden Ring 2024 gameplay` | Elden Ring @0.900 — the year is not a sequel |
| `…items Pocketpair Palworld` | Palworld @0.850 — unchanged |
| `a tale of two bush ganks League of Legends` | League of Legends @0.992 — unchanged |

On device the **offline index answers first**, and there RE9 resolves at **1.000** (the caption
is an exact alternative name), so the share sheet shows one confident tap rather than a chooser.
Cross-checked against the real asset. `BG3 is amazing` now resolves at 0.808 — a chooser, but
found at all for the first time.

**The Android half still needs a new build to reach anyone.**

### 2b. An ALL-CAPS YouTube title matched nothing

Reported while testing: `SIDEMEN AMONG US ULTIMATE DRAFT MODE: PICK EVERY ROLE IN THE GAME`
resolved to nothing at all. Two causes, both about the candidate *budget* rather than scoring.

Capitalisation is the signal `properNounRuns` runs on, and an ALL-CAPS title has none — every
word looks like a title word, so the whole caption becomes one twelve-word "run". The budget
then goes entirely on that run's shrinking prefixes: `SIDEMEN AMONG US ULTIMATE DRAFT`,
`SIDEMEN AMONG US`, `SIDEMEN`. Measured: **`"Among Us"` ranks 27th of 35 candidates**, past the
Worker's 6-search budget and past the offline index's 24-candidate cap.

The offline cap was raised to 64. It exists to stop a pathological caption burning CPU, and at
24 it was far tighter than that needed — each attempt there is a hash lookup, not the IGDB
request the Worker's budget of 6 is rationing. Swept across the captured-caption corpus at 24
vs 64, **exactly one answer changes**: this one, from nothing to `Among Us @0.742`. The ranking
already puts good candidates first, so a longer tail can only surface matches that were
previously unreachable.

0.742 is deliberately not confident — two short words — so the share sheet offers it as a
choice rather than asserting it. For a game name buried in channel branding that is the right
outcome. Asserted end to end against the real index in `OfflineGameIndexTest`.

**The Worker still returns nothing for this shape**, because raising *its* budget means more
IGDB requests against a 4/sec quota. On device the offline index answers first and covers it;
a caption that only reaches the Worker (a link whose page title is ALL-CAPS) still won't match.
Known, accepted, cheap to revisit if it shows up again.

**Also still open, same class:** a hashtag can't verify offline. `#EldenRing` normalizes to the
single token `eldenring`, so whole-word containment against `Elden Ring` fails and the hit is
dropped — the same "matched on one string, scored against another" shape as the alt-name bug,
but not fixed by the same change. The Worker's lexical fallback catches it at ~0.5, so it lands
in the chooser rather than nowhere.

### 3. DISCOVER needed a clear button

Added, and only while there is something to clear. The field is the only way back to the rails,
so clearing it was previously holding backspace across a whole game title.

### 4. "Games added from DISCOVER never appeared in PILE or in DRAW" — still open

**No root cause found.** Traced the whole write path — `addToPile` → `gameDao.upsert` →
`pileDao.insert(state = BACKLOG)` → `observeByState(BACKLOG)` — plus every way a row could
later vanish. Ruled out: destructive migrations (schema is v1, no
`fallbackToDestructiveMigration`), `REPLACE` cascading through a foreign key (there is none
from `pile_entries`), seed-id collisions (seed ids are all negative, all distinct), a state
with no tab (all five states are tabs), and a `DrawSelector` filter excluding new games
(unknown length never disqualifies). No device was attached this session to reproduce on.

What shipped is the two things that are provably wrong regardless of the cause, plus the
instrumentation to make the next report diagnosable:

- **The write is now `NonCancellable`.** Tapping a card and then immediately tapping the nav
  bar is the normal way to use this screen, and three suspending DAO calls in `viewModelScope`
  are three chances for a cleared ViewModel to cancel the write halfway — losing the add
  silently, with the tick already on screen. A few ms of local SQLite work isn't worth making
  interruptible.
- **`addedGameIds` now comes from Room** (`PileDao.observeAllGameIds`, deliberately not the
  `games` join) instead of an in-memory set. The tick used to evaporate the moment you left the
  screen, so a returning user couldn't tell an add from a no-op.
- **Every tap now answers.** "X ADDED TO YOUR PILE" or "X IS ALREADY IN YOUR PILE" — the second
  half is the tester's own suggestion. The search row's add button stays *enabled* when already
  added, because a disabled button that does nothing on tap is how this ambiguity started.

> **Open question for the next session — ask Mikhil before re-investigating.** Does the
> affected tester see *any* games in their pile (share-target adds in particular), or is it
> empty? "DISCOVER adds specifically are lost" and "nothing ever persists" are different bugs
> with no shared cause, and the answer picks which one to chase.

### 5. STACK gave no hint that it moves

STACK is PILE's default view and its only gesture is a vertical drag that nothing asked for —
a tester read the receding cards as decoration. Two breathing chevrons and the word SWIPE down
the right edge, where they can't cover a cover. Dismissed on the **first drag** (not a timer,
not a tap — the drag is the only event that proves the teach worked) and remembered in
DataStore, so it's a one-time teach rather than a permanent label on the signature view.
Hidden outright for a one-game pile.

### Two decisions taken 2026-08-24 (not code)

**The 12-tester gate has two hurdles, not one — an earlier reading of this was wrong.**
Mikhil pushed back on it and was right. Google's own page (support.google.com
answer/14151465) is unambiguous on both halves:

- *The clock:* "At least 12 testers must be opted in to your closed test when you apply for
  production access, and they must have been opted in continuously for the preceding 14 days."
  No usage metric. Opt-in starts and satisfies this.
- *The review:* the production-access form asks for "details about tester engagement during
  your closed test, including: **Whether testers used all available app features**" and
  "**Whether tester usage matched expected production user behavior**". Google reviews the
  submission and can reject it.

So the current tester behaviour — 12 people, 2-3 minutes a day, most of them in the first two
days — clears the clock and is a **genuine risk at the review stage**. The actionable response
is not more testers, it's directing the ones we have at specific features so the form can be
answered truthfully. See `docs/01-PLAY-STORE-CRITICAL-PATH.md`. Tester-marketplace sites push a
quantified version of this claim and all of them sell tester pools; the general point is
Google's own, their thresholds are not.

**EEA/UK/CH will be excluded from country availability rather than shipping a CMP.** The
missing UMP/consent flow has been on the risk list since 2026-08-15 as an ads-policy gap.
Decision: drop those countries instead of building consent — the complexity isn't worth it at
this stage of the project.

> ⚠️ **Do this in the right order.** Country availability applies per track. If any current
> closed tester is in an excluded country they lose access, which drops them out of the count
> and restarts *their* 14 days. **Check where the existing testers are before changing
> availability**, and consider restricting production availability only, leaving the closed
> track open.

### What a fresh session should pick up

1. **Answer the open question in item 4** — does the affected tester see *any* games in their
   pile? Everything else about that bug is already ruled out; the answer picks which of two
   unrelated causes to chase.
2. **Device-verify this build.** The STACK swipe hint, the DISCOVER confirmation banner and the
   playtime labels ("98 HRS" for Minecraft, "ENDLESS") have **never rendered on hardware**.
3. **Build the single-game share card** — the "complete, rate, and share games" judging
   criterion is the weakest of the three: CLEARED and RANK exist, but a game cannot be shared,
   only the whole pile. `ShareCardRenderer` already does Canvas → PNG → FileProvider →
   `ACTION_SEND` and the app holds no storage permission, so this is a `renderGameCard()` plus a
   SHARE row in the existing action menu. **No Play data-declaration change**: a locally
   rendered image handed to the system share sheet falls under the user-initiated-transfer
   exemption. Setting `EXTRA_TEXT` to a caption containing the game name also gets the
   "add this game" bridge for free — the app is already a share target that resolves game
   names, so a recipient shares the message back into it. An `https://` App Link (the GitHub
   Pages domain could host `assetlinks.json`) would make it one tap instead of two; deferred as
   unnecessary risk before production access.
4. **Still unbuilt from the spec:** Customer Center, FREE PLAY mode, 4 of the 5 share cards,
   clipboard-nudge detection.
5. **Submission kit** (`docs/07-SUBMISSION-KIT.md`) — video, Design Award and Catvertising
   write-ups. Not started.

### State after this session

- `:app:testDebugUnitTest` green — 72 tests, 7 suites. `npm test` in `worker/` green — 45 tests.
- Worker `tsc --noEmit` clean, **deployed** (version `51e24d40`) and verified on real traffic —
  see the tables in item 2. `versionCode 4` testers already have the server-side half.
- **No device was attached**, so none of this has been on glass. The STACK swipe hint and the
  DISCOVER confirmation banner are both new UI that has never rendered on hardware.
- **`versionCode 6` / `0.6.0` is built and current** (2026-08-24 08:51) and carries
  everything above — `app/build/outputs/bundle/release/app-release.aab`, 32.4 MB, signed.
  `versionCode 6` / `versionName 0.6.0` were read back out of the bundle's own manifest
  rather than trusted from `build.gradle.kts`. **`versionCode 5` was already consumed by
  Internal testing**, which is why this is 6 — check what a track has already seen before
  assuming the next number. Mikhil is moving the Closed track off `versionCode 4` onto
  this build on 2026-08-24.

---

## 2026-08-19 (latest) — reading RevenueCat: bot traffic, and the real tester count

Mikhil asked how many unique users the app has and why some show as United States when he knows
his testers are Mauritian. Both questions turned out to have the same answer, found by cross-
referencing `list-customers` against `first_seen_at`/`last_seen_at`/`platform_version` rather
than trusting the country field at face value — see [[verify-config-with-traffic-not-source]],
same lesson applied to a metrics dashboard instead of a health check.

**RevenueCat's "customer" count is anonymous installs, not people.** `ContinueApplication.kt`
calls `Purchases.configure()` with no `appUserID` and the app never calls `logIn` — correct for
an app with no accounts, but it means every reinstall or data-clear mints a fresh anonymous ID.
39 total customers over 28 days against ~17 real testers is expected, not a red flag.

**14 of the 39 "US" customers are Google's Play pre-launch device farm, not misattributed
Mauritian IPs.** Three signals nailed it: every one of them is Android **API 30** exactly, while
real Mauritius customers span API 31–36; `first_seen_at` equals `last_seen_at` to the millisecond
for all of them (one launch, never returned); and they cluster tightly on build-upload days
(3–4 at a time, minutes apart, on 2026-08-11/12/14/15) and are **absent** on days with no upload.
Real geolocation is fine — nothing to fix here, and no CMP/consent-flow implication either.

**Last 12 hours (2026-08-19, from the moment Mikhil posted to recruit testers): 16 new
customers, 15 Mauritius + 1 Malaysia, zero US.** No farm noise in this batch — it's the tester
wave landing, trickling in over the day rather than all at once, which is the expected shape for
a social post reaching people at different times. Net of Mikhil's own ~4 manual reinstalls
during testing, that's roughly a dozen real testers in one window alone.

**For the Devpost writeup: never quote RevenueCat's raw customer/user count.** Quote the
Mauritius-only figure, or describe the split explicitly — a judge who spots 14 single-session
US installs on uniform Android 11 will read it exactly as this session did.

---

## 2026-08-19 (latest) — the share target was quietly broken, in four separate ways

**The report:** shares from YouTube and Instagram put the raw URL — or a fragment of one — into
the manual-entry field, so every fallback started by selecting and deleting a link. Mikhil's
call, which is the right one: *a bare URL in the field is worse than an empty field, and a
partial title is better than either.*

Reproduced against the deployed Worker before changing anything. The single reported symptom
turned out to be **four independent bugs**, only one of which was the prefill itself.

### 1. The prefill fell back to the raw share text (app)

`ShareTargetViewModel` held `lastRawText` and used it whenever resolution failed —
`bestLocalGuess = localCandidates.firstOrNull() ?: text`, and `fallBackToManualEntry()` used the
raw text directly. `TitleParser` correctly reduces a bare URL to *nothing*, and the `?: text`
then put the URL back. Now there is a single `cleanPrefill()` that returns a cleaned fragment or
**null**, and `lastPrefill` replaces `lastRawText` — so an unresolvable Instagram link yields an
empty field, which is what was asked for.

### 2. A link was only detected if the text *started* with one (Worker)

`looksLikeUrl()` tested `/^https?:\/\//`, so the extremely common
`"this boss is insane https://youtu.be/…"` shape skipped Stage 1 entirely: the video was never
looked up, **and** the whole string including the URL went into the IGDB search, which duly
matched a game literally called *Insane* at 0.83 confidence. Replaced with `extractUrl()`, which
finds a link anywhere and returns the matched substring so the caller can subtract it — the
surrounding caption is kept and searched alongside whatever the link resolves to. Verified:
`"hollow knight silksong instagram.com/reel/…"` now resolves **Hollow Knight Silksong at 1.00**
despite Instagram itself being unresolvable.

### 3. Scheme-less links were treated as prose (both sides)

Share sheets send `youtu.be/abc` without a scheme at least as often as with one. Both title
parsers stripped `https?://…` and `www.…` but nothing else, so `youtu.be/dQw4w9WgXcQ` survived
cleaning intact — **this is the "part of a URL" Mikhil saw in the field**. Both parsers now strip
bare `host.tld/path` too, and `extractUrl()` normalises such links to https before resolving.
The SSRF boundary is unchanged: `resolveUrlToText`'s host allowlist still decides what may
actually be fetched, and the scheme-less host list is only about recognition.

### 4. The search budget was spent on the least likely candidates (Worker)

The one nobody reported, and the reason the field appeared so often. `windows()` emitted every
word window strictly longest-first, which is backwards for a near-exact search engine — long
windows almost never match. On `"Elden Ring Shadow of the Erdtree is brutal"`, `"Elden Ring"`
ranked **10th of 23**, outside the 6-search budget, and the resolve settled for *"Ring Master I:
The Shadow of Filias"* at 0.29. Windows are now ordered **whole run → shrinking prefixes →
shrinking suffixes → interior**, with connectors trimmed off both ends of every window (no more
`"Elden Ring Shadow of"` or `"of Legends"`).

Measured against production, same caption, before and after:

| | before | after |
|---|---|---|
| `"Elden Ring Shadow of the Erdtree is brutal"` | Ring Master I: The Shadow of Filias @ 0.29 | **Elden Ring @ 0.90** |
| `"…Pocketpair Palworld"` (existing fixture) | Palworld @ 0.85 | Palworld @ 0.85 (no regression) |

0.90 clears the 0.85 confidence threshold, so that share is now a **one-tap add** rather than a
manual-entry dead end.

### The new `suggestion` field

`/resolve` now returns `suggestion` — the top-ranked candidate string — for the app to prefill
with. It comes from the same ranking the searches used, so it is the cleanest fragment
available, and because the title parser builds it, **it can never be a URL**. Null when there is
nothing better than a bare link. The app prefers it, then the locally-cleaned resolved title,
then its own local guess; every rung is URL-free by construction and the whole chain may be null.

Old clients ignore the new field (`ignoreUnknownKeys = true`), and the accuracy fixes are
server-side — so **testers on `versionCode 4` get better matching immediately**, without an
update. Only the empty-field behaviour needs the new build.

### Guarded by tests, not just by having been fixed

`worker/test/shareLinks.test.ts` (7 new cases) plus two new cases in `TitleParserTest.kt`, all
asserting the user-visible property directly: *nothing link-shaped may ever come out of the
title parser*, and the real game name must rank inside the search budget. The Palworld and
League fixtures from 2026-08-12 are re-asserted so the reordering can't regress them. Worker: 35
tests pass. App: all unit tests pass.

**One process note worth keeping.** The bare-host regex was written into the file through a
Python heredoc and its `\b` became a literal backspace byte, so the pattern silently never
matched — the tests caught it, and `cat -A` is what made it visible. Every source file was then
scanned for stray control characters (none). Writing regexes through a shell heredoc is a bad
idea in this repo; use the editing tools directly.

---

## 2026-08-19 (later) — the first tester report, and what it actually meant

**The report:** DISCOVER "feels a bit empty" — TRENDING NOW and SHORT & SWEET aren't much — and
the tester said they'd rather scroll vertically through a long library of games.

**What was accepted, and what was declined.** The complaint is valid; the proposed fix isn't.
An endless browsable catalogue was declined on three independent grounds, recorded here because
it will be proposed again:

1. It changes what the app is. CONTINUE? manages a backlog; IGDB is the catalogue. An infinite
   library makes this a worse games database than the one it's already built on.
2. It is the product's own thesis inverted. docs/02-PRODUCT-SPEC.md §3: "87 games, two free
   hours, total paralysis, and you end up scrolling instead of playing." DRAW exists *because*
   scrolling is the failure mode. Shipping an infinite scroll of games the user doesn't even own
   builds the disease into the cure.
3. It is expensive exactly where this stack is cheapest. Rails are fixed queries cached under one
   KV key each; infinite scroll is paginated, unbounded and per-user, against the 1,000 KV
   writes/day cap that docs/12-SECURITY.md calls the binding constraint.

**The real finding was underneath it.** A tester spent enough time on DISCOVER to judge it thin,
which means they never found the **share target** — the app's actual discovery mechanism and its
headline feature. A grep confirmed why: the share flow was mentioned **nowhere in the app**. Not
in onboarding (whose "SEED YOUR PILE" step offered a disabled Steam import and a search button),
not in any empty state, nowhere. It was discoverable only by already knowing to look in the OS
share sheet. That is a far more valuable bug than the one that was reported.

### Fix 1 — five rails instead of two (Worker + app)

docs/02-PRODUCT-SPEC.md §2c always specified five rails. Three were missing, so the tester was
looking at a screen that was simply unfinished. Now shipping: TRENDING NOW · NEW RELEASES ·
SHORT & SWEET · HIDDEN GEMS · a genre rail keyed to the user's own pile.

New Worker routes `/games/new`, `/games/gems`, `/games/genre?name=` — **deployed and verified in
production** (all four bindings listed on deploy; every rail returns 20 real games; an
unresolvable genre returns 0 and writes nothing).

Every query was probed against live IGDB *before* being written into the Worker, and two of the
three obvious formulations turned out to be wrong:

- **HIDDEN GEMS on `total_rating` is worthless.** `total_rating > 80 & total_rating_count > 5`
  returns *Bubsy 3D* and *PokéOne* at 100/100 — a handful of user ratings is enough to float a
  joke listing. Rebuilt on `aggregated_rating` (press coverage, which nothing brigades) with
  `aggregated_rating_count >= 5`, and `total_rating_count < 200` is what makes it *hidden*
  rather than merely good. Now returns The Witness, Metaphor: ReFantazio, Shadow Gambit.
- **NEW RELEASES sorted by date is a rail of nobodies.** The newest thing in IGDB at any moment
  is whatever indie was catalogued this morning. The same 180-day window ordered by rating count
  gives the releases a backlog actually accumulates.
- A third suspicion was wrong and worth recording: the notable-first list *looked* like it
  contained unreleased games, and printing the real dates proved they were genuine 2026 releases.
  Checking beat assuming in both directions.

`game_status = null` (the "fully released" case) drops early-access entries. Note it is
`game_status`, not the deprecated `status` — see the standing IGDB trap in the header comments of
`IgdbGameProvider`.

**The genre rail resolves names, never ids.** The app sends IGDB's genre *name* (which it already
holds on its cached games); the Worker resolves it in memory against a cached copy of the whole
genre table and caches results only under the resolved **id**. That ordering is the point: keying
the cache on a client-supplied string would let anyone mint unlimited KV entries against the
tightest quota in the stack. Unresolvable names cost one cheap lookup and write nothing.

**`CACHE_VERSION` was deliberately not bumped.** The rails only add new keys and no existing query
changed shape, so a bump would have cold-started every cache while the closed test was live, for
nothing.

App-side, the rails now load **one coroutine each and fill in as they land**, rather than the
screen waiting on the slowest of five sequential round trips. They're declared up front in
display order so nothing re-shuffles under the user's thumb, and a rail that returns empty simply
doesn't render.

### Fix 2 — the app now says the share target exists

Three places, all copy:

- **Onboarding's seed step** now names it as the best way to add a game.
- **PILE's empty state** carries it as supporting text (`EmptyState` already had a `supporting`
  slot, added the last time a tester asked what a screen was for).
- **DISCOVER** leads with a card saying it out loud: *"Saw a game in a video? Hit share in TikTok,
  YouTube or Reddit and pick CONTINUE?"* — on the exact screen where the misunderstanding
  happened.

### On briefing the testers

Brief them on the **core loop**; don't mention the competition. Telling people it's a hackathon
entry buys generous feedback instead of honest feedback, and the share loop is a feature nobody
can find by exploring — leaving it unbriefed just burns a tester. Record the unbriefed result
first, though: "a tester used the app without ever discovering the share target" is evidence
available exactly once, and it is the whole justification for Fix 2.

---

## 2026-08-19 — closed testing started, and the STACK view

### The clock is running

Mikhil confirmed **closed testing has officially started** and Play Console reports the
**12-tester requirement as met** (~14–16 invited; the opted-in figure is the one that counts and
Play only surfaces it as met/not-met on the Dashboard's production-access card — the track's
Testers tab shows *invited*, which is a different and always-larger number).

Two live checks run this session, both green — worth doing at the start of any session now that
real testers are hitting the app:

- `https://mikhil-sec.github.io/GamesToPlay/privacy.html` → **HTTP 200**. GitHub Pages is on and
  the policy URL in the listing resolves.
- `https://continue-worker.gamestoplay.workers.dev/health?deep=1` → `{"ok":true,
  "provider":"igdb","sampleCount":20}`. Real IGDB data, not the seed fallback.

**What can still break the 14 days, in the order it's likely to happen:**

1. **A tester uninstalls or leaves the group.** Dropping under 12 *resets* the counter, it
   doesn't pause it. The headroom over 12 is the only defence.
2. **Pausing/halting the track, or starting a second closed track.** Keep shipping into the one
   that's counting.
3. **A repeated `versionCode`.** The next upload must be **5 or higher** — `4` is spent. Pushing
   a new build into the closed track is otherwise safe and does *not* reset the tester clock.

**Both now confirmed (2026-08-19):** the clock started **2026-08-19**, and **`versionCode 4` is
the build on the closed track**. So the earliest production-access application is **2026-09-02**,
review is typically hours to a few days, and the Shipaton deadline is 2026-09-30 — roughly three
weeks of slack. The next upload must be **`versionCode 5` / `versionName 0.5.0`** (Mikhil keeps
the two in step deliberately).

### STACK — the signature view, built

`feature/pile/PileStackView.kt`. The pile as physical cases receding into the screen, flicked
through with momentum and snapped to a card — docs/02-PRODUCT-SPEC.md §1 called it "the wow
view" and it was the last named gap in PILE. `PileViewMode` is now `STACK, GRID, LIST`, **STACK
is the default** (as the spec always said), and the mode is now genuinely **persisted** to
DataStore, which the spec also asked for and which nothing had ever implemented.

**Hand-rolled rather than built on `Pager`, deliberately.** A pager lays its pages out end to
end; this view is *defined* by pages overlapping — every card is drawn in the same place and
pushed apart purely by transform. Fighting a pager's layout to fake overlap is more code than
owning the gesture, and owning it matches `DrawCardStack`'s existing idiom: an `Animatable`
holding a fractional card index, a `VelocityTracker`, `rememberSplineBasedDecay` to project the
fling target, then a spring to snap to the nearest whole card.

Decisions worth not re-litigating later:

- **Drag up advances**, matching scroll convention rather than the physical "pull a case toward
  you" metaphor. The cost is that the receding stack and the departing card both travel *up* the
  same strip of screen — paid for by making the departing card **scale up and fade out inside
  the first ~45% of its travel**, so it reads as passing the camera instead of merging into the
  pile behind it.
- **Per-frame transforms live inside `graphicsLayer`**, read through a lambda, so they run in the
  draw phase. Only the rounded card index is allowed to recompose (via `derivedStateOf`) — it
  changes a handful of times per fling instead of sixty times a second.
- **Covers use `IgdbImage.GRID`, not `HERO`.** The front card is ~600px wide on a 1080p phone and
  IgdbImage's own measurements note IGDB *upscales* past the source (a typical cover's original
  is only 600x800), so HERO would buy layout-correct pixels and no detail. Sharing the token with
  the grid means one cache entry per game: switching views is instant, and promoting a card to
  the front never re-fetches and flashes.
- **The fling is capped at 8 cards.** Momentum is the point, but uncapped decay across an 87-game
  pile lands somewhere nobody aimed for with nothing decoded yet.
- **Titles are not printed on the cards.** Six stacked title strips is six competing labels, and
  a game case doesn't caption itself. One caption sits below the pile and dims through each
  hand-off, so it's never seen attached to the wrong cover.
- **One detent haptic per card passed**, drag and fling alike — the decelerating burst at the end
  of a flick is the reel-stopping feel the cabinet is for.
- **STACK's header is a fixed block, not a list item.** GRID and LIST became one scrolling
  surface on 2026-08-15; STACK can't join them because it owns a vertical drag gesture and the
  two would fight for every drag. Instead the stack takes the leftover height via `weight(1f)`
  and sizes its cards off `BoxWithConstraints`, so expanding the filters *shrinks the cards*
  rather than pushing them off-screen — the 2026-08-15 lesson applied without the scroll.
- The view toggle now **cycles** through three modes showing the icon of the mode you'd switch
  *to*, using the enum's own order so the two can't drift.

**Compiles clean and the unit tests pass. Nothing here has been seen on a device** — `adb
devices` was empty again. Three values can only really be judged on glass, and all three are
one-line changes at the top of `PileStackView.kt`:

1. **`DEPTH_TILT_DEGREES` sign.** The cards tilt with `rotationX = -(4° × depth)`, intended as
   the top edge leaning away. If the pile looks like it's leaning *toward* the viewer, flip the
   sign — the magnitude is deliberately small (capped at 12°) so a wrong sign reads as slightly
   odd rather than broken.
2. **Drag direction.** If flicking up to advance feels backwards next to the DRAW cards, invert
   the sign in `onVerticalDrag` and in the fling velocity together.
3. **`DRAG_UNIT_FRACTION` (0.45 of card height).** How far you drag to move one card. Nothing but
   a thumb can tell you if that's right.

---

## 2026-08-15 (later) — the closed-testing paperwork, written end to end

No app code changed. This session attacked the *other* half of the critical path: the store
listing, which is what actually blocks a closed-test rollout (an internal-testing upload needs
almost no paperwork, which is why `versionCode 1` sailed through and why this looked deceptively
close to done).

**New: `docs/13-STORE-LISTING.md` — the answer sheet.** Every Play Console field with its answer
already written: app name/short/full description (length-checked against Play's 30/80/4000
limits), category and tags, the Data Safety declaration, the IARC content-rating answers, target
audience, the advertising-ID declaration, release notes, and the order the sections have to be
filled in to unblock fastest.

**The Data Safety form was derived from sources, not guessed**, because it's the most common
rejection cause here and a wrong answer outlives the mistake. Google's published Mobile Ads SDK
disclosure gives four of the six declarations (device IDs, app interactions, diagnostics, and
IP-derived approximate location — all *shared*); RevenueCat's own docs give purchase history
(collected, not shared, not linked, since our app user IDs are anonymous). The sixth is ours:
search terms and shared captions reach the Worker, and the KV cache outlives the request, so
"in-app search history" is declared rather than claimed ephemeral. Explicitly *not* declared:
photos — shared images are OCR'd on-device by ML Kit and never leave the phone, and the app has
no photo-library permission at all.

**Privacy policy + landing page written and committed** as `docs/privacy.html` and
`docs/index.html` (plus `docs/.nojekyll`), styled in the app's own palette. Publishing is a
`git push` and one GitHub Pages toggle (main → `/docs`), landing at
`https://mikhil-sec.github.io/GamesToPlay/privacy.html`. Two claims were caught and removed
while writing it, both of which would have been false:
- an EEA/UK **consent flow** — the app has no CMP at all (nothing references
  `UserMessagingPlatform`), which is a real gap under Google's EU user consent policy and is now
  logged as such rather than papered over;
- a **RevenueCat app user ID shown on the YOU tab** as the handle for deletion requests — it
  isn't displayed anywhere, so the policy asks for the Play order ID instead.

**Store graphics generated:** `store/icon-512.png`, `store/icon-1024.png` and
`store/feature-graphic-1024x500.png`, produced by `tools/StoreAssets.java` (plain JDK 21 + AWT,
no build, no dependency) from the launcher icon's own path coordinates, `Color.kt`, and the
bundled Chakra Petch faces — so the store mark and the installed icon are the same artwork and
can't drift. All original: no box art, no third-party logo, no influencer branding.

**Still needs a device: the screenshots.** `adb devices` was empty again this session.
`tools/capture_screenshots.sh` prompts through the shot list and captures with `exec-out`; they
must come from `versionCode 4` on a *phone*, since the 2026-08-15 layout round changed every
screen worth showing.

**Listing copy is scoped to the shipped build on purpose.** The Devpost draft in
`docs/07-SUBMISSION-KIT.md` sells Steam import, Free Play Mode and five share cards; one is
disabled and two don't exist. The Play description covers only what `0.4.0` actually does.

---

## 2026-08-15 — phone vs tablet: the layout round

Mikhil ran `0.3.0` on both a phone and the Galaxy Tab and reported that **the same build looks
right on the tablet and wrong on the phone** — the second time this project has learned that a
wide device hides an entire class of bug (see 2026-08-14 item 1, and bug #9 in §3). Everything
here is a fix to *shared* chrome, so all of it improves the tablet too.

**Confirmed working on a device, first time:** the GO PRO paywall ("looks REALLY good") and the
card-dispenser deal animation. Offline mode was also exercised — it works, with the caveats in
§Offline below.

### 1. The coin counter was taxing every screen 52dp of header

It lived in `ArcadeScaffold`'s top bar on all four tabs. On a tablet that's free; on a phone it
pushed PILE's tabs visibly down and made the whole screen read as starting too low. It now
appears **only on DRAW (where coins are spent) and YOU (the account screen)** —
`ArcadeScaffold(showCoinCounter =)`, decided in `ContinueNavHost` from the current route.

**The subtle part:** the top bar still renders when the counter is hidden, as a zero-content
`Row` that consumes `WindowInsets.statusBars`. Deleting the bar outright would have been the
obvious move and would have re-broken 2026-08-14 item 2 — the app draws edge-to-edge, so with
no top bar the content doesn't sit *higher*, it slides *under the clock*.

### 2. The raised DRAW button was never centred on its own slot

On the phone the gold DRAW circle sat on top of the "DISCOVER" label. Root cause: the bar has
**two items on the left and one on the right**, so its empty middle column is centred at
**62.5%** of the width — but the button was `align(TopCenter)` on the whole bar, i.e. **50%**.
It was always 12.5%-of-screen-width too far left; a tablet's 240dp-wide columns simply had room
to absorb the error and a phone's 90dp ones did not.

Fixed by making the button a **real child of the nav Row** (a weighted `Box` in the middle
slot), so it self-centres on its own column whatever the item counts are. It still uses
`offset`, never `padding`, for the -24dp raise — see bug #9.

Nav labels also got smaller and tighter (10sp/0.02em, `maxLines = 1`, ellipsised, clamped to
the column). "DISCOVER" is the widest word in the bar and at a raised system font scale it
outgrew its own column, which is what made the collision visible rather than merely present.

### 3. PILE is now one scrolling surface, not a fixed header over a scrolling grid

Reported on **both** devices: expanding SORT & FILTER ate the screen and you could never scroll
*past* the filters — only the covers moved. Tabs, the time-budget bar, the action row and the
filter chips are now **full-span items inside the LazyVerticalGrid / LazyColumn** rather than a
`Column` wrapped around them. Same fix in both view modes; the empty state became an item too.

All the lazy lists in the app (PILE, DISCOVER search, DISCOVER rails, YOU) also picked up 32dp
of bottom `contentPadding` — the raised DRAW button overhangs the nav bar by 24dp and was
sitting on top of the last row.

### 4. DRAW's dials could crowd the lever out of existence

On a phone the three dial sections wrap to eight-plus rows of chips once a pile has a realistic
platform list, leaving the lever a pink sliver that **could not be dragged at all** — the
signature interaction of the app, unreachable. Three changes:

- The dials **collapse**, defaulting to collapsed on short screens (measured with
  `BoxWithConstraints`, not a hardcoded phone/tablet breakpoint, so landscape is covered too).
- When collapsed, a one-line summary (`2 HOURS · STORY · 3 PLATFORMS`) keeps the settings
  visible — same rule as PILE's active-filter count: **hiding a control must never hide its
  state**, or a draw comes back filtered by something the user can't see.
- The lever is **measured before** the dials region and so always keeps its full pull height;
  the dials scroll in whatever space is genuinely left. Collapsed, the lever centres in the
  whole cabinet, which is the better screen anyway — one thing to do, in the middle.

The pull threshold became a **fraction of the track** (0.68) instead of a hardcoded 150dp, so
the short-screen (<560dp) 150dp track still commits at the same relative point.

### Offline — covers now survive it

Mikhil's offline pass found the pile works but **no cover art loads**. Room held the games;
every cover still went to the network, so an offline pile was a wall of grey. Two fixes:

- `ContinueApplication` now implements `SingletonImageLoader.Factory` with an explicit **192MB
  Coil disk cache** (plus a 25% memory cache and crossfade). An IGDB cover URL contains the
  image's own hash, so a cached cover can never be stale — only ever absent.
- `GameCard` paints the title's initial **underneath** the cover unconditionally, so a card
  with no art yet reads as the game instead of an empty slab. It used to draw the initial only
  when `coverUrl == null`, which is the one case that *isn't* the common one.

This does not make first-ever-seen covers appear offline — nothing can — and offline search is
still limited to the on-device index, which Mikhil correctly judged acceptable.

**Verified:** `assembleDebug` + `test` green (both variants), `bundleRelease` signed as
`versionCode 4`. **Not verified on a device — `adb devices` was empty for this whole session.**
Every item above is a layout change reasoned from the screenshots, so the next device run
should start on a *phone*, on PILE and DRAW.

---

## 2026-08-14 (later) — the offline index is now real, and wired in

Continuation of the same day's session. `TWITCH_CLIENT_SECRET` was pasted into
`worker/.dev.vars` (created earlier this session, previously blocked on this one paste) and
`node tools/igdb_dump_index.mjs` was run for real.

**One bug found immediately, fixed before shipping anything:** the built index's `year` column
was the literal string `"NaN"` for every row. Cause: the CSV data dump encodes
`first_release_date` as an **already-formatted datetime string** (`"2023-08-15 00:00:00"`),
while the REST API (what `IgdbGameProvider.ts` talks to) returns the **same field name** as a
Unix timestamp. The indexer copied the REST-API parsing (`Number(value) * 1000` into `Date`),
which doesn't throw on a non-numeric string — `Number("2023-08-15…")` is `NaN`, and `new
Date(NaN).getUTCFullYear()` silently returns `NaN`, which then serializes as the string "NaN".
Exactly the shape of bug this project has now hit three times (see bug #10 in §3 below, and the
`backgroundUrl: null` miss from earlier today): **a field with the same name means a different
encoding on a different surface, and nothing errors when you get it wrong.** Fixed in
`tools/igdb_dump_index.mjs`'s new `parseDumpYear()`, which is now the one place in the repo
that knows the dump's date format differs from the API's. Rebuilt after the fix: **0 rows** with
"NaN" confirmed by direct check of the output file.

**What the pipeline actually produced**, downloading all four dumps for real (~350MB total,
resumed cleanly through the 5-minute presigned-URL expiry, no sandbox issues this time):

| Dump | Size | Rows kept |
|---|---|---|
| `games` | 289.8 MB | 17,095 (of 372,146 scanned — `game_type=0`, no version parent, ≥3 ratings) |
| `covers` | 41.0 MB | 332,444 indexed |
| `alternative_names` | 18.4 MB | 24,398 attached |
| `game_time_to_beats` | 0.9 MB | 7,362 indexed |

Output: `app/src/main/assets/game_index.tsv.gz`, **0.58 MB gzipped** — a trivial APK-size cost
for 17k games' worth of exact + alternative-name matching.

### Wired into the app, not left as a built-but-unused asset

Building the index was necessary but not sufficient — the point was to fix share matching, and
that meant actually replacing the matching path. New:

- **`core/util/GameNameCandidates.kt`** — Kotlin port of `worker/src/resolve/candidates.ts`'s
  `rankedCandidates()`/`verifyAgainstText()`/`normalize()`. This logic had no prior Kotlin
  mirror (only `titleParser.ts`'s candidate *extraction* was ported, as `TitleParser.kt`,
  deliberately left unchanged this session). Kept behaviourally identical to the TS on purpose:
  9 tests in `GameNameCandidatesTest.kt` mirror `worker/test/candidates.test.ts` line for line,
  same fixture captions (`PALWORLD`, `LEAGUE`), same assertions.
- **`core/offline/OfflineGameIndex.kt`** — loads the gzipped TSV from assets, builds a
  normalized-name -> record map (every alternative name too), and mirrors `resolveGame.ts`'s
  `matchCandidates()`: try ranked candidates in order, score every hit against the *original*
  text (never the candidate that found it — same anti-false-positive rule as the server), stop
  early once confident. The one structural difference from the server: "search" is an exact
  dictionary lookup instead of an IGDB API call, because the whole dictionary is on-device now.
  Parsing is factored into a pure `internal fun parse(InputStream)` specifically so
  `OfflineGameIndexTest` can exercise it against the **real shipped asset file** from a plain
  JVM test — no Android/Robolectric needed — including a regression test that fails if "NaN"
  ever reappears in the shipped file, and a live check that `"BG3"` resolves via
  `alternative_names` (the whole reason this pipeline exists).
- **`ShareTargetViewModel`** now tries the offline index *first*, before any network call. A
  confident offline hit (`GameNameCandidates.CONFIDENT_ENOUGH = 0.9`, same threshold as the
  Worker) skips the Worker resolve entirely — **CLAUDE.md constraint #5 (offline-first) is now
  actually true for the headline demo feature**, not just claimed for it, and shares in
  airplane mode work. A weaker or absent offline hit falls through to the existing network
  path, and the two candidate lists are merged by IGDB id (safe: both are the same `games`
  table, one read via REST, one via CSV dump), keeping whichever confidence is higher per id.
- **`OfflineGameIndex` is warmed up at app startup** (`ContinueApplication.onCreate`, same
  pattern already used for `SeedLoader`) so decompressing ~41k rows doesn't happen on the
  critical path of the first share.
- **`IgdbImage.coverUrl(imageId, size)`** added — the dump's `covers` table gives a bare
  `image_id`, not a URL, so offline matches need to build one the same way the Worker does.

**Not done, and deliberately out of scope for this pass:** the offline record only carries
`id`/`name`/`coverImageId`/`ratingCount` — not year/rating/hours, even though the index has
them. Enriching a `GameEntity` with that data on add (for offline-matched games specifically)
would be a real improvement over the current "bare id+name+cover, then null forever" add flow —
but that flow is pre-existing for *every* share-added game, offline or online, so fixing it
belongs to its own pass rather than riding along here.

**Verified:** `assembleDebug` + `test` green (14 new tests, all passing, both variants), worker
`tsc`/tests untouched and still green, `versionCode 3` `.aab` rebuilt with the new asset and
re-signed. **Not yet verified on a device** — nobody has shared a real caption against a phone
running this build.

### An unrelated pre-existing lint error, found and (mostly) fixed while verifying

Ran `./gradlew lint` as an extra check before finalizing — not previously part of this
project's verification loop. Found one real lint **error** (not warning), pre-existing and
unrelated to anything this session touched: `feature/share/PileShareScreen.kt`'s `produceState`
call tripped `ProduceStateDoesNotAssignValue`. Traced it and confirmed it was **functionally
harmless** — `PileShareViewModel`'s `isLoading` flips `true`→`false` exactly once, which re-keys
and reruns the producer regardless — but genuinely fragile shape (a conditional-only
`value = …` is a real bug the moment a key can change more than once, which is exactly what the
check exists to catch). Rewrote to extract the branch into a `pileCardOrNull()` helper so the
lambda has one unconditional assignment. **The lint error still fires on the rewritten code**,
even after three structurally different attempts and a `--rerun-tasks` to rule out caching —
this looks like a limitation of this lint-checker version with this particular
`produceState<T>(initialValue=…, key)` overload/generic-argument shape, not a real bug. Left as
a known-harmless, unresolved lint finding rather than sunk further time into it; `lint` is not
part of the app's actual verification loop (`assembleDebug`/`test`, both green, are).

---

## 2026-08-14 — first phone test, the paywall, and IGDB data dumps

Everything before this was tested on a Galaxy Tab S6 Lite. The first run on a phone found a
**layout break that a tablet structurally could not surface**, which is the headline lesson of
the session.

### Nine reported items, and what changed

1. **PILE's header was broken on a phone.** The five state tabs and three icon buttons shared
   one un-scrollable `Row`. A `Row` hands leftover children zero width rather than wrapping, so
   `NOW PLAYING` rendered as a squeezed vertical sliver and the last three tabs were
   **unreachable entirely**. Tabs are now a full-width `horizontalScroll` strip of their own,
   with the actions moved to their own row below.
   **Lesson, and it rhymes with bug #9:** a wide test device can hide a layout bug indefinitely,
   the same way a screen that hides the bottom bar hid a crash in it. Test the narrow case.
2. **Coin counter sat under the status bar.** `enableEdgeToEdge` is on but neither bar consumed
   insets. `ArcadeScaffold`'s top bar now takes `statusBars` padding and the nav bar takes
   `navigationBars` padding — background first, padding second, so the cabinet colour still runs
   edge to edge behind both.
3. **Filters can now be collapsed** (§1 of the report). Sort + filter chips cost four rows on a
   phone, more than the games did. They're behind a `SORT & FILTER` disclosure, collapsed by
   default, which carries the **active-filter count** so a hidden filter can never silently
   explain an empty-looking pile. The GRID/LIST pair also became one button showing the mode
   you'd switch *to*, reclaiming 48dp of header.
4. **Covers were pixelated on the DRAW card** (§2). The Worker serves one `t_cover_big` URL —
   **264x352** — which is fine in a 3-column grid and ~3x upscaled on the DRAW card. The app now
   rewrites the size token per surface (`core/util/IgdbImage.kt`, which carries the measured
   size/byte table).
5. **Credits Roll key art was much worse** (§4), and for a different reason: `backgroundUrl` was
   **hardcoded `null`** in the Worker, so it fell back to painting that same 264px cover across
   a whole screen. Now populated from IGDB `screenshots`/`artworks`. See §Key art below — the
   obvious ordering of those two is the wrong one.
6. **The share sheet had no exit** (§9). No X, no outside-tap dismiss; `windowIsFloating=false`
   means the platform never gives you `windowCloseOnTouchOutside`, so the only way out really
   was to kill the app. Added a scrim that dismisses on tap and an X in the header.
7. **Sharing from TikTok/YouTube yanked the user into the app** (§8). Root cause was
   `android:launchMode="singleTask"` on `ShareTargetActivity`: singleTask forces the activity to
   be the root of its own task, so the system runs a task-switch animation instead of drawing
   over the caller. Removed (back to `standard`) + `taskAffinity=""`, so the sheet is pushed
   onto the *sharing app's* task and Back returns to the video.
8. **The DRAW deal animation** (§3) went from three grey rectangles sliding up — which read as a
   spinner — to cards **ejected one at a time from a dispenser slot**: squashed inside the
   machine, sprung past their resting place, fanned on settle, with the slot pulsing as each one
   passes. Built from transforms only, no art. A literal slot-machine reel rig was considered
   and **not** built: it needs real art to not look cheap, and the request was explicitly "only
   if you're confident". `DEAL_ANIMATION_MS` is now shared with `DrawViewModel` so the phase
   can't end mid-eject the way a hardcoded 900ms would have.
9. Everything else in the 0.2.0 build was reported working.

### Key art — screenshots beat artworks, counterintuitively

Artwork is promotional key art with no HUD, so it looks like the obvious pick. Its aspect ratio
is unconstrained: Elden Ring's first artwork returns from `t_1080p` as **1920x295**, an
ultrawide banner that upscales ~8x when cropped into a portrait phone — worse than the cover it
replaced. Screenshots are captures and are dependably 1920x1080. So: **screenshots first,
artworks as fallback.** Verified against live responses, not the schema.

Games added before this change have `backgroundUrl == null` in Room forever, since nothing
re-reads a game once it's in the pile. `CreditsRollViewModel` now backfills key art on demand
via a new `/games/{id}` data-source method, after the state emit so the cinematic still starts
on time.

### The paywall is built (Week 4's biggest gap) — hand-designed, not RevenueCatUI

`feature/paywall/` is a **custom Compose paywall**, reachable from the DRAW gate's GO PRO
**and** from a new banner on YOU — previously the only route to it was exhausting the daily
free draw, so a judge could easily never have seen it.

**Why not RevenueCatUI.** It was built with RevenueCatUI first and replaced the same session.
A dashboard-rendered paywall can be re-themed without shipping an APK, which is real value —
but its templates can't produce the arcade cabinet this app is, and a paywall that looks like
every other RevenueCat paywall is a bad answer in a design category. The part of the
dashboard's value that actually mattered is kept: **prices are still read from the store at
runtime** (`ProTier.priceFormatted` is Google's own `Price.formatted`), so a price change in
Play Console still needs no code change, and it is localised for free.

On the screen: a pulsing cabinet marquee, four perks that are each a capability the shipped
build really has (with the free-tier limit each one removes stated, so the value is checkable),
selectable Monthly/Lifetime tier cards, a CTA whose label follows the selection, plain-words
subscription terms, and RESTORE PURCHASE.

Three decisions worth not re-litigating:
- **Lifetime is preselected.** Monthly exists largely to make Lifetime look obvious
  (docs/07-SUBMISSION-KIT.md §Pricing rationale), so the default should be the one we want
  taken — and the one that isn't a recurring charge someone has to remember to cancel.
- **Renewal terms are on the screen, not only in the store listing.** Play policy requires it,
  and a trial that converts silently is the fastest route to a refund and a one-star review.
- **A cancelled purchase shows no error.** A cancel is a choice, not a failure; treating it as
  an error is how a paywall starts feeling hostile.

**`core/billing/ProTier.kt`** is the new seam: `feature/paywall` never imports a RevenueCat
type. That is what lets `FakeBillingRepository` return convincing tiers, so **the paywall is
fully demoable and screenshottable in a debug build**, which cannot reach Play Billing at all.
The real implementation flattens RevenueCat's
`Package` -> `StoreProduct` -> `SubscriptionOption` -> `PricingPhase` chain, which is also
where the free-trial length actually lives (it is *not* on the product).

`DrawViewModel.goPro()` is **gone**. It used to buy `currentOfferingPackage()` — whatever
happened to be first in the offering — with no price shown and no choice between tiers.

`restorePurchases()` was added to `BillingRepository` at the same time. Play requires a restore
path for non-consumables and Lifetime is one; without it a reinstall silently loses a real
purchase.

### IGDB data dumps — pipeline built, not yet run

Access was granted for our Client ID on 2026-08-14. `tools/igdb_dump_index.mjs` is written and
syntax-clean but **has never been executed**, because it needs `TWITCH_CLIENT_SECRET`, which
correctly exists only inside `wrangler secret put`. It reads from `worker/.dev.vars`
(gitignored) — one paste unblocks it.

The important architectural decision is recorded in docs/08-GAME-DATA.md: **the index ships in
the app, not the Worker**, because Workers Free allows only **10ms of CPU per request** and
scanning a multi-megabyte index blows that on the first request into every cold isolate.

Worker changes this session: `backgroundUrl` populated, `CACHE_VERSION` v2 → v4. Deployed and
verified against live traffic (all three rate limiters still bound). 28/28 Worker tests pass,
`tsc --noEmit` clean, `assembleDebug` + `test` green. **Nothing in this session has been run on
a device** — `adb devices` was empty throughout.

---

## 2026-08-12 (last) — security audit before making the repo public

Full pass ahead of pushing to the public GitHub repo. **Findings and fixes are written up in
`docs/12-SECURITY.md`** — read that, not this summary, before touching the Worker or secrets.

- **Secrets: clean.** No secret file has *ever* been committed (whole-history scan). Every live
  secret file is gitignored. The only secret-shaped strings in history are the RevenueCat
  *public* key and `getProperty("storePassword")` call sites.
- **The Worker had no abuse controls at all** — a public, unauthenticated endpoint on free-tier
  quotas. The binding constraint is **KV writes: 1,000/day**; one uncached search is one write,
  so ~1,000 scripted queries would have killed caching for the day and dumped everything onto
  IGDB. Added three rate limiters (per-IP, a tighter one for the 6x-amplifying `/resolve`, and
  an account-wide backstop against distributed abuse), amplification caps, and input/body size
  limits.
- **Real SSRF fixed.** `/resolve` fetches client-supplied URLs and gated them with
  `hostname.includes("tiktok.com")` — so `vm.tiktok.com.attacker.example` passed and could have
  aimed our server-side fetch at any host. Now dot-anchored matching, https-only, with tests.
- **`/steam/owned` was the worst amplifier**: unbounded `Promise.all` over a whole Steam
  library turned one request into thousands of IGDB searches. Capped at 100, sequential.
- **CORS `*` removed** (it let any web page use us as a free games API) and internal error
  messages no longer leak to clients.
- **Android:** `usesCleartextTraffic="false"` added. Backup rules confirmed to exclude
  DataStore, so backup/restore can't duplicate coins.

> **Repeat of a lesson this project keeps learning:** the first rate-limiter deploy **silently
> did nothing** — wrangler v3 ignores `[[ratelimits]]` without warning, and 150/150 requests
> still returned `200`. Only firing real traffic caught it. Fixed by upgrading to wrangler v4
> (needs `@cloudflare/workers-types@5` in the same install). Same shape as the IGDB `category`
> bug: **config that looks applied but isn't.** Always verify with traffic, never with source.

Worker test count is now **28** (added `test/security.test.ts`).

---

## 2026-08-12 (later) — first real device-feedback pass, six fixes

Mikhil tested the `versionCode 1` build against the fixed Worker and reported six things. All
six are fixed; this is the first round of changes driven by someone actually using the app.

1. **Tapping a game in PILE silently teleported it to NOW PLAYING.** A BACKLOG card's `onClick`
   called `moveToPlaying` directly, so the card just vanished from the list with no
   confirmation and no visible undo — it read as a bug, not an action. Both tap and long-press
   now open the same **MOVE TO** chooser offering all five states (NOW PLAYING / CLEARED / THE
   PILE / WANTED / RETIRED), each with a one-line description, with the game's current state
   shown as a disabled row rather than hidden so the list never reshuffles. CLEARED still
   routes through the Credits Roll, and NOW PLAYING still routes through `moveToPlaying` so
   the cap-of-3 swap prompt survives. All five `PileState` values already existed — only the
   menu was incomplete.
2. **Coin balance was invisible.** `CoinCounter` existed but was never placed. It's now in a
   top bar in `ArcadeScaffold` (plus a `PRO` badge), fed by a new `AppChromeViewModel`, and
   hidden on the same immersive routes as the bottom bar.
3. **Rewarded ad played but granted nothing** — `RealBillingRepository.earnCoins()` and
   `spendCoins()` both returned a hardcoded `SpendResult.Error` about missing server-side
   verification, so the entire coin economy was dead in release builds. Now backed by a real
   on-device `CoinLedger` (DataStore). See §Coins below for why local is the right call.
4. **Share target matched nothing from a YouTube title** — the big one, see §Share matching.
5. **Manual-entry field prefilled a whole video caption** with no way to clear it. Added a
   trailing clear icon and a placeholder.
6. **IGDB partnership reply arrived.** IGDB confirmed we're a commercial use and asked seven
   questions to start a (free) agreement. See `docs/11-IGDB-PARTNERSHIP.md`.

**Also fixed, unprompted — an actual compliance gap:** the mandatory IGDB attribution existed
only on the YOU tab, which shows *no* IGDB data, while DISCOVER (search results, both rails,
all cover art) had none. Attribution added to DISCOVER. This is contractual under the
partnership terms, not cosmetic.

**AdMob ad unit ids are now `BuildConfig` values** (`ADMOB_UNIT_COIN`,
`ADMOB_UNIT_FREE_PLAY`) read from `local.properties`, defaulting to Google's public test
units. Real units exist but won't fill ads until the app is live on Play, so test units stay
correct for now — and the switch is a config edit, not a code change.

### Share matching — why it failed and what changed

Measured against the live IGDB API, **`search` is near-exact, not fuzzy**:

| Query | Results |
|---|---|
| `Each Pal has their own method of transporting items Pocketpair Palworld` | 0 |
| `Pocketpair Palworld` | **0** |
| `Palworld` | 3 |
| `a tale of two bush ganks League of Legends` | 0 |
| `League of Legends` | 11 |

One extra word kills the query. We were sending IGDB the entire caption, so it essentially
never matched — even though the game's name was sitting right there in the string.

New `worker/src/resolve/candidates.ts` builds an **ordered shortlist of substrings** to try
(hashtags → whole capitalized runs → windows inside those runs → trailing n-grams → the full
caption), and `resolveGame` searches the top 6 with an early exit. Crucially, every hit is
scored by `verifyAgainstText` — **does this game's name actually appear as whole words in the
original caption?** — which is what makes searching a single word like `"Palworld"` safe.
Longer matched names beat shorter ones, and roman numerals are folded (`III` ⇄ `3`).

Verified live after deploy:

| Caption | Match |
|---|---|
| `…items📦 Pocketpair Palworld` | **Palworld** (0.85) — beats the real game *"Pal"* |
| `a tale of two bush ganks 🤔 League of Legends` | **League of Legends** (0.99) |
| `Baldur's Gate 3 romance guide` | **Baldur's Gate III** (0.97) |
| `Final Fantasy VII Rebirth is peak` | **Final Fantasy VII** (0.97) |
| `my cat sat on the keyboard lol` | *no match → manual entry* ✅ |

Resolve's searches now also go through the KV cache under the same key space as
`/games/search`, so a share warms the search cache and vice versa — without that, one share
could spend 6 uncached IGDB requests against a 4 req/sec ceiling.

`titleParser.ts`/`TitleParser.kt` were deliberately **not** changed, so their intentional
Kotlin/TypeScript mirroring and 13 shared test cases still hold. 9 new tests cover the new
module, built from the two real captions captured off Mikhil's device.

### Coins — deliberately on-device, and why

`CoinLedger` (DataStore) is now the source of truth for the COIN balance, not RevenueCat.
Two reasons, both structural:
- **CLAUDE.md constraint #5 is offline-first**, and coins gate DRAW — the app's central
  action. A server-authoritative balance makes the core loop fail on a train.
- **RevenueCat virtual currency cannot be credited from a client at all.** Granting goes
  through the REST API with the *secret* key, which must never ship in a public repo's APK.

So: coins **earned** in-app are authoritative locally; coins **purchased** are granted by
RevenueCat server-side and folded in via `creditPurchased`. This is not fraud-proof — clearing
app data resets it — and that's an accepted trade-off until AdMob server-side verification is
possible (which needs real ad units, which need a production Play listing). Everything is
shaped so that swap changes the *source*, not any call site: features only ever talk to
`BillingRepository`.

---

## 2026-08-12 — DISCOVER and search returned zero games (fixed)

Mikhil installed the Internal-testing build and found **no games at all** — empty DISCOVER,
empty search. Before IGDB was wired up, the seed set at least showed placeholder titles, so
this was a regression. Two independent bugs stacked, and either one alone would have been
survivable:

1. **Worker (root cause).** Every list query filtered on `where category = 0`. IGDB
   **deprecated `category` in favour of `game_type`** and stopped populating it. Filtering a
   deprecated field is not a syntax error — IGDB answers `200 OK` with `[]`. So
   `/games/search`, `/games/trending`, and `/games/short` all returned zero results with no
   error anywhere. Fixed by switching to `game_type = 0`. See bug #10 in §3.
2. **App (why it was total, not partial).** `WorkerGameDataSource` used
   `runCatching { … }.getOrElse { seedFallback() }`. A **successful** response carrying an
   empty list is not an exception, so the fallback never fired and 426 seeded games sat unused
   in Room while the UI showed nothing. Fixed so empty is treated like failure. See bug #11.

**Three things made this hard to see, and all three are now fixed:**
- `/health` only asserted that credentials *existed*, so it cheerfully reported
  `{"ok":true,"provider":"igdb"}` for a Worker serving nothing. There is now a
  `/health?deep=1` that runs a real query and returns `ok:false` on zero results.
- `cached()` stored the empty arrays for 6–24h, so the bug outlived its own cause. It now
  **refuses to cache empty lists**, and cache keys are **versioned** (`CACHE_VERSION` in
  `kv.ts`) so a query-shape change invalidates everything at once.
- Nothing failed loudly. IGDB's error body is now included in thrown errors.

**Fixed in the same pass, because the data was finally real enough to judge:**
- **Playtimes are now populated.** Every list result carries real `hastily`/`normally`/
  `completely` hours from `/game_time_to_beats`, fetched in **one** extra batched request per
  list rather than one per game. They were all `null` before — which quietly starved the time
  budget and DRAW's TIME dial, the two features that most need them.
- **TRENDING is no longer unreleased games.** It sorted by `hypes`, which counts *pre-release*
  follows, so a backlog app was recommending games nobody can play yet. Now: released in the
  last 3 years, sorted by `total_rating_count`.
- **SHORT & SWEET is now actually short.** It approximated "short" with `aggregated_rating >
  75`, which is a quality filter wearing a length costume. Now sourced from real time-to-beat
  data under 8 hours — returns Portal (4h), Journey (3h), Limbo (4h), Inside (4h).
- **Removed the `PULL THE LEVER` placeholder onboarding step**, which shipped to testers
  showing them internal text: *"is a Week 3 build (docs/06-BUILD-ROADMAP.md)"*. DRAW has been
  fully built since 2026-08-06, so the step was stale as well as embarrassing. Onboarding now
  ends at SEED, and **"SEARCH FOR GAMES" lands on DISCOVER** instead of dropping the user on
  an empty PILE to find search themselves.

**Verified after deploy** (real responses, not assumptions): `/health?deep=1` →
`{"ok":true,"sampleCount":20}`; search "elden ring" → Elden Ring with cover art and 45h/119h/
174h playtimes; trending → Clair Obscur, Silksong, Balatro; short → Portal 4h, Journey 3h.

**Consequence worth knowing:** the root fix was *server-side*, so the **already-uploaded
`versionCode 1` build now shows games without reinstalling.** The new `versionCode 2` build is
needed only for the app-side items (empty-list fallback, onboarding cleanup).

---

## 2026-08-11 infra session — what changed (credentials/backend, not app code)

No app code changed this session. What moved:
- **RevenueCat dashboard fully configured** via MCP: 6 products (3 subscriptions, 3
  consumables), 2 offerings (`default` is_current, `coins`), 6 packages, entitlement
  attachments, COIN auto-grants. Exact ids and the required matching Play Console product
  table are in `docs/09-PENDING-INPUTS.md`. Pricing was revised down from the original spec:
  `continue_pro_lifetime` $39.99→$9.99, `coins_500` $6.99→$4.99.
- **Cloudflare Worker deployed for real**, `https://continue-worker.gamestoplay.workers.dev`,
  with Twitch secrets set — `/health` confirms the **real IGDB provider is live**, not the
  seed fallback. This means Share Target auto-match (§ below) now actually works end-to-end.
- **First signed release App Bundle built**: `app-release.aab` (`versionCode 1`,
  `versionName "0.1.0"`) at `app/build/outputs/bundle/release/`, ready for Play Console
  Internal testing upload. Not yet confirmed uploaded as of session end.
- **Correction to the Play Store critical path**: the 12-tester/14-day closed-testing gate is
  **per-app, not per-account** — Mikhil's other app on the same account hasn't cleared it
  either, so CONTINUE? has no head start. This is now flagged as the single biggest schedule
  risk — see `docs/01-PLAY-STORE-CRITICAL-PATH.md`.
- Play Console setup (products, purchase options) was in progress when the session ended —
  **status of the 6 Play-side products is unconfirmed**, check `docs/09-PENDING-INPUTS.md`.

---

## TL;DR

**Phase 1 (Weeks 1–2) and most of Phase 2 (Weeks 2 remainder, 3, and partial 4/5) are built,
and now confirmed running on a real device**, not just compiling. This session added the
three signature Week 3 features (DRAW, Credits Roll, pairwise ranking) in full, plus the
Week 2 remainder (Stacks CRUD, GRID/LIST toggle, platform/genre/length filters), the
DRAW-side economy loop from Week 4 (CONTINUE? gate, coin spend, ad-watch-for-coin, GO PRO
trigger — all against `Fake`/real-but-gracefully-empty billing), and a first slice of Week 5
(YOU tab stats/trophies, one working share card). See §6 for the precise per-week ledger,
including what's still genuinely missing in each week.

**One real crash was found and fixed installing on-device** (negative `Modifier.padding` on
the raised DRAW button — see §3 bug #9). After the fix, the app installs, launches, and the
bottom nav bar (with DRAW button) renders without crashing on a Samsung Galaxy Tab S6 Lite.
**That's as far as on-device verification got this session** — the app was confirmed alive
and in the foreground (no further crash in logcat), but nobody had manually tapped through
DRAW's lever, the swipe cards, Credits Roll, RANK, Stacks, filters, or the share card on the
device before the session ended (context window ran long, session was wrapped up to hand off
fresh). **A fresh session should pick up by asking Mikhil what he's tested and finish tapping
through the rest**, fixing whatever else logcat turns up the same way bug #9 was fixed. Treat
anything not explicitly listed as "on-device confirmed" in §2 as compile-verified only, not
interaction-verified.

**Not built this session, and worth knowing before you assume otherwise:** the STACK 3D
"wow" view (GRID/LIST only), the RevenueCatUI paywall screen itself, FREE PLAY mode, Customer
Center, 4 of the 5 share card types, the full Time Budget tap-to-expand visualization, and the
clipboard nudge's actual detection logic (the settings toggle persists, but nothing watches
the clipboard yet). None of these are blocked on credentials — they're just not built yet.

**Also worth knowing:** the Android Share Target (docs/02-PRODUCT-SPEC.md §2a, the "share
from YouTube/TikTok" feature) is fully built, registers correctly in the OS share sheet, and
**its auto-match now works for real** — the Worker is deployed and, since the 2026-08-12 fix
above, `/resolve` has actual IGDB games to match against (it was matching against nothing
before, so every share silently fell through to manual entry). This is the headline demo
feature and it has never been exercised on-device; worth putting near the top of the
device-testing pass.

---

## 1. Local build environment (now set up, don't redo this)

| Thing | Path |
|---|---|
| Android SDK | `C:\Android\sdk` (Platform 36, Build-Tools 35.0.0, Platform-Tools) |
| JDK for Gradle | `C:\Android\jdk21\jdk-21.0.12+8` (Temurin 21 — **required**, the JDK 25 on this machine is too new for Gradle 8.10.2) |
| `local.properties` | Has `sdk.dir=C:/Android/sdk` and the **real** RevenueCat public key |

**To build:** always set `JAVA_HOME` to the JDK 21 path first, e.g. in Git Bash:
```bash
export JAVA_HOME="/c/Android/jdk21/jdk-21.0.12+8"
cd "/c/Dev/GamesToPlay"
./gradlew assembleDebug     # or assembleRelease, test, etc.
```
Without that `JAVA_HOME` override, `gradlew` will pick up the system JDK 25 and likely fail.

**Known environment quirk:** this sandbox's network cannot sustain large single-connection
downloads (curl gets connection-reset after several minutes on anything >~50MB). Gradle/Maven
dependency resolution (many small-to-medium files) works fine. If a large binary is ever
needed again, either fetch it in byte-range chunks (see `tools/chunked_download.sh`) or have
the user download it in their browser and extract it locally — that's what got the Android
SDK cmdline-tools and JDK 21 installed this session.

---

## 2. What's verified working (ran for real, not just typechecked)

| Check | Result |
|---|---|
| `./gradlew assembleDebug` | ✅ BUILD SUCCESSFUL (Phase 2 re-verified) |
| `./gradlew assembleRelease` | ✅ BUILD SUCCESSFUL — R8 minify + resource shrink both passed clean, a good signal against missing-class/reflection issues from all the new Hilt/Room/Compose code |
| `./gradlew test` | ✅ BUILD SUCCESSFUL — 35/35 unit tests pass (debug + release variants), including the two new pure-logic suites below |
| `TitleParserTest` (Kotlin) | ✅ 13/13 pass |
| `MoodMapperTest` (Kotlin, new) | ✅ 8/8 pass — docs/08-GAME-DATA.md mood table |
| `DrawSelectorTest` (Kotlin, new) | ✅ 9/9 pass — docs/02-PRODUCT-SPEC.md §3 weighted selection, hard filters, relaxation |
| `PairwiseRankerTest` (Kotlin, new) | ✅ 6/6 pass — docs/02-PRODUCT-SPEC.md §5 insertion-position math |
| `worker/test/titleParser.test.ts` (TypeScript port) | ✅ 13/13 pass via `node --test --experimental-strip-types` (unchanged this session) |
| Worker `tsc --noEmit` | ✅ zero errors (unchanged this session) |
| Actual on-device run | ✅ **Re-verified 2026-08-06** on the same Galaxy Tab S6 Lite (device `R52W70DGYEA`) — installs, launches, no crash, process stays alive in foreground. Found and fixed one real launch-time crash in the process (bug #9 in §3). ⚠️ **But** this only confirms cold-launch-to-PILE-screen; DRAW's lever, swipe cards, Credits Roll, RANK, Stacks, filters, and the share card have **not** been manually tapped through on-device yet — do that next (see TL;DR and §8). |
| `wrangler dev` (local Worker server) | ❌ Still not verified — same sandbox limitation as before. Not needed in practice: deploy-then-curl against the real Worker is the loop that's been working. |
| **Deployed Worker returning real IGDB data** | ✅ **Verified 2026-08-12 against live responses.** `/health?deep=1` → `{"ok":true,"sampleCount":20}`; `/games/search?q=elden ring` → Elden Ring with cover art, 95.2 rating, 45h/119h/174h playtimes; `/games/trending` → 20 released games with covers; `/games/short` → Portal 4h, Journey 3h, Limbo 4h. |

---

## 3. Bugs found and fixed while getting the real build working

These are worth knowing so they don't get silently reintroduced:

1. **Nested KDoc comments.** Kotlin block comments nest. Three doc comments contained a
   literal `` `/games/*` `` or `` `image/*` `` — the embedded `/*` opened a second comment
   level that the closing `*/` didn't fully close, causing "Unclosed comment" across three
   unrelated-looking files (`GameDto.kt`, `ShareTargetActivity.kt`, `ShareTargetViewModel.kt`).
   Rephrased to avoid literal `/*` inside any comment.
2. **Wrong Font import package.** Used `androidx.compose.ui.font.*`; the real package is
   `androidx.compose.ui.text.font.*`. Fixed in `core/design/Type.kt`.
3. **Missing experimental opt-in.** Variable-font support (`Font(..., variationSettings = ...)`)
   needs `@OptIn(ExperimentalTextApi::class)`. Added at file level in `Type.kt`.
4. **`com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter` doesn't resolve
   from Kotlin sources in this project's toolchain** — the dependency resolves fine in
   Gradle's graph and the class is visible to `javap`, but `kotlinc` reports "Unresolved
   reference" for it. Root cause not diagnosed (didn't seem worth more time). **Replaced
   entirely** with an in-house `core/network/JsonConverterFactory.kt` (~20 lines) built on
   kotlinx.serialization's own public `serializer(Type)` JVM helper. The dependency was
   removed from `gradle/libs.versions.toml` and `app/build.gradle.kts`. If you ever see that
   jakewharton artifact suggested again (e.g. by an IDE quick-fix), don't re-add it without
   testing — prefer `JsonConverterFactory`.
5. **`local.properties` had the placeholder RevenueCat key**, not the real one from
   `CLAUDE.md`. Fixed — real key `goog_uEkBWERrtERDlPxqQbcYpxvxXEH` is in there now.
   `local.properties` is gitignored, so this fix doesn't show up in git history — if you ever
   regenerate `local.properties` from `local.properties.example`, remember to swap this back in.
6. **Real crash found on-device**: `local.properties` also still had the literal placeholder
   `ca-app-pub-xxxxxxxxxxxxxxxx~xxxxxxxxxx` for `ADMOB_APP_ID`. AdMob's `MobileAdsInitProvider`
   validates that ID at process startup — *before* `Application.onCreate` runs — so an invalid
   ID crashed the app on every single launch with `IllegalStateException: Invalid application
   ID`, on a real device, immediately. **Fixed** by using Google's official public test AdMob
   App ID (`ca-app-pub-3940256099942544~3347511713` — safe, documented, not a secret) until a
   real one exists. Re-verified after the fix: installs, launches, and renders cleanly on a
   real Samsung Galaxy Tab S6 Lite. **Same lesson as bug #5** — check every value in
   `local.properties` against real/valid data, not just the RevenueCat key, whenever it's
   regenerated from the template.
7. **`Modifier.graphicsLayer` is in `androidx.compose.ui.graphics`, not `androidx.compose.ui.draw`**,
   on Compose UI 1.7.6 (this project's BOM). Importing the intuitive-looking `androidx.compose.ui.draw.graphicsLayer`
   compiles as a plain unresolved-reference error on the import line itself — no ambiguity,
   no warning, it just silently fails to resolve. Confirmed by inspecting the actual class in
   the cached `ui-release-api.jar` (`androidx/compose/ui/graphics/GraphicsLayerModifierKt.class`).
   Hit in `DrawScreen.kt`, `DrawCardStack.kt`, `CreditsRollScreen.kt` — fixed by switching the
   import. If an IDE or a memory of an older Compose version suggests `ui.draw`, don't trust
   it on this BOM.
8. **`GraphicsLayer` recording (`rememberGraphicsLayer()` / `drawLayer()` / `toImageBitmap()`)
   from `androidx.compose.ui.graphics.layer` does not resolve on this BOM (2024.12.01 /
   Compose UI 1.7.6)** — that capture API landed in a later Compose release. Attempted to use
   it for the share-card screen and hit `Unresolved reference` on every entry point.
   **Don't reach for it again without first bumping the Compose BOM** — the share card
   renderer was rewritten to draw directly with `android.graphics.Canvas` instead (see
   `core/share/ShareCardRenderer.kt`), which has zero Compose-version risk and produces the
   identical PNG deliverable.
9. **Real crash found on-device (2026-08-06, Phase 2 install)**: `core/ui/ArcadeScaffold.kt`
   used `Modifier.padding(top = (-24).dp)` to visually raise the circular DRAW button above
   the bottom nav bar. `Modifier.padding` throws `IllegalArgumentException: Padding must be
   non-negative` on negative values — it always would have, but this code path only executes
   once the bottom bar actually renders, and it never had during Phase 1's on-device check
   because that check happened on the onboarding screen, which hides the bottom bar. First
   real exercise of the bottom bar (i.e. reaching PILE) crashed on launch every time. **Fixed**
   by switching to `Modifier.offset(y = (-24).dp)` — `offset` (unlike `padding`) explicitly
   allows negative values and is the correct API for this kind of visual displacement.
   Re-verified: installs, launches, reaches PILE, bottom bar with the raised DRAW button
   renders without crashing. **Lesson**: a screen that hides its bottom bar (or any shared
   chrome) can mask a crash in that chrome indefinitely — don't treat "onboarding works
   on-device" as "the bottom bar works on-device already," check both explicitly.
10. **A deprecated IGDB filter silently returned zero rows (2026-08-12).** Every Worker list
    query used `where category = 0`; IGDB deprecated `category` in favour of `game_type` and
    stopped populating it. **Filtering on a deprecated IGDB field returns `200 OK` with `[]`,
    not a 4xx** — so there was no error to find, in any log, at any layer. Fixed by switching
    to `game_type = 0` (see `MAIN_GAMES_ONLY` in `IgdbGameProvider.ts`, which now carries a
    comment saying why). **Lesson:** a third-party filter that stops matching is invisible —
    it looks exactly like "no results exist." When a provider deprecates a field, assume the
    field will one day return nothing rather than erroring, and put at least one assertion on
    *non-empty* results somewhere you'll actually see it.
11. **`runCatching { … }.getOrElse { fallback }` does not catch an empty success
    (2026-08-12).** `WorkerGameDataSource`'s offline-first fallback to the Room seed set only
    fired on a thrown exception, so when the Worker returned `{"results":[]}` with a 200, the
    app rendered nothing while 426 usable seeded games sat in the database. Fixed with an
    explicit `.ifEmpty { seedFallback() }`. **Lesson:** "offline-first" has to include
    "empty-first" — to a user, a healthy backend serving nothing is indistinguishable from an
    outage, so it should degrade identically. (The rewrite also stopped swallowing
    `CancellationException`, which `runCatching` catches and which breaks structured
    concurrency.)

---

## 4. What's built (Phase 1 = roadmap Weeks 1–2)

- **Project scaffold**: Gradle Kotlin DSL, Compose, Hilt, Room, DataStore, Navigation,
  Retrofit + kotlinx.serialization, Coil3. `minSdk 26` / `targetSdk 36`.
- **Release signing**: `app/keystore/continue-release.jks` generated and wired via
  `key.properties` (gitignored). **Back this keystore up in two places — still not done as
  far as this assistant knows; ask the user to confirm.**
- **Design system**: color/type/motion tokens, bundled Chakra Petch/Inter/JetBrains Mono
  fonts, `ArcadeButton`, `ArcadeDrawButton`, `GameCard`, `CoinCounter`, `ArcadeScaffold`,
  `EmptyState`.
- **Room data layer**: all five entities/DAOs from `docs/05-TECH-ARCHITECTURE.md` exactly
  (three playtime fields on `GameEntity`, etc).
- **Offline seed set**: 426 real, curated game titles (genre-median playtime estimates, not
  IGDB-accurate) in `app/src/main/assets/seed_games.json`, regenerable via
  `tools/generate_seed.mjs`. IDs are negative so they never collide with real IGDB ids.
- **`GameDataSource`**: Retrofit client to the Worker, with automatic fallback to the Room
  seed data when the Worker is unreachable (offline-first is real, not just claimed).
- **Billing/Ads seams**: `BillingRepository`/`AdRepository` interfaces, Fake (debug) + Real
  (release) impls, Hilt-wired. **`Purchases.configure()` runs for real in
  `ContinueApplication.onCreate`** — every install gets a real anonymous RevenueCat
  app-user ID now that the placeholder key bug (§3.5) is fixed. See §5 for exactly how far
  this goes and doesn't go.
- **PILE screen**: grid view, all 5 states, NOW PLAYING cap-of-3 with swap-out prompt,
  sort (shortest-first / recent / A-Z), compact Time Budget bar.
- **DISCOVER screen**: 300ms-debounced search, instant add, TRENDING/SHORT & SWEET rails.
- **Android Share Target**: transparent bottom-sheet Activity for `text/plain` and `image/*`,
  ML Kit on-device OCR for the image path, full graceful-degradation ladder (confident →
  ambiguous top-3 → manual search, never dead-ends).
- **Title parser**: pure function, unit-tested in both Kotlin (app) and TypeScript (Worker),
  kept intentionally identical.
- **Onboarding**: cold open → pile-size question → seed step (search/skip; Steam import
  disabled with a "coming soon" label, correctly deferred to Week 4) → first-draw placeholder
  → done.
- **Cloudflare Worker** (`/worker`): all endpoints from the spec. `GameProvider` abstraction
  with `SeedGameProvider` (active today, no credentials needed) and `IgdbGameProvider`
  (switches on automatically once `TWITCH_CLIENT_ID`/`SECRET` are set via `wrangler secret
  put` — no code changes needed). Two-stage `/resolve` pipeline. `/coins/spend` and
  `/steam/owned` correctly fail closed (501) pending `REVENUECAT_SECRET_KEY` /
  `STEAM_API_KEY`. **Not deployed** — that's a `wrangler login && wrangler deploy` away,
  entirely on the user, not blocked on anything code-related.

---

## 4b. What's built (Phase 2 = roadmap Week 2 remainder, Week 3, and slices of 4/5)

Built in this session, on top of Phase 1, with nothing here blocked on Play Store/Twitch/AdMob
credentials — every item below runs against real Room data and (where applicable) the `Fake`
billing/ads repos in debug builds, same pattern as Phase 1.

**Week 3 — DRAW, Credits Roll, pairwise ranking (all three, in full):**
- `core/util/MoodMapper.kt` — genres/themes/release-year → `Mood` set, per the docs/08 table,
  unit-tested (`MoodMapperTest`).
- `feature/draw/DrawSelector.kt` — the full weighted-sampling algorithm from
  docs/02-PRODUCT-SPEC.md §3: hard filters (platform, time-budget-with-tolerance) relaxed in
  order with an on-screen "loosened to fit" message, then weighted sampling on mood match
  (×3), never-drawn-before (×2), longest-in-pile (×1.5, scaled over 180 days), highly-rated
  (×1.3), and 14-day snooze decay (×0.2 → back to 1.0). Pure function, unit-tested
  (`DrawSelectorTest`, 9 cases).
- `feature/draw/DrawScreen.kt` + `DrawCardStack.kt` + `DrawGateScreen.kt` — the full DRAW UI:
  TIME/MOOD/PLATFORM dial chips, a real drag-with-spring-resistance lever (not a button) that
  fires on threshold with a heavy haptic, a staggered card-deal animation, 3D flip-to-reveal
  cards, and velocity-aware swipe-to-verdict in all 4 directions (up = PLAYING IT → moves to
  NOW PLAYING; left = NOT TONIGHT → 14-day snooze; right = SAVE FOR LATER → pinned; down =
  RETIRE → dropped). Each card shows its "why matched" reasons.
- The **CONTINUE? economy gate** (docs §3): free users get 1 draw/day (tracked via
  `DrawDao.since()`), the 2nd+ attempt shows the looping 9→0 countdown screen with INSERT COIN
  (watch a rewarded ad → +1 coin via a new `BillingRepository.earnCoins()`), USE A COIN
  (spends via existing `spendCoins`), and GO PRO (queries the real
  `Purchases.sharedInstance.getOfferings()` via a new `currentOfferingPackage()` — correctly
  shows "not live yet" today since no Play Console product exists, and will Just Work once one
  does, no code changes needed).
- `feature/completion/CreditsRollScreen.kt` — the ~7-second skippable cinematic (CRT flash →
  typewriter "GAME CLEARED" → key-art fade/zoom → scrolling stats → +5 coins → RANK IT ▸),
  wired from a new long-press "MARK COMPLETE" action on NOW PLAYING cards in `PileScreen`.
  Completion coin grant reuses the same `earnCoins()` gate as ad rewards (fails closed on
  `RealBillingRepository` with the same "not configured yet" message, same as everywhere else
  coins are involved).
- `feature/rank/PairwiseRanker.kt` + `RankViewModel.kt` + `RankScreen.kt` — coarse bucket pick
  (LOVED/LIKED/FINE/NAH) then binary-search pairwise comparisons (capped at 5, per docs), with
  the insertion-position math split into a pure, unit-tested object (`PairwiseRankerTest`, 6
  cases) separate from the interactive ViewModel. Captures an optional 140-char verdict and a
  WOULD REPLAY toggle. Ends on "YOUR #N OF ALL TIME".

**Week 2 remainder:**
- GRID/LIST view toggle on PILE (**STACK's 3D "wow" view was not built** — see §6, this is a
  real gap, not just a rename).
- Platform / genre / length-bucket (`<5h`, `5-15h`, `15-40h`, `40h+`) filter chips, derived
  live from whatever's actually in the pile.
- Full Stacks CRUD: create, rename, delete (with a confirmation dialog), add/remove a game
  (from a new "ADD TO STACK" section in PILE's long-press menu, and from the Stacks screen
  itself), and the **Free: 2 stacks / Pro: unlimited** cap is enforced with a clear message —
  `feature/stacks/`.

**Week 4 slice (DRAW-adjacent economy only — paywall UI itself is still Week 4 remainder):**
- `BillingRepository.earnCoins()` and `currentOfferingPackage()` added to the interface; `Fake`
  grants/no-ops appropriately for debug demoing, `Real` fails closed with the same honest
  "not configured yet" pattern used throughout this codebase for anything touching
  not-yet-live Play Console products.
- The ad-watch-for-coin and coin-spend UI flows are real and wired end-to-end in the DRAW
  gate — this is the first place in the app any screen actually injects `BillingRepository` or
  `AdRepository` (Phase 1's build status noted zero screens did; that's no longer true).

**Week 5 slice:**
- `feature/profile/` (YOU tab) rebuilt from its Phase-1 stub: THIS YEAR stats (cleared, hours,
  longest clear, fastest clear, current draw-streak), HIGH SCORES (the live ranked list from
  RANK), TROPHIES (FIRST CONTINUE, PILE SLAYER, CRITIC, SPRING CLEANING, NIGHT SHIFT — real
  unlock conditions against Room data, not hardcoded), and working HAPTICS/CLIPBOARD-DETECTION
  settings toggles wired to the existing `UserPreferencesRepository`.
- One of five share cards — **"THE PILE"** ("N HOURS. N GAMES. SEND HELP.") — end-to-end:
  rendered natively via `android.graphics.Canvas` (see bug #8 in §3 for why not live Compose
  capture), saved through a newly-added `FileProvider`, and fired via real `ACTION_SEND`.
  Reachable from a new share icon on PILE.

---

## 5. RevenueCat — precisely how far the integration goes

**Real and working:**
- SDK configured with the real public key at app startup → anonymous app-user ID flows to
  the actual RevenueCat project (`proj747a0e7c`) from the first install, no products needed.
- `BillingRepository` listens for entitlement changes and correctly reads the `pro`
  entitlement's active/expiry state.
- The purchase call (`Purchases.sharedInstance.purchase(...)`) is written against the real
  SDK 10.12.0 API and compiles/ships correctly (proven by the successful signed release build).

**Also real and working as of Phase 2:**
- DRAW's CONTINUE? gate injects `BillingRepository` and `AdRepository` for real (§4b) — coin
  spend, ad-watch-for-coin, and the GO PRO purchase attempt (which correctly queries real
  offerings and no-ops gracefully today) are all live UI, not just seams.
- `earnCoins()` (ad rewards, completion rewards) and `currentOfferingPackage()` exist on the
  interface now.

**New as of 2026-08-11 — dashboard config is now complete ahead of Play Console:**
- All 6 products, both offerings (`default` is_current, `coins`), all 6 packages, the
  entitlement attachments, and COIN auto-grants were created via the RevenueCat MCP. They
  read as "not found in store" until Play Console has products with the matching ids — the
  exact id table lives in `docs/09-PENDING-INPUTS.md`. **Nothing further is needed on the
  RevenueCat side** except uploading the Play service account JSON and building the paywall.
- Consequence for the app: `currentOfferingPackage()` will start returning a real package
  the moment Play products go live, with no code change. The `default` offering already
  exists, so `getOfferings().current` is no longer null-by-configuration.

**Still not real — genuine Week 4 remainder:**
- No `PaywallView`/RevenueCatUI paywall screen anywhere — GO PRO's fallback message ("not live
  yet") is honest and correct today, but there's no actual paywall UI waiting behind it for
  when a real offering exists. That's still to build.
- No `ProGate` composable applied broadly across Pro-gated features (stack-limit enforcement
  in §4b checks `isPro` inline, which works, but there's no reusable gate component yet).
- No ad→Free-Play-Mode flow (only ad→coin exists).
- No Customer Center.

---

## 6. What's NOT built, by roadmap week — and what's actually blocking it

| Week | Scope | Status | Blocked on user? |
|---|---|---|---|
| 2 (remainder) | Stacks CRUD, filters | ✅ Built (§4b) | — |
| 2 (remainder) | **STACK view** — the signature receding-3D-stack "wow" view | ✅ **Built 2026-08-19** (`feature/pile/PileStackView.kt`), now the default view. Not yet seen on a device | — |
| **3** | DRAW, Credits Roll, pairwise ranking | ✅ Built in full (§4b) | — |
| 4 | Virtual currency spend, ad-watch-for-coin, GO PRO purchase attempt | ✅ Built (§4b) | — |
| 4 | **Paywall UI** (`PaywallView`/RevenueCatUI), reusable `ProGate`, ad→Free-Play-Mode, Customer Center, Steam import review screen | ❌ Not built | Paywall UI itself can be built now against no products (it'll just show nothing purchasable); Customer Center same. Real products still need Play Console. |
| 5 | YOU tab stats/trophies, 1 of 5 share cards ("THE PILE") | ✅ Built (§4b) | — |
| 5 | **4 remaining share cards** (CLEARED, HIGH SCORE, THE STACK, YEAR IN GAMES), full Time Budget tap-to-expand visualization, **clipboard nudge detection logic** (toggle exists, nothing watches the clipboard yet) | ❌ Not built | No |
| 6–8 | Motion/haptics polish pass, real-device testing, submission assets | Not started (correctly — still too early for polish/assets; but **real-device testing of everything in §4b is overdue**, see §2) | No |

**Genuinely blocked on Play Store account / Twitch credentials:**
- Real game data/cover art (Worker auto-falls-back to the seed set meanwhile — nothing
  breaks, screens just show placeholder-ish data)
- Real RevenueCat products/offerings, real AdMob ad units
- Actual Play Store listing, the 14-day closed-testing clock

**Everything else is just not built yet, not blocked.**

---

## 7. Pending on Mikhil (see `docs/09-PENDING-INPUTS.md` for the authoritative table)

As of 2026-08-06: Google Play developer account and Twitch/IGDB Client ID are both still in
progress, **expected to land by roughly 2026-08-14** (end of the following week, per Mikhil).
Nothing built or planned in the near term needs to wait for either.

---

## 8. Recommended next priority

> **Superseded 2026-08-19 — read this first.** The closed test is running and the privacy
> policy is live, so the two items that used to head this list are **done**. Current order:
> 0. **Protect the clock.** Don't let the opted-in count fall under 12, don't pause or duplicate
>    the track, and make the next upload `versionCode 5`+. Nothing else in this project matters
>    as much for the next fortnight.
> 1. **Get a phone on `adb`.** Three separate pieces of work are now stacked up behind "needs a
>    device": the four 2026-08-15 layout fixes, RANK/Stacks/offline share matching, and the new
>    STACK view's three feel-dependent constants. One session with a phone clears all of it.
> 2. **Install a debug build on a *phone*** and re-check PILE (the new STACK view, then scroll
>    past the filters in GRID) and DRAW (hide/show dials, pull the lever) — the four fixes from
>    2026-08-15 are layout changes that have not been seen on glass. `./gradlew installDebug` is
>    faster for iteration, and the debug build shows a fully populated paywall via
>    `FakeBillingRepository`.
> 3. Still never exercised on a device at all: **RANK, Stacks, and offline share matching** (try
>    sharing a caption in airplane mode — a confident match should resolve with no network).
> 4. Then, in value order: the **CMP/UMP consent flow** (the one real policy gap — no consent
>    flow means EEA/UK/CH ad traffic breaches Google's EU user consent policy), the 4 remaining
>    share cards, Free Play Mode, Customer Center.
>
> **Closed, no longer needed:** pasting `TWITCH_CLIENT_SECRET` (done); designing a dashboard
> paywall (hand-built, and now device-confirmed as the best-looking screen in the app).


Still #1, and now narrower: PILE, DISCOVER, search, the rewarded ad, and the share target have
been exercised on a real device (2026-08-12) and their bugs fixed. **The DRAW lever, swipe
cards, Credits Roll, RANK, and Stacks have not.** Those are the gesture/physics-heavy screens —
exactly the ones most likely to read fine in source and feel wrong on glass.

1. **Finish real-device verification of everything in §4b.** The app now installs and reaches
   PILE without crashing (bug #9 fixed), but that's the *start* of on-device testing, not the
   end of it — nobody has actually pulled the DRAW lever, swiped a card, sat through Credits
   Roll, or run a RANK comparison on a real screen yet. The gesture/physics code (drag
   thresholds, velocity commit, spring specs) is exactly the kind of thing that reads fine in
   source and feels wrong on glass, or crashes on an interaction path cold-launch never
   exercises (same lesson as bug #9). Do this before demoing or recording anything. Ask
   Mikhil what he's already tapped through before assuming a clean slate.
2. **The STACK 3D view.** It's the one piece of Week 2/3 the design doc calls out by name as
   "the wow view" and it's the biggest visible gap left in the pile-browsing experience.
   After that, Week 4's paywall UI and Week 5's remaining share cards are the next-highest
   value, in either order — neither is blocked on anything.
