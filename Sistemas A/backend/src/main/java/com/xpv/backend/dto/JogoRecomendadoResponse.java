package com.xpv.backend.dto;

/** Um jogo recomendado que o usuário ainda não possui (ver RecomendacaoService). */
public record JogoRecomendadoResponse(Long appId, String nome) {
}
