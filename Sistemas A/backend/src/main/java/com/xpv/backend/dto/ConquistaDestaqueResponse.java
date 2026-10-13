package com.xpv.backend.dto;

/**
 * Uma conquista específica escolhida para exibir no perfil - usado por
 * CONQUISTAS_ESPECIFICAS. conquistaChave vai junto (além do nome/ícone só
 * para exibição) porque o front-end precisa dela para poder reconstruir o
 * pedido original ao salvar uma lista de destaques atualizada.
 */
public record ConquistaDestaqueResponse(Long appId, String jogoNome, String conquistaChave,
                                         String conquistaNome, String conquistaIcone) {
}
