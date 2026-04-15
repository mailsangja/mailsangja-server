package com.mailsangja.db.adapter.user;

import com.mailsangja.db.entity.user.UserDevice;
import com.mailsangja.db.module.user.UserDeviceJpaRepositoryModule;
import com.mailsangja.db.port.UserDeviceRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserDeviceRepositoryAdapter implements UserDeviceRepositoryPort {

    private final UserDeviceJpaRepositoryModule userDeviceJpaRepositoryModule;

    @Override
    public UserDevice save(UserDevice userDevice) {
        return userDeviceJpaRepositoryModule.save(userDevice);
    }

    @Override
    public Optional<UserDevice> findByFcmTokenAndDeletedAtIsNull(String fcmToken) {
        return userDeviceJpaRepositoryModule.findByFcmTokenAndDeletedAtIsNull(fcmToken);
    }

    @Override
    public List<UserDevice> findAllByMailAccountIdAndDeletedAtIsNull(UUID mailAccountId) {
        return userDeviceJpaRepositoryModule.findAllByMailAccountIdAndDeletedAtIsNull(mailAccountId);
    }
}
