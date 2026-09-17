package com.mikhilnaika.continueapp.core.data

/** docs/02-PRODUCT-SPEC.md §1 — the five pile states. */
enum class PileState {
    BACKLOG,   // THE PILE — owned/intended, not started. The default.
    PLAYING,   // NOW PLAYING — hard cap of 3.
    COMPLETED, // CLEARED
    DROPPED,   // RETIRED
    WISHLIST,  // WANTED — not owned yet.
}

/** docs/05-TECH-ARCHITECTURE.md — how a pile entry got added. */
enum class AddSource {
    SEARCH,
    SHARE_TARGET,
    STEAM_IMPORT,
    CLIPBOARD,
    /** Added from a friend's pile in FRIENDS. Stored as text, so this needed no migration. */
    FRIEND,
}

/** docs/02-PRODUCT-SPEC.md §5 — the coarse bucket a ranked game falls into. */
enum class RankBucket {
    LOVED,
    LIKED,
    FINE,
    NAH,
}

/** docs/02-PRODUCT-SPEC.md §3 — the TIME dial on the DRAW machine. */
enum class TimeBudget {
    THIRTY_MIN,
    TWO_HOURS,
    ALL_NIGHT,
    A_WHOLE_WEEKEND,
}

/** docs/02-PRODUCT-SPEC.md §3 / docs/08-GAME-DATA.md — the MOOD dial. */
enum class Mood {
    COZY,
    CHAOS,
    STORY,
    BRAIN,
    NOSTALGIA,
}
