# Production Access Application — draft answers

> **Status: drafted 2026-09-07, not yet submitted.**
> Everything below is paste-ready, but read §0 first — three things should be checked or done
> *before* you open the form.
>
> **The form is seven questions, every one capped at 300 characters** (confirmed in the live
> form, 2026-09-07 — 3 on page 1, 2 on page 2, 2 on page 3). **2,100 characters is the whole
> application.** All seven paste-ready answers are at the top of §2. The long-form answers below
> them have no field to go in; keep them for a reviewer follow-up, an appeal, or Devpost.
>
> Play Console → **Test and release → Testing → Closed testing → Apply for production access**.
> Google's field labels have changed more than once; the questions below are grouped by what
> Google's own support page (answer/14151465) says the form asks for. Paste each answer into
> the closest matching field rather than expecting a 1:1 match.

---

## 0. Before you open the form

| # | Item | Why it matters here |
|---|---|---|
| 1 | **Confirm the opted-in count reads "met" on the Closed testing dashboard.** | There is no live opted-in number in Play Console — only the met/not-met bar. The Testers tab shows *invited*, which is always higher. Don't submit against an assumed 15. |
| 2 | **Confirm the closed track has run continuously since 2026-08-19.** | The clock is per-tester continuous opt-in. 2026-08-19 → 2026-09-07 is 19 days, comfortably past 14. |
| 3 | **Do NOT change country availability before submitting.** | The EEA/UK/CH exclusion decided 2026-08-24 is still outstanding. Country availability is per track — excluding a country a current tester lives in removes their access and restarts *their* 14 days. Do it on the **production** track only, after access is granted. See §5. |

Applying for production access does **not** publish anything. It is a reviewed form; approval
just unlocks the production track. The EEA/UK/CH ads gap therefore blocks the *production
release*, not this application.

---

## 1. The facts these answers rest on

**Verified from the repo and this session:**

- Closed test live on the Closed track since **2026-08-19**; today is **2026-09-07** (19 days).
- Closed track is currently on **`versionCode 8` / `0.8.0`**, and has run there ~5 days with
  **no bugs or defects reported** — only forward-looking feature suggestions.
- Track history: **`4` → `6` → `7` → `8`.** Four builds reached testers during the closed test,
  each answering a specific feedback round. (Corroborated independently: Google's pre-launch
  device farm produced `0.7.0` installs on 2026-08-26 and `0.8.0` installs on 2026-08-28, which
  only happens on upload.)
- **~21 tester-reported items across four feedback rounds**, all addressed. Full table in §3.
- **One crash**, reported by three testers and carried as two Play Console issues, root-caused
  and fixed in the next build.
- **150 app unit tests + 50 Worker tests, 0 failures.**

**Verified against RevenueCat this session — the tester population:**

Filtering the 78 RevenueCat customer records by the device-farm signature (Android API 30
exactly, `first_seen_at` equal to `last_seen_at` to the millisecond, US/BR, clustered on
build-upload days) removes 48 of them and leaves 30 real installs. Of those:

- **21 are on `0.8.0` and were last seen between 2026-09-03 and today**, one of them minutes ago.
- **Not one non-farm install has a `first_seen_at` later than 2026-08-21.** That matters: a
  reinstall mints a fresh anonymous id, so a September first-seen would be churn. There is none.
  Every currently-active install has been continuously present since 19–21 August — ~18 days.
- One of the 21 (`first_seen` 2026-08-12, before the closed test) is Mikhil's own device.

This is install-level evidence, not Play's opt-in metric, so it doesn't *prove* the opt-in
count. But it is a strong independent corroboration of "comfortably more than 12, continuously,
for well over 14 days" — and it's a good deal firmer than the ~15 estimate it replaces.

**Verified against RevenueCat this session — testers did complete purchases:**

Sampled 6 of the active testers. **Five had completed transactions**, all `store: play_store`,
all `environment: sandbox` (Play license-tester cards, no real money):

