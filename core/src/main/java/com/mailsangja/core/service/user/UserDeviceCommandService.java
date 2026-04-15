package com.mailsangja.core.service.user;

import com.mailsangja.core.common.exception.fcm.FcmTokenErrorCode;
import com.mailsangja.core.common.exception.fcm.FcmTokenException;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.entity.user.UserDevice;
import com.mailsangja.db.port.UserDeviceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDeviceCommandService {

    private final UserDeviceRepositoryPort userDeviceRepositoryPort;

    @Transactional
    public void registerFcmToken(User user, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            throw new FcmTokenException(FcmTokenErrorCode.INVALID_FCM_TOKEN);
        }

        Optional<UserDevice> existing = userDeviceRepositoryPort.findByFcmTokenAndDeletedAtIsNull(fcmToken);

        if (existing.isPresent()) {
            UserDevice device = existing.get();
            if (device.getUser().getId().equals(user.getId())) {
                // 이미 동일 사용자에게 등록된 토큰 → 멱등 처리
                return;
            }
            // 다른 사용자 소유 토큰 → 소유권 해제 후 재등록
            device.delete();
            userDeviceRepositoryPort.save(device);
        }

        userDeviceRepositoryPort.save(UserDevice.builder()
                .user(user)
                .fcmToken(fcmToken)
                .build());
    }
}
