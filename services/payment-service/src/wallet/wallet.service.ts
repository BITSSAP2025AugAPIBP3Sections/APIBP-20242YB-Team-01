
import { Pool } from "pg";

const pool = new Pool({
  host: process.env.PGHOST || "localhost",
  port: Number(process.env.PGPORT) || 5435,
  user: process.env.PGUSER || "postgres",
  password: process.env.PGPASSWORD || "postgres",
  database: process.env.PGDATABASE || "postgres"
});

export interface WalletState {
  userId: string;
  balance: number;
  locked: number;
}


/**
 * Ensure wallet exists in DB
 */
export async function ensureWallet(userId: string): Promise<WalletState> {
  const res = await pool.query("SELECT * FROM public.wallets WHERE user_id = $1", [userId]);
  if (res.rows.length > 0) {
    const w = res.rows[0];
    return { userId: w.user_id, balance: w.balance, locked: w.locked };
  }
  // Create wallet if not exists
  await pool.query("INSERT INTO public.wallets (user_id, balance, locked) VALUES ($1, $2, $3)", [userId, 0, 0]);
  return { userId, balance: 0, locked: 0 };
}


export async function getBalance(userId: string) {
  const w = await ensureWallet(userId);
  return { balance: w.balance, locked: w.locked };
}


/**
 * Add funds to wallet (external payment -> deposit)
 */
export async function addFunds(userId: string, amount: number) {
  const w = await ensureWallet(userId);
  const newBalance = w.balance + amount;
  await pool.query("UPDATE public.wallets SET balance = $1 WHERE user_id = $2", [newBalance, userId]);
  return { ok: true, balance: newBalance };
}


/**
 * Freeze amount for a bid
 */
export async function freezeAmount(userId: string, amount: number): Promise<{ ok: boolean; reason?: string }> {
  const w = await ensureWallet(userId);
  const available = w.balance - w.locked;
  if (available < amount) {
    return { ok: false, reason: "insufficient_funds" };
  }
  const newLocked = w.locked + amount;
  await pool.query("UPDATE public.wallets SET locked = $1 WHERE user_id = $2", [newLocked, userId]);
  return { ok: true };
}


/**
 * Unfreeze (release) amount (user outbid)
 */
export async function unfreezeAmount(userId: string, amount: number) {
  const w = await ensureWallet(userId);
  const newLocked = Math.max(0, w.locked - amount);
  await pool.query("UPDATE public.wallets SET locked = $1 WHERE user_id = $2", [newLocked, userId]);
  return { ok: true };
}


/**
 * Deduct locked amount when finalizing payment
 */
export async function deductLocked(userId: string, amount: number): Promise<{ ok: boolean; reason?: string; balance?: number }> {
  const w = await ensureWallet(userId);
  if (w.locked < amount) return { ok: false, reason: "locked_amount_insufficient" };
  let newLocked = w.locked - amount;
  let newBalance = w.balance - amount;
  if (newBalance < 0) newBalance = 0; // safety
  await pool.query("UPDATE public.wallets SET locked = $1, balance = $2 WHERE user_id = $3", [newLocked, newBalance, userId]);
  return { ok: true, balance: newBalance };
}
