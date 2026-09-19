package com.mikhilnaika.continueapp.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rule that turns RevenueCat's COIN balance into coins in the cabinet. RevenueCat's balance
 * only ever grows from the app's point of view (grants on purchase, renewal and verified ads;
 * spending happens locally), so the ledger credits growth and nothing else.
 */
class StoreGrantTest {

    @Test
    fun `first sync credits everything RevenueCat has granted`() {
        // A fresh install of someone who already bought PRO: the 50-coin grant arrives in full.
        assertEquals(50, storeGrantSince(seen = 0, storeBalance = 50))
    }

    @Test
    fun `a renewal credits only the new grant`() {
        assertEquals(50, storeGrantSince(seen = 50, storeBalance = 100))
    }

    @Test
    fun `nothing new credits nothing`() {
        // Every launch and every foreground asks; almost all of them must be no-ops.
        assertEquals(0, storeGrantSince(seen = 100, storeBalance = 100))
    }

    @Test
    fun `a verified ad's single coin is credited`() {
        assertEquals(1, storeGrantSince(seen = 100, storeBalance = 101))
    }

    @Test
    fun `a balance that goes down never takes coins back`() {
        // A refund clawing back a grant, or a manual adjustment in the dashboard. Coins already
        // in someone's cabinet stay there; the baseline just follows RevenueCat down.
        assertEquals(0, storeGrantSince(seen = 100, storeBalance = 50))
    }

    @Test
    fun `after a drop the next grant is still credited in full`() {
        // The caller stores the lower balance as the new baseline, so the next +50 counts.
        assertEquals(50, storeGrantSince(seen = 50, storeBalance = 100))
    }
}
