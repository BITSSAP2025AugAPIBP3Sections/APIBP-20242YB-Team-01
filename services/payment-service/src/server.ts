import express from "express";
import bodyParser from "body-parser";
import { CONFIG } from "./config";
import { initRabbit } from "./rabbit/publisher";
import {
  depositHandler,
  createPaymentHandler,
  walletHandler,
  freezeHandler,
  unfreezeHandler,
  deductHandler,
} from "./api/payment.controller";

async function start() {
  await initRabbit();

  const app = express();
  app.use(bodyParser.json());

  // routes
  app.post("/wallet/deposit", depositHandler);
  app.post("/payment/create", createPaymentHandler);
  app.get("/wallet/:userId", walletHandler);
  app.post("/wallet/freeze", freezeHandler);
  app.post("/wallet/unfreeze", unfreezeHandler);
  app.post("/wallet/deduct", deductHandler);

  const server = app.listen(CONFIG.port, () => {
    console.log(`Payment service listening on ${CONFIG.port}`);
  });

  // graceful shutdown
  process.on("SIGINT", () => {
    console.log("Shutting down...");
    server.close(() => process.exit(0));
  });
}

start().catch((err) => {
  console.error("Failed to start service:", err);
  process.exit(1);
});
