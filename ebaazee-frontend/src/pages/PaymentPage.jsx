import React, { useState } from "react";
import styles from "../css/PaymentPage.module.css";
import orderData from "../data/OrderData.json";
import { useNavigate } from "react-router-dom";

export default function PaymentPage() {
    const [step, setStep] = useState(1);
    const [method, setMethod] = useState("");
    const { orderId, items = [], shipping = 0, taxRate = 0 } = orderData;
    const [paid, setPaid] = useState(false);
    const navigate = useNavigate();

    // Compute totals
    const subtotal = items.reduce((sum, i) => sum + i.price, 0);
    const tax = Math.round(subtotal * taxRate * 100) / 100;
    const total = subtotal + tax + shipping;

    const handleNext = (e) => {
        e.preventDefault();
        if (method) setStep(2);
    };

    const handlePayment = () => {
        alert("Payment successful!");
        setPaid(true);

        // ─── AFTER payment succeeds, place the bid ───────────────────
        const pending = localStorage.getItem("pendingBid");
        if (pending) {
            try {
                const { productId, bidAmount } = JSON.parse(pending);
                const token = localStorage.getItem("token");
                fetch(
                    `http://localhost:9090/api/bids/place/${productId}?bidAmount=${bidAmount}`,
                    {
                        method: "POST",
                        headers: {
                            Authorization: `Bearer ${token}`,
                        },
                    }
                )
                    .then((res) => {
                        if (!res.ok) {
                            return res.text().then((txt) => {
                                console.error("Bid placement failed:", txt);
                                return;
                            });
                        }
                        // Show confirmation
                        alert("✅ Your bid was placed successfully!");
                        // Clear the “pendingBid”
                        localStorage.removeItem("pendingBid");
                        // Optional: redirect to “My Auctions”
                        // navigate("/myauction");
                    })
                    .catch((err) =>
                        console.error("Error during place‐bid call:", err)
                    );
            } catch (err) {
                console.error("Invalid pendingBid in localStorage:", err);
            }
        }
    };

    const downloadInvoice = () => {
        alert("Invoice downloaded!");
    };

    return (
        <div className={styles.container}>
            <h1 className={styles.pageTitle}>Payment</h1>

            {/* Stepper */}
            <div className={styles.stepper}>
                <div
                    className={`${styles.step} ${step >= 1 ? styles.active : ""}`}
                    onClick={() => setStep(1)}
                >
                    <div className={styles.circle}>1</div>
                    <div className={styles.label}>Payment Method</div>
                </div>

                <div className={`${styles.connector} ${step > 1 ? styles.active : ""}`} />

                <div
                    className={`${styles.step} ${step >= 2 ? styles.active : ""}`}
                    onClick={() => step >= 2 && setStep(2)}
                    style={{ cursor: step >= 2 ? "pointer" : "not-allowed" }}
                >
                    <div className={styles.circle}>2</div>
                    <div className={styles.label}>Summary &amp; Checkout</div>
                </div>

                <div className={`${styles.connector} ${step > 2 ? styles.active : ""}`} />

                <div
                    className={`${styles.step} ${step >= 3 ? styles.active : ""}`}
                    onClick={() => step >= 3 && setStep(3)}
                    style={{ cursor: step >= 3 ? "pointer" : "not-allowed" }}
                >
                    <div className={styles.circle}>3</div>
                    <div className={styles.label}>Track Order</div>
                </div>
            </div>

            {/* Content */}
            <div className={styles.content}>
                {step === 1 && (
                    <form className={styles.methodForm} onSubmit={handleNext}>
                        <h2 className={styles.instructionTitle}>Select a payment method</h2>
                        <p className={styles.instructionText}>
                            Choose one of the options below to proceed.
                        </p>

                        <div className={styles.optionList}>
                            <label className={styles.option}>
                                <input
                                    type="radio"
                                    name="paymentMethod"
                                    value="stripe"
                                    checked={method === "stripe"}
                                    onChange={(e) => setMethod(e.target.value)}
                                />
                                <span>⚡ Stripe</span>
                            </label>
                            <label className={styles.option}>
                                <input
                                    type="radio"
                                    name="paymentMethod"
                                    value="razorpay"
                                    checked={method === "razorpay"}
                                    onChange={(e) => setMethod(e.target.value)}
                                />
                                <span>💳 RazorPay</span>
                            </label>
                        </div>

                        <button
                            type="submit"
                            className={styles.nextButton}
                            disabled={!method}
                        >
                            Go to Checkout
                        </button>
                    </form>
                )}

                {step === 2 && (
                    <div className={styles.summary}>
                        <h2>Summary &amp; Checkout</h2>
                        <p>
                            Review your chosen method:{" "}
                            <strong>
                                {method === "stripe" ? "⚡ Stripe" : "💳 RazorPay"}
                            </strong>
                        </p>
                        <p className={styles.orderId}>
                            Order ID: <strong> {orderId}</strong>
                        </p>

                        {/* Line items */}
                        <div className={styles.orderSummary}>
                            {items.map((item) => (
                                <div key={item.id} className={styles.row}>
                                    <span>{item.name}</span>
                                    <span>${item.price.toFixed(2)}</span>
                                </div>
                            ))}
                            <div className={styles.divider} />
                            <div className={styles.row}>
                                <span>Subtotal</span>
                                <span>${subtotal.toFixed(2)}</span>
                            </div>
                            <div className={styles.row}>
                                <span>Tax (8%)</span>
                                <span>${tax.toFixed(2)}</span>
                            </div>
                            <div className={styles.row}>
                                <span>Shipping</span>
                                <span>${shipping.toFixed(2)}</span>
                            </div>
                            <div className={styles.divider} />
                            <div className={styles.rowTotal}>
                                <strong>Total</strong>
                                <strong>${total.toFixed(2)}</strong>
                            </div>
                        </div>

                        {!paid ? (
                            <button className={styles.payButton} onClick={handlePayment}>
                                Pay Now with {method === "stripe" ? "Stripe" : "RazorPay"}
                            </button>
                        ) : (
                            <div className={styles.postPaymentButtons}>
                                <button
                                    onClick={downloadInvoice}
                                    className={styles.secondaryButton}
                                >
                                    📄 Download Invoice
                                </button>
                                <button
                                    onClick={() => setStep(3)}
                                    className={styles.primaryButton}
                                >
                                    🚚 Go to Track Order
                                </button>
                            </div>
                        )}
                    </div>
                )}

                {step === 3 && (
                    <div className={styles.trackOrder}>
                        <h2>Track Your Order</h2>
                        <p>
                            Order <strong>{orderId}</strong> has been processed and is on its
                            way!
                        </p>
                        <ul className={styles.trackingSteps}>
                            <li>📦 Packed</li>
                            <li>🚚 Shipped</li>
                            <li>🕒 In Transit</li>
                            <li>🏠 Out for Delivery</li>
                        </ul>
                    </div>
                )}
            </div>
        </div>
    );
}
