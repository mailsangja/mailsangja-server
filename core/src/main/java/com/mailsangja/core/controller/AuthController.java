package com.mailsangja.core.controller;

import com.mailsangja.core.controller.docs.AuthControllerDocs;
import com.mailsangja.core.dto.auth.LoginRequest;
import com.mailsangja.core.dto.auth.RegisterRequest;
import com.mailsangja.core.dto.auth.UserInfoResponse;
import com.mailsangja.core.facade.AuthFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthFacade authFacade;

    @Override
    @PostMapping("/api/v1/auth/register")
    public ResponseEntity<UserInfoResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authFacade.register(request));
    }

    @Override
    @PostMapping("/api/v1/auth/login")
    public ResponseEntity<UserInfoResponse> login(@RequestBody LoginRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse httpResponse) {
        return ResponseEntity.ok(authFacade.login(request, httpRequest, httpResponse));
    }
}
