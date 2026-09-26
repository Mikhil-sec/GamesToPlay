# CONTINUE? — Submission Kit

Draft copy and asset checklists. Finalize in week 7; draft now so nothing is written at 2am.

---

## ⚠️ READ FIRST — fact-check before writing any Devpost copy (added 2026-09-18)

**The write-ups below were drafted on 2026-08-12, before most of the app existed, and they
describe the *plan*.** Several features they sell were never built. Judges can install the app
and read the public repo, so a claim that's false is worse than one left out. Checked against the
code on 2026-09-18:

> **Update 2026-09-19 (v12):** three rows below changed. **FREE PLAY**, **RevenueCat-verified
> rewarded ads** and **RevenueCat COIN reaching the app** are now *built* in `versionCode 12`
> (`docs/10` 2026-09-19 v12). They become claimable only once (a) the AdMob SSV URL and RevenueCat
> reward rules are configured and (b) each is seen working on a device on the live build. Until
> then, treat them as ❌ in any copy.
>
> **✅ Cleared 2026-09-19 (late):** Mikhil configured the AdMob SSV URLs and both RevenueCat reward
> rules and reported "everything done and works" on `versionCode 14` (Internal track): verified
> INSERT COIN, FREE PLAY, the COIN bridge, and the music packs. **Claimable once 14 is live in
> production.** Still true and worth saying precisely: INSERT COIN falls back to a local coin if
> verification can't complete; FREE PLAY never does.

| Claimed in the drafts below | Reality in `versionCode 11` (see v12 note above) | Where |
|---|---|---|
| **FREE PLAY MODE** — ad grants 60 min of real `pro` | ❌ **Not built.** Only an ad-unit loader (`loadFreePlayAd`) exists; no UI, no temporary entitlement | Catvertising write-up |
| Share-card **themes** (HOLOGRAPHIC FOIL etc.) as a coin/ad sink | ❌ **Not built.** Three cards exist (CLEARED, THE PILE, HIGH SCORES), one design each | Catvertising, Design |
| **Steam import** + "one ad unlocks the rest" | ❌ **Not built** (onboarding code marks it roadmap) | Catvertising |
| Rewards granted "only on AdMob's **server-side verification**" | ❌ **False.** Coins are granted client-side in `CoinLedger`; SSV needs real ad units on a live listing and was never wired. Say so honestly or leave it out | Catvertising |
| **Customer Center**, **remote paywalls** | ❌ **Not built.** The GO PRO paywall is hand-built Compose on RevenueCat offerings/entitlements (`docs/04-MONETIZATION.md` §Paywall explains why) | Next Gen, Design |
| Coins as **RevenueCat Virtual Currency** in the app | ❌ **Checked 2026-09-18: false as a working feature.** Coin packs can't be bought in v11 (no UI, `coins` offering never read); nothing reads RevenueCat's COIN balance; `CoinLedger.creditPurchased` has zero callers. RevenueCat *does* auto-grant COIN on Pro purchases (testers hold 600–4,250 there), but it never reaches the app. Honest framing: "COIN is configured in RevenueCat with purchase auto-grants; the in-app balance is an offline ledger; bridging them is next". **The same applies to "Pro includes a monthly coin drop."** Details: `docs/10` 2026-09-18 | Catvertising, Next Gen, Design |
| THE STACK / YEAR IN GAMES cards | ❌ Not built | Design |
| "Published on Google Play" | ✅ **True as of 2026-09-19**: `versionCode 11` is live in production, `https://play.google.com/store/apps/details?id=com.mikhilnaika.continueapp` | Everywhere |
| Rewarded ads work on the live app | 🟠 AdMob verified and linked 2026-09-19 (real ad units). **Watch one real ad on the live app before claiming it** (a new app can see "no fill" for a while). Still **client-side** rewards, not SSV; see the SSV row above | Catvertising |
| Pro-trial for judges | 🟠 2026-09-18: 7-day trial **live in the store**; **200 one-time promo codes** created (spreadsheet held privately, never in the repo). Both need a device check. Judge steps: Play Store → Payments & subscriptions → **Redeem code** → open CONTINUE? → YOU tab shows PRO (else GO PRO → RESTORE PURCHASE). Only put codes in a judges-only field (`docs/09`) | Deliverables |

