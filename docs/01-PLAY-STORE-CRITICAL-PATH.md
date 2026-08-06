# PRIORITY ZERO — Google Play Critical Path

> This document outranks every feature in the product spec. A perfect app with no store
> listing loses three of our four categories. Work these items **before** and **in parallel
> with** all development.

## The rule that decides our timeline

Personal Google Play developer accounts created after **Nov 13, 2023** must:

1. Run a **closed test** with **at least 12 testers opted in** (accepted the invite *and*
   installed under the matching Google account),
2. keep them opted in for **14 consecutive days**,
3. then **apply** for production access (a 3-section application),
4. wait for review — Google says "usually 7 days or less",
5. *then* publish to production, which has its own review.

Invited-but-not-installed does **not** count. If your opted-in count drops below 12, the
14-day clock is at risk. Organization accounts are exempt — personal accounts are not.

**Consequence: the app must be uploadable to a closed track within days of the account going
live, long before it is finished.** That's fine. The closed test measures nothing about
quality; it just has to be a real, installable, non-crashing app.

---

## Week 1 checklist — do these before writing feature code

### A. Google Play account (blocking everything)
- [ ] Complete registration and pay the $25 fee — **target: within 3 days**
- [ ] Submit identity verification documents immediately; this is the slow part
- [ ] Set up the developer profile, contact email, and (if personal) the required
      public-facing details
- [ ] **Consider an organization account if you can get a D-U-N-S number quickly** — it is
      exempt from the 12-tester rule entirely. Probably too slow, but worth 10 minutes of
      checking, because it would remove our single largest risk outright.

### B. Recruit testers — start today, in parallel with A
- [ ] Collect **18 Gmail addresses** (target 18 so we survive 6 dropouts)
- [ ] Sources: university friends and coursemates, your course Discord/WhatsApp groups,
      family, r/androiddev's tester-exchange threads, Shipaton community Discord (other
      entrants have the same problem — swap testers with them)
- [ ] Put them in a Google Group — Play Console accepts a group address, which makes adding
      and auditing testers far easier than a raw email list
- [ ] Brief them explicitly, in writing:
      > "Accept the invite, install the app, and **leave it installed for 14 days**.
      > You don't need to use it. Uninstalling breaks the test and costs me the deadline."
- [ ] Track who has actually installed. Play Console shows the opted-in count — **check it
      daily** for the full 14 days.

### C. AdMob (also has approval latency)
- [ ] Create the AdMob account and link it to the Play app
- [ ] Create **real** rewarded ad units (test IDs cannot be used with server-side
      verification, which our whole ad-reward design depends on)
- [ ] Connect AdMob to RevenueCat so units sync to the RevenueCat dashboard
- [ ] Set each rewarded unit's SSV callback to
      `https://api.revenuecat.com/v1/incoming-webhooks/admob-ssv-rewarded`

### D. RevenueCat
- [ ] Create the `CONTINUE` project (separate from the existing `ScrapSiege` project)
- [ ] Create the Play Store app entry, entitlements, offerings, products, virtual currency
- [ ] Enable **Charts v3** (required for ad monetization features)
- [ ] Grab the **public** Android SDK key for the app; keep secret keys server-side only
- Details in `docs/04-MONETIZATION.md`

### E. First upload (the moment the account is live)
Ship whatever exists. Minimum bar for a closed-track build:
- [ ] Launches without crashing
- [ ] Has the real app icon and name
- [ ] Has at least one real screen (the PILE list is enough)
- [ ] Signed release build with a **keystore you have backed up** (losing it means losing
      the ability to update the app — back it up to two places, today)
- [ ] Play App Signing enabled
- [ ] `targetSdk` meets the current Play requirement (36)

---

## Store listing assets (needed at first upload — don't defer)

Play blocks submission without these, so produce them early rather than at 2am on Sept 29.

- [ ] App icon **512×512** (store) and the adaptive launcher icon
- [ ] Feature graphic **1024×500**
- [ ] Phone screenshots — at least 4, and produce the Devpost-required
      **1179×2556, no device frame** set at the same time
- [ ] Short description (80 chars)
- [ ] Full description (4000 chars) — draft in `docs/07-SUBMISSION-KIT.md`
- [ ] Privacy policy URL — **required** because we serve ads and collect analytics.
      Host it as a static page on the same Cloudflare account as the Worker.
- [ ] **Data Safety form** — declare ad-serving, device identifiers, and any analytics.
      Getting this wrong is a common rejection cause; fill it at first upload.
- [ ] Content rating questionnaire — declare that the app **contains ads**
- [ ] "Contains ads" checkbox on the store listing
- [ ] Target audience: 13+ (avoids the strict child-directed ad rules entirely)

---

## Compliance traps specific to this app

| Trap | Rule | What we do |
|---|---|---|
| **Influencer likeness** | Shipaton disqualifies any use of an influencer's name, likeness, logo, or brand | Nothing in the app, listing, icon, video, or repo references Mr Lewis Blogs or any creator. The Devpost write-up may *name the category* — that's it. |
| **Game box art / trademarks** | Third-party game art is shown under IGDB's API terms | Display IGDB attribution with a live hyperlink. Never claim ownership. Keep box art out of our *icon*, *feature graphic*, and *promo* materials — those must be original art. |
| **Demo video music** | No copyrighted music without permission | Use a royalty-free track with a written license, or original audio. Keep the license receipt. |
| **IGDB terms** | Free tier expects monetizing consumer projects to make contact | Email `partner@igdb.com` in week 1 and keep the reply as compliance evidence. Show the required attribution. |
| **Ads + children** | Play family policy | Target audience 13+; do not opt into the Designed for Families programme. |

---

## Contingency ladder

If Play publication is clearly not going to make it:

1. **By Sept 5** — if the closed test hasn't started, assume the store categories are lost.
   Pivot all remaining effort into the **Next Gen** submission: repo quality, README,
   architecture docs, and a longer, more polished demo video.
2. **By Sept 15** — if production access is granted but the release is stuck in review,
   submit to Devpost anyway with the closed/open-testing URL and note the pending status.
   An **open testing** track produces a public Play URL and is worth switching to as a hedge
   the moment production access looks doubtful.
3. **Always** — the Next Gen entry costs us nothing extra, because the repo is public and
   documented from day one. Never let it lapse.
