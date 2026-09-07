package com.xpv.backend.controller;

import com.xpv.backend.dto.AuthResponse;
import com.xpv.backend.dto.GoogleLoginRequest;
import com.xpv.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public AuthResponse loginComGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginComGoogle(request.idToken());
    }
}
