/**
 * Capability tokens for orders placed without an account.
 *
 * A guest has no session, so nothing about a later request proves the order is
 * theirs. Checkout returns a one-time token for exactly that, and it is kept
 * here so the confirmation page - and a revisit from the same browser - can
 * present it. Scoped per order id rather than a single "last order" value, so
 * placing a second order does not lock the shopper out of the first.
 */
const STORAGE_KEY = 'guest_order_tokens';

type TokenMap = Record<string, string>;

function read(): TokenMap {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as TokenMap) : {};
  } catch {
    // Corrupt or unavailable storage must not break checkout; the shopper just
    // loses the ability to reopen the order from this browser.
    return {};
  }
}

export function rememberGuestOrderToken(orderId: string, token: string): void {
  if (typeof localStorage === 'undefined' || !orderId || !token) return;
  try {
    const tokens = read();
    tokens[orderId] = token;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
  } catch {
    /* storage full or blocked - non-fatal */
  }
}

export function getGuestOrderToken(orderId: string): string | null {
  if (typeof localStorage === 'undefined' || !orderId) return null;
  return read()[orderId] ?? null;
}