- **Four bought the one-time lifetime upgrade** (`prodbf1ac2c0d6`) on 19–20 August — three in
  Mauritius, one in Malaysia. Each shows the `pro` entitlement **active**, so the entitlement
  gate is confirmed working end to end against a real Play transaction.
- **One bought the monthly subscription** (`prodb3fdae1092`) **five separate times across
  20–25 August** — sandbox subscriptions expire in minutes, so this is a tester deliberately
  re-running the subscription flow on five different days.
- One had no transactions.

This was a sample, not an exhaustive count, so don't state a precise number of purchasers.
"Testers completed both the one-time upgrade and the monthly subscription through Play's
license-tester flow" is fully supported and is what the answers below say.

**Do not put RevenueCat's numbers in this form.** Two separate traps:

1. **The customer count.** 77–78 customers is anonymous *installs* — the app configures
   `Purchases` with no `appUserID` — and 48 of them are Google's own pre-launch device farm.
   Quoting 77 next to a tester count invites exactly the question you don't want asked.
2. **The revenue figures.** Those sandbox transactions carry dollar amounts ($9.99, $19.95,
   even $27.93). They are meaningless — sandbox pricing artefacts, not money. Never repeat them
   anywhere, and never imply the closed test generated revenue.

---

## 2. The answers

