import express from "express";

const app = express();
app.use(express.json());

app.post("/charge", (req, res) => {
    const { amount, source } = req.body;

    // Always success for simulation
    res.json({
        status: "success",
        transactionId: "tx_" + Date.now(),
        amount,
        source
    });
});

app.post("/refund", (req, res) => {
    res.json({
        status: "success",
        refundId: "refund_" + Date.now()
    });
});

app.post("/verify", (req, res) => {
    res.json({ verified: true });
});

app.listen(9000, () => console.log("Fake Payment Gateway running on :9000"));
