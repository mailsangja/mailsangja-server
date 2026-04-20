package com.mailsangja.db.module.user;

import com.mailsangja.db.entity.user.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeviceJpaRepositoryModule extends JpaRepository<UserDevice, UUID> {

    Optional<UserDevice> findByFcmTokenAndDeletedAtIsNull(String fcmToken);

    @Query("""
            SELECT ud FROM UserDevice ud
            WHERE ud.user.id = (
                SELECT ma.user.id FROM MailAccount ma
                WHERE ma.id = :mailAccountId AND ma.active = true AND ma.deletedAt IS NULL
            )
            AND ud.deletedAt IS NULL
            """)
    List<UserDevice> findAllByMailAccountIdAndDeletedAtIsNull(@Param("mailAccountId") UUID mailAccountId);
}
