package com.xpv.backend.dto;

import java.util.List;

/**
 * "Dê uma chance": jogos que o usuário já tem na biblioteca mas nunca jogou
 * (no máximo 5). "Recomendações": sempre 20 jogos que o usuário não possui,
 * escolhidos por gênero (ver RecomendacaoService).
 */
public record RecomendacaoResponse(List<JogoResponse> deUmaChance, List<JogoRecomendadoResponse> recomendacoes) {
}
