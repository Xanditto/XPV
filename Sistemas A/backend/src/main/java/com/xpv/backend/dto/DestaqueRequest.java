package com.xpv.backend.dto;

import com.xpv.backend.model.TipoDestaque;

import java.util.List;

/**
 * Um destaque desejado, dentro da lista enviada em PUT /api/destaques (a
 * lista inteira substitui os destaques atuais do usuário - no máximo 2).
 */
public record DestaqueRequest(TipoDestaque tipo, String plataforma, List<DestaqueJogoRequest> jogos) {
}
