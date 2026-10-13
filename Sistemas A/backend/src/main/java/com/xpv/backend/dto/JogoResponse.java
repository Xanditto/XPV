package com.xpv.backend.dto;

import com.xpv.backend.model.Jogo;

import java.time.Instant;

public record JogoResponse(Long appId, String nome, String imagem, double horasJogadas,
                            int conquistasObtidas, int conquistasTotais, Instant ultimoAcesso,
                            double horasWindows, double horasMac, double horasLinux, double horasDeck) {

    public static JogoResponse from(Jogo jogo) {
        return new JogoResponse(jogo.getAppId(), jogo.getNome(), jogo.getImagem(), jogo.getHorasJogadas(),
                jogo.getConquistasObtidas(), jogo.getConquistasTotais(), jogo.getUltimoAcesso(),
                jogo.getHorasWindows(), jogo.getHorasMac(), jogo.getHorasLinux(), jogo.getHorasDeck());
    }
}
