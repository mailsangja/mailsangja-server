package com.mailsangja.worker.service.notification;

import com.mailsangja.db.port.UserDeviceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDeviceCommandService {

    private final UserDeviceRepositoryPort userDeviceRepositoryPort;

    @Transactional
    public void expireFcmToken(String fcmToken) {
        userDeviceRepositoryPort.findByFcmTokenAndDeletedAtIsNull(fcmToken)
                .ifPresent(device -> {
                    device.delete();
                    userDeviceRepositoryPort.save(device);
                });
    }
}
