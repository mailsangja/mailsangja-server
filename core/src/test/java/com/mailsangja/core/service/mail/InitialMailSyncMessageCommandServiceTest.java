package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.config.properties.InitialMailSyncRabbitProperties;
import com.mailsangja.core.config.properties.MailTaskRabbitProperties;
import com.mailsangja.core.dto.mail.InitialMailSyncMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;

@ExtendWith(MockitoExtension.class)
@DisplayName("InitialMailSyncMessageCommandService 테스트")
class InitialMailSyncMessageCommandServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private InitialMailSyncRabbitProperties initialMailSyncRabbitProperties;

    @Mock
    private MailTaskRabbitProperties mailTaskRabbitProperties;

    @InjectMocks
    private InitialMailSyncMessageCommandService initialMailSyncMessageCommandService;

    @Nested
    @DisplayName("초기 메일 동기화 발행")
    class Publish {

        @Test
        @DisplayName("유효한 메시지면 exchange와 routing key로 발행한다")
        void publish_유효한메시지면Exchange와RoutingKey로발행한다() {
            // given
            InitialMailSyncMessage message = createMessage("GMAIL");
            willReturn("initial.mail.sync").given(initialMailSyncRabbitProperties).getTaskName();
            willReturn("mail.exchange").given(mailTaskRabbitProperties).getExchange();
            willReturn("mail.sync.initial").given(initialMailSyncRabbitProperties).getRoutingKey();

            // when
            initialMailSyncMessageCommandService.publish(message);

            // then
            ArgumentCaptor<CorrelationData> captor = ArgumentCaptor.forClass(CorrelationData.class);
            then(rabbitTemplate).should().convertAndSend(
                    eq("mail.exchange"),
                    eq("mail.sync.initial"),
                    eq(message),
                    captor.capture()
            );
            assertEquals(message.mailAccountId().toString(), captor.getValue().getId());
        }

        @Test
        @DisplayName("메시지가 Gmail 계정이 아니면 예외를 반환한다")
        void publish_메시지가Gmail계정이아니면예외를반환한다() {
            // given
            InitialMailSyncMessage message = createMessage("OUTLOOK");

            // when
            MailAccountException exception = assertThrows(
                    MailAccountException.class,
                    () -> initialMailSyncMessageCommandService.publish(message)
            );

            // then
            assertEquals("MS-MAIL-INVALID-INITIAL-MAIL-SYNC-MESSAGE", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("발행 중 AMQP 예외가 발생해도 전파하지 않는다")
        void publish_발행중Amqp예외가발생해도전파하지않는다() {
            // given
            InitialMailSyncMessage message = createMessage("GMAIL");
            willReturn("initial.mail.sync").given(initialMailSyncRabbitProperties).getTaskName();
            willReturn("mail.exchange").given(mailTaskRabbitProperties).getExchange();
            willReturn("mail.sync.initial").given(initialMailSyncRabbitProperties).getRoutingKey();
            doThrow(new AmqpException("failed"))
                    .when(rabbitTemplate)
                    .convertAndSend(eq("mail.exchange"), eq("mail.sync.initial"), eq(message), any(CorrelationData.class));

            // when
            initialMailSyncMessageCommandService.publish(message);

            // then
            then(rabbitTemplate).should().convertAndSend(
                    eq("mail.exchange"),
                    eq("mail.sync.initial"),
                    eq(message),
                    any(CorrelationData.class)
            );
        }
    }

    private InitialMailSyncMessage createMessage(String provider) {
        return new InitialMailSyncMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                provider,
                "user@gmail.com"
        );
    }
}
