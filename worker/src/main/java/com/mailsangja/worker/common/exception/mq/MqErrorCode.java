package com.mailsangja.worker.common.exception.mq;

import com.mailsangja.worker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MqErrorCode implements ErrorCode {

    INVALID_RABBITMQ_QUEUE_TTL(500, "MS-MQ-INVALID-RABBITMQ-QUEUE-TTL", "RabbitMQ Queue TTL 설정이 올바르지 않습니다."),
    INVALID_RABBITMQ_TASK_NAME(500, "MS-MQ-INVALID-RABBITMQ-TASK-NAME", "RabbitMQ Task Name 설정이 올바르지 않습니다."),
    INVALID_LABEL_RECLASSIFY_MESSAGE(400, "MS-MQ-INVALID-LABEL-RECLASSIFY-MESSAGE", "라벨 재분류 메시지 값이 올바르지 않습니다."),
    INVALID_REPLY_DRAFT_SUGGESTION_MESSAGE(400, "MS-MQ-INVALID-REPLY-DRAFT-SUGGESTION-MESSAGE", "답장 초안 추천 메시지 값이 올바르지 않습니다."),
    INVALID_LABEL_RECLASSIFY_CONFIG(500, "MS-MQ-INVALID-LABEL-RECLASSIFY-CONFIG", "라벨 재분류 배치 설정 값이 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
