package com.xpv.backend.dto;

import com.xpv.backend.model.TipoDestaque;

import java.util.List;

/**
 * Um destaque já resolvido para exibição: os dados dos jogos (nome, capa,
 * horas, conquistas) vêm sempre da biblioteca sincronizada atual, nunca de
 * um valor congelado no momento da escolha.
 */
public record DestaqueResponse(Long id, TipoDestaque tipo, int posicao, String plataforma,
                                Double horasTotais, List<JogoDestaqueResponse> jogos) {
}
