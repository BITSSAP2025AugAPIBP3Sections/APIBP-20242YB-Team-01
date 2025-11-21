package mail

import (
    "log"
    "os"
    "strings"
    "time"

    gomail "github.com/go-mail/mail/v2"
)

type GmailClient struct {
    Dialer *gomail.Dialer
    From   string
}

// NewGmailClient sets up Gmail SMTP using an App Password.
func NewGmailClient(from, appPassword string) *GmailClient {
    d := gomail.NewDialer(
        "smtp.gmail.com",
        587,
        from,
        appPassword,
    )

    // Set timeout so the service never hangs
    d.Timeout = 10 * time.Second

    return &GmailClient{
        Dialer: d,
        From:   from,
    }
}

func (c *GmailClient) SendPlain(to, subject, body string) error {
    m := gomail.NewMessage()
    m.SetHeader("From", c.From)
    m.SetHeader("To", to)
    m.SetHeader("Subject", subject)
    m.SetBody("text/plain", body)

    // Diagnostic: attempt to Dial first to separate auth/connect errors from send errors.
    // Prepare credential display: masked by default, reveal only when DEBUG_SHOW_SECRETS=1
    showSecrets := false
    v := os.Getenv("DEBUG_SHOW_SECRETS")
    if v == "1" || strings.EqualFold(v, "true") {
        showSecrets = true
    }

    pwd := c.Dialer.Password
    maskedPwd := ""
    if pwd != "" {
        if len(pwd) <= 4 {
            maskedPwd = strings.Repeat("*", len(pwd))
        } else {
            maskedPwd = strings.Repeat("*", len(pwd)-4) + pwd[len(pwd)-4:]
        }
    }

    if showSecrets {
        log.Printf("SMTP: dialing host=%s port=%d username=%s password=%s", c.Dialer.Host, c.Dialer.Port, c.Dialer.Username, pwd)
    } else {
        log.Printf("SMTP: dialing host=%s port=%d username=%s password=%s", c.Dialer.Host, c.Dialer.Port, c.Dialer.Username, maskedPwd)
    }
    s, err := c.Dialer.Dial()
    if err != nil {
        log.Printf("SMTP: dial error: %v", err)
        return err
    }
    log.Printf("SMTP: dial succeeded to host=%s port=%d username=%s", c.Dialer.Host, c.Dialer.Port, c.Dialer.Username)
    defer func() {
        if err := s.Close(); err != nil {
            log.Printf("SMTP: failed to close sender: %v", err)
        }
    }()
    // Log send attempt
    log.Printf("SMTP: attempting to send message to=%s subject=%s", to, subject)
    if err := gomail.Send(s, m); err != nil {
        log.Printf("SMTP: send error: %v", err)
        return err
    }
    log.Printf("SMTP: send OK to=%s", to)

    return nil
}

func (c *GmailClient) SendHTML(to, subject, html string) error {
    m := gomail.NewMessage()
    m.SetHeader("From", c.From)
    m.SetHeader("To", to)
    m.SetHeader("Subject", subject)
    m.SetBody("text/html", html)

    // Diagnostic: attempt to Dial first to separate auth/connect errors from send errors.
    log.Printf("SMTP: dialing host=%s port=%d username=%s", c.Dialer.Host, c.Dialer.Port, c.Dialer.Username)
    s, err := c.Dialer.Dial()
    if err != nil {
        log.Printf("SMTP: dial error: %v", err)
        return err
    }
    log.Printf("SMTP: dial succeeded to host=%s port=%d username=%s", c.Dialer.Host, c.Dialer.Port, c.Dialer.Username)
    defer func() {
        if err := s.Close(); err != nil {
            log.Printf("SMTP: failed to close sender: %v", err)
        }
    }()

    // Log send attempt
    log.Printf("SMTP: attempting to send HTML message to=%s subject=%s", to, subject)
    if err := gomail.Send(s, m); err != nil {
        log.Printf("SMTP: send error: %v", err)
        return err
    }
    log.Printf("SMTP: send OK to=%s", to)

    return nil
}
