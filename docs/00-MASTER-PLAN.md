# CONTINUE? — Master Plan

## 1. The timeline is not one month. It's eight weeks — and one gate eats three of them.

You believed we had ~1 month. The actual Shipaton 2026 submission window is
**July 31 → Sept 30, 2026**. Today is **Aug 5**, so there are **~8 weeks**.

That is the good news. The bad news is bigger:

> **Three of our four target categories require a URL to a fully published Play Store app.**
> You don't have a developer account yet. New *personal* Play accounts must run a closed
> test with **12 testers opted in for 14 consecutive days** before they can even *apply*
> for production access — and that application takes up to 7 more days to review.

Worst-realistic case, counting from your stated "applied by Aug 8":

| Step | Duration | Ends |
|---|---|---|
| Account registration + identity verification | 3–14 days | ~Aug 11–22 |
| Closed test: 12 testers, 14 **consecutive** days | 14 days | ~Aug 25 – Sept 5 |
| Production access application review | ≤7 days | ~Sept 12 |
| Production release review | 1–7 days | ~Sept 19 |
| **Buffer before deadline** | | **~11 days** |

It fits. It does **not** fit if anything slips twice. See
`docs/01-PLAY-STORE-CRITICAL-PATH.md` — that document is priority zero and its week-1
actions matter more than any feature in this plan.

**The strategic consequence:** we build in an order that produces a *shippable, uploadable
APK in week 1*, not a beautiful app in week 6. The closed-testing clock runs on a skeleton.
Polish happens *while the clock is already ticking*.

**The safety net:** the Next Gen Award needs no store release at all — just the public repo,
the license, and the video. If Play slips catastrophically we still have a funded entry in a
$15,000 category. This is why the repo is open-source from commit one, not as an afterthought.

---

## 2. The product thesis

The Gaming Influencer brief asks for a bucket list where players "save, organize, complete,
rate, and share" games. Three judging criteria: **discovery/organization**,
**completion/sharing**, and — the one that actually decides the winner —

> *"Does managing the backlog feel enjoyable rather than another chore?"*

Every existing backlog app (Backloggd, Grouvee, GG, Backloggery) is a **database with box
art**. A spreadsheet. And a spreadsheet of 87 unplayed games is a **guilt machine** — the
"pile of shame". People abandon these apps because opening them feels bad.

**CONTINUE? reframes the pile as an arcade continue screen.** In an arcade, `CONTINUE?` isn't
failure — it's a second chance, a countdown, a coin, and you're back in. The unfinished games
aren't a debt. They're a queue of second chances.

That reframe drives three product decisions no competitor makes:

**(a) The app answers "what should I play?", not "what do I own?"**
The real pain isn't recording games — it's *paralysis*. 87 games, two free hours, and you end
up rewatching something instead. **DRAW** is a physical arcade lever that takes your time,
mood, and platform and deals you three cards from your own pile. That's the daily habit.
Nobody opens a spreadsheet daily. People open a slot machine daily.

**(b) Ranking, not rating.**
Five-star ratings are uncalibrated — everything lands on 8/10 and means nothing. CONTINUE?
uses **pairwise comparison**: "which did you enjoy more?", binary-searched into your list.
Output is a *true personal ranking* — "your #4 of all time" — which is dramatically more
shareable and more arguable than a star count. (Beli proved this for restaurants.)

**(c) Ads that give rather than take.**
Covered in §4. Short version: an arcade continue *literally costs a coin*, so a rewarded ad
is the single most thematically native monetization event this app could possibly have.

---

## 3. Feature → judging criterion map

Build nothing that doesn't earn a row here.

### Gaming Influencer Award

| Criterion | Feature | Why it wins |
|---|---|---|
| **Discovery** — save games fast | **Android Share Target** | Watching a YouTube review → Share → CONTINUE? → a bottom sheet resolves the title against IGDB and adds it in ~2s, without leaving YouTube. This is the single best answer to "can users quickly save games when they discover them" that Android permits, and iOS entrants literally cannot match it. |
| | Steam import | Seed 143 real games in one tap. Turns an empty app into a real pile instantly — also the best possible demo-video moment. |
| | Clipboard detection, in-app search, barcode scan | Every capture path covered. |
| **Organization** | Stacks, smart filters, Now-Playing cap of 3 | Opinionated: capping "currently playing" at 3 is a *design stance* about focus, and judges notice stances. |
| | **Time Budget bar** | "412 hrs · 87 games · at 6 hrs/week you finish in 2029." Organization that's emotionally legible, not just sortable. |
| **Completion** | **Credits Roll** ritual | Finishing a game triggers a cinematic credits sequence with your stats. Completion becomes a *reward*, not a checkbox. |
| **Rating** | Pairwise ranking | Genuinely novel in this category. Produces a rank, not a number. |
| **Sharing** | Arcade high-score share cards | Your Top 10 rendered as an arcade high-score table. Beautiful, native to the theme, extremely postable. |
| **Enjoyment** | DRAW, haptics, motion, coins, streaks | The whole app is built around this criterion. |

