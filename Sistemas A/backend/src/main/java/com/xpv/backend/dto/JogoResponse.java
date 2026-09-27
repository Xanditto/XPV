package com.xpv.backend.dto;

import com.xpv.backend.model.Jogo;

public record JogoResponse(Long appId, String nome, String imagem, double horasJogadas,
                            int conquistasObtidas, int conquistasTotais) {

    public static JogoResponse from(Jogo jogo) {
        return new JogoResponse(jogo.getAppId(), jogo.getNome(), jogo.getImagem(), jogo.getHorasJogadas(),
                jogo.getConquistasObtidas(), jogo.getConquistasTotais());
    }
}
