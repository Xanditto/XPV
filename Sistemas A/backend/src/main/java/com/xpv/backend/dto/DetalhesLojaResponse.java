package com.xpv.backend.dto;

import com.xpv.backend.service.SteamService;

import java.util.List;

/** Ficha da loja da Steam de um jogo - descrição, gêneros, desenvolvedora/publicadora, data de lançamento, nota do Metacritic, categorias, capturas de tela e preço. */
public record DetalhesLojaResponse(String descricao, List<SteamService.Genero> generos, List<String> desenvolvedoras,
                                    List<String> publicadoras, String dataLancamento, Integer notaMetacritic,
                                    List<SteamService.Categoria> categorias, List<SteamService.Captura> capturas,
                                    boolean gratuito, String precoFormatado, boolean disponivelParaCompra) {

    public static DetalhesLojaResponse from(SteamService.DetalhesLoja detalhes) {
        return new DetalhesLojaResponse(detalhes.descricao(), detalhes.generos(), detalhes.desenvolvedoras(),
                detalhes.publicadoras(), detalhes.dataLancamento(), detalhes.notaMetacritic(),
                detalhes.categorias(), detalhes.capturas(), detalhes.gratuito(), detalhes.precoFormatado(),
                detalhes.disponivelParaCompra());
    }
}
