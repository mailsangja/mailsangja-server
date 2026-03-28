package com.mailsangja.core.common.auth;

import com.mailsangja.core.common.exception.auth.AuthErrorCode;
import com.mailsangja.core.common.exception.auth.AuthException;
import com.mailsangja.core.service.user.UserQueryService;
import com.mailsangja.db.entity.user.User;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static com.mailsangja.db.entity.user.Role.ADMIN;

@Component
@RequiredArgsConstructor
public class AuthArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserQueryService userQueryService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class)
                || parameter.hasParameterAnnotation(AuthAdmin.class)
                && parameter.getParameterType().isAssignableFrom(User.class);
    }

    @Override
    public User resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer,
                                @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED);
        }

        String username = authentication.getName();
        User user = userQueryService.findByUsername(username);

        if (parameter.hasParameterAnnotation(AuthAdmin.class) && user.getRole() != ADMIN) {
            throw new AuthException(AuthErrorCode.FORBIDDEN_ROLE);
        }

        return user;
    }
}
