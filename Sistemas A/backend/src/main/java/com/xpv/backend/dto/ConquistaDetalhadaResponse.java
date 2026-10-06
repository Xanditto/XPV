package com.xpv.backend.dto;

import com.xpv.backend.service.SteamService;

/** Uma conquista já obtida pelo usuário num jogo - usado para montar a lista de escolha do destaque CONQUISTAS_ESPECIFICAS. */
public record ConquistaDetalhadaResponse(String chave, String nome, String descricao, String icone) {
    public static ConquistaDetalhadaResponse from(SteamService.ConquistaDetalhada conquista) {
        return new ConquistaDetalhadaResponse(conquista.chave(), conquista.nome(), conquista.descricao(), conquista.icone());
    }
}
