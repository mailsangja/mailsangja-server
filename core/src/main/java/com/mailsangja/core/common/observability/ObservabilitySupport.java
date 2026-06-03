package com.mailsangja.core.common.observability;

import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ObservabilitySupport {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String WORK_ID = "workId";
    public static final String USER_ID = "userId";
    public static final String MAIL_ACCOUNT_ID = "mailAccountId";
    public static final String JOB_ID = "jobId";
    public static final String GMAIL_THREAD_ID = "gmailThreadId";
    public static final String GMAIL_MESSAGE_ID = "gmailMessageId";
    public static final String HISTORY_ID = "historyId";
    public static final String EVENT_TYPE = "eventType";
    public static final String ROUTING_KEY = "routingKey";
    public static final String QUEUE = "queue";

    private static final List<String> PROPAGATED_KEYS = List.of(
            WORK_ID,
            USER_ID,
            MAIL_ACCOUNT_ID,
            JOB_ID,
            GMAIL_THREAD_ID,
            GMAIL_MESSAGE_ID,
            HISTORY_ID,
            EVENT_TYPE
    );

    private static final List<String> PAYLOAD_KEYS = List.of(
            USER_ID,
            MAIL_ACCOUNT_ID,
            JOB_ID,
            GMAIL_THREAD_ID,
            GMAIL_MESSAGE_ID,
            HISTORY_ID,
            EVENT_TYPE
    );

    public Scope openScope(Map<String, ?> values) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        putAll(values);
        return new Scope(previousContext);
    }

    public Scope openRabbitScope(Message rawMessage, Object payload) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.putAll(extractHeaders(rawMessage));
        values.putAll(extractPayloadValues(payload));
        values.put(ROUTING_KEY, rawMessage.getMessageProperties().getReceivedRoutingKey());
        values.put(QUEUE, rawMessage.getMessageProperties().getConsumerQueue());
        values.putIfAbsent(WORK_ID, currentWorkIdOrNew());
        return openScope(values);
    }

    public MessagePostProcessor rabbitHeaders() {
        return message -> {
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            for (String key : PROPAGATED_KEYS) {
                String value = MDC.get(key);
                if (!isBlank(value)) {
                    headers.put(key, value);
                }
            }
            return message;
        };
    }

    public String currentWorkIdOrNew() {
        String workId = MDC.get(WORK_ID);
        if (!isBlank(workId)) {
            return workId;
        }
        return UUID.randomUUID().toString();
    }

    private Map<String, String> extractHeaders(Message rawMessage) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, Object> headers = rawMessage.getMessageProperties().getHeaders();
        for (String key : PROPAGATED_KEYS) {
            Object value = headers.get(key);
            if (value != null) {
                values.put(key, toMdcValue(value));
            }
        }
        return values;
    }

    private Map<String, String> extractPayloadValues(Object payload) {
        Map<String, String> values = new LinkedHashMap<>();
        if (payload == null) {
            return values;
        }

        Class<?> payloadType = payload.getClass();
        for (String key : PAYLOAD_KEYS) {
            try {
                Method method = payloadType.getMethod(key);
                if (method.getParameterCount() == 0) {
                    Object value = method.invoke(payload);
                    if (value != null) {
                        values.put(key, toMdcValue(value));
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // Payloads are heterogeneous records; missing observability fields are expected.
            }
        }
        return values;
    }

    private void putAll(Map<String, ?> values) {
        if (values == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isAllowedKey(key) && value != null) {
                String mdcValue = toMdcValue(value);
                if (!isBlank(mdcValue)) {
                    MDC.put(key, mdcValue);
                }
            }
        }
    }

    private boolean isAllowedKey(String key) {
        return PROPAGATED_KEYS.contains(key) || ROUTING_KEY.equals(key) || QUEUE.equals(key);
    }

    private String toMdcValue(Object value) {
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static final class Scope implements AutoCloseable {

        private final Map<String, String> previousContext;

        private Scope(Map<String, String> previousContext) {
            this.previousContext = previousContext;
        }

        @Override
        public void close() {
            if (previousContext == null) {
                MDC.clear();
                return;
            }
            MDC.setContextMap(previousContext);
        }
    }
}
