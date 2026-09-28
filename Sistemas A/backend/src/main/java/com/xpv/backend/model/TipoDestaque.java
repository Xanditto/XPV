package com.xpv.backend.model;

/** Os tipos de destaque de perfil que o usuário pode escolher (máximo 2 por vez, ver Destaque). */
public enum TipoDestaque {
    /** Soma das horas jogadas em todos os jogos de uma plataforma (ex.: Steam). */
    HORAS_PLATAFORMA,
    /** Um jogo escolhido pelo usuário, com capa, horas jogadas e conquistas. */
    JOGO_FAVORITO,
    /** De 2 a 6 jogos escolhidos pelo usuário entre os que já estão 100% platinados. */
    PERFECCIONISTA,
    /** Os 3 jogos com mais horas jogadas - calculado automaticamente, sem seleção. */
    MAIS_JOGADOS
}
