package com.mailsangja.core.dto.fcm;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FCM 토큰 등록 요청")
public record RegisterFcmTokenRequest(
        @Schema(description = "FCM 디바이스 토큰 값", example = "dkjfhaksdjfhaksjdfh...")
        String fcmToken
) { }
