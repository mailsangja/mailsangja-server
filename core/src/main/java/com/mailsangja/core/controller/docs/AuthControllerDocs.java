package com.mailsangja.core.controller.docs;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.dto.auth.LoginRequest;
import com.mailsangja.core.dto.auth.RegisterRequest;
import com.mailsangja.core.dto.auth.UserInfoResponse;
import com.mailsangja.db.entity.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

public interface AuthControllerDocs {

    ResponseEntity<UserInfoResponse> register(RegisterRequest request);

    ResponseEntity<UserInfoResponse> login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    ResponseEntity<UserInfoResponse> me(@AuthUser User user);
}
