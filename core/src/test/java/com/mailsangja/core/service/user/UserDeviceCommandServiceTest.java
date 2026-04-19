package com.mailsangja.core.service.user;

import com.mailsangja.core.common.exception.fcm.FcmTokenException;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.entity.user.UserDevice;
import com.mailsangja.db.port.UserDeviceRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeviceCommandServiceTest {

    @Mock
    private UserDeviceRepositoryPort userDeviceRepositoryPort;

    @InjectMocks
    private UserDeviceCommandService userDeviceCommandService;

    @Test
    void registerFcmToken_같은사용자같은토큰은멱등하게추가저장하지않는다() {
        User user = createUser("same-user");
        String fcmToken = "token-123";
        UserDevice existingDevice = UserDevice.builder()
                .id(UUID.randomUUID())
                .user(user)
                .fcmToken(fcmToken)
                .build();

        when(userDeviceRepositoryPort.findByFcmTokenAndDeletedAtIsNull(fcmToken))
                .thenReturn(Optional.of(existingDevice));

        userDeviceCommandService.registerFcmToken(user, fcmToken);

        verify(userDeviceRepositoryPort, never()).save(any(UserDevice.class));
    }

    @Test
    void registerFcmToken_다른사용자의같은토큰은기존토큰을삭제후현재사용자로재등록한다() {
        User oldUser = createUser("old-user");
        User newUser = createUser("new-user");
        String fcmToken = "token-456";
        UserDevice existingDevice = UserDevice.builder()
                .id(UUID.randomUUID())
                .user(oldUser)
                .fcmToken(fcmToken)
                .build();

        when(userDeviceRepositoryPort.findByFcmTokenAndDeletedAtIsNull(fcmToken))
                .thenReturn(Optional.of(existingDevice));

        userDeviceCommandService.registerFcmToken(newUser, fcmToken);

        verify(userDeviceRepositoryPort, times(2)).save(any(UserDevice.class));
        assertTrue(existingDevice.isDeleted());
    }

    @Test
    void registerFcmToken_빈토큰이면예외를던진다() {
        User user = createUser("invalid-user");

        assertThrows(FcmTokenException.class, () -> userDeviceCommandService.registerFcmToken(user, " "));
        verify(userDeviceRepositoryPort, never()).save(any(UserDevice.class));
    }

    private User createUser(String username) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("tester")
                .username(username + "@example.com")
                .password("encoded-password")
                .plan(Plan.FREE)
                .role(Role.USER)
                .creditUsage(0)
                .build();
    }
}
