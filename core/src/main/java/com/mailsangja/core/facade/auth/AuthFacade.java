package com.mailsangja.core.facade.auth;

import com.mailsangja.core.common.exception.user.UserErrorCode;
import com.mailsangja.core.common.exception.user.UserException;
import com.mailsangja.core.dto.auth.LoginRequest;
import com.mailsangja.core.dto.auth.RegisterRequest;
import com.mailsangja.core.dto.auth.UserInfoResponse;
import com.mailsangja.core.service.user.UserCommandService;
import com.mailsangja.core.service.user.UserQueryService;
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
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade {

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

            return UserInfoResponse.from(
                    userQueryService.findByUsername(request.username())
            );
        } catch (BadCredentialsException e) {
            throw new UserException(UserErrorCode.INVALID_CREDENTIALS);
        }
    }

}
