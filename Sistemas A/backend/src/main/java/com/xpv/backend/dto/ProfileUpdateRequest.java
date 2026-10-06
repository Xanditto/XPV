package com.xpv.backend.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(min = 3, max = 30, message = "O nickname deve ter entre 3 e 30 caracteres") String nickname,
        String avatar,
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres") String descricao) {
}
