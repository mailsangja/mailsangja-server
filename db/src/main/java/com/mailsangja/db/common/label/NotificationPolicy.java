package com.mailsangja.db.common.label;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 정책 (URGENT: 즉시 알림 / INHERIT: 기본 동작(알림 전송) / SILENT: 알림 안 함)")
public enum NotificationPolicy {
    @Schema(description = "매칭 라벨 중 하나라도 URGENT면 반드시 알림 전송")
    URGENT,

    @Schema(description = "URGENT/SILENT가 없을 때 기본적으로 알림 전송")
    INHERIT,

    @Schema(description = "매칭 라벨 중 SILENT가 있으면 알림 전송 안 함 (URGENT보다 낮은 우선순위)")
    SILENT
}
