package com.xpv.backend.dto;

public record AuthResponse(String sessionToken, boolean precisaDefinirSenha, UsuarioResponse usuario) {
}
