import express from "express";
import bodyParser from "body-parser";
import { CONFIG } from "./config";
import { initRabbit } from "./rabbit/publisher";
import { initDb } from "./wallet/wallet.service";
import {
  depositHandler,
  createPaymentHandler,
  walletHandler,
  freezeHandler,
  unfreezeHandler,
  deductHandler,
} from "./api/payment.controller";

async function start() {
  // ensure DB schema and rabbit are ready
  await initDb();
  await initRabbit();

  const app = express();
  // capture raw body for debugging parse errors
  app.use(
    bodyParser.json({
      limit: "1mb",
      verify: (req: express.Request, _res: express.Response, buf: Buffer, encoding: string) => {
        try {
          (req as any).rawBody = buf.toString((encoding as BufferEncoding) || "utf8");
        } catch (e) {
          (req as any).rawBody = undefined;
        }
      },
    })
  );

  // body-parser JSON error handler: give more helpful 400 and log raw body for debugging
  app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
    if (err && (err.type === "entity.parse.failed" || err.status === 400)) {
      console.error("Body parse error:", err.message);
      console.error("Raw body:", (req as any).rawBody);
      console.error("Headers:", req.headers);
      return res.status(400).json({ error: "invalid_json", message: err.message });
    }
    return next(err);
  });

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
