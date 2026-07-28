import { useState } from "react";
import * as api from "./api";
import type { CartView, TransactionView, LoyaltyProfile } from "./api";

const CATALOG = [
  { productId: "prod-1", name: "Widget", unitPrice: 100.0 },
  { productId: "prod-coffee", name: "Coffee Bag", unitPrice: 12.5 },
  { productId: "prod-mug", name: "Ceramic Mug", unitPrice: 8.0 },
];

export default function App() {
  const [customerId, setCustomerId] = useState("cust-001");
  const [loyalty, setLoyalty] = useState<LoyaltyProfile | null>(null);
  const [cart, setCart] = useState<CartView | null>(null);
  const [coupon, setCoupon] = useState("SAVE10");
  const [card, setCard] = useState("4111111111111111");
  const [receipt, setReceipt] = useState<TransactionView | null>(null);
  const [error, setError] = useState("");

  const guard = (fn: () => Promise<void>) => async () => {
    setError("");
    try {
      await fn();
    } catch (e) {
      setError(String((e as Error).message));
    }
  };

  const ensureCart = async (): Promise<CartView> => {
    if (cart && cart.status === "OPEN") return cart;
    const c = await api.createCart("store-001", customerId);
    setCart(c);
    setReceipt(null);
    return c;
  };

  const add = (p: (typeof CATALOG)[number]) =>
    guard(async () => {
      const c = await ensureCart();
      const updated = await api.addItem(c.id, p.productId, p.name, p.unitPrice, 1);
      setCart(updated);
    })();

  const lookup = guard(async () => {
    setLoyalty(await api.getLoyalty(customerId));
  });

  const pay = guard(async () => {
    if (!cart) throw new Error("cart is empty");
    const txn = await api.checkout(cart.id, card, coupon || null);
    setReceipt(txn);
    setCart(null);
    if (loyalty) setLoyalty(await api.getLoyalty(customerId));
  });

  const total = cart ? cart.items.reduce((s, i) => s + i.unitPrice * i.quantity, 0) : 0;

  return (
    <div className="pos">
      <header>
        <h1>RetailForge POS</h1>
        <span className="store">store-001</span>
      </header>

      {error && <div className="error">{error}</div>}

      <div className="cols">
        <section className="catalog">
          <h2>Products</h2>
          {CATALOG.map((p) => (
            <button key={p.productId} className="product" onClick={() => add(p)}>
              <span>{p.name}</span>
              <span className="price">${p.unitPrice.toFixed(2)}</span>
            </button>
          ))}

          <h2>Loyalty</h2>
          <div className="loyalty">
            <input value={customerId} onChange={(e) => setCustomerId(e.target.value)} />
            <button onClick={lookup}>Look up</button>
          </div>
          {loyalty && (
            <div className="profile">
              <b>{loyalty.name}</b> · {loyalty.tier} · {loyalty.pointsBalance} pts
            </div>
          )}
        </section>

        <section className="cart">
          <h2>Cart</h2>
          {cart && cart.items.length > 0 ? (
            <ul>
              {cart.items.map((i) => (
                <li key={i.id}>
                  <span>{i.name}</span>
                  <span>${(i.unitPrice * i.quantity).toFixed(2)}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="muted">empty — tap a product</p>
          )}
          <div className="subtotal">
            <span>subtotal</span>
            <span>${total.toFixed(2)}</span>
          </div>

          <label>Coupon</label>
          <input value={coupon} onChange={(e) => setCoupon(e.target.value)} placeholder="SAVE10" />
          <label>Card</label>
          <input value={card} onChange={(e) => setCard(e.target.value)} />
          <div className="hint">…0002 declines · …0069 times out</div>
          <button className="pay" disabled={!cart} onClick={pay}>
            Charge
          </button>
        </section>

        <section className="receipt">
          <h2>Receipt</h2>
          {receipt ? (
            <div className="rcpt">
              <div className="rid">txn {receipt.id.slice(0, 8)}</div>
              <div className={"badge " + receipt.status.toLowerCase()}>{receipt.status}</div>
              <Row k="subtotal" v={receipt.subtotal} />
              <Row k="discount" v={-receipt.discount} />
              <Row k="tax" v={receipt.tax} />
              <Row k="total" v={receipt.total} bold />
              {receipt.authCode && <div className="auth">auth {receipt.authCode}</div>}
            </div>
          ) : (
            <p className="muted">no transaction yet</p>
          )}
        </section>
      </div>
    </div>
  );
}

function Row({ k, v, bold }: { k: string; v: number; bold?: boolean }) {
  return (
    <div className={"row" + (bold ? " bold" : "")}>
      <span>{k}</span>
      <span>${v.toFixed(2)}</span>
    </div>
  );
}