### RevenueCat Design Award
- **Innovation:** the DRAW lever (physics-driven, haptic), pairwise ranking, the Time Budget
  visualization, the Credits Roll, share-cards rendered from live Compose.
- **Aesthetics:** a committed neo-arcade/CRT system — see `docs/03-DESIGN-SYSTEM.md`. The
  bar is *restraint*: modern layout and spacing, retro only in accents. Cheap retro looks
  cheap; expensive retro looks like a design award.

### Catvertising Award
- Three-layer stack (subscription + consumable IAP + virtual currency) with **ads as the
  free on-ramp into all of it**, unified in RevenueCat. See `docs/04-MONETIZATION.md`.
- The headline mechanic: **watch an ad → get 60 minutes of real Pro**, server-verified via
  RevenueCat's temporary-entitlement ad reward. Ads don't interrupt the app; they *unlock* it.

### Next Gen Award
- Public repo, MIT license, clean architecture, real README, meaningful commit history.
- The RevenueCat integration is unusually deep (entitlements + virtual currency + ad rewards
  + remote paywalls + Customer Center), which is exactly what "thoughtfully use RevenueCat"
  is asking for.

---

## 4. What we are deliberately NOT building

Scope discipline is how this ships. Explicitly out for v1:

- ❌ Social graph / following / friends' piles — huge backend, low judging return
- ❌ User accounts & login — anonymous RevenueCat ID + optional cloud backup only
- ❌ Reviews/comments feed — moderation liability, no criterion rewards it
- ❌ iOS / KMP — the "Ship Kotlin Everywhere" award is a trap that halves our polish
- ❌ Price tracking / deals — different app
- ❌ Achievements pulled from PSN/Xbox — API access is slow to obtain

---

## 5. Risk register

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| **R1** | **Play account verification or closed testing slips** | Loses 3 of 4 categories | Register within 3 days; recruit + collect 12 tester Gmail addresses *this week*; upload a skeleton build the hour the account is live. Next Gen is the fallback. |
| **R2** | Fewer than 12 testers stay opted in for 14 consecutive days | Resets the clock | Recruit **18**, not 12. Brief them that they must *install and keep it installed*. Check the opt-in count daily in Play Console. |
| **R3** | **Game data provider fails** — RAWG already did, on Aug 5 | Every screen breaks | Switched to IGDB. Provider hidden behind a `GameDataSource` abstraction so a swap is one file. Bundled offline seed set of ~500 games is now mandatory, so the app and the demo video work even if every upstream API is down. |
| **R4** | AdMob SSV needs *real* ad units, not test IDs | Ad rewards unverifiable | Create the AdMob account and real rewarded units in week 1, alongside Play registration. AdMob approval also takes days. |
| **R5** | Secret leakage in a public repo | Disqualification risk / abuse | All secrets in the Worker. `.gitignore` `local.properties`. Pre-commit secret scan. |
| **R6** | Scope creep kills polish | Mediocre on every axis | The roadmap has a hard feature freeze at end of week 6. |
| **R7** | Play policy rejection (ads/data safety) | Delay near deadline | Complete the Data Safety form and ad declarations at first upload, not at the end. |

---

## 6. Success definition

Ranked, so tradeoffs resolve themselves:

1. App is **live on Google Play** before Sept 20 (unlocks 3 categories + buffer).
2. **DRAW, Share Target, Credits Roll, pairwise ranking, and share cards** all work and look
   finished — these are the five things judges will actually see.
3. The **ad → coin / ad → temporary Pro** loop works end-to-end with server verification.
4. The demo video is **under 2 minutes** and shows the app on a real device.
5. Repo is public, MIT-licensed, and genuinely readable.
