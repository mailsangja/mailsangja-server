package com.mailsangja.core.dto.auth;

import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "사용자 정보 응답")
public record UserInfoResponse(
        @Schema(description = "사용자 고유 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "사용자 이름", example = "홍길동")
        String name,

        @Schema(description = "로그인 아이디", example = "hong@mailsangja.com")
        String username,

        @Schema(description = "구독 플랜 (FREE, BASIC, PREMIUM)")
        Plan plan,

        @Schema(description = "크레딧 사용량", example = "0")
        int creditUsage
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getPlan(),
                user.getCreditUsage()
        );
    }
}
