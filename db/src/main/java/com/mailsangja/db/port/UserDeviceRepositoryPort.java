package com.mailsangja.db.port;

import com.mailsangja.db.entity.user.UserDevice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeviceRepositoryPort {
    UserDevice save(UserDevice userDevice);
    Optional<UserDevice> findByFcmTokenAndDeletedAtIsNull(String fcmToken);
    List<UserDevice> findAllByMailAccountIdAndDeletedAtIsNull(UUID mailAccountId);
}
