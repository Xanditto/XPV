package com.xpv.backend.dto;

import com.xpv.backend.model.Jogo;

/** Dados de um jogo resolvidos a partir da biblioteca atual, para exibir dentro de um destaque. */
public record JogoDestaqueResponse(Long appId, String nome, String imagem, double horasJogadas,
                                    int conquistasObtidas, int conquistasTotais) {
    public static JogoDestaqueResponse from(Jogo jogo) {
        return new JogoDestaqueResponse(jogo.getAppId(), jogo.getNome(), jogo.getImagem(), jogo.getHorasJogadas(),
                jogo.getConquistasObtidas(), jogo.getConquistasTotais());
    }
}
