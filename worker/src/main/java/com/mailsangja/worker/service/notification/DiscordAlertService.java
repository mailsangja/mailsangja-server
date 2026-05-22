package com.mailsangja.worker.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.worker.config.properties.DiscordAlertProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordAlertService {

    private static final int MAX_ERROR_LENGTH = 800;
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final DiscordAlertProperties discordAlertProperties;
    private final ObjectMapper objectMapper;

    public void sendDlqAlert(Message message, Throwable cause) {
        String webhookUrl = discordAlertProperties.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Discord webhook URL not configured — skipping DLQ alert.");
            return;
        }

        String queueName = message.getMessageProperties().getConsumerQueue();
        String messageId = message.getMessageProperties().getMessageId();
        int payloadSize = message.getBody().length;
        String errorMessage = cause != null ? cause.getMessage() : "unknown";

        try {
            String payload = buildPayload(queueName, messageId, payloadSize, errorMessage);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Failed to send DLQ alert to Discord. queue={} messageId={}", queueName, messageId, e);
        }
    }

    private String buildPayload(String queueName, String messageId, int payloadSize, String errorMessage)
            throws JsonProcessingException {
        String truncatedError = errorMessage != null && errorMessage.length() > MAX_ERROR_LENGTH
                ? errorMessage.substring(0, MAX_ERROR_LENGTH) + "…"
                : errorMessage;

        Map<String, Object> payload = Map.of(
                "username", "Mailsangja Worker",
                "embeds", List.of(Map.of(
                        "title", "🚨 DLQ Alert — Retries Exhausted",
                        "color", 15158332,
                        "timestamp", Instant.now().toString(),
                        "fields", List.of(
                                Map.of("name", "Queue", "value", nullSafe(queueName), "inline", false),
                                Map.of("name", "Message ID", "value", nullSafe(messageId), "inline", true),
                                Map.of("name", "Payload", "value", payloadSize + " B", "inline", true),
                                Map.of("name", "Error", "value", nullSafe(truncatedError), "inline", false)
                        )
                ))
        );
        return objectMapper.writeValueAsString(payload);
    }

    private String nullSafe(String value) {
        return value != null ? value : "unknown";
    }
}
