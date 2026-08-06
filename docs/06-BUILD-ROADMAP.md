# CONTINUE? — Build Roadmap

**Aug 5 → Sept 30, 2026 · 8 weeks**

Two tracks run in parallel. The **Store track** is the critical path and gates three of four
categories; the **Build track** is what judges see. Never let a build task delay a store task.

```
        W1      W2      W3      W4      W5      W6      W7      W8
STORE   ████████████████████████████████████░░░░░░░
        register  closed test (14d)   prod access  release
BUILD   ████████████████████████████████████████████████████████
        skeleton  core    signature  money   polish  FREEZE  submit
```

---

## Week 1 (Aug 5–11) — "Uploadable"

**The only goal that matters: a signed release APK that installs and doesn't crash.**
Features are secondary this week. The closed-testing clock cannot start without a build.

**Store track (do first, every day)**
- [ ] Complete Play Console registration + identity verification
- [ ] Collect **18** tester Gmail addresses into a Google Group
- [ ] Create the AdMob account; create 2 real rewarded units
- [ ] Create the RevenueCat `CONTINUE` project, app, entitlement, products, offering

**Build track**
- [ ] Project scaffold: Compose, Hilt, Room, Navigation, DataStore
- [ ] **Design system first** — theme, tokens, typography, spacing, the core composables
      (`ArcadeButton`, `GameCard`, `CoinCounter`). Building tokens before screens is what
      keeps a 6-week app visually coherent.
- [ ] Cloudflare Worker deployed with `/games/search`, `/games/:id`, `/resolve`, `/health` (IGDB-backed, KV-cached)
- [ ] PILE screen with grid view, backed by Room
- [ ] DISCOVER search wired to the Worker → add to pile
- [ ] Email `partner@igdb.com` re: commercial use; generate the offline seed set
- [ ] App icon (v1) + release signing + **keystore backed up twice**
- [ ] **Upload to closed testing the hour the account is live**

**Exit criteria:** APK on the closed track; testers invited.

---

## Week 2 (Aug 12–18) — "Core loop"

The 14-day clock should be running. Now build the thing.

- [ ] Pile states, state transitions, NOW PLAYING with the cap of 3
- [ ] Stacks: create, add/remove, drag to reorder
- [ ] Sort & filter, including the "shortest first" chip
- [ ] **Android Share Target** + the title parser (with unit tests) + `/resolve`
- [ ] Time Budget bar (compact form)
- [ ] Onboarding flow
- [ ] Stack view (the 3D-ish card stack) — first pass
- [ ] Daily check on the tester opt-in count

**Exit criteria:** you can add a game from YouTube in 2 seconds and organize a real pile.

---

## Week 3 (Aug 19–25) — "The signature"

The features that win the Gaming and Design awards. Give this week the most energy.

- [ ] **DRAW**: dials, the lever with spring physics + haptics
- [ ] Selection algorithm with mood mapping and weighting
- [ ] Card deal animation, flip, and swipe verdicts (velocity-based commit)
- [ ] **Credits Roll** completion ritual
- [ ] **Pairwise ranking** — buckets, binary-search placement, comparison UI
- [ ] Coin animation component (the reusable mascot motion)

**Exit criteria:** the full emotional arc works — draw a game, play it, clear it, rank it.

---

## Week 4 (Aug 26 – Sept 1) — "Money"

- [ ] RevenueCat SDK wired: entitlements, `isPro`, customer info listener
- [ ] Remote paywall (RevenueCatUI), themed
- [ ] Virtual currency: balance read, earn events, Worker `/coins/spend`
- [ ] AdMob rewarded ads, both units, with `enableRewardVerification()`
- [ ] **`CONTINUE?` / INSERT COIN screen**
- [ ] **FREE PLAY MODE** — temporary Pro entitlement from an ad, with the live countdown
- [ ] Pro gating across all features via a single `ProGate`
- [ ] Customer Center in settings
- [ ] Steam import (Worker + review screen + free/Pro split)

**Exit criteria:** ad → verified coin → spend → unlock works end-to-end on a real device.
**Also: apply for production access as soon as the 14 days complete.**

---

## Week 5 (Sept 2–8) — "Share & stats"

- [ ] Share-card renderer (Compose → Bitmap → `FileProvider`)
- [ ] All 4 core cards: CLEARED, HIGH SCORE, THE PILE, THE STACK
- [ ] Share themes + coin unlock + ad-for-single-use
- [ ] YOU tab: high scores, stats, trophies
- [ ] Time Budget full visualization
- [ ] Clipboard nudge
- [ ] Empty states, error states, offline behaviour
- [ ] Offline seed dataset bundled

**Exit criteria:** every judging criterion in the Gaming brief has a finished feature.

---

## Week 6 (Sept 9–15) — "Polish" → **FEATURE FREEZE at end of week**

No new features after this week. None.

- [ ] Motion pass: every transition, every spring, shared-element transitions
- [ ] Haptics pass
- [ ] Sound pass (original/CC0 only, licence documented)
- [ ] Performance: Baseline Profile, scroll jank, cold start under 1.5s
- [ ] Test on 3+ real devices, including a cheap one and a tablet
- [ ] Accessibility: content descriptions, contrast, reduce-motion handling
- [ ] Final app icon, feature graphic, all screenshots
- [ ] Privacy policy hosted; Data Safety form completed
- [ ] **Production release submitted** once access is granted

**Exit criteria:** app is live (or in final review) and feels finished.

---

## Week 7 (Sept 16–22) — "Submission assets"

- [ ] **Demo video** — script, record, edit, publish to YouTube (under 2:00). See
      `docs/07-SUBMISSION-KIT.md`. Budget 2 full days; this is the artifact most judges
      actually experience.
- [ ] Devpost text description
- [ ] Design Award write-up (what to look at, in order)
- [ ] Catvertising write-up (the three-layer stack + what we refused to do)
- [ ] Influencer category write-up (audience fit)
- [ ] README overhaul for Next Gen judges — architecture, build steps, screenshots
- [ ] Repo made public with the MIT licence detected in the About section
- [ ] 1024×1024 icon + 1179×2556 unframed screenshots
- [ ] Play promo code generated as a trial backup

---

## Week 8 (Sept 23–30) — "Buffer & submit"

Deliberately mostly empty. Something will go wrong; this is where it gets fixed.

- [ ] Submit to Devpost **by Sept 27** — never on deadline day
- [ ] Fix whatever store review flags
- [ ] Bug triage from testers
- [ ] `#BuildInPublic` posts if pursuing that award

---

## Build order rules

1. **Design tokens before screens.** Retrofitting a visual system across 12 screens is how
   week 6 disappears.
2. **Room before network.** The UI reads from the DB always; the network only fills it.
   This gives offline support for free instead of as a later migration.
3. **Ship the skeleton early, always.** Any week without a green release build is a week the
   store track is at risk.
4. **Animations last within a feature, but never skipped.** Get the feature correct with
   default transitions, then do a dedicated motion pass. Do not interleave.
5. **If behind: cut features, never cut polish.** A smaller app that feels immaculate beats
   a bigger app that feels unfinished — on every single criterion these judges use.

## Cut list, in the order things get cut

If week 5 arrives and you're behind, drop from the top:

1. Barcode scanning
2. Clipboard nudge
3. Cloud backup / Firebase
4. Year in Games (Wrapped)
5. App skins (keep share themes — they're the better coin sink)
6. Trophies
7. Stack view 3D (grid view alone is fine)

**Never cut:** DRAW, Share Target, Credits Roll, pairwise ranking, share cards, the ad→coin
→unlock loop. Those six *are* the submission.
