package com.xpv.backend.dto;

/**
 * Um jogo escolhido para um destaque, identificado pela conta vinculada e
 * pelo appId da Steam. conquistaChave é usada só em CONQUISTAS_ESPECIFICAS,
 * para identificar qual conquista daquele jogo foi escolhida.
 */
public record DestaqueJogoRequest(Long contaPlataformaId, Long appId, String conquistaChave) {
}
