package com.xpv.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DefinirSenhaRequest(
        @NotBlank @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres") String senha) {
}
