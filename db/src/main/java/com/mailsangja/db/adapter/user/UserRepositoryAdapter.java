package com.mailsangja.db.adapter.user;

import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.module.user.UserJpaRepositoryModule;
import com.mailsangja.db.port.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepositoryModule userJpaRepositoryModule;

    @Override
    public User save(User user) {
        return userJpaRepositoryModule.save(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepositoryModule.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userJpaRepositoryModule.findByUsername(username);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepositoryModule.existsByUsername(username);
    }
}
