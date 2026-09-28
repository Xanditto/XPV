package com.xpv.backend.dto;

/** Um jogo escolhido para um destaque, identificado pela conta vinculada e pelo appId da Steam. */
public record DestaqueJogoRequest(Long contaPlataformaId, Long appId) {
}
