package com.mailsangja.core.service.user;

import com.mailsangja.core.common.exception.fcm.FcmTokenException;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.entity.user.UserDevice;
import com.mailsangja.db.port.UserDeviceRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDeviceCommandService 테스트")
class UserDeviceCommandServiceTest {

    @Mock
    private UserDeviceRepositoryPort userDeviceRepositoryPort;

    @InjectMocks
    private UserDeviceCommandService userDeviceCommandService;

    @Nested
    @DisplayName("registerFcmToken")
    class RegisterFcmToken {

        @Test
        @DisplayName("같은 사용자의 같은 토큰이면 저장하지 않는다")
        void registerFcmToken_같은사용자의같은토큰이면저장하지않는다() {
            // given
            User user = createUser("same-user");
            String fcmToken = "token-123";
            UserDevice existingDevice = UserDevice.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .fcmToken(fcmToken)
                    .build();

            given(userDeviceRepositoryPort.findByFcmTokenAndDeletedAtIsNull(fcmToken))
                    .willReturn(Optional.of(existingDevice));

            // when
            userDeviceCommandService.registerFcmToken(user, fcmToken);

            // then
            then(userDeviceRepositoryPort).should(never()).save(any(UserDevice.class));
        }

        @Test
        @DisplayName("다른 사용자의 같은 토큰이면 기존 토큰을 삭제하고 재등록한다")
        void registerFcmToken_다른사용자의같은토큰이면기존토큰을삭제하고재등록한다() {
            // given
            User oldUser = createUser("old-user");
            User newUser = createUser("new-user");
            String fcmToken = "token-456";
            UserDevice existingDevice = UserDevice.builder()
                    .id(UUID.randomUUID())
                    .user(oldUser)
                    .fcmToken(fcmToken)
                    .build();

            given(userDeviceRepositoryPort.findByFcmTokenAndDeletedAtIsNull(fcmToken))
                    .willReturn(Optional.of(existingDevice));

            // when
            userDeviceCommandService.registerFcmToken(newUser, fcmToken);

            // then
            then(userDeviceRepositoryPort).should(times(2)).save(any(UserDevice.class));
            assertTrue(existingDevice.isDeleted());
        }

        @Test
        @DisplayName("빈 토큰이면 예외를 반환한다")
        void registerFcmToken_빈토큰이면예외를반환한다() {
            // given
            User user = createUser("invalid-user");

            // when then
            assertThrows(FcmTokenException.class, () -> userDeviceCommandService.registerFcmToken(user, " "));
            then(userDeviceRepositoryPort).should(never()).save(any(UserDevice.class));
        }
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
