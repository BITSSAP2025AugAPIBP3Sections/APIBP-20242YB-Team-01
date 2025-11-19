package main

import (
	"context"
	"encoding/json"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/joho/godotenv"
	"github.com/Yaman-kumarsahu/notification-service/internal/config"
	"github.com/Yaman-kumarsahu/notification-service/internal/events"
	"github.com/Yaman-kumarsahu/notification-service/internal/mail"
)

type AuctionNotification struct {
	Email   string `json:"email"`
	Message string `json:"message"`
}

func main() {

	if err := godotenv.Load(); err != nil {
        log.Println("⚠️ No .env file found")
    }
	cfg := config.LoadEmailConfig()

	// Determine sender email
	from := cfg.Username
	if from == "" {
		from = cfg.From
	}

	if from == "" || cfg.Password == "" {
		log.Fatal("❌ SMTP_USERNAME / SMTP_FROM and SMTP_PASSWORD must be set")
	}

	// Initialize the new Gmail client
	gmailClient := mail.NewGmailClient(from, cfg.Password)

	// Handler for RabbitMQ messages
	handler := func(eventName string, payload []byte) {
		log.Printf("➡️ Handling event=%s payload=%s", eventName, string(payload))

		var data AuctionNotification
		if err := json.Unmarshal(payload, &data); err != nil {
			log.Println("❌ Invalid notification payload:", err)
			return
		}

		if data.Email == "" {
			log.Println("❌ Email field is empty in the notification payload")
			return
		}

		// Log send attempt (do not log password)
		log.Printf("✉️ Attempting to send email from=%s to=%s subject=%s", from, data.Email, "Auction Notification")

		// Send the email
		err := gmailClient.SendPlain(
			data.Email,
			"Auction Notification",
			data.Message,
		)

		if err != nil {
			log.Printf("❌ Failed to send email to=%s error=%v", data.Email, err)
			return
		}

		log.Println("✅ Email sent successfully to:", data.Email)
	}

	// RabbitMQ configuration
	amqpURL := os.Getenv("RABBITMQ_URL")
	if amqpURL == "" {
		amqpURL = "amqp://guest:guest@localhost:5672/"
	}

	queueName := os.Getenv("QUEUE_NAME")
	if queueName == "" {
		queueName = "notifications"
	}

	consumer, err := events.NewConsumer(amqpURL, queueName, handler)
	if err != nil {
		log.Fatal(err)
	}

	// Graceful shutdown handling
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	if err := consumer.Start(ctx); err != nil {
		log.Fatal(err)
	}
}
