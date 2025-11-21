package events

import (
	"context"
	"log"
	"strings"

	"github.com/rabbitmq/amqp091-go"
)

type NotificationHandler func(eventName string, payload []byte)

type Consumer struct {
	conn    *amqp091.Connection
	channel *amqp091.Channel
	queue   amqp091.Queue
	handler NotificationHandler
}

// NewConsumer connects to RabbitMQ at amqpURL and declares the provided queueName.
// Use RABBITMQ_URL and QUEUE_NAME env vars in main to set these values.
// NewConsumer connects to RabbitMQ at amqpURL, declares the provided queueName,
// ensures the 'events' exchange exists and binds routing keys provided in queueBindings (comma-separated)
// Use RABBITMQ_URL and QUEUE_NAME / QUEUE_BINDINGS env vars in main to set these values.
func NewConsumer(amqpURL, queueName, queueBindings string, handler NotificationHandler) (*Consumer, error) {
	// Connect to RabbitMQ
	conn, err := amqp091.Dial(amqpURL)
	if err != nil {
		log.Println("❌ Failed to connect to RabbitMQ:", err)
		return nil, err
	}
	log.Println("🔌 Connected to RabbitMQ")

	ch, err := conn.Channel()
	if err != nil {
		log.Println("❌ Failed to open channel:", err)
		return nil, err
	}

	// Declare queue
	q, err := ch.QueueDeclare(
		queueName, // queue name
		true,      // durable
		false,     // auto-delete
		false,     // exclusive
		false,     // no-wait
		nil,       // args
	)
	if err != nil {
		log.Println("❌ Failed to declare queue:", err)
		return nil, err
	}
	log.Println("📥 Declared queue:", q.Name)

	// Ensure 'events' exchange exists and bind routing keys
	if err := ch.ExchangeDeclare("events", "topic", true, false, false, false, nil); err != nil {
		log.Println("❌ Failed to declare exchange 'events':", err)
		return nil, err
	}
	log.Println("🔁 Ensured exchange 'events'")

	if queueBindings != "" {
		// binding keys are comma-separated
		var keys = strings.Split(queueBindings, ",")
		for _, k := range keys {
			key := strings.TrimSpace(k)
			if key == "" {
				continue
			}
			if err := ch.QueueBind(q.Name, key, "events", false, nil); err != nil {
				log.Printf("❌ Failed to bind queue %s to events/%s: %v", q.Name, key, err)
				return nil, err
			}
			log.Printf("🔗 Bound queue %s -> events/%s", q.Name, key)
		}
	} else {
		// If no bindings specified, bind queue to a default routing key equal to queueName (backwards compatible)
		if err := ch.QueueBind(q.Name, queueName, "events", false, nil); err != nil {
			log.Printf("❌ Failed to bind queue %s to events/%s: %v", q.Name, queueName, err)
			return nil, err
		}
		log.Printf("🔗 Bound queue %s -> events/%s (default)", q.Name, queueName)
	}

	return &Consumer{
		conn:    conn,
		channel: ch,
		queue:   q,
		handler: handler,
	}, nil
}

func (c *Consumer) Start(ctx context.Context) error {
	msgs, err := c.channel.Consume(
		c.queue.Name,
		"",
		true,  // auto-ack
		false, // exclusive
		false,
		false,
		nil,
	)
	if err != nil {
		log.Println("❌ Failed to start consuming messages:", err)
		return err
	}

	log.Println("✅ Notification consumer listening on queue:", c.queue.Name)

	for {
		select {
		case msg := <-msgs:
			eventName := msg.RoutingKey
			// Log the incoming message payload (helpful for debugging). Keep concise.
			log.Printf("🔔 Received message - event=%s payload=%s", eventName, string(msg.Body))

			// Protect handler from panics so the consumer can continue processing.
			func() {
				defer func() {
					if r := recover(); r != nil {
						log.Printf("❌ Notification handler panicked: %v", r)
					}
				}()
				c.handler(eventName, msg.Body)
			}()

		case <-ctx.Done():
			log.Println("🛑 Stopping notification consumer...")
			return c.Shutdown()
		}
	}
}

func (c *Consumer) Shutdown() error {
	if err := c.channel.Close(); err != nil {
		return err
	}
	return c.conn.Close()
}
