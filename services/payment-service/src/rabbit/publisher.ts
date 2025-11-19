import amqp from "amqplib";
import { CONFIG } from "../config";

let channel: amqp.Channel | null = null;

export async function initRabbit() {
  const conn = await amqp.connect(CONFIG.rabbitUrl);
  channel = await conn.createChannel();
  console.log("✅ Connected to RabbitMQ");
}

/**
 * Publish an event to a routing key (queue name or topic)
 */
export async function publish(routingKey: string, payload: object) {
  if (!channel) {
    throw new Error("rabbit channel not initialized");
  }
  const buf = Buffer.from(JSON.stringify(payload));
  // publish to default exchange with routing key
  channel.publish("", routingKey, buf, { contentType: "application/json" });
  console.log(`Published event ${routingKey}`);
}
