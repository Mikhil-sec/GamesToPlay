package com.mikhilnaika.continueapp.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device balance of the COIN virtual currency.
 *
 * **Why the balance is local rather than server-authoritative.** CLAUDE.md's constraint #5 is
 * that the app works fully offline, and coins gate DRAW — the app's central action. A balance
 * that lives only in RevenueCat would make the core loop fail on a train. RevenueCat's virtual
 * currency also can't be credited from a client at all: granting goes through the REST API
 * with the *secret* key, which must never ship in a public repo's APK.
 *
 * So: coins **earned** in-app (rewarded ads, clearing a game, streaks) are authoritative here,
 * and coins **purchased** are granted by RevenueCat server-side on the purchase and folded in
 * via [creditPurchased].
 *
 * **This is deliberately not fraud-proof, and that's the right call for now.** A determined
 * user could clear app data to reset. The alternative — AdMob server-side verification (SSV)
 * calling the Worker, which then adjusts the balance through RevenueCat — needs real AdMob ad
 * units, which need a production Play listing (docs/09-PENDING-INPUTS.md). Everything here is
 * shaped so that swap is a change of *source*, not of call sites: features only ever talk to
 * [com.mikhilnaika.continueapp.core.billing.BillingRepository].
 */
@Singleton
class CoinLedger @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val BALANCE = intPreferencesKey("coin_balance")
        /** Tracked separately so a future server reconciliation can tell earned from bought. */
        val LIFETIME_EARNED = intPreferencesKey("coin_lifetime_earned")
        val LIFETIME_PURCHASED = intPreferencesKey("coin_lifetime_purchased")

        /**
         * Reward keys already paid out — see [earnOnce].
         *
         * A set of opaque strings rather than a per-feature flag so that any future
         * once-per-thing reward (a trophy, a streak milestone) reuses the same mechanism
         * instead of inventing another one that has to be remembered about.
         */
        val CLAIMED_REWARDS = stringSetPreferencesKey("claimed_rewards")
    }

    /** Enough for one DRAW on first launch, so the economy is discoverable before it bites. */
    private val startingBalance = 3

    val balance: Flow<Int> = dataStore.data.map { it[Keys.BALANCE] ?: startingBalance }

    suspend fun current(): Int = balance.first()

    /** Credits coins from an in-app reward (ad watch, game cleared, streak). */
    suspend fun earn(amount: Int): Int = adjust(amount, Keys.LIFETIME_EARNED)

    /**
     * Credits [amount] the **first** time [rewardKey] is claimed, and never again.
     *
     * Closing the one economy hole a closed tester found: clearing a game paid coins, and
     * nothing stopped the same game being moved back to THE PILE and cleared again, so the
     * "+5 COINS" of the Credits Roll was an unbounded faucet for anyone who noticed. The reward
     * is a property of the *game*, not of the transition, so the key is the game — re-clearing
     * a game you genuinely replayed is still celebrated, it just isn't paid for twice.
     *
     * The check and the write share one `edit` transaction, so two taps racing each other
     * (a double-tap on CLEARED, or the Credits Roll being recomposed) can't both pass.
     *
     * @return the new balance, or `null` if [rewardKey] had already been claimed.
     */
    suspend fun earnOnce(rewardKey: String, amount: Int): Int? {
        var updated: Int? = null
        dataStore.edit { prefs ->
            val claimed = prefs[Keys.CLAIMED_REWARDS].orEmpty()
            if (rewardKey in claimed) return@edit
            prefs[Keys.CLAIMED_REWARDS] = claimed + rewardKey
            val balance = (prefs[Keys.BALANCE] ?: startingBalance) + amount
            prefs[Keys.BALANCE] = balance
            prefs[Keys.LIFETIME_EARNED] = (prefs[Keys.LIFETIME_EARNED] ?: 0) + amount
            updated = balance
        }
        return updated
    }

    /**
     * Burns [rewardKey] without paying anything out.
     *
     * What a backdated clear does. Logging games finished before the app existed is
     * record-keeping, not an achievement, and paying for it would hand anyone who wanted coins
     * a faster faucet than the one [earnOnce] just closed — you can backdate a hundred games in
     * a minute. Marking the key claimed also means the same game can't then be un-cleared and
     * re-cleared for the coins it deliberately didn't earn.
     */
    suspend fun markClaimed(rewardKey: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CLAIMED_REWARDS] = prefs[Keys.CLAIMED_REWARDS].orEmpty() + rewardKey
        }
    }

    /** Whether [rewardKey] has already been paid out (or deliberately burned). */
    suspend fun isClaimed(rewardKey: String): Boolean =
        dataStore.data.map { it[Keys.CLAIMED_REWARDS].orEmpty() }.first().contains(rewardKey)

    /** Credits coins that RevenueCat granted for a real purchase. */
    suspend fun creditPurchased(amount: Int): Int = adjust(amount, Keys.LIFETIME_PURCHASED)

    /**
     * Debits [amount] if affordable.
     *
     * @return the new balance, or `null` if the balance was too low — callers must treat null
     *   as "declined" and not deliver the goods. The read and write happen inside one
     *   `edit` transaction so two rapid taps can't both pass the affordability check.
     */
    suspend fun spend(amount: Int): Int? {
        var result: Int? = null
        dataStore.edit { prefs ->
            val balance = prefs[Keys.BALANCE] ?: startingBalance
            if (balance >= amount) {
                val updated = balance - amount
                prefs[Keys.BALANCE] = updated
                result = updated
            }
        }
        return result
    }

    private suspend fun adjust(amount: Int, lifetimeKey: Preferences.Key<Int>): Int {
        var updated = 0
        dataStore.edit { prefs ->
            updated = (prefs[Keys.BALANCE] ?: startingBalance) + amount
            prefs[Keys.BALANCE] = updated
            prefs[lifetimeKey] = (prefs[lifetimeKey] ?: 0) + amount
        }
        return updated
    }
}
