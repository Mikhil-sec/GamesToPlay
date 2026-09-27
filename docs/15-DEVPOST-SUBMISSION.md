# CONTINUE? — Devpost submission, field by field

Paste-ready text for every field on the RevenueCat Shipaton 2026 submission form, written
2026-09-26. Every claim here was checked against the code, the live build (`versionCode 14`) or
RevenueCat on that date. Voice is first person singular (solo entrant). The sponsor influencer's
name appears nowhere in our text; the category dropdown is Devpost's own.

Images for the gallery and thumbnail are in `store/devpost/`; the video is
`video/out/CONTINUE-demo.mp4`.

---

## Project overview

**Project name** (60 max; this is 51):

```
CONTINUE? — the games you started deserve an ending
```

**Elevator pitch** (200 max; this is 176):

```
A gaming backlog for Android that plays like an arcade cabinet. Save games from any app, pull a lever to decide what to play tonight, and roll the credits when you finally finish.
```

**Thumbnail:** `store/devpost/00_thumbnail.png` (3:2).

---

## About the project (Markdown)

```markdown
**Live on Google Play** · 1-minute-54 demo above · open source (MIT)

## Inspiration

Every gamer I know has a pile of shame: games bought on sale, gifted, recommended by a friend, and never finished. Every backlog app I tried was a spreadsheet with box art, and a spreadsheet of unplayed games is a guilt machine, so people stop opening it.

The fix wasn't a better list. **A backlog is a decision problem**, not a storage problem. And the arcade already solved the emotional side of it: in an arcade, `CONTINUE?` was never failure. It was a second chance, a countdown and a coin. So I built the backlog as a cabinet.

## What it does

- **Save from anywhere.** Share a YouTube or TikTok link, a message, or a screenshot to CONTINUE? and a sheet slides up over the app you're in with the game already identified. (How that works is below; it's the hardest thing in the app.)
- **Organize, honestly.** A time budget tells you the truth, *1,167 hours, 29 games, finished by 2030*, based on the hours a week *you* actually play (set it and the year moves) and recalculated live as you filter. One set of filters works across the whole app, NOW PLAYING is capped at three because focus is a feature, and a stats screen breaks your pile down by genre, length and how varied your taste is.
- **Decide.** Set the dials (time, mood, genre) and pull the lever. It deals three cards from *your own* pile and says why each one fits tonight.
- **Complete.** Clearing a game rolls the credits: your own stats scroll over the key art.
- **Rate, head to head.** No stars. "Which did you enjoy more?" binary-searches each game into a personal all-time ranking.
- **Share.** Your HIGH SCORES as an arcade table, and your whole pile as a link a friend can follow, with games you have in common marked.

## The part the video can't show: the share-matching engine

A shared link or caption is never a clean title. "Elden Ring Shadow of the Erdtree is brutal" is a sentence, not a name, and IGDB's search is near-exact: `Palworld` finds the game, `Pocketpair Palworld` finds nothing.

1. **Links become titles.** YouTube and TikTok links are expanded on my backend to the video's title and channel.
2. **Titles become candidates.** A tokenizer builds an ordered shortlist of substrings of at most six words. It drops caption filler ("official trailer", "gameplay", "reaction") and stopwords, keeps connectors that live *inside* titles ("of", "the"), and folds sequel numerals (III ↔ 3).
3. **Candidates are matched on the phone.** I built a pipeline over IGDB's data dumps that ships a **17,095-game index with alternative names** (0.58 MB) inside the app, so "BG3" and regional titles resolve, and matching is a dictionary lookup that works **with no network at all**. Screenshots are read with on-device OCR, with no photo permission.
4. **Every match is scored against the original text.** Confident matches add in one tap; ambiguous ones show a short chooser; hopeless ones open a blank field with an honest explanation instead of guessing wrong.

It went from "Elden Ring … is brutal" matching the wrong game at 0.29 confidence to the right game at 0.90. The same ranking code exists in TypeScript on the backend and in Kotlin on the phone, tested against the same fixture captions so the two paths can never disagree.

## How I built it

Kotlin and Jetpack Compose, with Hilt, Room, DataStore, Retrofit and Coil. **Offline-first:** Room is the source of truth and the UI only reads from it, so the pile, DRAW, and matching shared text and screenshots all work on a plane. A Cloudflare Worker (TypeScript, KV cache) is the only thing that talks to IGDB. RevenueCat runs the whole revenue stack: Pro entitlements, the `COIN` virtual currency, and rewarded-ad verification.

Every sound and all three music packs are **synthesised from code** (oscillators and seeded noise, no samples) and released as CC0. Even the demo video was built in code: Remotion for the motion graphics, and a score composed from the app's own instruments. I built it solo, pair-programming with Claude Code.

## Security and privacy on a public repo

The repository is public and the backend has no user accounts, so **nothing secret can ship in the app**. I wrote a threat model before going public:

- **Keys live only on the Worker.** The APK carries only values that are public by design. I scanned the full git history for secrets before publishing.
- **Abuse is bounded.** Three rate limiters guard the backend: 120 req/min per IP, 20/min on the expensive resolve route, and a 600/min global cap kept under IGDB's own limit so I throttle myself before IGDB bans me. I also added input caps, removed a wildcard CORS header, capped request fan-out, and made errors stop leaking internals. Each control was proven with live traffic, not trusted from config.
- **I found and fixed an SSRF.** The link resolver checked hosts with a substring match, so `tiktok.com.attacker.example` would have passed.
- **FRIENDS needs no accounts and stores nothing on my server.** The pile travels in the link's URL *fragment*, which browsers never send. Each phone signs its links with its own ECDSA P-256 key, so nobody can forge an update to "your" pile on a friend's phone. The decoder is strict and bounded, with 20+ hostile-input tests.
- **Three permissions, total:** internet, network state, vibrate. No photos, contacts or location, and no account to create.
- **Consent done properly.** A real UMP consent flow for EEA/UK users, rather than excluding a third of the developed world from the store.

## Challenges I ran into

- **Google Play's gate for new developer accounts:** 12 testers, opted in for 14 consecutive days, before production access. I recruited them, ran four feedback rounds (~21 reports), and **shipped four builds during the test**, one answering each round.
- **Config that looks applied but isn't.** My first rate-limiter deploy silently did nothing: 150 of 150 test requests got through, because an older tool version ignored the setting without a warning. A deprecated IGDB field returned empty instead of an error. Since then I verify with traffic, never with the config file.
- **A tester found a HAPTICS toggle wired to nothing.** I audited every control and found two more. Now each setting is enforced where the capability lives, not where it's called.

## Accomplishments I'm proud of

- It's **live on Google Play** in every country, and closed-test testers completed real (sandbox) purchases of both Pro tiers.
- **Ads verified by RevenueCat:** a rewarded ad can grant a coin, or a full hour of the real `pro` entitlement.
- **213 app unit tests and 75 Worker tests**, plus a migration test that fails the build if a database change would crash upgrading users.

## What I learned

A green health check is not working software. Assert on real content. The free tier's 10 ms CPU limit, not taste, decided where matching runs. And users forgive a small app almost anything except a control that does nothing.

## What's next

I want CONTINUE? to be where every game lives between "that looks good" and "credits rolled".

- **The pile keeps itself up to date.** Steam import first, then console libraries where the platforms allow it, with playtime sync so games move to NOW PLAYING and CLEARED on their own.
- **Every platform from one codebase.** It's all Kotlin, so Compose Multiplatform can take most of it to iOS and desktop.
- **Curated piles.** Friends' piles are only the start. The same signed-link format can carry a critic's or a community's "finish these before 2027" list, with the games you already own marked.
- **Short links on a proper domain.** A pile link is long because it *carries the pile*, which is exactly why nothing is stored on my server. Short, branded links would mean storing piles, so they'll be opt-in, and the private long link stays the default.
- **A year in games.** An end-of-year recap card built from your clears and rankings.
- **Hardening for scale.** Play Integrity attestation on the backend, and a server-side coin ledger.

*Game data freely provided by IGDB.com.*
```

