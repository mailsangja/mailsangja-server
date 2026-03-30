package com.mailsangja.core.dto.auth;

import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.entity.user.enums.Plan;

import java.util.UUID;

public record UserInfoResponse(
        UUID id,
        String name,
        String username,
        Plan plan,
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
