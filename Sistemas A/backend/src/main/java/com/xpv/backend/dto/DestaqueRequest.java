package com.xpv.backend.dto;

import com.xpv.backend.model.TipoDestaque;

import java.util.List;

/**
 * Um destaque desejado, dentro da lista enviada em PUT /api/destaques (a
 * lista inteira substitui os destaques atuais do usuário). imagem é usada só
 * por IMAGEM_PERSONALIZADA e texto só por CAIXA_TEXTO.
 */
public record DestaqueRequest(TipoDestaque tipo, String plataforma, List<DestaqueJogoRequest> jogos,
                               String imagem, String texto) {
}
