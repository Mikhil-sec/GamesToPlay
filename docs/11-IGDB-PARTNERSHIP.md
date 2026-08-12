# IGDB Commercial Partnership

> Status as of **2026-08-12**: Christian Frithiof (Business Leader, IGDB/Twitch) has replied to
> the initial outreach and asked seven questions to start the agreement. **Answer now, not
> later** — see the reasoning below. Draft answers are ready; only the two items marked
> ⬜ need Mikhil's input before sending.

---

## Why answer now rather than "when the app is better"

1. **We already meet their definition of commercial.** Their reply is explicit: *"whether
   through subscriptions, in app purchases, advertising … or other forms of monetization."*
   CONTINUE? has all three. The partnership isn't a nice-to-have we grow into — we're in the
   category that needs it *today*, while shipping IGDB data to Play testers.
2. **Shipaton requires authorization to use third-party APIs.** A signed agreement (or, at
   minimum, this thread) is the compliance evidence. Turning up to judging with an unanswered
   partnership request from the data provider is a category of risk worth zero.
3. **Legal turnaround is not instant** and the deadline is Sept 30, 2026. Their next step is
   *"I'll ask our legal team to prepare the agreement"* — that clock should start now.
4. **It's free.** *"We offer commercial partnerships and the API will still be free of charge."*
   There is no cost-side reason to wait.
5. **Nothing in their questions requires a finished app.** Q3 explicitly accepts screenshots
   in place of a public link.

## What "data dumps" actually buys us — this is the big one

[Data dumps](https://api-docs.igdb.com/#data-dumps) are **bulk downloads of entire IGDB tables**
(games, covers, genres, time-to-beat, …), enabled per-ClientID for partners only. For this app
specifically that is a structural upgrade, not a convenience:

- **The 4 req/sec ceiling stops mattering.** It's currently the single biggest constraint on
  the Worker's design — it's why `shortAndSweet` batches, why playtimes are fetched in one
  request instead of per-game, and why `/resolve` has a 6-search budget. With a local dump,
  matching becomes a local index lookup.
- **Share-target matching gets dramatically better.** Today we're limited to guessing a few
  substrings and asking IGDB about each (see `worker/src/resolve/candidates.ts`). Against a
  full local name index we could match *every* n-gram at once and pick the best — the exact
  problem that made "Pocketpair Palworld" hard.
- **It makes offline-first honest.** CLAUDE.md constraint #5 says the pile must fully work
  offline. Today that's backed by a hand-curated 426-game seed set with genre-median playtime
  estimates. A dump replaces that with the real catalogue and real time-to-beat data.
- **We keep the data if the partnership ever ends** — *"you will be able to keep all the data
  you retrieve from us, in the case of partnership termination."* That removes the RAWG-style
  single-provider risk that already bit this project once (docs/08-GAME-DATA.md).

## Attribution obligation — status

They ask for *"user facing attribution to IGDB.com"*, with the examples being a "View on
IGDB.com" link. Current state as of 2026-08-12:

- ✅ YOU tab (`feature/profile/ProfileScreen.kt`) — "The data was freely provided by IGDB.com"
- ✅ DISCOVER (`feature/discover/DiscoverScreen.kt`) — added 2026-08-12; this was a real gap,
  since DISCOVER is the screen actually rendering their data and it had none
- ⬜ Consider a "View on IGDB.com" deep link on a game detail view when one exists — that's
  closest to the examples they gave, and IGDB's `url` field is already available on the API.

## Draft reply — answers to their seven questions

> Fill the ⬜ items, then send. Keep their reply: it's compliance evidence.

**1. Product/project name?**
CONTINUE? — an Android app.

**2. Please describe how you intend to use the data and API?**
CONTINUE? is a gaming backlog manager (not a game) that helps players finish the games they've
already started. IGDB is the sole source of game metadata: names, cover art, release dates,
genres, themes, platforms, and `game_time_to_beat` values.

All IGDB access goes through a single Cloudflare Worker we control — the app never calls the
API directly. The Worker holds the Twitch credentials, caches every response in Workers KV
(6–24h for lists, 7d for individual games), and is the only thing that ever talks to IGDB, so
the 4 req/sec limit is respected regardless of how many users the app has.

Data is used to (a) search and add games to a personal backlog, (b) power a recommendation
feature that suggests what to play next based on available time, using the
hastily/normally/completely time-to-beat values, and (c) identify a game from a shared social
media link (e.g. a YouTube video about a game) so it can be added in one tap.

The app is monetized via subscriptions, one-time purchases, and rewarded ads, which is why
we're seeking a commercial partnership.

**3. Is the project available online for us to take a look at? If not, can you share
screenshots?**
It's in closed testing on Google Play (not yet public). The source is public at
⬜ *[GitHub repo URL]*, and screenshots are attached. Happy to add you to the tester list if
that's useful.

**4. Will you be entering the partnership as an individual or as a company?**
As an individual.

**5. Name and email to the person signing the agreement.**
Mikhil Naika — ⬜ *[which email? Consider the academic address, since it's also the Devpost
account for the student category]*

**6. Your ClientID when using the IGDB API.**
`bvzzw402pd90vedamcjuibfde6i73y`

**7. An email we can use to contact you and your team regarding future technical and product
updates and announcements.**
⬜ *[same as #5, or a separate address]*

---

**One thing worth adding to the reply:** ask whether attribution on DISCOVER + the YOU tab is
sufficient, or whether they want it on every screen rendering their data. Getting that
confirmed in writing now is cheaper than rediscovering it during Play review or judging.
