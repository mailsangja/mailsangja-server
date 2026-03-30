package com.mailsangja.core.service.user;

import com.mailsangja.core.common.exception.user.UserErrorCode;
import com.mailsangja.core.common.exception.user.UserException;
import com.mailsangja.core.dto.auth.RegisterRequest;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.entity.user.enums.Plan;
import com.mailsangja.db.entity.user.enums.Role;
import com.mailsangja.db.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserException(UserErrorCode.DUPLICATE_USERNAME);
        }

        User user = User.builder()
                .name(request.name())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .plan(Plan.FREE)
                .role(Role.USER)
                .creditUsage(0)
                .build();

        return userRepository.save(user);
    }
}
