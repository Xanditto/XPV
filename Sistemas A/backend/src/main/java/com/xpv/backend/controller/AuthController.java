package com.xpv.backend.controller;

import com.xpv.backend.dto.AuthResponse;
import com.xpv.backend.dto.DefinirSenhaRequest;
import com.xpv.backend.dto.GoogleLoginRequest;
import com.xpv.backend.dto.LoginSenhaRequest;
import com.xpv.backend.dto.UsuarioResponse;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.service.AuthService;
import com.xpv.backend.service.CurrentUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserResolver currentUserResolver;

    /** Login das próximas vezes: e-mail ou nickname + senha. */
    @PostMapping("/login")
    public AuthResponse loginComSenha(@Valid @RequestBody LoginSenhaRequest request) {
        return authService.loginComSenha(request.identificador(), request.senha());
    }

    /** Login da primeira vez (ou enquanto a senha não foi definida). */
    @PostMapping("/google")
    public AuthResponse loginComGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginComGoogle(request.idToken());
    }

    /** Define a senha logo após o primeiro login com Google. */
    @PostMapping("/senha")
    public UsuarioResponse definirSenha(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                         @Valid @RequestBody DefinirSenhaRequest request) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        return authService.definirSenha(usuario.getId(), request.senha());
    }
}
