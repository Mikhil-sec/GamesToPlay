import type { Env } from "../types.ts";

interface SpendRequest {
  appUserId: string;
  amount: number;
  sku: string;
}

/**
 * Server-side price table — never trust a price sent by the client
 * (docs/05-TECH-ARCHITECTURE.md §`/coins/spend`). Extend as real skus are added.
 */
const PRICE_TABLE: Record<string, number> = {
  extra_draw: 1,
};

/**
 * Not live yet — needs a RevenueCat *secret* key (docs/09-PENDING-INPUTS.md, blocked on the
 * Play Console products existing first). Fails closed with a clear 501 rather than faking a
 * balance, matching RealBillingRepository.spendCoins on the app side.
 */
export async function spendCoins(
  env: Env,
  request: SpendRequest,
): Promise<{ status: number; body: unknown }> {
  const expectedPrice = PRICE_TABLE[request.sku];
  if (expectedPrice === undefined) {
    return { status: 400, body: { ok: false, error: "UNKNOWN_SKU" } };
  }
  if (request.amount !== expectedPrice) {
    return { status: 400, body: { ok: false, error: "PRICE_MISMATCH" } };
  }
  if (!env.REVENUECAT_SECRET_KEY) {
    return {
      status: 501,
      body: { ok: false, error: "REVENUECAT_SECRET_KEY not configured (docs/09-PENDING-INPUTS.md)" },
    };
  }

  // Real call against RevenueCat's v2 virtual-currency transactions API goes here once the
  // secret key is set — confirm the exact endpoint/payload against current RevenueCat docs
  // when implementing, per docs/05-TECH-ARCHITECTURE.md's explicit caveat that this API is
  // newer than the rest of the SDK surface.
  return { status: 501, body: { ok: false, error: "NOT_YET_IMPLEMENTED" } };
}
