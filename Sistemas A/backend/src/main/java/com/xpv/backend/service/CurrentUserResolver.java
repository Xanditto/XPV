package com.xpv.backend.service;

import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final SessionService sessionService;
    private final UsuarioRepository usuarioRepository;

    public Usuario resolver(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão ausente ou inválida");
        }
        String token = authorizationHeader.substring("Bearer ".length());
        Long usuarioId = sessionService.resolverUsuarioId(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão expirada, faça login novamente"));
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
}
