package com.mailsangja.worker.service.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.mailsangja.worker.config.properties.FcmProperties;
import com.mailsangja.worker.dto.notification.NewMailPushContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushCommandService {

    private static final String DEFAULT_NOTIFICATION_TITLE = "새 메일이 도착했습니다";

    private final UserDeviceQueryService userDeviceQueryService;
    private final UserDeviceCommandService userDeviceCommandService;
    private final FirebaseApp firebaseApp;
    private final FcmProperties fcmProperties;

    public void sendNewMailPush(NewMailPushContext context) {
        List<String> tokens = userDeviceQueryService.findFcmTokensByMailAccountId(context.mailAccountId());
        if (tokens.isEmpty()) {
            return;
        }

        String title = StringUtils.hasText(context.subject()) ? context.subject() : DEFAULT_NOTIFICATION_TITLE;
        String threadDetailUrl = buildThreadDetailUrl(context);

        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                .putData("title", title)
                .putData("body", context.snippet() != null ? context.snippet() : "")
                .putData("mailAccountId", context.mailAccountId().toString())
                .putData("alias", context.alias())
                .putData("threadId", context.threadId() != null ? context.threadId().toString() : "")
                .putData("messageId", context.messageId() != null ? context.messageId().toString() : "")
                .putData("threadDetailUrl", threadDetailUrl)
                .addAllTokens(tokens);
        if (StringUtils.hasText(fcmProperties.getLogoImageUrl())) {
            messageBuilder.putData("image", fcmProperties.getLogoImageUrl());
        }

        MulticastMessage message = messageBuilder.build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance(firebaseApp).sendEachForMulticast(message);

            if (response.getFailureCount() > 0) {
                expireUnregisteredTokens(tokens, response);
            }

            log.info(
                    "FCM push sent: mailAccountId={} successCount={} failureCount={}",
                    context.mailAccountId(),
                    response.getSuccessCount(),
                    response.getFailureCount()
            );
        } catch (FirebaseMessagingException e) {
            log.warn("FCM push failed: mailAccountId={} error={}", context.mailAccountId(), e.getMessage());
        }
    }

    private String buildThreadDetailUrl(NewMailPushContext context) {
        String template = fcmProperties.getThreadDetailUrlTemplate();
        if (!StringUtils.hasText(template) || context.threadId() == null) {
            return "";
        }
        return template
                .replace("{mailAccountId}", context.mailAccountId().toString())
                .replace("{threadId}", context.threadId().toString())
                .replace("{messageId}", context.messageId() != null ? context.messageId().toString() : "");
    }

    private void expireUnregisteredTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) {
                continue;
            }

            FirebaseMessagingException exception = sendResponse.getException();
            MessagingErrorCode errorCode = exception != null ? exception.getMessagingErrorCode() : null;
            String token = tokens.get(i);
            String maskedToken = maskToken(token);

            if (errorCode == MessagingErrorCode.UNREGISTERED) {
                userDeviceCommandService.expireFcmToken(token);
                log.info("Expired FCM token soft-deleted: token={}", maskedToken);
            } else {
                log.warn("FCM token send failed: token={} errorCode={} message={}",
                        maskedToken,
                        errorCode,
                        exception != null ? exception.getMessage() : "unknown");
            }
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 12) {
            return "***";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}
