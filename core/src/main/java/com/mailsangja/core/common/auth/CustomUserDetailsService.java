package com.mailsangja.core.common.auth;

import com.mailsangja.core.service.user.UserQueryService;
import com.mailsangja.db.entity.user.User;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserQueryService userQueryService;

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        User user = userQueryService.findByUsername(username);
        return new CustomUserDetails(user);
    }
}
