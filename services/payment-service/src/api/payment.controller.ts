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
    const { userId } = req.body;
    let amount = req.body.amount;
    const source = req.body.source;
    if (!userId || amount === undefined || amount === null) return res.status(400).json({ error: "userId/amount required" });
    amount = typeof amount === "string" ? Number(amount) : amount;
    if (typeof amount !== "number" || !Number.isFinite(amount) || amount < 0) return res.status(400).json({ error: "invalid amount" });

    // call gateway (charge)
    const gwResp = await postToGateway("/charge", { userId, amount, source }).catch((e) => {
      // publish payment.failed
      publish("payment.failed", { userId, amount, reason: e.message }).catch(() => {});
      throw e;
    });

    // on success, add funds to wallet (in a real app do this after webhook verification)
    await wallet.addFunds(userId, amount);

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
    const { userId } = req.body;
    let amount = req.body.amount;
    const meta = req.body.meta;
    if (!userId || amount === undefined || amount === null) return res.status(400).json({ error: "userId/amount required" });
    amount = typeof amount === "string" ? Number(amount) : amount;
    if (typeof amount !== "number" || !Number.isFinite(amount) || amount < 0) return res.status(400).json({ error: "invalid amount" });

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
  const { userId } = req.body;
  let amount = req.body.amount;
  if (!userId || amount === undefined || amount === null) return res.status(400).json({ error: "userId/amount required" });
  amount = typeof amount === "string" ? Number(amount) : amount;
  if (typeof amount !== "number" || !Number.isFinite(amount) || amount < 0) return res.status(400).json({ error: "invalid amount" });

  const r = await wallet.freezeAmount(userId, amount);
  const email = req.body.email;
  if (!r.ok) {
    // publish a failure event so notifier can alert the user (include email if present)
    const failMsg = `Your payment of ${amount} could not be reserved: ${r.reason}`;
    await publish("payment.failed", { userId, amount, reservationId: r.reservationId, email, reason: r.reason, message: failMsg }).catch(() => {});
    return res.status(400).json({ ok: false, reason: r.reason });
  }
  const reservationId = r.reservationId;
  const reserveMsg = `Your payment of ${amount} has been reserved (reservation ${reservationId}).`;
  await publish("payment.locked", { userId, amount, reservationId, email, message: reserveMsg, ts: Date.now() });
  return res.json({ ok: true, reservationId });
}

/**
 * POST /wallet/unfreeze
 */
export async function unfreezeHandler(req: Request, res: Response) {
  const { userId } = req.body;
  let amount = req.body.amount;
  if (!userId || amount === undefined || amount === null) return res.status(400).json({ error: "userId/amount required" });
  amount = typeof amount === "string" ? Number(amount) : amount;
  if (typeof amount !== "number" || !Number.isFinite(amount) || amount < 0) return res.status(400).json({ error: "invalid amount" });

  await wallet.unfreezeAmount(userId, amount);
  await publish("payment.unlocked", { userId, amount, ts: Date.now() });
  return res.json({ ok: true });
}

/**
 * POST /wallet/deduct -> finalize payment (winner)
 * body: { userId, amount, auctionId }
 */
export async function deductHandler(req: Request, res: Response) {
  const { userId, auctionId } = req.body;
  let amount = req.body.amount;
  if (!userId || amount === undefined || amount === null) return res.status(400).json({ error: "userId/amount required" });
  amount = typeof amount === "string" ? Number(amount) : amount;
  if (typeof amount !== "number" || !Number.isFinite(amount) || amount < 0) return res.status(400).json({ error: "invalid amount" });

  const r = await wallet.deductLocked(userId, amount);
  if (!r.ok) {
    // include reservationId if present in body
    const reservationId = req.body.reservationId;
    const email = req.body.email;
    const msg = `Your payment of ${amount} failed: ${r.reason}`;
    await publish("payment.failed", { userId, amount, reservationId, email, reason: r.reason, message: msg });
    return res.status(400).json({ ok: false, reason: r.reason });
  }
  const reservationId = req.body.reservationId;
  const email = req.body.email;
  const successMsg = `Your payment of ${amount} for auction ${auctionId || "#"} succeeded. Reservation ${reservationId}`;
  await publish("payment.success", { userId, amount, auctionId, reservationId, email, message: successMsg, ts: Date.now() });
  return res.json({ ok: true, balance: r.balance });
}
