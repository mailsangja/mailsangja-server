package com.mailsangja.worker.service.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
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

        Notification.Builder notificationBuilder = Notification.builder()
                .setTitle(title)
                .setBody(context.snippet());
        if (StringUtils.hasText(fcmProperties.getLogoImageUrl())) {
            notificationBuilder.setImage(fcmProperties.getLogoImageUrl());
        }

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(notificationBuilder.build())
                .putData("mailAccountId", context.mailAccountId().toString())
                .putData("alias", context.alias())
                .putData("gmailThreadId", context.gmailThreadId())
                .putData("messageId", context.messageId() != null ? context.messageId().toString() : "")
                .putData("threadDetailUrl", threadDetailUrl)
                .addAllTokens(tokens)
                .build();

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
        if (!StringUtils.hasText(template)) {
            return "";
        }
        return template
                .replace("{mailAccountId}", context.mailAccountId().toString())
                .replace("{gmailThreadId}", context.gmailThreadId());
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

            if (errorCode == MessagingErrorCode.UNREGISTERED) {
                userDeviceCommandService.expireFcmToken(token);
                log.info("Expired FCM token soft-deleted: token={}...{}", token.substring(0, 8), token.substring(token.length() - 4));
            } else {
                log.warn("FCM token send failed: token={}...{} errorCode={} message={}",
                        token.substring(0, 8), token.substring(token.length() - 4),
                        errorCode,
                        exception != null ? exception.getMessage() : "unknown");
            }
        }
    }
}
