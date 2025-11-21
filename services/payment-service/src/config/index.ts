import dotenv from "dotenv";

dotenv.config();

export const CONFIG = {
  port: process.env.PORT || "8080",
  rabbitUrl: process.env.RABBITMQ_URL || "amqp://guest:guest@localhost:5672",
  gatewayBaseUrl: process.env.GATEWAY_BASE_URL || "http://localhost:9000",
  gatewayApiKey: process.env.GATEWAY_API_KEY || "",
};
