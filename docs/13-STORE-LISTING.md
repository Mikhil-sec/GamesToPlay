# CONTINUE? — Store Listing & Closed Testing Answer Sheet

> **What this is:** every field Play Console will ask for before it lets you roll out a closed
> test, with the answer already written. Copy/paste it. Nothing here needs a decision except the
> three things marked **⚠ DECIDE**.
>
> Written 2026-08-15 against `versionCode 4` / `0.4.0`. Every claim in the listing copy below
> describes a feature that is **actually in that build** — nothing from the Devpost draft in
> `docs/07-SUBMISSION-KIT.md` that isn't shipped yet (Steam import, Free Play Mode, four of the
> five share cards) appears here. Play treats a listing that oversells the build as a
> misrepresentation, and testers just find it annoying.

---

## 0. The order things must happen in

Play blocks a closed-test rollout until the whole **App content** section and the **Main store
listing** are green. This is the sequence that unblocks fastest:

| # | Step | Blocked by | Time |
|---|---|---|---|
| 1 | Publish the privacy policy page (§1) | nothing | 10 min |
| 2 | Capture 4+ phone screenshots (§3) | a phone with `0.4.0` installed | 30 min |
| 3 | Fill Main store listing (§2, §3) | 1 and 2 | 15 min |
| 4 | App content: privacy policy, ads, app access, content rating, target audience, data safety, ad ID (§4–§10) | 1 | 45 min |
| 5 | Create the tester Google Group + collect 12–18 emails (§11) | nothing — **do this in parallel from the start** | ongoing |
| 6 | Upload `versionCode 4` .aab to **Closed testing**, add the group, roll out (§12) | 3, 4, 5 | 15 min |
| 7 | Every tester accepts the opt-in link **and installs** | 6 | the real bottleneck |
| 8 | 14 consecutive days with ≥12 opted in, then apply for production access | 7 | 14 days |

**Step 7 is the whole gate.** Everything above it is an evening's work; step 7 depends on other
people, so start §11 today and do the paperwork while you wait.

---

## 1. Privacy policy — publish it with GitHub Pages

The page is already written and committed: `docs/privacy.html`, plus `docs/index.html` as a
landing/support page and an empty `docs/.nojekyll` (which stops GitHub trying to run Jekyll over
the markdown docs).

**Setup — five clicks, one time:**

1. Commit and push the three new files:
   ```bash
   git add docs/index.html docs/privacy.html docs/.nojekyll
   git commit -m "Add privacy policy and landing page for Play listing"
   git push
   ```
2. GitHub → the `GamesToPlay` repo → **Settings** → **Pages** (left sidebar).
3. **Build and deployment → Source:** `Deploy from a branch`.
4. **Branch:** `main`, folder: **`/docs`** → **Save**.
5. Wait ~60 seconds, then confirm both URLs load:
   - Privacy policy → **`https://mikhil-sec.github.io/GamesToPlay/privacy.html`**
   - Landing/support → `https://mikhil-sec.github.io/GamesToPlay/`

> **Check it before pasting it into Play.** A 404 on the privacy policy URL is a rejection, and
> Play's checker follows the link. If Pages says "Your site is live" but you get a 404, give it
> another minute and hard-refresh — first publish is slow.

