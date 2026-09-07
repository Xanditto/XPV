package com.xpv.backend.dto;

public record AuthResponse(String sessionToken, UsuarioResponse usuario) {
}