---

## Built with (up to 25 tags; Devpost suggests as you type, pick the closest match)

```
kotlin, jetpack-compose, android, material-design, hilt, room, datastore, retrofit, kotlinx-serialization, coil, revenuecat, google-admob, google-ump, ml-kit, cloudflare-workers, cloudflare-kv, typescript, igdb, oembed, python, numpy, remotion, react, gradle
```

## "Try it out" links

```
https://play.google.com/store/apps/details?id=com.mikhilnaika.continueapp
https://github.com/Mikhil-sec/GamesToPlay
```

## Image gallery (upload in this order, up to 15)

1. `store/devpost/01_hero.png` → `07_architecture.png` (seven 3:2 slides, 1920×1280)
2. `store/icon-1024.png`: the rules say "include a 1024x1024 app icon"; this is its only home on the form
3. `store/devpost/screenshots-1179x2556/2_draw.png` and `3_continue.png`: the rules require at least
   one 1179×2556 screenshot with no device frame. They'll show letterboxed in a 3:2 gallery; that's fine.
   (All six are in that folder if you'd rather swap.)

Captions (Devpost lets you add one per image; optional, keep them short):
01 "Live on Google Play" · 02 "Share from any app; it identifies the game, even offline" ·
03 "Pull the lever: three cards from your own pile" · 04 "Every ad is opted into, and verified by
RevenueCat" · 05 "Roll the credits, then rank head to head" · 06 "Follow a friend's pile, no accounts"
· 07 "Offline-first, no secrets in the app"

## Video demo link

The YouTube URL (Public).

## URL to your published Android app

```
https://play.google.com/store/apps/details?id=com.mikhilnaika.continueapp
```

## (Next Gen) URL to your code repository

```
https://github.com/Mikhil-sec/GamesToPlay
```

## (Next Gen) Student email

Your academic email (not the gmail).

## (Next Gen) Minor-entrant checkbox

**Tick it.** It confirms "either this submission does not include a Minor Entrant, or…", and you
are an adult, so the first half is true.

## RevenueCat project ID

```
proj747a0e7c
```

## Promo code field (judges only)

Single line. Five codes, then a pointer. Replace the `CODE` placeholders from your spreadsheet:

```
Lifetime PRO, one-time codes (each works once, try the next if used): CODE1 · CODE2 · CODE3 · CODE4 · CODE5 — more codes, redeem steps and a no-code option in the judges' notes.
```

---

## HAMM Award: monetization model

```
CONTINUE? makes money three ways, and all three are run by RevenueCat so they behave as one system rather than three tactics.

1. PRO: Monthly US$3.99 (7-day free trial for new subscribers) or Lifetime US$9.99. Unlimited draws and stacks, no ads, and a coin grant (600 with Lifetime, 50 every month with Monthly, delivered through RevenueCat's COIN virtual currency).
2. COINS: the soft currency. Earned by watching a rewarded ad, clearing a game, or through PRO. Spent to continue past the daily free draw, or on music packs.
3. REWARDED ADS, only ever opted into: INSERT COIN for one coin, or FREE PLAY for a full hour of the real pro entitlement. Both are verified server-side by RevenueCat before anything is granted.

Why this model:
- A backlog manager is a tool you own, not a service you rent. So Lifetime is the destination, and Monthly exists mainly to make Lifetime obvious: it pays for itself in 2.5 months. I deliberately cut the annual tier. At any sane price it would sit above Lifetime, a decoy nobody should pick.
- The free tier is genuinely useful (one draw a day), and the upsell happens at the moment of intent: the CONTINUE? screen, when you want another draw. There, a coin, an ad and PRO are offered side by side.
- FREE PLAY is a trial that needs no payment method. People who feel PRO for an hour are the people who buy PRO, and the ad pays for the hour.
- Every coin has a sink people actually want (continues, music), so ads earn something real instead of inflating a number.

Results, honestly: the app went live on Google Play on 19 September 2026. Closed-test testers completed sandbox purchases of both tiers, with entitlements unlocking. I have no revenue figures worth quoting yet, and I'm not going to inflate install counts that include Google's automated pre-launch devices.
```

## RevenueCat Design Award: distinctive design elements

```
The idea: the arcade is the interaction model, not a skin. Every ritual in the app is a physical arcade moment, built to feel like an object.

What to look at, in order:
1. The cold open (first launch). A CRT powers on (beam line, vertical open, glare cooling) into a glowing marquee and a blinking INSERT COIN, with its own sound.
2. DRAW. A real lever you drag down: it ratchets, bottoms out with a haptic buzz, and three cards deal from a dispenser and flip. Rituals are skippable; nothing makes a fast user wait.
3. The CONTINUE? screen. A magenta countdown that loops rather than locks you out. It's atmosphere, never punishment.
4. The Credits Roll. Clearing a game types out GAME CLEARED and scrolls your own stats over the key art, with a coin payout. It's the emotional peak, and there is deliberately no ad anywhere near it.
5. Head-to-head ranking and the HIGH SCORES card (an arcade leaderboard of your real top ten).
6. Sound. 14 effects and 3 music packs (arcade chiptune, lo-fi, synthwave), all synthesised from code, mastered to one loudness, and never played over your own music.

The system: dark only, one gold accent carrying the app, and green and magenta as punctuation (green means cleared, magenta means the countdown). Chakra Petch for arcade moments, Inter for everything functional, JetBrains Mono for numbers so counters don't jitter. Modern layout, retro only in accents.

Iterated with real users: four closed-test rounds changed the design. A crash in the 3D stack, a tablet layout hiding a phone-only break, a lifetime button that said INSERT COIN instead of PURCHASE. Each fix shipped to testers within days.
```

## Catvertising Award: how the app uses RevenueCat Ads

```
The premise: in an arcade, continuing always cost a coin. So in CONTINUE? an ad is never an interruption. It is the coin, and you choose to insert it.

Placements (all rewarded, all user-initiated, all explicitly priced):
1. INSERT COIN on the CONTINUE? screen. Free users get one draw a day. The next pull shows a countdown with four choices side by side: watch an ad for a coin, spend a coin, FREE PLAY, or go PRO. The ad is one option among honest alternatives.
2. FREE PLAY on the same screen, and as "NOT SURE YET?" on the paywall. One ad grants 60 minutes of the real pro entitlement, with a live FREE PLAY 59:12 clock in the top bar. It is the most generous thing an ad can do: it hands you more app, and it is my best conversion path.

How it plugs into RevenueCat:
- Reward verification. Each rewarded impression carries a RevenueCat token as AdMob server-side-verification data. RevenueCat reward rules grant the reward server-side: 1 COIN of virtual currency for INSERT COIN, and the pro entitlement for 60 minutes for FREE PLAY. The app polls for the verified result ("VERIFYING WITH REVENUECAT…" on screen). If verification can't complete, INSERT COIN still pays the coin locally so nobody watches an ad for nothing; FREE PLAY never grants anything unverified.
- One currency. Coins that RevenueCat grants (verified ads, and PRO's 600 or 50-a-month) are bridged into the same in-app balance as coins earned by clearing games, so ads, the subscription and the currency are one economy.
- Ad revenue beside subscription revenue. Load, impression, click and paid events go to RevenueCat's ad tracker, so each customer's ad revenue sits next to their purchases.

What I refused to build, which matters as much: no interstitials, no banners, no app-open ads, nothing between draws, and no ad anywhere near the Credits Roll. You never monetize the emotional peak. PRO users see zero ads. EEA/UK users get a proper consent flow rather than being excluded from the store.
```

## Influencer Award: how the app targets the category

```
The brief asks for a gaming bucket list where players easily save, organize, complete, rate and share the games they want to play. CONTINUE? answers each verb with one specific mechanic, and adds the one the brief implies: decide.

- SAVE: share from anywhere. The audience of a gaming creator discovers games in videos; CONTINUE? turns "share this video" into "this game is in my pile" without leaving the video app. A matching engine reads the video's title against a 17,095-game index (with alternative names like "BG3"), on the phone.
- ORGANIZE: a time budget that tells the truth (1,167 hours, finished by 2030), one set of filters across the whole app, and a hard cap of three games in NOW PLAYING, because focus is a feature.
- DECIDE: set time, mood and genre, pull the lever, and get three cards from your own pile with the reason each fits. A creator's audience doesn't need more recommendations; it needs to pick one.
- COMPLETE: finishing a game rolls the credits with your own stats. Completion should feel like something.
- RATE: no stars. Head-to-head picks place each game in a personal all-time ranking. "Your #4 of all time" is more honest than 8/10.
- SHARE: a HIGH SCORES card that is arguable by design (the currency of gaming communities), and a pile link friends can follow, with shared games marked. No accounts needed.

No influencer's name, likeness, brand or logo appears anywhere in the app, its store listing or its marketing.
```

## Additional notes for the judges

Replace `CODE` placeholders with **10–15 more codes** (not the five from the promo field).

```
How to get PRO (pick any):

A) No code, no payment method: FREE PLAY. Pull the DRAW lever twice (the first draw each day is free), then on the CONTINUE? screen tap FREE PLAY and watch one ad. That's 60 minutes of the real PRO entitlement, verified by RevenueCat.

B) Promo code, Lifetime PRO, one-time use each. If one says it's been used, try the next:
CODE6 · CODE7 · CODE8 · CODE9 · CODE10 · CODE11 · CODE12 · CODE13 · CODE14 · CODE15
To redeem: Play Store → your profile icon → Payments & subscriptions → Redeem code (or open https://play.google.com/redeem?code=CODE on the phone). Then open CONTINUE?. If PRO doesn't show, tap GO PRO → RESTORE PURCHASE.

C) New subscribers get a 7-day free trial on Monthly (GO PRO → MONTHLY).

Worth trying yourself: share any gaming video from the YouTube app to CONTINUE? and watch it identify the game. Or switch on flight mode and share a game's name from any app: matching still works offline.

Design notes, threat model and build history are in the repo's docs/ folder (docs/12-SECURITY.md is the threat model). Privacy policy: https://mikhil-sec.github.io/GamesToPlay/privacy.html
Game data freely provided by IGDB.com.
```

## RevenueCat Growth Fund checkbox

Your call; ticking it only signals interest.

---

## GitHub "About" box (repo page → ⚙ next to About)

- **Description:** `An Android gaming backlog that plays like an arcade cabinet. Kotlin, Jetpack Compose, RevenueCat, Cloudflare Workers. Built for RevenueCat Shipaton 2026.`
- **Website:** `https://play.google.com/store/apps/details?id=com.mikhilnaika.continueapp`
- **Topics:** `android kotlin jetpack-compose revenuecat cloudflare-workers igdb hackathon`
- Leave "Releases/Packages" as they are. The MIT licence is already detected and shown there.

---

## YouTube upload, step by step

1. (Skipped: SMS verification isn't available from Mauritius. Nothing below needs it.)
2. https://studio.youtube.com → **Create → Upload videos** → `video/out/CONTINUE-demo.mp4`.
   Upload the file as-is; it's a high-bitrate 1080p60 master, which is what YouTube wants.
3. **Details**
   - Title: `CONTINUE? — the games you started deserve an ending | RevenueCat Shipaton 2026`
   - Description:
     ```
     CONTINUE? is a gaming backlog for Android that plays like an arcade cabinet. Save games from any app, pull a lever to decide what to play tonight, earn coins only by choice (never forced ads), and roll the credits when you finish.

     Built solo for RevenueCat Shipaton 2026. Recorded on a real Android phone running the live Google Play build.
     Game data freely provided by IGDB.com. All music and sound effects are original and released under CC0.
     ```
   - Thumbnail: pick one of the three auto-generated frames: the one with the phone beside a big
     headline (the CONTINUE? countdown screen if offered). Avoid a black or text-only frame.
   - Audience: **"No, it's not made for kids"** (a "made for kids" video loses comments and some
     embed features; it's an app for adults and teens).
4. **Show more**
   - Altered or synthetic content: **No** (it's screen recording plus motion graphics; this
     question is about realistic AI-generated people or events).
   - Paid promotion: leave unticked.
   - Tags: `android app, game backlog, revenuecat, shipaton, jetpack compose, kotlin, indie app`
   - Category: **Science & Technology**. Language: English.
   - License: Standard YouTube License. **Allow embedding: ON** (Devpost embeds it; this is
     on by default, but check it).
5. **Video elements**: skip (end screens and cards distract in a 2-minute demo).
6. **Checks**: wait for "No issues found" on copyright. The music is original, so it should pass;
   if anything is flagged, stop and dispute. Don't publish with a claim.
7. **Visibility: Public** (the rules say "publicly visible"; Unlisted is a risk). Publish.
8. Wait until processing shows **HD** (10–30 min) before pasting the link, so the first judges
   don't get 360p. Open the link in a private window to confirm it plays logged-out.
9. Paste the plain `https://youtu.be/…` link into Devpost's "Video demo link". Check the embed on
   your Devpost project preview.

---

## Before you press submit

- [ ] **`versionCode 14` is in production** (Play Console → Production). The FREE PLAY and
      verified-ads claims describe 14. If it's still only on Internal testing, promote it today;
      review can take a day or more.
- [ ] Promo codes: take them from the spreadsheet **in order** and mark them used in your copy.
      5 in the promo field, 10 different ones in the judges' notes. Never post codes anywhere public.
- [ ] Devpost account uses your **academic email** for the Next Gen field.
- [ ] GitHub repo "About" box filled in (above), and the latest commit pushed. Judges for Next Gen
      read the repo.
- [ ] Watch your own Devpost page preview once, logged out: video plays, images in order, links work.
- [ ] Submit by **29 Sept** to have a day of slack. Devpost stays editable until the deadline
      (30 Sept, 11:45pm PDT).
