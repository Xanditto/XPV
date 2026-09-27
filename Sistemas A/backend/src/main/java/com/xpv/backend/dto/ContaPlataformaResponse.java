package com.xpv.backend.dto;

import com.xpv.backend.model.ContaPlataforma;

public record ContaPlataformaResponse(
        Long id,
        String plataforma,
        String identificador,
        String nickname,
        String avatar,
        Boolean perfilPublico) {

    public static ContaPlataformaResponse from(ContaPlataforma conta) {
        return new ContaPlataformaResponse(
                conta.getId(),
                conta.getPlataforma().getNome(),
                conta.getIdentificador(),
                conta.getNickname(),
                conta.getAvatar(),
                conta.getPerfilPublico());
    }
}
