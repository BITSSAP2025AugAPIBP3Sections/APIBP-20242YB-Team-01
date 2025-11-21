import amqp from "amqplib";
import { CONFIG } from "../config";

let channel: amqp.Channel | null = null;

export async function initRabbit() {
  const conn = await amqp.connect(CONFIG.rabbitUrl);
  channel = await conn.createChannel();
  // ensure events exchange exists
  await channel.assertExchange("events", "topic", { durable: true });
  console.log("✅ Connected to RabbitMQ and ensured 'events' exchange");
}

/**
 * Publish an event to a routing key (queue name or topic)
 */
export async function publish(routingKey: string, payload: object) {
  if (!channel) {
    throw new Error("rabbit channel not initialized");
  }
  const buf = Buffer.from(JSON.stringify(payload));
  // publish to named exchange 'events' so multiple queues can bind to the same routing key
  channel.publish("events", routingKey, buf, { contentType: "application/json", persistent: true });
  console.log(`Published event ${routingKey} to exchange 'events'`);
}
