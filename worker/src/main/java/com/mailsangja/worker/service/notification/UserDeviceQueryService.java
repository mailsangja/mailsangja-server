package com.mailsangja.worker.service.notification;

import com.mailsangja.db.entity.user.UserDevice;
import com.mailsangja.db.port.UserDeviceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDeviceQueryService {

    private final UserDeviceRepositoryPort userDeviceRepositoryPort;

    public List<String> findFcmTokensByMailAccountId(UUID mailAccountId) {
        return userDeviceRepositoryPort.findAllByMailAccountIdAndDeletedAtIsNull(mailAccountId)
                .stream()
                .map(UserDevice::getFcmToken)
                .toList();
    }
}
