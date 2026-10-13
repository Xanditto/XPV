package com.xpv.backend.controller;

import com.xpv.backend.dto.ProfileUpdateRequest;
import com.xpv.backend.dto.UsuarioResponse;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.UsuarioRepository;
import com.xpv.backend.service.CurrentUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final CurrentUserResolver currentUserResolver;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public UsuarioResponse obterPerfil(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        return UsuarioResponse.from(usuario);
    }

    @PutMapping
    public UsuarioResponse atualizarPerfil(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                            @Valid @RequestBody ProfileUpdateRequest request) {
        Usuario usuario = currentUserResolver.resolver(authorization);

        if (request.nickname() != null && !request.nickname().isBlank()) {
            usuario.setNickname(request.nickname());
        }
        if (request.avatar() != null && !request.avatar().isBlank()) {
            usuario.setAvatar(request.avatar());
        }
        if (request.descricao() != null) {
            usuario.setDescricao(request.descricao());
        }

        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }
}