**What IS true and worth selling:** everything in the next section. For each feature,
`docs/10-BUILD-STATUS.md` (dated entries, newest first) has the why, the bugs found and the
verification, and `docs/02-PRODUCT-SPEC.md` has the design.

### Shipped since the drafts were written — fold these in

- **FRIENDS (versionCode 11, 2026-09-17), the strongest new story for Design and Gaming.**
  Share your whole pile as a link; a friend with CONTINUE? taps it and follows your pile from a
  FRIENDS tab, with games in common marked and one-tap add-to-my-pile. **No accounts and no
  server storage**: the pile rides in the URL *fragment* (browsers never send it to a server), and
  each phone signs its links with its own random key so nobody else can overwrite "your" pile on
  a friend's phone. It updates when you share again, not live. Spec §6, security §6b, privacy §3b.
- **The friend loop for single games (2026-09-12).** RECOMMEND IT / DARE THEM TO FINISH IT links
  unfold in WhatsApp/Discord with real key art (an Open Graph landing page on the Worker) and open
  straight into the app via verified Android App Links. The Credits Roll got a SHARE THIS CLEAR
  button. Cards are now properly designed (Chakra Petch, key art, CRT scanlines), 1080×1350.
- **Consent done properly (versionCode 10, 2026-09-12).** A real UMP consent flow instead of
  excluding 32 EEA/UK/CH countries, so the app ships everywhere. **This is a Catvertising
  argument:** "we built the dialog rather than delete a third of the developed world."
  Verified on a device 2026-09-17.
- **Share target that works offline.** A 17k-game index built from IGDB data dumps ships inside
  the app; screenshots are OCR'd on-device (ML Kit), with no photo permission.
