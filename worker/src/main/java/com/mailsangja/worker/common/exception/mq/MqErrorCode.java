package com.mailsangja.worker.common.exception.mq;

import com.mailsangja.worker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MqErrorCode implements ErrorCode {

    INVALID_RABBITMQ_QUEUE_TTL(500, "MS-MQ-INVALID-RABBITMQ-QUEUE-TTL", "RabbitMQ Queue TTL 설정이 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
