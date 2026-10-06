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
    MAIS_JOGADOS,
    /** Uma imagem enviada pelo próprio usuário. */
    IMAGEM_PERSONALIZADA,
    /** Top 3 plataformas vinculadas por horas jogadas - calculado automaticamente, sem seleção. */
    TEMPO_POR_PLATAFORMA,
    /** Um texto livre escrito pelo usuário. */
    CAIXA_TEXTO,
    /** De 1 a 20 conquistas específicas (de jogos diferentes) escolhidas pelo usuário entre as que já obteve. */
    CONQUISTAS_ESPECIFICAS
}
