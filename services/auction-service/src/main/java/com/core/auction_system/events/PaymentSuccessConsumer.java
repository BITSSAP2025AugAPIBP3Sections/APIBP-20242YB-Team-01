package com.core.auction_system.events;

import com.core.auction_system.model.Bid;
import com.core.auction_system.model.Product;
import com.core.auction_system.repository.BidRepository;
import com.core.auction_system.service.ProductService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentSuccessConsumer {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    @Value("${rabbit.url:amqp://guest:guest@rabbitmq:5672}")
    private String rabbitUrl;
    @Value("${payment.consumer.queue:payment-service-consumer}")
    private String queueName;
    private Connection connection;
    private Channel channel;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private ProductService productService;

    @PostConstruct
    public void start() {
        exec.execute(() -> {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setUri(rabbitUrl);
                connection = factory.newConnection();
                channel = connection.createChannel();

                // ensure exchange exists
                channel.exchangeDeclare("events", BuiltinExchangeType.TOPIC, true);

                // declare a durable queue for this consumer
                channel.queueDeclare(queueName, true, false, false, null);

                // bind to payment.success and payment.failed routing keys
                channel.queueBind(queueName, "events", "payment.success");
                channel.queueBind(queueName, "events", "payment.failed");

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                    String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    try {
                        JsonNode node = mapper.readTree(body);
                        String userId = node.has("userId") ? node.get("userId").asText() : null;
                        String auctionId = node.has("auctionId") ? node.get("auctionId").asText() : null;
                        String reservationId = node.has("reservationId") ? node.get("reservationId").asText() : null;
                        String eventKey = delivery.getEnvelope().getRoutingKey();

                        boolean processed = false;

                        // correlate by reservationId if present
                        if (reservationId != null && !reservationId.isEmpty()) {
                            Optional<com.core.auction_system.model.Bid> maybe =
                                    bidRepository.findByReservationId(reservationId);
                            if (maybe.isPresent()) {
                                Bid b = maybe.get();
                                // idempotency: if already PAID and this is payment.success, skip
                                if ("PAID".equals(b.getStatus()) && "payment.success".equals(eventKey)) {
                                    processed = true;
                                } else if ("payment.success".equals(eventKey)) {
                                    b.setStatus("PAID");
                                    bidRepository.save(b);
                                    // mark product as sold/frozen
                                    Product p = b.getProduct();
                                    if (p != null) {
                                        if (!Boolean.TRUE.equals(p.getSold())) {
                                            p.setFrozen(true);
                                            p.setSold(true);
                                            p.setBuyerId(b.getBidderId());
                                            productService.updateProduct(p.getId(), p);
                                        }
                                    }
                                    processed = true;
                                } else if ("payment.failed".equals(eventKey)) {
                                    b.setStatus("FAILED");
                                    bidRepository.save(b);
                                    // do not change product state
                                    processed = true;
                                }
                            }
                        }

                        // If we processed a payment.success for a reservationId, publish winner/loser notifications
                        if ("payment.success".equals(eventKey) && reservationId != null && !reservationId.isEmpty()) {
                            try {
                                Optional<Bid> maybeWin = bidRepository.findByReservationId(reservationId);
                                if (maybeWin.isPresent()) {
                                    Bid winBid = maybeWin.get();
                                    Product winProd = winBid.getProduct();

                                    // Winner notification payload
                                    try {
                                        com.fasterxml.jackson.databind.node.ObjectNode winner =
                                                mapper.createObjectNode();
                                        winner.put("userId", String.valueOf(winBid.getBidderId()));
                                        winner.put("amount", winBid.getAmount());
                                        winner.put("auctionId", winProd != null && winProd.getId() != null ?
                                                String.valueOf(winProd.getId()) : "");
                                        winner.put("reservationId", reservationId);
                                        winner.put("message", "Congratulations! You won the auction for '" +
                                                (winProd != null ? winProd.getName() : "item") + "' with bid " +
                                                winBid.getAmount());
                                        channel.basicPublish("events", "auction.winner", null,
                                                mapper.writeValueAsBytes(winner));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    // Loser notifications: all other bids for same product
                                    try {
                                        java.util.List<Bid> allBids = bidRepository.findByProduct(winProd);
                                        for (Bid other : allBids) {
                                            if (other.getId() != null && other.getId().equals(winBid.getId())) {
                                                continue;
                                            }
                                            try {
                                                com.fasterxml.jackson.databind.node.ObjectNode loser =
                                                        mapper.createObjectNode();
                                                loser.put("userId", other.getBidderId() == null ? "" :
                                                        String.valueOf(other.getBidderId()));
                                                loser.put("amount", other.getAmount());
                                                loser.put("auctionId", winProd != null && winProd.getId() != null ?
                                                        String.valueOf(winProd.getId()) : "");
                                                loser.put("reservationId", other.getReservationId() == null ? "" :
                                                        other.getReservationId());
                                                loser.put("message",
                                                        "Your bid of " + other.getAmount() + " did not win for '" +
                                                                (winProd != null ? winProd.getName() : "item") + "'");
                                                channel.basicPublish("events", "auction.loser", null,
                                                        mapper.writeValueAsBytes(loser));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // fallback: only on payment.success and only if reservationId path didn't process
                        if (!processed && "payment.success".equals(eventKey) && auctionId != null) {
                            try {
                                Integer prodId = Integer.parseInt(auctionId);
                                Optional<Product> prodOpt = productService.getProductById(prodId);
                                if (prodOpt.isPresent()) {
                                    Product p = prodOpt.get();
                                    // mark product frozen/closed if not already sold
                                    if (!Boolean.TRUE.equals(p.getSold())) {
                                        p.setFrozen(true);
                                        productService.updateProduct(p.getId(), p);
                                    }
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        // Optionally update bid status: mark top bid as paid (best-effort)
                        if (auctionId != null && "payment.success".equals(eventKey)) {
                            try {
                                Integer prodId = Integer.parseInt(auctionId);
                                Optional<Product> prodOpt = productService.getProductById(prodId);
                                if (prodOpt.isPresent()) {
                                    Product p = prodOpt.get();
                                    // find top bid
                                    Optional<Bid> top = bidRepository.findTopByProductOrderByAmountDesc(p);
                                    if (top.isPresent()) {
                                        Bid tb = top.get();
                                        if (tb.getStatus() == null || !"PAID".equals(tb.getStatus())) {
                                            tb.setStatus("PAID");
                                            bidRepository.save(tb);
                                        }
                                    }
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        // ack on success
                        try {
                            channel.basicAck(deliveryTag, false);
                        } catch (IOException ackEx) {
                            // if ack fails, rethrow so outer catch nacks
                            throw ackEx;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        try {
                            // requeue message for retry (true) - consider DLQ for repeated failures
                            channel.basicNack(deliveryTag, false, true);
                        } catch (IOException ioEx) {
                            ioEx.printStackTrace();
                        }
                    }
                };

                // use manual acks for reliability
                boolean autoAck = false;
                channel.basicConsume(queueName, autoAck, deliverCallback, consumerTag -> {
                });

            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            exec.shutdownNow();
        } catch (Exception ignored) {
        }
    }
}
