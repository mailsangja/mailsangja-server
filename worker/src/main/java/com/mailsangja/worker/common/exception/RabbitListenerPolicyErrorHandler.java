package com.mailsangja.worker.common.exception;

import com.rabbitmq.client.Channel;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component("rabbitListenerPolicyErrorHandler")
public class RabbitListenerPolicyErrorHandler implements RabbitListenerErrorHandler {

    private final Map<ErrorCode, ErrorHandlingPolicy> errorPolicies = Map.of(
            MailPushErrorCode.GOOGLE_REFRESH_TOKEN_MISSING, ErrorHandlingPolicy.ACK
    );

    @Override
    public Object handleError(
            Message amqpMessage,
            Channel channel,
            org.springframework.messaging.Message<?> message,
            ListenerExecutionFailedException exception
    ) throws Exception {
        BaseException baseException = findBaseException(exception);
        ErrorHandlingPolicy policy = getPolicy(baseException);

        switch (policy) {
            case ACK -> {
                log.warn(
                        "Skipping Rabbit message by error policy. queue={} messageId={} errorCode={} policy={}",
                        amqpMessage.getMessageProperties().getConsumerQueue(),
                        amqpMessage.getMessageProperties().getMessageId(),
                        baseException.getErrorCode().getCode(),
                        policy
                );
                return null;
            }
            case RETHROW -> throw exception;
        }

        throw exception;
    }

    private ErrorHandlingPolicy getPolicy(BaseException baseException) {
        if (baseException == null) {
            return ErrorHandlingPolicy.RETHROW;
        }
        return errorPolicies.getOrDefault(baseException.getErrorCode(), ErrorHandlingPolicy.RETHROW);
    }

    private BaseException findBaseException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof BaseException baseException) {
                return baseException;
            }
            current = current.getCause();
        }
        return null;
    }

    private enum ErrorHandlingPolicy {
        ACK,
        RETHROW
    }
}
