package com.mailsangja.core.dto.inbox;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "읽지 않은 수신 스레드 수 응답")
public record UnreadCountResponse(
        @Schema(description = "읽지 않은 수신 스레드 수", example = "5")
        long unreadCount
) {

    public static UnreadCountResponse of(long unreadCount) {
        return new UnreadCountResponse(unreadCount);
    }
}