> ### ⚠️ Page 1 has a 300-character limit (confirmed in the live form, 2026-09-07)
>
> The **first three** questions cap answers at 300 characters. Page 2 does not — the long-form
> answers further down are for those. Use the compressed versions below for page 1; every count
> here is measured, not estimated (283 / 278 / 286 characters).
>
> ⚠️ **Q2 is not the question the long-form §2 assumed.** Its live wording asks about feature
> coverage and realistic usage, not about how you communicated — see the note under it. Q1 and
> Q3 below are matched to the headings further down; **if their live wording differs too, say so
> and they get re-cut**, because which question carries which fact drives the whole compression.
>
> What had to go from page 1, so you know the trade: crash-report monitoring, the reasoning for
> making the feedback form anonymous, and the "went back to each reporter to confirm the fix"
> line. All survive on page 2, so the application as a whole loses nothing.
>
> **1. Recruitment** *(283 chars)*
>
> ```
> Personally — coursemates, friends and family who play games and have real backlogs of their own. No paid tester service. I collected each Gmail address individually, added each to the tester list, and briefed every tester myself. Over 12 opted in, continuously, since 19 August 2026.
> ```
>
> **2. Engagement, feature coverage and realistic usage** *(278 chars)*
>
> The live wording, confirmed 2026-09-07, is **not** "how did you engage with testers" — it is:
> *"Describe the engagement you received from testers during your closed test. Include whether
> or not testers utilised all of the features in your app and whether tester usage was
> consistent with how you would expect a real user to use your app. If not, describe the
> differences that you would expect to see."*
>
> Three asks in 300 characters. Answer them in the order asked, and answer the second and third
> **affirmatively** — a hedge here is what invites the follow-up.
>
> ```
> 21 reports over 4 rounds, each answered with a build they retested. Every feature was covered across the group, 3 testers going end to end, including both paid upgrades via licence testers. Usage matched production: you open it briefly to pick a game, then close it and go play.
> ```
>
> Why it is built this way:
> - **"each answered with a build they retested"** is the strongest engagement evidence you have
>   — it shows a loop, not a one-way inbox, and it is the fact a reviewer cannot get anywhere
>   else.
> - **"across the group, 3 testers going end to end"** answers "all features" truthfully. Claiming
>   every tester used every feature is the overclaim that gets probed; naming the depth makes the
>   coverage claim credible instead.
> - **"both paid upgrades via licence testers"** is the part most closed tests cannot say at all.
>   Say "licence testers" explicitly — a reviewer assumes test accounts anyway, and being vague
>   about it is the only way to make it look bad.
> - **The last clause pre-empts the trap.** If the reviewer looks at session lengths they will
>   see 2–3 minutes. Stating *first* that brief sessions are the intended shape turns that from a
>   red flag into corroboration. Note it does not volunteer the number — it explains the pattern.
>
> Do **not** spend characters here on the "if not, describe the differences" clause. It is
> conditional on answering *no*; answering it anyway concedes a gap you do not have.
>
> **Runner-up** *(276 chars)*, if you would rather lead with entitlements than with tester depth:
>
> ```
> 21 reports over 4 rounds, each answered with a build they retested. Every feature was covered, including both paid upgrades via licence testers with entitlements unlocking. Usage matched production: this app is opened briefly to pick a game, then closed so you go and play it.
> ```
>
> ⚠️ **The WhatsApp / Play-feedback-limit / anonymous-Google-Form material now has no home on
> page 1.** It was written for a question this form does not ask there. It is genuinely strong
> — noticing a platform limitation and working around it — so place it wherever the form does
> ask how you communicated with testers, or in the page 2 long-form answer below.
>
> **3. What the feedback was** *(286 chars)*
>
> ```
> Around 21 items over four rounds: one crash (the 3D stack view), wrong playtime figures, share-to-app matching the wrong game, duplicate search results, three settings wired to nothing, and missing actions — removing a game, logging ones finished before install. Current build: no bugs.
> ```
>
> ---
>
> ### Page 2 — also 300 characters each (confirmed 2026-09-07)
>
> **4. "Who is the intended audience of your app?"** *(278 chars)*
>
> ```
> Gamers aged 13 and over who own far more games than they finish — console, PC and mobile players whose backlog has grown through sales, bundles and subscription catalogues. It is for people who want to enjoy what they already own rather than buy more. No child-directed content.
> ```
>
> The age band is deliberate and must stay: it matches the **13+ target audience** and content
> rating already declared in Play Console, and the decision *not* to join Designed for Families.
> An audience answer that contradicts those declarations is a self-inflicted flag. "No
> child-directed content" is Play's own phrasing — it closes the question rather than inviting it.
>
> **5. "Describe how your app provides value to users"** *(272 chars)*
>
> ```
> It solves choice paralysis. You save games from any app via the share sheet, then tell it how long you have tonight and it picks from the games you already own, explaining why. Finishing one is celebrated with a credits roll. Works fully offline, and every ad is optional.
> ```
>
> Built to answer the question actually being asked — this field exists to filter out low-value
> and spam apps, so it names a real user problem, the two mechanisms that solve it, and the
> payoff. The last sentence is doing quiet compliance work: **"works fully offline"** says the
> app is not a thin web wrapper, and **"every ad is optional"** pre-empts any ad-policy concern
> by stating the no-interstitial principle without needing the word.
>
> *Alternative if you would rather open on the problem than the label* — same facts, 270 chars:
>
> ```
> Most people own more games than they can play and end up scrolling instead of choosing. This app saves games from any app via the share sheet, then recommends one from your own library based on the time you actually have tonight. Offline-first, and every ad is optional.
> ```
>
> ---
>
> ### Page 3 — "Your production readiness", 2 questions, still 300 characters (confirmed 2026-09-07)
>
> **6. "What changes did you make to your app based on what you learned during your closed
> test?"** *(280 chars)*
>
> ```
> Four builds shipped during the test. Fixed a crash three testers hit; a report that the haptics toggle did nothing led me to audit every control and find two more wired to nothing. Improved search and share-matching accuracy, and added game removal, backdating, filters and stats.
> ```
>
> It opens with **"Four builds shipped during the test"** because that is the answer to the
> question behind the question — did you actually act on the test, or just wait out the 14 days.
> The haptics clause is the most valuable sentence in the whole application: it shows a report
> that was *generalised* into an audit that found two more faults nobody reported. That is the
> difference between fixing tickets and learning from a test, and it costs 100 characters.
>
> **7. "How did you decide that your app is ready for production?"** *(287 chars)*
>
> ```
> Feedback stopped producing bugs. The current build has run for over a week with only feature requests coming back. The one crash was fixed and confirmed by the testers who hit it, purchases and entitlements work end to end, 200 automated tests pass, and database migrations are in place.
> ```
>
> The question asks *how you decided*, so it leads with the **criterion** rather than a claim —
> "feedback stopped producing bugs" is a decision rule, and everything after it is the evidence
> that the rule was met. Distinguishing feature requests from defects is deliberate: it shows you
> know the difference, which is itself a readiness signal.
>
> ⚠️ **Check "over a week" before pasting.** `versionCode 8` went to the Closed track on
> 2026-08-28, so 10 days by the calendar — but you described roughly 5 days of tester activity on
> it. If testers updated later than the upload, swap in **"for several days"**; the sentence
> works either way and an overstated duration is not worth the risk.
>
> ⚠️ **"200 automated tests"** = 150 app + 50 Worker. Accurate as one figure; don't inflate it.
>
> ---
>
> ### ⚠️ The whole form is seven 300-character fields
>
> Page 1 (3) + page 2 (2) + page 3 (2), every one capped — **2,100 characters is the entire
> application.** **The long-form answers in the rest of §2 have no field to go in.** Do not try
> to compress them into these seven: each short answer above is already carrying its own load,
> and diluting one to smuggle in extra material is a net loss.
>
> They are not wasted. Keep them for:
> - **a reviewer follow-up or an appeal**, where the evidence table in §3 and the detailed
>   feedback→fix narrative are exactly what is wanted;
> - **the Devpost writeup**, which has no such limit.
>
> If any of the five live questions is worded differently from what is recorded here, say so and
> that answer gets re-cut — which question carries which fact drives the whole compression.

