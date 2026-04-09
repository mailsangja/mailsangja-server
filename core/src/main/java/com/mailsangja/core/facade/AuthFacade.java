package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.user.UserErrorCode;
import com.mailsangja.core.common.exception.user.UserException;
import com.mailsangja.core.dto.auth.LoginRequest;
import com.mailsangja.core.dto.auth.RegisterRequest;
import com.mailsangja.core.dto.auth.UserInfoResponse;
import com.mailsangja.core.service.user.UserCommandService;
import com.mailsangja.core.service.user.UserQueryService;
import com.mailsangja.db.entity.user.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade {

    private static final String SESSION_COOKIE_NAME = "SESSION";

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public UserInfoResponse register(RegisterRequest request) {
        User user = userCommandService.register(request);
        return UserInfoResponse.from(user);
    }

    public UserInfoResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);

            return UserInfoResponse.from(userQueryService.findByUsername(request.username()));
        } catch (BadCredentialsException e) {
            throw new UserException(UserErrorCode.INVALID_CREDENTIALS);
        }
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, authentication);

        Cookie expiredSessionCookie = new Cookie(SESSION_COOKIE_NAME, "");
        expiredSessionCookie.setPath("/");
        expiredSessionCookie.setMaxAge(0);
        expiredSessionCookie.setSecure(true);
        expiredSessionCookie.setHttpOnly(true);
        httpResponse.addCookie(expiredSessionCookie);
    }
}
