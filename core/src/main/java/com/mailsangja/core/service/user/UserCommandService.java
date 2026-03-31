package com.mailsangja.core.service.user;

import com.mailsangja.core.common.exception.user.UserErrorCode;
import com.mailsangja.core.common.exception.user.UserException;
import com.mailsangja.core.dto.auth.RegisterRequest;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.mailsangja.db.entity.user.Plan.FREE;
import static com.mailsangja.db.entity.user.Role.USER;

@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepositoryPort.existsByUsername(request.username())) {
            throw new UserException(UserErrorCode.DUPLICATE_USERNAME);
        }

        User user = User.builder()
                .name(request.name())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .plan(FREE)
                .role(USER)
                .creditUsage(0)
                .build();

        return userRepositoryPort.save(user);
    }

    @Transactional
    public void updateDefaultAccount(User user, UUID mailAccountId) {
        user.updateDefaultAccount(mailAccountId);
        userRepositoryPort.save(user);
    }
}