### Q — How did you recruit testers for your closed test?

> I recruited every tester personally. They are university coursemates, friends and family who
> actually play games and have real backlogs of their own, which is the audience the app is
> built for — so their feedback would be about whether the product works rather than about
> completing a task. I did not use any paid tester service or tester-exchange group.
>
> I collected each person's Gmail address individually and added each one directly to the
> closed test's tester email list, so I knew exactly who had been invited and could follow up
> with each of them by name. I briefed every tester personally: what the app is for, how to join,
> and that I wanted to hear what confused them or broke rather than reassurance. More than 12
> testers opted in and the test has run continuously since 19 August 2026.

### Q — How did you engage with your testers during the closed test?

> Individually, and through three channels, because the first one alone was not enough.
>
> I messaged every tester one-to-one on WhatsApp rather than broadcasting to a group. I asked
> for specific things rather than "any thoughts": what did you expect to happen that didn't,
> what did you never find, what looked wrong. I also nudged each of them to submit their
> feedback formally through the Play Store's "send feedback to developer" option, so it would
> reach me through the official channel.
>
> That is where I hit a practical limit worth explaining. Play allows each tester only **one**
> piece of feedback through that channel, with a character limit — which is fine for a first
> impression but useless for a test that ran for weeks across several builds. My testers had
> more to say than the channel could carry. So I set up an **anonymous Google Form** and used it
> to collect everything after that first submission. Making it anonymous was deliberate: these
> are people I know personally, and I wanted them able to tell me something was bad without it
> being awkward.
>
> Alongside that I monitored Play Console's crash reporting throughout. One crash reached me as
> two Play Console issues and three separate tester messages on the same day, which is how I
> knew immediately how widespread it was rather than treating it as a one-off.
>
> Feedback arrived in four distinct rounds. Each round was answered with a new signed build
> promoted to the closed track — four builds reached testers during the test — and after each one
> I went back to the specific tester who had reported the issue and asked them to confirm the fix
> on their own device.

### Q — What feedback did you receive from your testers?

