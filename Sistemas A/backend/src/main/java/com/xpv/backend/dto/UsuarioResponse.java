package com.xpv.backend.dto;

import com.xpv.backend.model.Usuario;

public record UsuarioResponse(Long id, String email, String nickname, String avatar) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getNickname(), usuario.getAvatar());
    }
}
