# Notification Service

A Go-based notification service that listens to RabbitMQ events and sends email notifications.

## Features
- Listens to RabbitMQ queue for notification events
- Sends emails using SMTP
- Configurable via environment variables
- Docker-ready


## Environment Variables
Create a `.env` file in the project root with the following content:

```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
SMTP_FROM=your_email@gmail.com
RECIPIENT_EMAIL=recipient@example.com
```

Replace the values with your actual SMTP credentials. For Gmail, use an App Password, not your regular password. Set `RECIPIENT_EMAIL` to the address you want to receive the test notification.


## Running Locally
```bash
git clone <repo-url>
cd notification-service
go mod tidy
go run ./cmd/notifier/main.go
```

## Sending a Test Notification

To send a test email, use the provided `test_publish.go` script. The recipient's email will be read from the `RECIPIENT_EMAIL` variable in your `.env` file.

Run the script:
```bash
go run test_publish.go
```

## What You Need to Configure
- `.env` file: SMTP credentials for sending emails
- `test_publish.go`: Set the recipient's email address and message in the payload
- RabbitMQ: Should be running and accessible at `amqp://guest:guest@localhost:5672/`

## Docker Usage
Build and run the service in Docker:
```bash
docker build -t notification-service .
```


## RabbitMQ
Ensure RabbitMQ is running and accessible at `amqp://guest:guest@localhost:5672/`.

## License
MIT

## Using Gmail SMTP (App Password) — Step-by-step

Gmail requires an App Password for SMTP if your account has 2-Step Verification enabled. Follow these steps to create and use an App Password:

1. Enable 2-Step Verification for your Google account:
   - Visit: https://myaccount.google.com/security
   - Enable "2-Step Verification" if it's not already enabled.

2. Create an App Password:
   - From the same Security page, open "App passwords".
   - Create a new app password (select "Mail" and the device or "Other"), then copy the 16-character password (e.g. `abcd efgh ijkl mnop`).
   - Use this 16-character code in place of your regular Google password for SMTP.

3. Gmail SMTP settings to use:

   - SMTP Host: `smtp.gmail.com`
   - Port: `587` (TLS)
   - Username: your full Gmail address (e.g. `you@gmail.com`)
   - Password: the 16-character App Password you generated

4. Quick Go example (plain SMTP using net/smtp)

```go
package main

import (
	"log"
	"net/smtp"
)

func main() {
	from := "your@gmail.com"
	password := "your-app-password" // 16-char code from Google
	to := []string{"recipient@example.com"}

	// Email message
	msg := []byte("Subject: Test Email from Go via Gmail\r\n" +
		"\r\n" +
		"This is a real SMTP email sent using Gmail.\r\n")

	// Auth
	auth := smtp.PlainAuth("", from, password, "smtp.gmail.com")

	// Send email
	err := smtp.SendMail("smtp.gmail.com:587", auth, from, to, msg)
	if err != nil {
		log.Fatal("SMTP error:", err)
	}

	log.Println("Email sent successfully!")
}
```

Notes:
- `net/smtp` is deprecated but still works. For richer features (HTML, attachments, templates), consider `github.com/jordan-wright/email` or `github.com/go-mail/mail` (the project already uses `gopkg.in/gomail.v2`).
- If you prefer a reusable function in this repo, see `internal/mail/gmail.go` for a small helper that sends mail via Gmail SMTP.

⚠️ Security: Never commit real App Passwords to source control. Use environment variables or secrets stores in production.
