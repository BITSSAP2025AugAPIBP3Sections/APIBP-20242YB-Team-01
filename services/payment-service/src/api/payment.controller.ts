import { Request, Response } from "express";
import * as wallet from "../wallet/wallet.service";
import { postToGateway } from "../gateway/client";
import { publish } from "../rabbit/publisher";

/**
 * POST /wallet/deposit
 * { userId, amount, source } -> calls external gateway to charge, on success add funds & publish deposit.added
 */
export async function depositHandler(req: Request, res: Response) {
  try {
    const { userId, amount, source } = req.body;
    if (!userId || !amount) return res.status(400).json({ error: "userId/amount required" });

    // call gateway (charge)
    const gwResp = await postToGateway("/charge", { userId, amount, source }).catch((e) => {
      // publish payment.failed
      publish("payment.failed", { userId, amount, reason: e.message }).catch(() => {});
      throw e;
    });

    // on success, add funds to wallet (in a real app do this after webhook verification)
    wallet.addFunds(userId, amount);

    // publish deposit.added
    await publish("deposit.added", { userId, amount, ts: Date.now() });

    return res.json({ ok: true, gateway: gwResp });
  } catch (err: any) {
    return res.status(502).json({ error: err.message || "gateway_error" });
  }
}

/**
 * POST /payment/create
 * create a payment intent: calls /create-payment-intent on gateway and returns its response
 */
export async function createPaymentHandler(req: Request, res: Response) {
  try {
    const { userId, amount, meta } = req.body;
    if (!userId || !amount) return res.status(400).json({ error: "userId/amount required" });

    const resp = await postToGateway("/create-payment-intent", { userId, amount, meta });
    return res.json(resp);
  } catch (err: any) {
    publish("payment.failed", { userId: req.body.userId, amount: req.body.amount, reason: err.message }).catch(() => {});
    return res.status(502).json({ error: err.message || "gateway_error" });
  }
}

/**
 * GET /wallet/:userId -> return balance and locked
 */
export async function walletHandler(req: Request, res: Response) {
  const userId = req.params.userId;
  if (!userId) return res.status(400).json({ error: "userId required" });
  const balance = await wallet.getBalance(userId);
  return res.json(balance);
}

/**
 * POST /wallet/freeze -> used by Auction Core (or Auction Core will use gRPC) 
 * body: { userId, amount }
 */
export async function freezeHandler(req: Request, res: Response) {
  const { userId, amount } = req.body;
  if (!userId || !amount) return res.status(400).json({ error: "userId/amount required" });

  const r = await wallet.freezeAmount(userId, amount);
  if (!r.ok) return res.status(400).json({ ok: false, reason: r.reason });
  await publish("payment.locked", { userId, amount, ts: Date.now() });
  return res.json({ ok: true });
}

/**
 * POST /wallet/unfreeze
 */
export async function unfreezeHandler(req: Request, res: Response) {
  const { userId, amount } = req.body;
  if (!userId || !amount) return res.status(400).json({ error: "userId/amount required" });

  wallet.unfreezeAmount(userId, amount);
  await publish("payment.unlocked", { userId, amount, ts: Date.now() });
  return res.json({ ok: true });
}

/**
 * POST /wallet/deduct -> finalize payment (winner)
 * body: { userId, amount, auctionId }
 */
export async function deductHandler(req: Request, res: Response) {
  const { userId, amount, auctionId } = req.body;
  if (!userId || !amount) return res.status(400).json({ error: "userId/amount required" });

  const r = await wallet.deductLocked(userId, amount);
  if (!r.ok) {
    await publish("payment.failed", { userId, amount, reason: r.reason });
    return res.status(400).json({ ok: false, reason: r.reason });
  }
  await publish("payment.success", { userId, amount, auctionId, ts: Date.now() });
  return res.json({ ok: true, balance: r.balance });
}
