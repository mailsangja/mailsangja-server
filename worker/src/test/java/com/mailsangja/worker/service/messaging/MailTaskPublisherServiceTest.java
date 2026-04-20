package com.mailsangja.worker.service.messaging;

import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.common.exception.mq.MqException;
import com.mailsangja.worker.config.properties.InitialMailSyncRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.config.properties.WatchRenewalRabbitProperties;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadBatchMessage;
import com.mailsangja.worker.dto.mail.watch.WatchRenewalMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailTaskPublisherService 테스트")
class MailTaskPublisherServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Nested
    @DisplayName("publishInitialMailSyncThreadBatch")
    class PublishInitialMailSyncThreadBatch {

        @Test
        @DisplayName("유효한 메시지와 프로퍼티면 스레드 배치 메시지를 발행한다")
        void publishInitialMailSyncThreadBatch_유효한메시지와프로퍼티면스레드배치메시지를발행한다() {
            // given
            MailTaskPublisherService service = new MailTaskPublisherService(
                    rabbitTemplate,
                    createMailTaskProperties(),
                    createWatchRenewalProperties(),
                    createInitialMailSyncProperties()
            );
            InitialMailSyncThreadBatchMessage message = new InitialMailSyncThreadBatchMessage(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "GMAIL",
                    "user@gmail.com",
                    List.of("thread-1", "thread-2")
            );

            // when
            service.publishInitialMailSyncThreadBatch(message);

            // then
            then(rabbitTemplate).should().convertAndSend(
                    eq("mail-task.exchange"),
                    eq("mail.initial-thread-batch"),
                    eq(message),
                    any(CorrelationData.class)
            );
        }

        @Test
        @DisplayName("threadId가 비어 있으면 초기 동기화 메시지 예외를 반환한다")
        void publishInitialMailSyncThreadBatch_threadId가비어있으면초기동기화메시지예외를반환한다() {
            // given
            MailTaskPublisherService service = new MailTaskPublisherService(
                    rabbitTemplate,
                    createMailTaskProperties(),
                    createWatchRenewalProperties(),
                    createInitialMailSyncProperties()
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.publishInitialMailSyncThreadBatch(new InitialMailSyncThreadBatchMessage(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "GMAIL",
                            "user@gmail.com",
                            List.of("thread-1", " ")
                    ))
            );

            // then
            assertEquals("MS-MAIL-INVALID-INITIAL-MAIL-SYNC-COMMAND", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("프로퍼티가 비어 있으면 MQ 예외를 반환한다")
        void publishInitialMailSyncThreadBatch_프로퍼티가비어있으면Mq예외를반환한다() {
            // given
            MailTaskRabbitProperties mailTaskProperties = new MailTaskRabbitProperties();
            mailTaskProperties.setExchange(" ");
            mailTaskProperties.setDeadLetterExchange("dead.exchange");
            mailTaskProperties.setRetryMaxAttempts(3);
            mailTaskProperties.setConcurrency(1);

            InitialMailSyncRabbitProperties initialMailSyncProperties = new InitialMailSyncRabbitProperties();
            initialMailSyncProperties.setThreadBatchTaskName("initial-thread-batch");

            MailTaskPublisherService service = new MailTaskPublisherService(
                    rabbitTemplate,
                    mailTaskProperties,
                    createWatchRenewalProperties(),
                    initialMailSyncProperties
            );

            // when
            MqException exception = assertThrows(
                    MqException.class,
                    () -> service.publishInitialMailSyncThreadBatch(new InitialMailSyncThreadBatchMessage(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "GMAIL",
                            "user@gmail.com",
                            List.of("thread-1")
                    ))
            );

            // then
            assertEquals("MS-MQ-INVALID-RABBITMQ-QUEUE-TTL", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("publishWatchRenewal")
    class PublishWatchRenewal {

        @Test
        @DisplayName("유효한 메시지와 프로퍼티면 watch 갱신 메시지를 발행한다")
        void publishWatchRenewal_유효한메시지와프로퍼티면Watch갱신메시지를발행한다() {
            // given
            MailTaskPublisherService service = new MailTaskPublisherService(
                    rabbitTemplate,
                    createMailTaskProperties(),
                    createWatchRenewalProperties(),
                    createInitialMailSyncProperties()
            );
            WatchRenewalMessage message = new WatchRenewalMessage(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "GMAIL",
                    "user@gmail.com"
            );

            // when
            service.publishWatchRenewal(message);

            // then
            then(rabbitTemplate).should().convertAndSend(
                    eq("mail-task.exchange"),
                    eq("mail.watch-renewal"),
                    eq(message),
                    any(CorrelationData.class)
            );
        }

        @Test
        @DisplayName("provider가 Gmail이 아니면 watch 갱신 예외를 반환한다")
        void publishWatchRenewal_provider가Gmail이아니면Watch갱신예외를반환한다() {
            // given
            MailTaskPublisherService service = new MailTaskPublisherService(
                    rabbitTemplate,
                    createMailTaskProperties(),
                    createWatchRenewalProperties(),
                    createInitialMailSyncProperties()
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.publishWatchRenewal(new WatchRenewalMessage(
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            "NAVER",
                            "user@naver.com"
                    ))
            );

            // then
            assertEquals("MS-MAIL-INVALID-GMAIL-WATCH-RENEWAL-REQUEST", exception.getErrorCode().getCode());
        }
    }

    private MailTaskRabbitProperties createMailTaskProperties() {
        MailTaskRabbitProperties properties = new MailTaskRabbitProperties();
        properties.setExchange("mail-task.exchange");
        properties.setDeadLetterExchange("mail-task.dead.exchange");
        properties.setRetryMaxAttempts(3);
        properties.setConcurrency(1);
        return properties;
    }

    private WatchRenewalRabbitProperties createWatchRenewalProperties() {
        WatchRenewalRabbitProperties properties = new WatchRenewalRabbitProperties();
        properties.setTaskName("watch-renewal");
        return properties;
    }

    private InitialMailSyncRabbitProperties createInitialMailSyncProperties() {
        InitialMailSyncRabbitProperties properties = new InitialMailSyncRabbitProperties();
        properties.setTaskName("initial-mail-sync");
        properties.setThreadBatchTaskName("initial-thread-batch");
        return properties;
    }
}
