package com.core.auction_system.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Component
public class RabbitPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitPublisher.class);

    @Value("${rabbit.url:amqp://guest:guest@rabbitmq:5672}")
    private String rabbitUrl;

    private Connection connection;
    private Channel channel;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void start() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(rabbitUrl);
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare("events", "topic", true);
            log.info("RabbitPublisher connected to {}", rabbitUrl);
        } catch (Exception e) {
            log.error("Failed to start RabbitPublisher: {}", e.getMessage(), e);
            // leave nulls; callers should handle missing channel
        }
    }

    public synchronized boolean publish(String exchange, String routingKey, Object payload) {
        if (channel == null || !channel.isOpen()) {
            log.warn("Rabbit channel not available to publish {}:{}", exchange, routingKey);
            return false;
        }
        try {
            byte[] body = mapper.writeValueAsBytes(payload);
            channel.basicPublish(exchange, routingKey, null, body);
            return true;
        } catch (IOException e) {
            log.warn("Failed to publish message {}:{} -> {}", exchange, routingKey, e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (IOException | TimeoutException ignored) {
        } catch (Exception e) {
            log.warn("Exception closing rabbit resources: {}", e.getMessage());
        }
    }
}
