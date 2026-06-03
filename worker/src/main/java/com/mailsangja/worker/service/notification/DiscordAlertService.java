package com.mailsangja.worker.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        int payloadSize = message.getBody().length;
        String rootCauseText = extractRootCauseMessage(cause);
        Map<String, String> payloadFields = extractPayloadFields(message.getBody());

        try {
            String payload = buildPayload(queueName, payloadSize, rootCauseText, payloadFields);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Failed to send DLQ alert to Discord. queue={}", queueName, e);
        }
    }

    private Map<String, String> extractPayloadFields(byte[] body) {
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            JsonNode node = objectMapper.readTree(body);
            node.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isNull()) return;
                if (value.isArray()) {
                    fields.put(entry.getKey(), value.size() + " items");
                } else {
                    fields.put(entry.getKey(), value.asText());
                }
            });
        } catch (Exception e) {
            log.debug("Failed to parse DLQ message body", e);
        }
        return fields;
    }

    private String buildPayload(String queueName, int payloadSize, String rootCauseText, Map<String, String> payloadFields)
            throws JsonProcessingException {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(Map.of("name", "Queue", "value", nullSafe(queueName), "inline", false));

        payloadFields.forEach((key, value) ->
                fields.add(Map.of("name", key, "value", nullSafe(value), "inline", true)));

        fields.add(Map.of("name", "Payload Size", "value", payloadSize + " B", "inline", true));

        String truncatedError = rootCauseText.length() > MAX_ERROR_LENGTH
                ? rootCauseText.substring(0, MAX_ERROR_LENGTH) + "…"
                : rootCauseText;
        fields.add(Map.of("name", "Root Cause", "value", "```\n" + truncatedError + "\n```", "inline", false));

        Map<String, Object> payload = Map.of(
                "username", "Mailsangja Worker",
                "embeds", List.of(Map.of(
                        "title", "🚨 DLQ Alert — Retries Exhausted",
                        "color", 15158332,
                        "timestamp", Instant.now().toString(),
                        "fields", fields
                ))
        );
        return objectMapper.writeValueAsString(payload);
    }

    private String extractRootCauseMessage(Throwable cause) {
        if (cause == null) return "unknown";
        Throwable root = cause;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage() != null ? root.getMessage() : "(no message)";
        String summary = root.getClass().getSimpleName() + ": " + message;
        String callSite = findFirstAppFrame(cause);
        return callSite != null ? summary + "\nat " + callSite : summary;
    }

    private String findFirstAppFrame(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            for (StackTraceElement frame : current.getStackTrace()) {
                if (frame.getClassName().startsWith("com.mailsangja")) {
                    return frame.toString();
                }
            }
            current = current.getCause();
        }
        return null;
    }

    private String nullSafe(String value) {
        return value != null ? value : "unknown";
    }
}
