package com.mikhilnaika.continueapp.navigation

object NavDestinations {
    const val ONBOARDING = "onboarding"
    const val PILE = "pile"
    const val DISCOVER = "discover"
    const val DRAW = "draw"
    const val PROFILE = "profile"
    const val CREDITS_ROLL = "credits/{entryId}"
    const val RANK = "rank/{gameId}"
    const val STACKS = "stacks"
    const val STATS = "stats"
    const val SHARE_PILE = "share/pile"
    const val PAYWALL = "paywall"
    const val FRIENDS = "friends"
    const val FRIEND_PILE = "friends/{friendId}"

    fun creditsRoll(entryId: Long) = "credits/$entryId"
    fun rank(gameId: Long) = "rank/$gameId"
    fun friendPile(friendId: Long) = "friends/$friendId"
}
