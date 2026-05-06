package com.mailsangja.db.port;

import com.mailsangja.db.entity.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByIdWithLock(UUID id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
