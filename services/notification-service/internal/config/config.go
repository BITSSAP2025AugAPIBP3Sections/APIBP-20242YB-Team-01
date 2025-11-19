package config

import (
	"log"
	"os"
	"strconv"
)

// EmailConfig holds SMTP configuration used by the notifier service.
type EmailConfig struct {
	Host     string
	Port     int
	Username string
	Password string
	From     string
}

// LoadEmailConfig reads SMTP configuration from environment variables.
// Defaults: SMTP_HOST=smtp.gmail.com, SMTP_PORT=587.
// Credentials (SMTP_USERNAME / SMTP_PASSWORD) are required to send mail; if
// missing they will be left empty and the caller should fail fast.
func LoadEmailConfig() EmailConfig {
	host := os.Getenv("SMTP_HOST")
	if host == "" {
		host = "smtp.gmail.com"
	}

	port := 587
	if p := os.Getenv("SMTP_PORT"); p != "" {
		if pi, err := strconv.Atoi(p); err == nil {
			port = pi
		} else {
			log.Printf("⚠️ Invalid SMTP_PORT=%s, falling back to %d", p, port)
		}
	}

	username := os.Getenv("SMTP_USERNAME")
	password := os.Getenv("SMTP_PASSWORD")
	from := os.Getenv("SMTP_FROM")
	if from == "" {
		from = username
	}

	if username == "" || password == "" {
		log.Println("⚠️ SMTP_USERNAME or SMTP_PASSWORD is not set. Email sending will fail until credentials are provided.")
	}

	return EmailConfig{
		Host:     host,
		Port:     port,
		Username: username,
		Password: password,
		From:     from,
	}
}