> Around twenty-one distinct items across four rounds. They fell into five groups:
>
> 1. **One crash.** The 3D "STACK" view of the game pile crashed on open for three testers.
> 2. **Data accuracy.** Game playtime estimates were wrong for some titles ("Minecraft says 900
>    hours"); the share-to-app feature matched the wrong game for some inputs; one game showed
>    no artwork; search returned duplicates and missed results when a title was typed without
>    spaces ("spiderman" vs "Spider-Man").
> 3. **Discoverability.** A tester judged the DISCOVER screen thin — and the real finding
>    underneath was that they had never discovered the app's share-to-add feature at all,
>    because nothing in the app mentioned it existed.
> 4. **Controls that did nothing.** The haptics toggle, a clipboard-detection switch, and an
>    hours-per-week input were all present in the UI and wired to nothing.
> 5. **Missing affordances and features.** No way to remove a game added by accident; no way to
>    log games finished before installing the app; filters too coarse and inconsistent between
>    screens; no way to reorder rankings; a confusing purchase button label.
>
> In the most recent round, on the current build, testers reported **no bugs or defects** — only
> suggestions for features to add later.

### Q — How did you use that feedback to improve your app?

> Every one of the twenty-one items was addressed, and four builds were shipped to the closed
> track during the test so testers could confirm the fixes on their own devices.
>
> Specific examples:
>
> - **The crash** was root-caused to a missing cache key in a Compose `derivedStateOf` that
>   froze an index bound to the list length at first composition. Fixed, and covered by a
>   regression test.
> - **Share-to-app matching** was reproduced against my backend before anything was changed and
>   turned out to be four separate bugs. One benchmark input went from matching the wrong game
>   at 0.29 confidence to the right one at 0.90.
> - **The haptics toggle** was the most useful report I got. The setting was stored and drawn,
>   but five screens each constructed their own haptics engine and none consulted it. It is now
>   a single injected instance that enforces the preference internally, so there is no longer a
>   way to fire a haptic that bypasses the switch. Finding it led me to audit every control in
>   the app; two more dead controls turned up and were fixed the same day.
> - **Accidental adds** now have a REMOVE FROM PILE action; **games finished before install**
>   can be backdated; **filters** were rebuilt on one shared vocabulary now used identically by
>   the pile, the recommendation feature and a new statistics screen; **rankings** can be
>   reordered and removed.
> - I also closed a **coin-economy exploit** that a tester's report led me to — the reward for
>   completing a game could be claimed repeatedly by moving the game back and forth. It is now
>   claimed once per game, transactionally.
>
> Separately, prompted by how much the closed test surfaced, I added a database migration path.
> The app previously had none, so the next change to its data model would have crashed on launch
> for every existing user while fresh installs looked fine. That is now covered by tests.
>
> The app carries 150 automated tests and its backend another 50; all pass.

### Q — Did your testers use all of the available app features?

> Collectively, yes — every feature in the app was exercised by at least one tester during the
> test, and the reported items span essentially the whole surface: the pile and its five states,
> the 3D stack view, the share-to-add flow from YouTube, TikTok, Reddit and Instagram, search,
> the DISCOVER browse screen, the recommendation draw, head-to-head ranking, the completion
> credits roll, the statistics screen, settings, and the GO PRO upgrade screen.
>
> That includes the paid features, which I made a point of getting covered. Testers completed
> real purchases through Play's billing flow using licence-tester accounts — both the one-time
> lifetime upgrade and the monthly subscription — and I verified in RevenueCat that the purchases
> registered against the correct products and that the entitlement unlocked the Pro features as
> intended. One tester ran the subscription flow five separate times over six days. The upgrade
> screen also produced its own feedback: one of the fixes shipped during this test was a purchase
> button whose label was too clever to be clear.
>
> Individually the depth varied, which I think is the honest answer. Around three testers went
> through the app end to end and are the source of most of the detailed reports. The rest
> concentrated on the core loop — add a game, get a recommendation, mark one finished — which is
> what I would expect the majority of real users to do.

### Q — Did tester usage match expected production user behaviour?

> Yes, and I want to be precise about this because the raw session lengths look short at first
> glance: most testers use the app for two to three minutes a day.
>
> That is the intended behaviour, not thin engagement. CONTINUE? is a backlog manager for
> gamers. A user opens it to save a game they just saw in a video, or to be told which of the
> games they already own to play tonight — and then they close it and go and play that game. The
> app's core feature exists specifically to stop people scrolling through lists instead of
> playing. A user spending forty minutes inside it would mean the product had failed at its own
> stated purpose. Short, frequent, purposeful sessions are the success case.
>
> Alongside that, the deeper sessions I did see — the testers who filled a pile, ranked a dozen
> games, and ran the recommendation feature repeatedly — match the heavier end of what I expect
> in production, and they are where most of the substantive feedback came from.

### Q — Why do you believe your app is ready for production?

> Three reasons.
>
> **It is stable.** The one crash that occurred during the closed test was reported,
> root-caused, fixed and confirmed by testers on a subsequent build. The current build has been
> on the closed track for several days with no defects reported at all — the only feedback now
> is suggestions for future features. The app has 150 automated tests, its backend 50 more, and
> a database migration path so existing users survive future updates.
>
> **It is complete for what it claims to do.** Every feature described in the store listing is
> built and working: saving games from a share sheet, an offline-capable game index so matching
> works with no connection, the pile, the recommendation draw, head-to-head ranking, statistics,
> and the upgrade screen. The app is offline-first — the on-device database is the source of
> truth, not a cache — so it does not fail when the network does. The billing integration is
> proven rather than assumed: testers completed both the one-time upgrade and the monthly
> subscription through Play billing during the closed test, and I confirmed each transaction
> registered against the right product and unlocked the right features.
>
> **It is compliant and honest.** Game data comes from IGDB under their terms, with their
> required attribution shown wherever their data appears. The privacy policy is published and
> the Data Safety declaration matches what the app actually does. On advertising: the app has no
> interstitial and no banner ads. Every ad in it is opt-in — the user taps a button that tells
> them exactly what they get in return before any ad loads. No ad has ever appeared unprompted
> and none will.

---

## 3. Evidence appendix — every tester report and what shipped

Keep this. If the review comes back asking for specifics, this is the answer; it is also the
Devpost writeup's raw material.

| Round | Reported | Shipped |
|---|---|---|
| **1 — Aug 19** | "DISCOVER feels a bit empty" | Five browse rails instead of two (three new backend routes); each query probed against live IGDB first — the obvious "hidden gems" formulation returned joke listings at 100/100 and was rebuilt on press ratings |
| | *(found underneath it)* tester had never discovered the share-to-add feature | The feature is now named in onboarding, in the empty pile state, and on DISCOVER itself — it had been mentioned nowhere in the app |
| | Shares from YouTube/Instagram dumped a raw URL into the manual field | Four separate bugs fixed; benchmark input went 0.29 (wrong game) → 0.90 (right game) |
| **2 — Aug 23** | "Minecraft says 900 hours" | Playtime rebuilt on per-field IGDB data, with an ENDLESS label for games that have no completion time |
| | "Resident Evil Requiem matched to Resident Evil OG" | Share auto-match accuracy reworked |
| | An ALL-CAPS YouTube title matched nothing | Query normalisation |
| | DISCOVER needed a way to clear a search | Clear button + add-confirmation banner |
| | STACK gave no hint it could be moved | Swipe hint |
| | "Games added from DISCOVER never appear in the pile" | **Not a bug** — the games were in the RETIRED tab. Investigated to ground and closed |
| **3 — Aug 26** | **Crash** opening STACK (3 testers, 2 Play Console issues) | Missing `remember` key in a `derivedStateOf` froze the index clamp at first composition. Fixed + regression test |
| | No way to undo an accidental add | REMOVE FROM PILE |
| | Upgrade screen's lifetime button said "INSERT COIN" | Now "PURCHASE" |
| | TikTok shares dead-ended on a list of wrong games | Now a blank field with an explanation when nothing matches confidently |
| **4 — Aug 28** | Haptics toggle did nothing | Single injected instance enforcing the preference internally; five per-screen engines removed |
| | *(found by audit)* clipboard switch and hours-per-week input also did nothing | Both made real |
| | Games cleared before install couldn't be logged | Backdating, with a date picker that avoids the UTC-vs-local off-by-one-day trap |
| | Filters too coarse, inconsistent between screens | One shared 22-facet vocabulary across genres, themes and game modes, used identically by the pile, the draw and the new statistics screen |
| | (no stats anywhere) | New STATS screen |
| | "Ghost of Tsushima has no banner" | The share flow was writing deliberately incomplete rows over complete ones; single owner for cache writes, plus a once-per-launch repair pass for already-affected devices |
| | "spiderman" didn't find "Spider-Man" | Query respelling, re-ranking and deduplication |
| | "Blasphemous appears twice and each can be added twice" | Deduplication |
| | Search results had no cover art / year / length | Added |
| | Browse rails had no way to see more | SHOW MORE |
| | *(Mikhil)* rankings couldn't be edited | Manual reorder and remove |
| | *(found while fixing)* completion reward was farmable | Claimed once per game, transactionally |
| **5 — Sept** | *(current build, ~5 days)* | **No bugs or defects reported** — feature suggestions only |

---

## 4. What not to say

- **Don't quote RevenueCat's customer or user counts** (see §1). They are anonymous installs
  plus device-farm traffic.
- **Don't quote the sandbox revenue figures, or imply the closed test earned anything.** The
  test purchases carry dollar amounts; they are licence-tester artefacts, not money. Say the
  purchases were made with test accounts if the topic comes up — a reviewer will assume that
  anyway for a closed test, and being vague about it is the only way to make it look bad.
- **Don't name a precise tester count.** Play Console doesn't expose one, and the RevenueCat
  analysis in §1 counts *installs*, not opted-in testers — close, but not the same measure, and
  not a number to defend in a reviewed form. "More than 12" is true, sufficient, and safe.
- **Don't claim every tester used every feature.** The drafted answer says coverage was achieved
  across the group and names the weak spot. That reads as someone who ran a real test.
- **Don't mention the hackathon, the deadline, or any competition.** It reframes the app as a
  submission rather than a product, and it is irrelevant to the reviewer.
- **Don't mention any influencer, brand or creator** — standing project rule.

---

## 5. After submitting

**Review time:** Google says "usually 7 days or less", sometimes longer. Submitting 2026-09-07
puts a decision around **2026-09-14**, leaving roughly two weeks before the 2026-09-30 deadline
for the production release *and its own review*. That is workable but has no second attempt in
it.

**Do not go quiet on the closed track while waiting.** Keep testers opted in until access is
granted — the requirement is that they were opted in continuously for the 14 days *preceding the
application*, and there is no upside to letting the count decay mid-review.

**Before the production release itself, in this order:**

1. **Exclude EEA/UK/CH from *production* country availability.** The app serves ads and has no
   UMP/CMP consent flow; this was the decision taken 2026-08-24 in place of building one.
   Leave the closed track's availability untouched so no current tester loses access.
2. Confirm the store listing's screenshots, Data Safety, content rating and ads declaration are
   still accurate for `versionCode 8` — the app gained a STATS screen since some of that was
   written.
3. Swap the AdMob test unit ids for the real ones and enable server-side verification. Real
   units generally won't fill until the app is actually live, which is expected, not a bug.

**Already done, contrary to what `docs/09-PENDING-INPUTS.md` said until today:** the Play
service account is configured in RevenueCat and validates clean —
`validate-app-credentials` returns `status: valid` on all three checks (subscription
validation, in-app product catalogue, subscription catalogue and base plans). That is also why
the testers' sandbox purchases carry real Play order ids and correct expiry tracking. Nothing to
do here.

**Hedge, per `docs/01-PLAY-STORE-CRITICAL-PATH.md`:** an **open testing** track produces a
public Play Store URL without waiting on production access. If the review is still pending
around **2026-09-15**, switch to open testing rather than gambling the store-dependent
categories on a single review queue.
