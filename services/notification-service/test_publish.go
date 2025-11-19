package main

import (
	"encoding/json"
	"log"
	"os"
	"github.com/joho/godotenv"
	"github.com/rabbitmq/amqp091-go"
)

func main() {
       // Load .env file
       _ = godotenv.Load()

       amqpURL := os.Getenv("RABBITMQ_URL")
       if amqpURL == "" {
	       amqpURL = "amqp://guest:guest@localhost:5672/"
       }

       conn, err := amqp091.Dial(amqpURL)
       if err != nil {
	       log.Fatal("Failed to connect to RabbitMQ:", err)
       }
       defer conn.Close()

       ch, err := conn.Channel()
       if err != nil {
	       log.Fatal("Failed to open channel:", err)
       }
       defer ch.Close()

       queueName := os.Getenv("QUEUE_NAME")
       if queueName == "" {
	       queueName = "notifications"
       }

       q, err := ch.QueueDeclare(
	       queueName,
	       true,
	       false,
	       false,
	       false,
	       nil,
       )
       if err != nil {
	       log.Fatal("Failed to declare queue:", err)
       }

       recipient := os.Getenv("RECIPIENT_EMAIL")
       if recipient == "" {
	       log.Fatal("RECIPIENT_EMAIL not set in .env")
       }

       payload := map[string]string{
	       "email":   recipient,
	       "message": "This is a test notification from the service.",
       }
       body, _ := json.Marshal(payload)

       err = ch.Publish(
	       "",         // exchange
	       q.Name,      // routing key
	       false,       // mandatory
	       false,       // immediate
	       amqp091.Publishing{
		       ContentType: "application/json",
		       Body:        body,
	       },
       )
       if err != nil {
	       log.Fatal("Failed to publish message:", err)
       }

       log.Println("Test notification published to queue.")
}
