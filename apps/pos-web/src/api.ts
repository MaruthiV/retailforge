async function j<T>(res: Response): Promise<T> {
  if (!res.ok) throw new Error((await res.text()) || res.statusText);
  return res.json();
}

export type CartView = {
  id: string;
  status: string;
  items: { id: number; productId: string; name: string; unitPrice: number; quantity: number }[];
};

export type TransactionView = {
  id: string;
  status: string;
  subtotal: number;
  discount: number;
  tax: number;
  total: number;
  authCode: string | null;
};

export type LoyaltyProfile = { id: string; name: string; pointsBalance: number; tier: string };

export const createCart = (storeId: string, customerId: string) =>
  fetch("/api/carts", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ storeId, customerId }),
  }).then((r) => j<CartView>(r));

export const addItem = (cartId: string, productId: string, name: string, unitPrice: number, quantity: number) =>
  fetch(`/api/carts/${cartId}/items`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ productId, name, unitPrice, quantity }),
  }).then((r) => j<CartView>(r));

export const checkout = (cartId: string, card: string, coupon: string | null) =>
  fetch(`/api/carts/${cartId}/checkout`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ card, coupon }),
  }).then((r) => j<TransactionView>(r));

export const getLoyalty = (customerId: string) =>
  fetch(`/api/loyalty/customers/${customerId}`).then((r) => j<LoyaltyProfile>(r));
