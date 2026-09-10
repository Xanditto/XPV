package com.xpv.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record RiotContaRequest(@NotBlank String riotId) {
}
