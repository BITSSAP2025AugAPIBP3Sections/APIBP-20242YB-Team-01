export type DepositResult = {
  ok: boolean;
  reason?: string;
};

export type WalletState = {
  userId: string;
  balance: number;
  locked: number; // total locked amount
};
