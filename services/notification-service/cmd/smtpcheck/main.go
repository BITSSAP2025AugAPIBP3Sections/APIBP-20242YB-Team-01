package main

import (
    "flag"
    "fmt"
    "log"
    "os"

    "github.com/Yaman-kumarsahu/notification-service/internal/config"
    gomail "github.com/go-mail/mail/v2"
)

func main() {
    check465 := flag.Bool("try465", false, "also try implicit TLS on port 465 if 587 fails")
    flag.Parse()

    cfg := config.LoadEmailConfig()

    fmt.Printf("Checking SMTP auth for %s@%s:%d\n", cfg.Username, cfg.Host, cfg.Port)

    d := gomail.NewDialer(cfg.Host, cfg.Port, cfg.Username, cfg.Password)
    d.Timeout = 15

    if tryDial(d) {
        os.Exit(0)
    }

    if *check465 {
        fmt.Println("Trying fallback to port 465 (implicit TLS)")
        d := gomail.NewDialer(cfg.Host, 465, cfg.Username, cfg.Password)
        d.SSL = true
        d.Timeout = 15
        if tryDial(d) {
            os.Exit(0)
        }
    }

    os.Exit(2)
}

func tryDial(d *gomail.Dialer) bool {
    fmt.Printf("Dialing %s:%d (user=%s)\n", d.Host, d.Port, d.Username)
    s, err := d.Dial()
    if err != nil {
        log.Printf("Dial failed: %v\n", err)
        return false
    }
    _ = s.Close()
    fmt.Println("✅ Authentication OK (dial succeeded)")
    return true
}
