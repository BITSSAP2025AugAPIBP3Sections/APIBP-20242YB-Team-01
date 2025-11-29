import React, { useState, useEffect } from "react";
import styles from "../css/Wallet.module.css";

export default function Wallet({ onBalanceUpdate }) {
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showDepositModal, setShowDepositModal] = useState(false);
  const [depositAmount, setDepositAmount] = useState("");
  const [depositSource, setDepositSource] = useState("card");
  const [depositFeedback, setDepositFeedback] = useState("");

  const fetchBalance = async () => {
    const token = localStorage.getItem("token");
    const userId = localStorage.getItem("id");
    
    if (!userId) {
      console.error("User ID not found");
      setLoading(false);
      return;
    }

    try {
      const response = await fetch(
        `http://localhost:8080/api/wallet/${userId}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      if (response.ok) {
        const data = await response.json();
        // Handle the response format: { balance, locked }
        const walletData = {
          available: data.balance || 0,
          locked: data.locked || 0,
        };
        setBalance(walletData);
        if (onBalanceUpdate) {
          onBalanceUpdate(walletData);
        }
      } else {
        console.error("Failed to fetch wallet balance");
      }
    } catch (err) {
      console.error("Error fetching wallet balance:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBalance();
    // Refresh balance every 30 seconds
    const interval = setInterval(fetchBalance, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleDeposit = async () => {
    const token = localStorage.getItem("token");
    const userId = localStorage.getItem("id");
    const amt = parseFloat(depositAmount);

    if (isNaN(amt) || amt <= 0) {
      setDepositFeedback("⚠️ Please enter a valid amount greater than 0");
      return;
    }

    try {
      const response = await fetch(
        `http://localhost:8080/api/wallet/deposit`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            userId,
            amount: amt,
            source: depositSource,
          }),
        }
      );

      const data = await response.json();

      if (response.ok) {
        setDepositFeedback(`✅ Successfully deposited $${amt}`);
        setDepositAmount("");
        // Refresh balance
        await fetchBalance();
        // Close modal after 2 seconds
        setTimeout(() => {
          setShowDepositModal(false);
          setDepositFeedback("");
        }, 2000);
      } else {
        setDepositFeedback(`❌ ${data.error || "Deposit failed"}`);
      }
    } catch (err) {
      console.error("Error depositing funds:", err);
      setDepositFeedback("⚠️ Unable to process deposit. Please try again.");
    }
  };

  if (loading) {
    return <div className={styles.walletLoading}>Loading wallet...</div>;
  }

  return (
    <>
      <div className={styles.walletContainer} onClick={() => setShowDepositModal(true)}>
        <div className={styles.walletIcon}>💰</div>
        <div className={styles.walletInfo}>
          <div className={styles.walletLabel}>Wallet</div>
          <div className={styles.walletBalance}>
            ${balance?.available?.toFixed(2) || "0.00"}
          </div>
          {balance?.locked > 0 && (
            <div className={styles.walletLocked}>
              (${balance.locked.toFixed(2)} locked)
            </div>
          )}
        </div>
      </div>

      {showDepositModal && (
        <div className={styles.modalOverlay} onClick={() => setShowDepositModal(false)}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <h2>Deposit Funds</h2>
            
            <div className={styles.balanceInfo}>
              <div className={styles.balanceRow}>
                <span>Available Balance:</span>
                <strong>${balance?.available?.toFixed(2) || "0.00"}</strong>
              </div>
              {balance?.locked > 0 && (
                <div className={styles.balanceRow}>
                  <span>Locked Balance:</span>
                  <strong>${balance.locked.toFixed(2)}</strong>
                </div>
              )}
              <div className={styles.balanceRow}>
                <span>Total Balance:</span>
                <strong>${((balance?.available || 0) + (balance?.locked || 0)).toFixed(2)}</strong>
              </div>
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="depositAmount">Amount to Deposit</label>
              <input
                id="depositAmount"
                type="number"
                value={depositAmount}
                onChange={(e) => setDepositAmount(e.target.value)}
                placeholder="Enter amount"
                min="0"
                step="0.01"
              />
            </div>

            <div className={styles.formGroup}>
              <label htmlFor="depositSource">Payment Method</label>
              <select
                id="depositSource"
                value={depositSource}
                onChange={(e) => setDepositSource(e.target.value)}
                className={styles.selectInput}
              >
                <option value="card">Credit/Debit Card</option>
                <option value="bank">Bank Transfer</option>
                <option value="paypal">PayPal</option>
              </select>
            </div>

            {depositFeedback && (
              <div
                className={
                  depositFeedback.startsWith("✅")
                    ? styles.successMsg
                    : styles.errorMsg
                }
              >
                {depositFeedback}
              </div>
            )}

            <div className={styles.modalActions}>
              <button onClick={handleDeposit} className={styles.depositButton}>
                Deposit
              </button>
              <button
                onClick={() => {
                  setShowDepositModal(false);
                  setDepositFeedback("");
                  setDepositAmount("");
                }}
                className={styles.cancelButton}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
