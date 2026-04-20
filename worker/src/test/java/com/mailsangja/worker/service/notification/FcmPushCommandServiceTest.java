package com.mailsangja.worker.service.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.mailsangja.worker.config.properties.FcmProperties;
import com.mailsangja.worker.dto.notification.NewMailPushContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmPushCommandService 테스트")
class FcmPushCommandServiceTest {

    @Mock
    private UserDeviceQueryService userDeviceQueryService;

    @Mock
    private UserDeviceCommandService userDeviceCommandService;

    @Mock
    private FirebaseApp firebaseApp;

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private BatchResponse batchResponse;

    @Mock
    private SendResponse successResponse;

    @Mock
    private SendResponse failureResponse;

    @Mock
    private FirebaseMessagingException firebaseMessagingException;

    @Nested
    @DisplayName("sendNewMailPush")
    class SendNewMailPush {

        @Test
        @DisplayName("FCM 토큰이 없으면 푸시를 전송하지 않는다")
        void sendNewMailPush_fcm토큰이없으면푸시를전송하지않는다() {
            // given
            FcmPushCommandService service = createService("https://cdn/logo.png", "/mail/{mailAccountId}/threads/{threadId}?messageId={messageId}");
            NewMailPushContext context = createContext("subject", "snippet");
            given(userDeviceQueryService.findFcmTokensByMailAccountId(context.mailAccountId())).willReturn(List.of());

            // when then
            assertDoesNotThrow(() -> service.sendNewMailPush(context));
        }

        @Test
        @DisplayName("미등록 토큰 응답이 오면 해당 토큰을 만료 처리한다")
        void sendNewMailPush_미등록토큰응답이오면해당토큰을만료처리한다() throws Exception {
            // given
            FcmPushCommandService service = createService("https://cdn/logo.png", "/mail/{mailAccountId}/threads/{threadId}?messageId={messageId}");
            NewMailPushContext context = createContext("", "snippet");
            List<String> tokens = List.of("12345678tokenAAAA", "12345678tokenBBBB");
            given(userDeviceQueryService.findFcmTokensByMailAccountId(context.mailAccountId())).willReturn(tokens);
            given(batchResponse.getFailureCount()).willReturn(1);
            given(batchResponse.getSuccessCount()).willReturn(1);
            given(batchResponse.getResponses()).willReturn(List.of(successResponse, failureResponse));
            given(successResponse.isSuccessful()).willReturn(true);
            given(failureResponse.isSuccessful()).willReturn(false);
            given(failureResponse.getException()).willReturn(firebaseMessagingException);
            given(firebaseMessagingException.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

            try (MockedStatic<FirebaseMessaging> mockedStatic = mockStatic(FirebaseMessaging.class)) {
                mockedStatic.when(() -> FirebaseMessaging.getInstance(firebaseApp)).thenReturn(firebaseMessaging);

                // when
                service.sendNewMailPush(context);

                // then
                then(userDeviceCommandService).should().expireFcmToken("12345678tokenBBBB");
            }
        }

        @Test
        @DisplayName("FirebaseMessagingException이 발생해도 예외를 전파하지 않는다")
        void sendNewMailPush_firebaseMessagingException이발생해도예외를전파하지않는다() throws Exception {
            // given
            FcmPushCommandService service = createService(null, "/mail/{mailAccountId}/threads/{threadId}");
            NewMailPushContext context = createContext("subject", "snippet");
            List<String> tokens = List.of("12345678tokenAAAA");
            given(userDeviceQueryService.findFcmTokensByMailAccountId(context.mailAccountId())).willReturn(tokens);
            given(firebaseMessaging.sendEachForMulticast(any())).willThrow(firebaseMessagingException);

            try (MockedStatic<FirebaseMessaging> mockedStatic = mockStatic(FirebaseMessaging.class)) {
                mockedStatic.when(() -> FirebaseMessaging.getInstance(firebaseApp)).thenReturn(firebaseMessaging);

                // when then
                assertDoesNotThrow(() -> service.sendNewMailPush(context));
            }
        }
    }

    private FcmPushCommandService createService(String logoImageUrl, String template) {
        FcmProperties properties = new FcmProperties();
        properties.setLogoImageUrl(logoImageUrl);
        properties.setThreadDetailUrlTemplate(template);
        return new FcmPushCommandService(userDeviceQueryService, userDeviceCommandService, firebaseApp, properties);
    }

    private NewMailPushContext createContext(String subject, String snippet) {
        return new NewMailPushContext(
                UUID.randomUUID(),
                "alias",
                subject,
                snippet,
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}
