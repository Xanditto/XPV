package com.xpv.backend.dto;

import com.xpv.backend.model.Avaliacao;

import java.time.Instant;

/** Avaliação resolvida com o nome/imagem atuais do jogo (ver Avaliacao). */
public record AvaliacaoResponse(Long id, Long contaPlataformaId, Long appId, String jogoNome, String jogoImagem,
                                 double nota, String resenha, Instant dataCriacao) {

    public static AvaliacaoResponse from(Avaliacao avaliacao, String jogoNome, String jogoImagem) {
        return new AvaliacaoResponse(avaliacao.getId(), avaliacao.getContaPlataformaId(), avaliacao.getAppId(),
                jogoNome, jogoImagem, avaliacao.getNota(), avaliacao.getResenha(), avaliacao.getDataCriacao());
    }
}
