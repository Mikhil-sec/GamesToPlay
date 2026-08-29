package com.mikhilnaika.continueapp.core.data

/**
 * The key a game's clear reward is paid against — see [CoinLedger.earnOnce].
 *
 * Keyed on the **game**, not the pile entry: an entry can be deleted and re-created (REMOVE
 * FROM PILE, then add the same game again), and if the reward followed the entry that would be
 * the loop this key exists to close. Namespaced so the claimed-reward set can hold other kinds
 * of one-time grant later without any chance of collision.
 */
fun clearRewardKey(gameId: Long): String = "clear:$gameId"
