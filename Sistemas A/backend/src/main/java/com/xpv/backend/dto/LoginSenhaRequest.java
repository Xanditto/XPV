package com.xpv.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginSenhaRequest(@NotBlank String identificador, @NotBlank String senha) {
}
