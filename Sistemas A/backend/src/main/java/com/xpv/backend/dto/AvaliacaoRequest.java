package com.xpv.backend.dto;

public record AvaliacaoRequest(Long contaPlataformaId, Long appId, Double nota, String resenha) {
}
