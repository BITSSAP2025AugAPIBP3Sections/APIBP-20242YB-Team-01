
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
 * Ensure the wallets table exists in the database. Safe to call multiple times.
 */
export async function initDb() {
  const sql = `CREATE TABLE IF NOT EXISTS public.wallets (
    user_id VARCHAR PRIMARY KEY,
    balance NUMERIC NOT NULL DEFAULT 0,
    locked NUMERIC NOT NULL DEFAULT 0
  )`;
  await pool.query(sql);
  console.log("✅ Ensured wallets table exists");
}


/**
 * Ensure wallet exists in DB
 */
export async function ensureWallet(userId: string): Promise<WalletState> {
  const res = await pool.query("SELECT * FROM public.wallets WHERE user_id = $1", [userId]);
  if (res.rows.length > 0) {
    const w = res.rows[0];
    // Postgres NUMERIC is returned as string by node-postgres — coerce to number
    const balance = typeof w.balance === "string" ? Number(w.balance) : w.balance;
    const locked = typeof w.locked === "string" ? Number(w.locked) : w.locked;
    return { userId: w.user_id, balance: Number.isFinite(balance) ? balance : 0, locked: Number.isFinite(locked) ? locked : 0 };
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
  const amt = typeof amount === "string" ? Number(amount) : amount;
  const newBalance = w.balance + (typeof amt === "number" && Number.isFinite(amt) ? amt : 0);
  await pool.query("UPDATE public.wallets SET balance = $1 WHERE user_id = $2", [newBalance, userId]);
  return { ok: true, balance: newBalance };
}


/**
 * Freeze amount for a bid
 */
export async function freezeAmount(userId: string, amount: number): Promise<{ ok: boolean; reservationId?: string; reason?: string }> {
  const w = await ensureWallet(userId);
  const amt = typeof amount === "string" ? Number(amount) : amount;
  const available = w.balance - w.locked;
  if (available < (typeof amt === "number" && Number.isFinite(amt) ? amt : 0)) {
    return { ok: false, reason: "insufficient_funds" };
  }
  const newLocked = w.locked + (typeof amt === "number" && Number.isFinite(amt) ? amt : 0);
  await pool.query("UPDATE public.wallets SET locked = $1 WHERE user_id = $2", [newLocked, userId]);
  // generate a reservation id to correlate events
  const reservationId = `${userId}-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
  return { ok: true, reservationId };
}


/**
 * Unfreeze (release) amount (user outbid)
 */
export async function unfreezeAmount(userId: string, amount: number) {
  const w = await ensureWallet(userId);
  const amt = typeof amount === "string" ? Number(amount) : amount;
  const newLocked = Math.max(0, w.locked - (typeof amt === "number" && Number.isFinite(amt) ? amt : 0));
  await pool.query("UPDATE public.wallets SET locked = $1 WHERE user_id = $2", [newLocked, userId]);
  return { ok: true };
}


/**
 * Deduct locked amount when finalizing payment
 */
export async function deductLocked(userId: string, amount: number): Promise<{ ok: boolean; reason?: string; balance?: number }> {
  const w = await ensureWallet(userId);
  const amt = typeof amount === "string" ? Number(amount) : amount;
  if (w.locked < (typeof amt === "number" && Number.isFinite(amt) ? amt : 0)) return { ok: false, reason: "locked_amount_insufficient" };
  let newLocked = w.locked - (typeof amt === "number" && Number.isFinite(amt) ? amt : 0);
  let newBalance = w.balance - (typeof amt === "number" && Number.isFinite(amt) ? amt : 0);
  if (newBalance < 0) newBalance = 0; // safety
  await pool.query("UPDATE public.wallets SET locked = $1, balance = $2 WHERE user_id = $3", [newLocked, newBalance, userId]);
  return { ok: true, balance: newBalance };
}
