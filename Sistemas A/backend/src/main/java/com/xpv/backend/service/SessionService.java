package com.xpv.backend.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessão simples em memória para o MVP da semana 1.
 * Sem expiração/refresh ainda — será substituída por um mecanismo
 * mais robusto (JWT próprio ou Spring Security) em semanas futuras.
 */
@Service
public class SessionService {

    private final Map<String, Long> sessoes = new ConcurrentHashMap<>();

    public String criarSessao(Long usuarioId) {
        String token = UUID.randomUUID().toString();
        sessoes.put(token, usuarioId);
        return token;
    }

    public Optional<Long> resolverUsuarioId(String token) {
        return Optional.ofNullable(sessoes.get(token));
    }
}
