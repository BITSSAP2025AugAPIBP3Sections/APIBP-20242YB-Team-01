# ---- Builder ----
FROM golang:1.22-alpine AS builder

WORKDIR /app

# Install build tools
RUN apk add --no-cache git

# Cache go mod
COPY go.mod go.sum ./
RUN go mod download

# Copy full project
COPY . .

RUN CGO_ENABLED=0 GOOS=linux go build -o notifier ./cmd/notifier/main.go

# ---- Final ----
FROM alpine:latest

WORKDIR /app

# Required for Gmail TLS
RUN apk add --no-cache ca-certificates

COPY --from=builder /app/notifier .

ENV SMTP_HOST=smtp.gmail.com \
    SMTP_PORT=587 \
    SMTP_USERNAME="" \
    SMTP_PASSWORD="" \
    SMTP_FROM="" \
    QUEUE_NAME="" \
    RABBITMQ_URL=""

EXPOSE 8080

# Run as non-root
USER nobody

CMD ["./notifier"]
