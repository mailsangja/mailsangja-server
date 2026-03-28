package com.mailsangja.core.common.auth;

import com.mailsangja.core.common.exception.ErrorResponse;
import com.mailsangja.core.common.exception.auth.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@Slf4j(topic = "UNAUTHORIZED_EXCEPTION_HANDLER")
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull AuthenticationException authenticationException) throws IOException {
        log.error("Not Authenticated Request", authenticationException);

        ErrorResponse errorResponse = ErrorResponse.from(AuthErrorCode.UNAUTHORIZED);
        String responseBody = objectMapper.writeValueAsString(errorResponse);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(AuthErrorCode.UNAUTHORIZED.getStatus());
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(responseBody);
    }
}
