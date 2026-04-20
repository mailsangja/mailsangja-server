package com.mailsangja.core.facade;

import com.mailsangja.core.dto.fcm.RegisterFcmTokenRequest;
import com.mailsangja.core.service.user.UserDeviceCommandService;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FcmFacade {

    private final UserDeviceCommandService userDeviceCommandService;

    public void registerFcmToken(User user, RegisterFcmTokenRequest request) {
        userDeviceCommandService.registerFcmToken(user, request.fcmToken());
    }
}