**What the policy says, and why it's short:** the app has no accounts, so almost all of it is
"this never leaves your device." The parts that aren't were written against the actual code:
the Worker receives search terms and shared text with no identifier attached; AdMob's
disclosures are quoted from [Google's own published table](https://developers.google.com/admob/android/privacy/play-data-disclosure);
RevenueCat's is what [their docs tell you to declare](https://www.revenuecat.com/docs/platform-resources/google-platform-resources/google-plays-data-safety).
The Data Safety form in §8 is derived from the same three sources, so the form and the policy
agree — which is exactly what Play's reviewers check.

**⚠ DECIDE — the support email.** The policy, the landing page and the listing all use
`naikamikhil@gmail.com` (your git/Cloudflare address). It becomes public on the store page. If
you'd rather use a dedicated address, it's one string in `docs/privacy.html`,
`docs/index.html`, and the listing fields below.

---

## 2. Main store listing — text

| Field | Limit | Value |
|---|---|---|
| **App name** | 30 | `CONTINUE? — Gaming Backlog` |
| **Short description** | 80 | `Your gaming backlog as an arcade cabinet. Pull the lever, finish what you start.` |
| **Full description** | 4000 | see below (2,885 chars) |

> On the name: `CONTINUE?` alone is the brand, but a brand-new app with a one-word generic
> title is invisible in search. `— Gaming Backlog` is the one concession to discoverability and
> it's still well inside Play's rules (no keyword stuffing, no "#1", no emoji).

### Full description (paste as-is)

```
Every gamer has a pile of unfinished games. Every app built to manage that pile is a spreadsheet with box art — and a spreadsheet of eighty-seven unplayed games is a guilt machine. So you stop opening it.

CONTINUE? treats your backlog the way an arcade treated a game over: not as failure, but as a second chance, a countdown, and a coin.

■ SAVE IT WITHOUT LEAVING THE VIDEO
See a game in a YouTube review, a Reddit thread, or a store page? Share it to CONTINUE?. A sheet slides up over the app you're already in, works out which game you meant, and adds it in about two seconds. Share a screenshot instead and the text is read on your device. Matching works with no signal at all, thanks to a game index that ships inside the app.

■ PULL THE LEVER
Recording games was never the hard part. Choosing is. Tell the cabinet how much time you actually have tonight, what mood you're in, and which platforms you can reach — then pull the lever. It deals three games from your own pile and tells you exactly why each one matched: fits your two hours, you've never touched it, it's been sitting there since March.

Swipe up to start it. Swipe left and it steps back for a fortnight. Swipe down and it's retired, guilt-free.

■ A PILE THAT TELLS YOU THE TRUTH
Five states, from WANTED to CLEARED, with a hard cap of three games in NOW PLAYING because focus is a feature. Filter by platform, genre or length. Sort shortest-first for the nights you want to actually finish something. A time budget bar does the arithmetic you've been avoiding: 412 hours, 87 games, and the year you'd finish at your current pace.

■ FINISHING SHOULD FEEL LIKE SOMETHING
Mark a game complete and it doesn't just tick a box — it plays a credits roll. Your hours, your stats, your key art, scrolling like the end of the game itself.

■ RANKING WITHOUT STARS
Nobody calibrates a five-star scale. CONTINUE? asks a simpler question: which of these two did you enjoy more? A handful of taps later your game has slotted into your personal all-time list. You don't get an 8/10 — you get your #4 of all time.

■ IT WORKS ON A TRAIN
Your pile, your covers, your rankings and your draws all work with no connection. The database on your phone is the source of truth, not a cache of someone's server.

■ ADS THAT ASK FIRST
No interstitials. No banners. No ad has ever appeared in this app on its own, and none ever will. Watch one only if you want a coin for an extra draw, and it will always tell you what you're getting first.

■ FREE, AND PRO
Free gives you the whole pile, unlimited saves, search, ranking, credits rolls, and a draw every day. PRO adds unlimited draws, unlimited stacks, a monthly coin drop, and removes ads entirely — as a small monthly subscription or a one-time purchase, your choice.

Game information: the data was freely provided by IGDB.com.

CONTINUE? — the games you started deserve an ending.
```

### Listing details

| Field | Value |
|---|---|
| App or game | **App** (not a game — this matters; a Game classification changes the rating questionnaire and the whole category) |
| Category | **Entertainment** (alternative: Productivity — Entertainment wins for a gaming audience) |
| Tags (up to 5) | Video games · Personal organization · Task management · Hobbies · Entertainment |
| Contact email | `naikamikhil@gmail.com` |
| Website | `https://mikhil-sec.github.io/GamesToPlay/` |
| Phone | leave blank |
| External marketing | Opt out is fine; nothing depends on it |

---

## 3. Graphics assets

**Already generated** by `tools/StoreAssets.java` (run it with the JDK 21 at
`C:\Android\jdk21\jdk-21.0.12+8` — `java tools/StoreAssets.java`, no build needed). All three
are original artwork built from the app's own launcher-icon geometry and Chakra Petch: no game
box art, no third-party logo, no influencer branding.

| File | Size | Where it goes |
|---|---|---|
| `store/icon-512.png` | 512×512, 32-bit | Play → Store listing → App icon |
| `store/feature-graphic-1024x500.png` | 1024×500 | Play → Store listing → Feature graphic |
| `store/icon-1024.png` | 1024×1024 | Devpost, press, `docs/07-SUBMISSION-KIT.md` |

**Still needed: phone screenshots — minimum 4, and this is the one thing that needs your
phone.** Take them from `versionCode 4`, not an older build; the 2026-08-15 layout fixes changed
every screen you'd want to show.

```bash
# with the phone plugged in and 0.4.0 running
export ADB=/c/Android/sdk/platform-tools/adb.exe
$ADB devices                              # confirm exactly one device
bash tools/capture_screenshots.sh         # walks you through the shot list
```

**Shot list, in listing order** — the first two are what most people ever see:

1. **DRAW mid-deal** — dials set, cards fanned out of the dispenser. The signature screen.
2. **The PILE** — a full grid of covers with the time-budget bar visible.
3. **The Credits Roll** — mid-scroll, key art behind the stats.
4. **RANK** — two games head to head.
5. **The GO PRO paywall** — the best-looking screen in the app, and it shows judges the
   monetization without them having to find it.
6. *(optional)* **The share sheet over YouTube** — the demo feature, but only if it captures
   cleanly.

Rules Play enforces: PNG or JPEG, 16:9 or 9:16, each side between 320px and 3840px, and the
long side no more than twice the short side. A raw phone screenshot satisfies all of that — do
not upscale, do not add device frames, do not add marketing text over them for the first pass.

> Separately, `docs/07-SUBMISSION-KIT.md` wants a **1179×2556, frameless** set for Devpost.
> Different requirement, later deadline — don't let it hold up the Play listing.

---

## 4. App content → Privacy policy

Paste `https://mikhil-sec.github.io/GamesToPlay/privacy.html`. Nothing else on this screen.

## 5. App content → Ads

- **Does your app contain ads?** → **Yes.**
- This puts a "Contains ads" badge on the listing. Required, and honest: the app serves
  rewarded ads from AdMob.

> **Related, and worth doing the same day:** in **AdMob → your app → Ad content rating**, set
> the maximum content rating to **Teen (T)** or lower. Your Play target audience includes
> 13–17-year-olds (§7), and letting AdMob serve mature ad content into a teen-rated app is a
> policy violation on the AdMob side even though Play never asks you about it.

## 6. App content → App access

- **Are all features available without special access?** → **Yes, all functionality is
  available without special access.**
- No login, no account, no region lock, no promo code needed. Nothing further to fill in.

## 7. App content → Content ratings (IARC questionnaire)

- **Email address for the rating certificate:** `naikamikhil@gmail.com`
- **Category:** **Utility, Productivity, Communication or Other.**
  **Not "Game."** CONTINUE? is a tool for gamers; picking Game changes the whole questionnaire
  and mis-categorises the listing.

| Question group | Answer |
|---|---|
| Violence (realistic, fantasy, blood, injury) | **No** to all |
| Sexuality / nudity | **No** |
| Language (profanity, crude humour) | **No** |
| Controlled substances (drugs, alcohol, tobacco) | **No** |
| Gambling (real money, or simulated casino games) | **No** — see the note below |
| Does the app allow users to interact with each other? | **No** (no chat, no accounts, no feed) |
| Does the app share the user's location with other users? | **No** |
| Does the app allow users to purchase digital goods? | **Yes** |
| Does the app contain ads? | **Yes** |
| Does the app share user-provided personal information with third parties? | **No** |

> **On the gambling question, because the app has a lever and coins and someone will ask.** The
> honest answer is No: nothing is wagered, there is no stake and no loss, the coin buys a
> deterministic extra draw rather than a chance at a prize, and the draw returns three games
> from the user's own library. There are no casino games, no slot payouts, no loot boxes and no
> randomised paid items. The arcade cabinet is a visual metaphor for choosing what to play, not
> a gambling mechanic.

Expected outcome: **PEGI 3 / ESRB Everyone / USK 0**, upgraded to a teen-ish band by the
"contains ads" and "digital purchases" answers depending on the board. Whatever comes back is
fine — there's no rating here that threatens the categories we're competing in.

## 8. App content → Data safety

**Does your app collect or share any required user data?** → **Yes.**
**Is all of the user data collected by your app encrypted in transit?** → **Yes.**
**Do you provide a way for users to request that their data is deleted?** → **Yes** →
deletion request URL: `https://mikhil-sec.github.io/GamesToPlay/privacy.html#deleting-your-data`
(the policy's §8 explains uninstall + the email route). There is no account to delete.

Declare exactly these five, and nothing else:

| Data type | Collected | Shared | Purposes | Required? | Linked to identity? | Why |
|---|---|---|---|---|---|---|
| **Financial info → Purchase history** | ✅ | ❌ | App functionality, Analytics | Required | **No** | RevenueCat records purchases against an anonymous app user ID. This is exactly what RevenueCat's docs tell you to declare. |
| **Device or other IDs** | ✅ | ✅ | Advertising or marketing, Analytics, Fraud prevention & security | Required | No | Google Mobile Ads SDK — advertising ID and app set ID. |
| **App activity → App interactions** | ✅ | ✅ | Advertising or marketing, Analytics, Fraud prevention & security | Required | No | Mobile Ads SDK collects launches, taps and ad views. |
| **App info & performance → Diagnostics** | ✅ | ✅ | Advertising or marketing, Analytics, Fraud prevention & security | Required | No | Mobile Ads SDK collects performance diagnostics. |
| **Location → Approximate location** | ✅ | ✅ | Advertising or marketing, Analytics, Fraud prevention & security | Required | No | The app requests **no** location permission, but Google's disclosure states the ads SDK uses the IP address to estimate general location. Declaring it is accurate; omitting it is the kind of thing that gets a listing pulled later. |

**Also declare — this one is ours, not an SDK's:**

| Data type | Collected | Shared | Purposes | Required? | Linked to identity? | Why |
|---|---|---|---|---|---|---|
| **App activity → In-app search history** | ✅ | ❌ | App functionality | Required | **No** | Search terms (and text you deliberately share into the app) go to our Cloudflare Worker to be matched against IGDB, and results are cached for up to 24h keyed by the term. No identifier is attached. Tick "ephemeral processing"? **No** — the cache outlives the request, so answer honestly. |

**Do NOT declare:**

- **Photos or videos.** Shared images are OCR'd on-device by ML Kit and never transmitted. The
  app has no photo-library permission at all. On-device-only processing is not collection.
- **Personal info** (name, email, address, phone). None is ever collected — there is no account.
- **Contacts, calendar, SMS, call logs, files, health, messages.** Not touched.
- **Precise location.** No location permission is requested.

> If you ever add RevenueCat customer attributes (email, name) or a custom app user ID, come
> back and add **Personal info** and flip purchase history to *linked to identity*. Until then
> the above is the accurate picture.

## 9. App content → Target audience and content

- **Target age groups:** **13–15, 16–17, 18 and over.** (13+ deliberately: it keeps us out of
  the Designed for Families programme and its child-directed ad rules entirely, which is the
  right call for an app that serves ads.)
- **Do not** opt into Designed for Families.
- **Does your store listing appeal to children?** → **No.** (It doesn't — dark UI, no cartoon
  mascots, copy aimed at adults with backlogs.)
- **Does your app unintentionally appeal to children?** → **No.**

## 10. The remaining App content declarations

| Section | Answer |
|---|---|
| **Advertising ID** — does your app use an advertising ID? | **Yes.** Purposes: **Advertising or marketing** + **Analytics**. (The `AD_ID` permission is merged in by the Play Services Ads SDK — you don't need to add it by hand, but Play will verify it's there.) |
| News app | No |
| COVID-19 contact tracing or status app | No |
| Data safety — independent security review | Not claimed (optional, skip) |
| Government apps | No |
| Financial features | **None of the above** — coins are an in-app cosmetic currency, not a financial product |
| Health apps | No |
| Play Games Services | Not used |

---

## 11. Testers — the actual critical path

**Target 18 emails so 12 survive dropouts.** Play counts a tester only once they have *accepted
the invite and installed on the matching Google account*; invited-but-not-installed counts for
nothing, and the count dropping below 12 puts the 14-day clock at risk.

> **What actually happened (recorded 2026-09-07): the Google Group was not used.** Mikhil added
> each tester's Gmail address individually to the closed test's email list, which made following
> up with people by name easier. The Group steps below are still sound advice for a larger test,
> but **don't describe the process as Group-based** in the production-access application or
> anywhere else — see `docs/14-PRODUCTION-ACCESS.md`.
>
> **Also learned the hard way:** the Play Store's "send feedback to developer" option allows each
> tester **one** submission, with a character limit. Testers had more to say than it could carry,
> so feedback after that first submission was collected through an **anonymous Google Form**.
> Plan that overflow channel up front next time.

### Create the Google Group (once, 5 minutes)

1. [groups.google.com](https://groups.google.com) → **Create group**.
2. Name: `CONTINUE? Testers` · Email: `continue-testers@googlegroups.com` (pick any free
   address; note the exact one, Play needs it).
3. **Who can join the group:** *Invited users only*. **Who can post:** managers only — nobody
   wants a mailing list.
4. Create, then **Members → Add members** → paste all the addresses → **turn OFF "Require
   invitation"** so they're added directly. An invite that has to be accepted is a second thing
   your testers can forget to do.
5. In Play Console: **Testing → Closed testing → Testers →** paste the group address.

### The brief to send them (copy/paste)

> **Subject: 2 minutes to help me ship my app (and then just… leave it installed)**
>
> I've built an Android app called CONTINUE? — a backlog manager for gamers that works like an
> arcade cabinet. To publish it on the Play Store, Google requires 12 people to test it for 14
> days straight. That's the only reason I'm asking.
>
> **What I need:**
> 1. Reply with the Gmail address you use on your Android phone (it must be a Gmail/Google
>    account, and it must be the one signed in on the phone).
> 2. I'll send you a link. Tap it, tap "Become a tester", then install from Play.
> 3. **Leave it installed for 14 days.** That's it. You don't have to open it again.
>
> **Please don't uninstall it before I tell you we're done** — if the count drops below 12 the
> clock resets and I miss my deadline. If you want to actually use it, even better: pull the
> lever and tell me what breaks.
>
> Takes two minutes. Thank you.

### While the clock runs

- Check **Play Console → Closed testing → the opted-in tester count daily.** It is the only
  number that matters for 14 days.
- Anyone who says "I clicked the link" but doesn't show up in the count almost certainly
  installed under a different Google account than the one you invited. That's the usual failure.
- Day 14 → **apply for production access** (a three-section application). Google says "usually
  7 days or less" to review. Deadline is 2026-09-30, so this must start now.

---

## 12. The release itself

- **Track:** Closed testing (create a track, or use the default one). Not Internal testing —
  internal testing does **not** count towards the 14-day requirement.
- **Bundle:** `app/build/outputs/bundle/release/app-release.aab`, `versionCode 4` / `0.4.0`,
  already signed. `versionCode 1` is on Internal testing; 2 and 3 were never uploaded.
- **Countries:** select all, or at minimum every country your testers actually live in. A
  tester in an unselected country cannot install and will silently not count.
- **Release name:** `0.4.0 (4)`
- **Release notes** (≤500 chars per language, en-US):

```
First closed test build.

• DRAW: set your time, mood and platforms, pull the lever, and get three games from your own pile — with the reason each one matched.
• Share any game into your pile from another app, even with no signal.
• Credits roll when you clear a game, head-to-head ranking instead of stars.
• Everything works offline.

Thank you for testing. Bugs and blunt feedback: naikamikhil@gmail.com
```

---

## 13. Known gaps — worth knowing before you tick boxes

1. **No consent management platform (CMP) for EEA/UK users.** Google's EU user consent policy
   requires a certified CMP (the `UserMessagingPlatform` SDK) before serving ads to users in
   the EEA, UK or Switzerland. The app doesn't have one — nothing in the codebase references
   `UserMessagingPlatform`. Consequence: ad requests from those regions may be limited or
   unfilled. **It does not block the closed test or the listing**, and the privacy policy has
   been written not to claim a consent flow exists. If your testers or your launch market are
   in the EEA/UK, this is the next ads-related thing to build.
2. **Real AdMob unit IDs won't fill until the app is live.** Expected, not a bug — see
   `docs/09-PENDING-INPUTS.md`.
3. **The Play service-account JSON still isn't uploaded to RevenueCat.** Without it RevenueCat
   cannot verify real purchases, so a tester's purchase won't grant an entitlement. Play Console
   → Setup → API access → service account with *Financial data / Manage orders* + *View app
   information* → download JSON → upload in the RevenueCat Android app config. Do it before any
   tester tries to buy Pro.
4. **Data safety must be revisited if the app changes.** Adding Steam import (a third-party
   account), analytics, or crash reporting all change §8.