- From the four closed-test feedback rounds (~21 tester reports, four builds shipped to testers
  *during* the test — see `docs/14-PRODUCTION-ACCESS.md` §3): STATS screen, backdating cleared
  games, one filter vocabulary across PILE/DRAW/STATS, search respelling ("spiderman" → "spider
  man"), DISCOVER covers + SHOW MORE, RANK manual reorder, 3D STACK view, clipboard nudge,
  REMOVE FROM PILE, a Room migration lane with a test that fails the build if a migration is
  missing.
- **Real usage evidence** (for the Gaming write-up): testers completed real sandbox purchases
  of both lifetime and monthly Pro. ⚠️ Never quote RevenueCat customer counts or sandbox dollar
  amounts — they include Google's pre-launch device farm (see memory / `docs/10` 2026-09-07).
- **Engineering depth for Next Gen:** ~200 app unit tests + 75 Worker tests; a public-repo threat
  model (`docs/12-SECURITY.md`) including an SSRF fix, rate limiting that protects KV quota, and
  the signed pile-link format with a cross-language (Python) fixture test.

**The demo video script below was rewritten 2026-09-22** against `versionCode 14` (FRIENDS, FREE
PLAY and the verified INSERT COIN each get a beat; Steam import is gone).

---

## The bar and the field (researched 2026-09-19)

**Rule for using this section:** the benchmarks set the *quality bar*; the competitor rows are a
*feature comparison only*. No competitor's wording, structure or tone is a model for ours — several
are thin, and writing to their level would be writing down.

### The official 2026 criteria (devpost, verbatim) — every write-up answers these, in these words

| Category | Criteria |
|---|---|
| **Gaming** | "Gaming bucket list where players easily **save, organize, complete, rate, and share** games they want to play." → structure the Gaming write-up *and the video* around those five verbs, one proof each. |
| **Design** | "App that best represents the **craft** of app development — separate from viability as a business. Looking for innovative ideas and/or beautiful app design and animations." |
| **Catvertising** | "Most creative and effective use of ads as a monetization method. Looking for **clever placements**, **smart integration with the rest of the revenue stack**, and **an experience users don't hate**." |
| **Next Gen** | Judged on a video and open-source code; students only; no store release needed. |
| All | Video ≤ 2:00 — "judges are not required to watch beyond two minutes". Icon 1024², ≥1 screenshot 1179×2556 no frame, trial or promo code. |

### Benchmarks — what 2025 winners (same sponsor) actually submitted

- **Dayloop** (Design Award, 1st): ~2,100 words, standard Devpost headings, 4 screenshots, first-person
  story of *design iterations* ("it didn't have magic, so we added…"). Zero RevenueCat detail and zero
  metrics — **the craft story carried it**.
- **Vector Guard** (HAMM, 1st): ~1,200 words; opens on a hard number; one named, memorable
  monetization idea ("the 1:50 model") that *is* the pitch.
- **Crystal Abyss** (Staff & Sponsors, 1st): ~2,800 words; honest about what isn't finished yet;
  names its influences precisely.
- Across all 2025 winners: the winning move is **one crisp idea per category, named, with the
  reasoning shown**, not a feature list. Metrics help where the category is about traction (ours
  aren't). Honesty about limits read as credibility, not weakness.

**What that means for us:** each category write-up leads with its one idea — Gaming: *a backlog is a
decision problem, so it's a machine you pull*; Design: *the arcade is the interaction model, not a
skin* (lever, dispenser, continue screen, credits roll, CRT, synthesised sound); Catvertising: *the ad
is a coin you insert — every placement is a CONTINUE?, verified by RevenueCat, and FREE PLAY hands
over real PRO*. Show design iterations from the four tester rounds (real, dated, in `docs/10`).

### The field in the Gaming category (as of 2026-09-19; five entries public)

| Entry | Platform / store | What overlaps with us | What they have that we don't | What we have that they don't |
|---|---|---|---|---|
| Playwall | iOS, App Store | Share-sheet capture, IGDB via a Cloudflare Worker, comparative rating, "anti-guilt" framing | iCloud sync, yearly "receipt" | Android; DRAW machine; friends following piles; ads; video (none of the five has one yet) |
| QuestLog | iOS + Android (KMP), both stores | Mood/duration roulette, pile-of-shame counter, verdicts, share cards, IGDB | 17 languages, iOS | Physical lever/deal ritual, share-target from TikTok/YouTube incl. offline OCR, FRIENDS, Credits Roll, ads |
| NextUp | Web only, not on a store | Mood/time "pick my next" | Movies/TV too | A published app (required for Gaming) |
| Cibby | iOS, App Store (physical collections) | Game library | 3D boxes, soundtracks, 700k views on X, 700 TestFlight users | Backlog *completion* focus, draw, rank, friends |
| (2 more seen off-Devpost) | ? | ? | ? | ? |

**No other entry uses ads at all** as of today, so Catvertising may be close to uncontested among
gaming apps. That raises the value of v12's verified ads + FREE PLAY — *if* they work on the live
build. **Nobody has a video yet.** Ours is the biggest single lever left.

## Deliverables checklist

| Item | Required by | Status |
|---|---|---|
| Play Store URL (published) | Gaming, Design, Catvertising | ✅ live 2026-09-19: `https://play.google.com/store/apps/details?id=com.mikhilnaika.continueapp` |
| Public repo + OSI licence visible in About | Next Gen | ☐ |
| Demo video < 2:00, public on YouTube | All | ☐ |
| Text description | All | ☐ |
| 1024×1024 app icon | All | ☐ |
| Screenshot 1179×2556, **no device frame** | All | ☐ |
| Free trial **or** promo code | All except Next Gen | ☐ (7-day trial + backup code) — 🟠 2026-09-18: both created, trial verified in the store; device check pending, see `docs/09-PENDING-INPUTS.md` |
| Design write-up | Design Award | ☐ |
| Ads write-up | Catvertising | ☐ |
| Influencer category write-up | Gaming | ☐ |
| Academic email on Devpost | Next Gen | ☐ |

---

## Demo video: shooting script (rewritten 2026-09-22 against `versionCode 14`)

The single highest-leverage asset, and no rival had a video as of 09-19. The 08-12 script this
replaces sold Steam import, which was never built.

**✅ Recorded and rendered 2026-09-26** (Claude drove the phone over USB with scrcpy + adb). Final:
`video/out/CONTINUE-demo.mp4` (1920×1080, 60 fps, 1:51), thumbnail `video/out/thumbnail.png`. Clips
in `video/public/clips/` (gitignored), raw app screenshots for Devpost in `store/screens-raw/`.
What changed from the plan below: the camera "hand on lever" shot became a push-in on the screen
recording; the SAVE demo shares from a mock chat page (no creator videos, no real contacts); the
share shot pulls the HIGH SCORES card out of the phone instead of showing the system chooser (it
lists personal contacts); clip audio is muted and the app's own SFX are placed in the edit; a
clean status bar is painted over every clip. Hook numbers are the real demo pile: 29 games,
1,167 hours, finished by 2030.

**Revision 2 (same day, after Mikhil's review):** sublines cut to ~5 words and enlarged, because they were
unreadable in the Devpost embed; added a **music-pack shot** ("LIKE THIS SOUNDTRACK? 5 COINS.", which
closes the earn → spend coin loop for Catvertising; the score really is built from the NEON DRIVE
instruments); organize trimmed to 3 bars. 1:53.
**Deliberately left out:** a YouTube/TikTok share demo (showing YouTube's UI and a creator's video
risks both the trademark rule and the influencer rule; the chat demo shows the same matching), so
**say it in the Devpost text instead**; and DISCOVER (browsing, adds game art, serves no criterion
better than what's shown).

**It's built in code: `video/` (Remotion).** `video/timeline.json` is the edit, and it's the source of
truth for timings and headline text (the table below was the plan it was built from).
`video/SHOTLIST.md` is the recording checklist, and `video/README.md` explains the workflow. The
soundtrack is `tools/make_video_score.py`: an original, CC0 score made from the app's NEON DRIVE
instruments, arranged bar by bar to the timeline.

**Devpost rules:** under 2:00 (target **1:50**) · public on YouTube · shows the app *running on
the device* · **no third-party trademarks, copyrighted music or other material**.

### The look (what the reference videos have in common)

1. **The phone never fills the frame.** Screen recordings sit inside a phone frame on one
   designed background, taking ~55% of the frame height, and they are always moving slowly
   (push-in, drift, a few degrees of 3D tilt). A static full-screen recording looks like a bug report.
2. **One idea per shot, named in ≤4 words** in big type beside the phone. Those words carry the
   story, so the video works muted (many judges watch it muted in the gallery).
3. **One brand, start to finish.** Background `#08090C` with a soft gold (`#F7C948`) glow behind
   the phone and 3% scanlines; headlines in **Chakra Petch Bold, uppercase, gold**; sublines in
   **Inter, `#9AA3B2`**. The same fonts and colours as the app (`docs/03`).
4. **Zoom in on what matters.** A phone at 55% height is too small to read, so when the detail
   *is* the point (the reason a card matched, `YOUR #4 OF ALL TIME`), push in on it.
5. **Cut on the beat.** Shots last 3–5 s, cuts land on bar lines, and the app's own sound
   effects punctuate them.
6. **Hands prove it's real.** One or two camera shots of a real phone in a hand, in a dark room,
   satisfy "functioning on the device" without argument.

### Audio

- **Music = the app's own tracks** (`app/src/main/res/raw/music_*.ogg`, original, CC0, see
  `docs/AUDIO-LICENSE.md`). This removes the copyright risk entirely and keeps the sound on-brand.
  Bed: `music_neondrive_title` (synthwave, 104 bpm, so one bar ≈ 2.31 s). Switch to
  `music_arcade_continue` for the CONTINUE? gate, and `music_*_victory` under the Credits Roll.
- **Record with MUSIC off and SOUND on** (YOU tab), so the clips carry the real SFX (lever,
  deal, coin) and the music is laid in cleanly in the edit.
- **No voiceover by default.** Headlines carry it. Add VO only if it can be recorded clean (quiet
  room, mic close); a rough VO costs more than it adds.

### Third-party material (rule 4, plus the influencer rule)

- 🔴 **Never share from a YouTube/TikTok creator's video** for the SAVE shot. That is an
  influencer's likeness *and* their copyright, which is a double disqualification. Share from a chat
  message instead (a friend texting "you HAVE to play Hades").
- 🔴 **Never show an ad's content.** It is a third-party advertiser's material. Cut from the
  INSERT COIN tap straight to `VERIFYING WITH REVENUECAT…`.
- Game cover art is IGDB-provided and appears because the app is working as designed. Keep it
  incidental: never make a game's logo the hero of a shot, the title card or the thumbnail. Put
  the IGDB credit on the end card.
- No real people's names in the demo pile, friend names or messages.
- The "Get it on Google Play" badge is allowed under Google's badge guidelines. Use the official
  artwork unmodified.

### Shots (1:50)

| Time | Headline on screen | Shot (screen recording unless marked 📷 camera) | Sound |
|---|---|---|---|
| **0:00–0:06** | — | The real cold open: CRT powers on, `CONTINUE?` | `sfx_crt_on` |
| **0:06–0:14** | *{N} GAMES. {H} HOURS.* → *FINISHED BY {YEAR}.* | PILE time budget with the real numbers from the demo pile, then push in on the year | Music starts on the bar |
| **0:14–0:18** | **THE GAMES YOU STARTED DESERVE AN ENDING.** | Title card, then the logo | — |
| **0:18–0:28** | **SAVE IT FROM ANYWHERE** / *even offline, even a screenshot* | Messages: friend's text → share → CONTINUE? sheet slides over the chat → matched → ADD | `sfx_add` |
| **0:28–0:36** | **ORGANIZE THE PILE** / *max 3 in NOW PLAYING* | Three fast cuts: STACK 3D scroll · filters · STATS | `sfx_tick` |
| **0:36–0:40** | **A BACKLOG ISN'T A LIST PROBLEM.** / **IT'S A DECISION PROBLEM.** | Text card over a slow STACK drift | — |
| **0:40–0:44** | **SET THE DIALS** | DRAW: time + mood + genre dials | — |
| **0:44–0:47** | — | 📷 **Hand pulls the lever** on the real phone | `sfx_lever` |
| **0:47–0:56** | **PULL THE LEVER** | Cards deal and flip; push in on *why it matched*; swipe → PLAYING IT | `sfx_deal`, `sfx_flip`, `sfx_select` |
| **0:56–1:02** | **OUT OF DRAWS? CONTINUE?** | The gate: magenta countdown, INSERT COIN | Music → `arcade_continue` |
| **1:02–1:08** | **THE AD IS THE COIN.** / *opt-in, verified by RevenueCat* | Tap → *(cut, no ad content)* → `VERIFYING WITH REVENUECAT…` → coin lands | `sfx_coin` |
| **1:08–1:14** | **ONE AD = AN HOUR OF REAL PRO** / *no interstitials. ever.* | FREE PLAY: top bar `FREE PLAY 59:xx`, DRAW deals freely | `sfx_powerup` |
| **1:14–1:26** | **FINISH SOMETHING** | Mark cleared → the Credits Roll, allowed to play. Optional 📷 cut to the phone in hand | `*_victory` |
| **1:26–1:33** | **NO STARS. HEAD TO HEAD.** | RANK: `WHICH DID YOU ENJOY MORE?` ×2–3 → push in on `YOUR #N OF ALL TIME` | `sfx_select` |
| **1:33–1:43** | **SHARE YOUR PILE** / *no accounts. nothing stored on a server.* | HIGH SCORES card → share pile link → a friend's pile opens in FRIENDS (use `tools/make_pile_link.py` for the friend) | `sfx_friend` |
| **1:43–1:50** | **CONTINUE?** / *On Google Play now* | End card: logo, Play badge, `github.com/…` (Next Gen), "Built with RevenueCat", small IGDB credit | Music resolves |

The headlines follow the Gaming criteria verbs (save, organize, complete, rate, share) plus
DECIDE, which is our one idea, and the three CONTINUE? beats carry the whole Catvertising thesis.
Design is in every shot rather than in a section of its own.

### Recording checklist

- **Device:** an Android **phone**, not the Tab S6 Lite, because the frame and the judges' mental model
  are a phone. It must be a **free user with no draws left** for the gate and FREE PLAY shots (a
  Lifetime tester account never sees the gate), with the live Play build.
- **Demo pile:** ~30–60 real, varied games, some cleared and some ranked, so every screen has
  content; scroll everything once first so covers are cached. Record the time-budget numbers
  and use them in the hook.
- **Clean status bar** (Android demo mode; One UI may ignore parts of it, so set DND on as well):
  ```sh
  adb shell settings put global sysui_demo_allowed 1
  adb shell am broadcast -a com.android.systemui.demo -e command enter
  adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930
  adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
  adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
  adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
  # afterwards: ... -e command exit
  ```
- **Capture:** `scrcpy --record=shot01.mp4 -b 20M --max-fps=60` (records device audio on Android
  11+; `--no-playback` hides the mirror). One file per shot, and three takes of each. Move slowly and
  deliberately, and pause about 1 s before and after each tap so there's room to cut.
- **📷 Hand shots:** second phone, landscape, 4K/60 if available, locked exposure (tap-and-hold),
  room lights off, phone brightness at max, one warm lamp off to the side. Film the lever pull 5+
  times.
- **YouTube:** Public (not unlisted), title `CONTINUE? — the games you started deserve an ending`,
  a custom thumbnail (logo on void, no game art; needs a phone-verified YouTube account), and
  upload a caption file.

---

## Devpost text description (draft)

> **CONTINUE? — your pile of shame, finally fun.**
>
> Every gamer has a backlog. Mine had 87 games in it. Every app built to manage that backlog
> is a spreadsheet with box art — and a spreadsheet of 87 unplayed games is a guilt machine.
> So people stop opening them.
>
> CONTINUE? reframes the backlog as an arcade continue screen. In an arcade, `CONTINUE?`
> was never failure — it was a second chance, a countdown, and a coin. Your unfinished games
> aren't debt. They're a queue of second chances.
>
> **Save.** Share any game from anywhere on Android — a YouTube review, a Reddit thread, a
> Steam page — straight into your pile. A sheet slides up over the app you're already in,
> resolves the title, and adds it in about two seconds. Or import your whole Steam library
> in one tap.
>
> **Organize.** Stacks, smart filters, a "shortest first" view for when you actually want to
> finish something, and a hard cap of three games in NOW PLAYING because focus is a feature.
> A Time Budget bar tells you the truth: *412 hours. 87 games. At six hours a week, you're
> done in 2029.*
>
> **Decide.** The real problem isn't recording games, it's paralysis. Tell CONTINUE? how much
> time you have, what mood you're in, and what console you're near — then pull the lever. It
> deals you three cards from your own pile and tells you *why* each one matched.
>
> **Complete.** Finishing a game triggers a full Credits Roll — your stats scrolling like
> film credits over the key art, with a coin shower. Completion should feel like something.
>
> **Rate.** No stars. Star ratings are uncalibrated noise. CONTINUE? asks "which did you
> enjoy more?" and binary-searches the game into your personal all-time ranking. You don't
> get an 8/10 — you get *your #4 of all time*.
>
> **Share.** Your top ten as an arcade high-score table. Your completion as a rank card.
> Your pile as a stat card that says *"412 hours. 87 games. Send help."*
>
> Built with Kotlin and Jetpack Compose. Monetized with RevenueCat: a Pro subscription,
> consumable coin packs, and virtual currency earned from server-verified rewarded ads —
> all three layers in one revenue stack.

---

## Influencer category write-up (Gaming)

> **Category: Gaming — Gaming Influencer Award.**
>
> The brief asked for a gaming bucket list where players can easily save, organize, complete,
> rate, and share the games they want to play. CONTINUE? addresses each, and treats the
> third judging criterion — *does this feel enjoyable rather than a chore* — as the primary
> design constraint rather than a nice-to-have.
>
> **The audience** is the backlog gamer: someone with a Steam library in the hundreds, a
> Game Pass subscription, a Switch, and a sincere intention to play all of it. They already
> follow gaming creators precisely because they're trying to decide what's worth their
> limited hours. Their bottleneck isn't discovering games — creators solved that — it's
> **capturing** what they discover and then **choosing** among it.
>
> CONTINUE? serves that exact loop. Android's share sheet means a game recommended in a video
> reaches the pile in two seconds without leaving the video. The DRAW machine then does the
> deciding, which is the thing a 200-game library makes impossible. And the ranking and share
> cards give the audience a way to talk back — a personal top ten is inherently arguable,
> which is the currency of gaming communities.
>
> No influencer's name, likeness, brand, or logo appears anywhere in the app, its store
> listing, its artwork, or its marketing.

---

## Design Award write-up

> **What to look at, in order:**
>
> 1. **The DRAW lever and card deal (0:35 in the video).** The lever is genuinely
>    physics-driven — spring resistance, a detent, and a haptic at the bottom, not a button
>    with a canned animation. The three cards deal along distinct arcs with staggered timing
>    and settle at slightly randomized angles, so it reads as *dealt* rather than laid out.
>    Every card is interruptible mid-flight; swipe commits are decided by velocity, not
>    distance.
> 2. **The Credits Roll (1:05).** Clearing a game plays a ~7-second cinematic: CRT power-on,
>    per-letter title reveal, key art Ken Burns, film-style credits of your own stats, and a
>    coin-shower particle burst. It's the emotional peak of the app and it's skippable on tap.
> 3. **Pairwise ranking (1:22).** A new interaction for this category. Instead of a star
>    rating, two games go head to head and binary-search the new entry into your all-time
>    list in five taps or fewer.
> 4. **The share cards.** Rendered live from Compose to a bitmap, not from templates — the
>    HIGH SCORE card lays your real top ten out as an arcade leaderboard.
> 5. **The Time Budget visualization.** Every game in your pile as a proportional bar, with a
>    slider that recalculates the year you'd finish. Data as an emotional object.
>
> **The system.** Neo-arcade, dark only, built on a deliberate restraint: modern layout and
> spacing, retro only in accents. Scanlines appear at 3% opacity on exactly two surfaces.
> Chromatic aberration appears on exactly one word. One gold accent carries the whole app;
> green and magenta are punctuation. A single spinning-coin animation is reused as the app's
> mascot motion across earning, spending, adding, and clearing — one authored gesture instead
> of twelve unrelated ones.

---

## Pricing rationale — for the write-up and for judges' questions

> Decided 2026-08-12. Record it here because "why is your annual tier missing?" is exactly the
> kind of question a RevenueCat judge asks, and the honest answer is a *strategy*, not an
> oversight.

**The ladder is Monthly $3.99 and Lifetime $9.99. There is deliberately no annual tier.**

An annual plan at a sane price (~$19.99) would sit *above* Lifetime, which makes it strictly
irrational to buy — nobody rents for a year at twice the price of owning forever. Rather than
ship a decoy nobody should pick, the tier was cut. `continue_pro_annual` was created in
RevenueCat, then archived when the Play products were built (docs/09-PENDING-INPUTS.md).

**Monthly exists mainly to make Lifetime obvious.** At $3.99/mo, Lifetime pays for itself in
two and a half months — the comparison is instant and favours the option we actually want
chosen. Monthly is the low-commitment on-ramp and the anchor; Lifetime is the intended
destination.

**The known trade-off, stated plainly:** this suppresses recurring revenue, which is the
metric RevenueCat's own dashboards are built to showcase. That's an accepted cost, not an
oversight. The bet is that a backlog manager is a *tool you own*, not a service you rent, and
that pricing it that way converts a far higher share of a small user base. Coins and rewarded
ads carry the ongoing-revenue side instead — which is also what makes the Catvertising thesis
load-bearing here rather than bolted on.

**If the app finds real scale, revisit.** The plan is to raise Lifetime (~$24.99) or
reintroduce annual *only* once there's actual retention data to price against. Guessing at
elasticity with zero users would be theatre.

---

## Catvertising write-up

> **The premise: in an arcade, continuing has always cost a coin.** So in CONTINUE?, a
> rewarded ad isn't an interruption grafted onto the product — it's the product's central
> metaphor made literal.
>
> **The stack has three layers, unified by RevenueCat.**
> Ads sit at the bottom and feed **Coins** (RevenueCat Virtual Currency), which can equally
> be *bought* as consumable IAPs or *granted* monthly by the **Pro subscription**. Because
> all three converge on one currency, they're a single system rather than three tactics —
> and RevenueCat reports ad revenue and subscription revenue in one view of customer value.
>
> **Every ad is user-initiated, explicitly priced, and additive.**
>
> - **INSERT COIN.** Free users get one draw a day. The second draw opens an arcade continue
>   screen: watch an ad for a coin, spend a coin you have, or go Pro. The countdown loops
>   rather than locking anyone out — it's atmosphere, never a punishment.
> - **FREE PLAY MODE.** The strongest idea here: a rewarded ad grants **60 minutes of the
>   real `pro` entitlement**, using RevenueCat's server-verified temporary-entitlement ad
>   reward. The user gets genuine premium — unlimited draws, every share theme, full Steam
>   import — with a live countdown in the top bar. It's the best conversion mechanism in the
>   app, because people who feel Pro for an hour buy Pro. And it makes the ad unambiguously
>   *generous*: it hands the user more app.
> - **Theme unlock.** A premium share-card theme costs 25 coins or one ad for a single use.
>   Purely cosmetic; the user wanted the thing and chose the price.
> - **Import unlock.** Free Steam import shows 10 of your games; one ad unlocks the rest —
>   at the precise moment the user most wants the app to work.
>
> **What we refused to build,** because it matters as much as what we did:
> no app-open interstitials, no banners, no forced ads between draws, and no advertising
> anywhere near the Credits Roll. You never monetize the emotional peak.
>
> Rewards are granted only on AdMob's server-side verification callback into RevenueCat —
> never client-side — so they can't be spoofed. Pro subscribers see zero ads, and their coin
> stipend means they never touch the ad economy at all.

---

## Next Gen write-up

> Built solo, in eight weeks, as a student. The repository is public and MIT-licensed, with
> the Android app and its Cloudflare Worker backend in one place, plus the full design and
> architecture docs the project was built from.
>
> **Technical choices worth noting:** offline-first (Room is the source of truth, the network
> only fills the cache, so the pile works on a plane); a Worker backend that exists for a
> specific reason — a public repo cannot ship API keys, and RevenueCat's virtual-currency
> spend requires a secret key, so both live server-side; and a RevenueCat integration that
> goes well beyond a single paywall — entitlements, virtual currency, consumable IAP,
> server-verified ad rewards, temporary entitlements, remote paywalls, and Customer Center.

---

## Asset production notes

- **1024×1024 icon** — original artwork, no game art, no influencer branding. Must read at
  48dp.
- **1179×2556 screenshots, no device frames** — this is an iPhone 15 Pro resolution, so
  render at that exact size from an emulator configured to match, or compose them from
  captures. Do not upscale.
- **Screenshot picks (in order):** the DRAW cards mid-deal · the Credits Roll · pairwise
  ranking · the HIGH SCORE share card · the pile with the Time Budget bar.
- **Feature graphic 1024×500** — wordmark on the arcade cabinet, no box art.
- Keep every source file and licence receipt (fonts, audio) in `assets/` in the repo.
