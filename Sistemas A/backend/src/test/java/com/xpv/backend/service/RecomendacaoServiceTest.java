package com.xpv.backend.service;

import com.xpv.backend.dto.RecomendacaoResponse;
import com.xpv.backend.model.ContaPlataforma;
import com.xpv.backend.model.Jogo;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.ContaPlataformaRepository;
import com.xpv.backend.repository.JogoRepository;
import com.xpv.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Cobre a Issue #16 (recomendação de jogos): "Dê uma chance" prioriza até 5
 * gêneros diferentes entre os jogos nunca jogados, e "Recomendações" são
 * sempre 20 jogos não possuídos, escolhidos pelo gênero mais jogado do
 * usuário, confirmados como disponíveis pra compra na loja da Steam (um
 * jogo pode ter sido removido de venda, como o Rocket League) e completados
 * com os mais populares da Steam quando o gênero não tem candidatos suficientes.
 */
@SpringBootTest
class RecomendacaoServiceTest {

    @Autowired
    RecomendacaoService recomendacaoService;

    @Autowired
    ContaPlataformaService contaPlataformaService;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    ContaPlataformaRepository contaPlataformaRepository;

    @Autowired
    JogoRepository jogoRepository;

    @MockitoBean
    SteamSpyService steamSpyService;

    @MockitoBean
    SteamService steamService;

    Usuario dono;
    ContaPlataforma contaSteam;

    @BeforeEach
    void configurar() {
        jogoRepository.deleteAllInBatch();
        contaPlataformaRepository.deleteAll();
        usuarioRepository.deleteAll();
        dono = usuarioRepository.save(new Usuario("google-sub-reco", "reco@gmail.com", "DonoReco", null));
        contaSteam = contaPlataformaService.vincularSteam(dono,
                new SteamService.PerfilSteam("76561198000000299", "Nick", "", true, false));

        when(steamSpyService.buscarPorGenero(anyString())).thenReturn(List.of());
        when(steamSpyService.top100Forever()).thenReturn(List.of());
        // Por padrão, qualquer jogo consultado na loja está disponível - os testes que querem
        // simular indisponibilidade (ex.: Rocket League) sobrescrevem isso pro appId específico.
        when(steamService.buscarDetalhesLoja(anyLong())).thenReturn(disponivel(80, false, "R$ 59,90"));
    }

    private SteamSpyService.JogoSteamSpy fakeJogo(long appId, String nome, String genero, long positivas, long negativas) {
        return new SteamSpyService.JogoSteamSpy(appId, nome, List.of(genero), positivas, negativas);
    }

    private SteamService.DetalhesLoja disponivel(Integer metacritic, boolean gratuito, String preco) {
        return new SteamService.DetalhesLoja("", List.of(), List.of(), List.of(), "", metacritic, List.of(), List.of(),
                gratuito, preco, true);
    }

    private SteamService.DetalhesLoja indisponivel() {
        return new SteamService.DetalhesLoja("", List.of(), List.of(), List.of(), "", null, List.of(), List.of(),
                false, null, false);
    }

    @Test
    void deUmaChance_contemApenasJogosNuncaJogados_noMaximoCinco() {
        for (int i = 1; i <= 7; i++) {
            jogoRepository.save(new Jogo(contaSteam, (long) i, "Nunca Jogado " + i, "", 0.0, 0, 0));
            when(steamSpyService.buscarDetalhes((long) i)).thenReturn(fakeJogo(i, "Nunca Jogado " + i, "genero" + i, 100, 1));
        }
        jogoRepository.save(new Jogo(contaSteam, 100L, "Jogado", "", 10.0, 0, 0));

        RecomendacaoResponse resposta = recomendacaoService.recomendar(dono);

        assertThat(resposta.deUmaChance()).hasSize(5);
        assertThat(resposta.deUmaChance()).allSatisfy(jogo -> assertThat(jogo.horasJogadas()).isZero());
    }

    @Test
    void deUmaChance_prioriza5GenerosDiferentesQuandoHouverMaisJogosQueSlots() {
        // 7 jogos nunca jogados, mas so 3 generos distintos entre eles (4x indie, 2x rpg, 1x strategy).
        long[] idsIndie = {1L, 2L, 3L, 4L};
        for (long id : idsIndie) {
            jogoRepository.save(new Jogo(contaSteam, id, "Indie " + id, "", 0.0, 0, 0));
            when(steamSpyService.buscarDetalhes(id)).thenReturn(fakeJogo(id, "Indie " + id, "indie", 100, 1));
        }
        jogoRepository.save(new Jogo(contaSteam, 5L, "Rpg A", "", 0.0, 0, 0));
        when(steamSpyService.buscarDetalhes(5L)).thenReturn(fakeJogo(5L, "Rpg A", "rpg", 100, 1));
        jogoRepository.save(new Jogo(contaSteam, 6L, "Rpg B", "", 0.0, 0, 0));
        when(steamSpyService.buscarDetalhes(6L)).thenReturn(fakeJogo(6L, "Rpg B", "rpg", 100, 1));
        jogoRepository.save(new Jogo(contaSteam, 7L, "Strategy A", "", 0.0, 0, 0));
        when(steamSpyService.buscarDetalhes(7L)).thenReturn(fakeJogo(7L, "Strategy A", "strategy", 100, 1));

        RecomendacaoResponse resposta = recomendacaoService.recomendar(dono);

        // So ha 3 generos distintos entre os 7 jogos nunca jogados (indie/rpg/strategy) - a
        // primeira passada garante um de cada antes de completar os 2 slots restantes com
        // o que sobrar, então os 5 escolhidos sempre cobrem os 3 generos, independente da
        // ordem em que o banco devolve as linhas.
        assertThat(resposta.deUmaChance()).hasSize(5);
        Map<Long, String> generoPorAppId = Map.of(
                1L, "indie", 2L, "indie", 3L, "indie", 4L, "indie",
                5L, "rpg", 6L, "rpg",
                7L, "strategy");
        Set<String> generosEscolhidos = resposta.deUmaChance().stream()
                .map(jogo -> generoPorAppId.get(jogo.appId()))
                .collect(java.util.stream.Collectors.toSet());
        assertThat(generosEscolhidos).containsExactlyInAnyOrder("indie", "rpg", "strategy");
    }

    @Test
    void recomendacoes_naoIncluiJogosQueOUsuarioJaPossui() {
        jogoRepository.save(new Jogo(contaSteam, 100L, "Jogado", "", 50.0, 0, 0));
        when(steamSpyService.buscarDetalhes(100L)).thenReturn(fakeJogo(100L, "Jogado", "action", 1000, 10));
        when(steamSpyService.buscarPorGenero("action")).thenReturn(List.of(
                fakeJogo(100L, "Jogado", "action", 1000, 10),
                fakeJogo(200L, "Novo Jogo", "action", 900, 10)));

        RecomendacaoResponse resposta = recomendacaoService.recomendar(dono);

        assertThat(resposta.recomendacoes()).extracting("appId").doesNotContain(100L);
        assertThat(resposta.recomendacoes()).extracting("appId").contains(200L);
    }

    @Test
    void recomendacoes_descartaJogosComPoucasOuMasAvaliacoes() {
        jogoRepository.save(new Jogo(contaSteam, 100L, "Jogado", "", 50.0, 0, 0));
        when(steamSpyService.buscarDetalhes(100L)).thenReturn(fakeJogo(100L, "Jogado", "action", 1000, 10));
        when(steamSpyService.buscarPorGenero("action")).thenReturn(List.of(
                fakeJogo(200L, "Poucas Avaliacoes", "action", 5, 0),
                fakeJogo(300L, "Muitas Negativas", "action", 40, 60),
                fakeJogo(400L, "Jogo Bom", "action", 900, 10)));

        RecomendacaoResponse resposta = recomendacaoService.recomendar(dono);

        assertThat(resposta.recomendacoes()).extracting("appId").doesNotContain(200L, 300L);
        assertThat(resposta.recomendacoes()).extracting("appId").contains(400L);
    }

    @Test
    void recomendacoes_descartaJogoRemovidoDeVendaMesmoComBoasAvaliacoes() {
        jogoRepository.save(new Jogo(contaSteam, 100L, "Jogado", "", 50.0, 0, 0));
        when(steamSpyService.buscarDetalhes(100L)).thenReturn(fakeJogo(100L, "Jogado", "action", 1000, 10));
        when(steamSpyService.buscarPorGenero("action")).thenReturn(List.of(
                fakeJogo(252950L, "Rocket League - fora de venda", "action", 900, 10),
                fakeJogo(400L, "Jogo Disponivel", "action", 800, 10)));
        when(steamService.buscarDetalhesLoja(252950L)).thenReturn(indisponivel());

        RecomendacaoResponse resposta = recomendacaoService.recomendar(dono);

        assertThat(resposta.recomendacoes()).extracting("appId").doesNotContain(252950L);
        assertThat(resposta.recomendacoes()).extracting("appId").contains(400L);
    }

    @Test
    void recomendacoes_trazemNotaMetacriticEPreco() {
        jogoRepository.save(new Jogo(contaSteam, 100L, "Jogado", "", 50.0, 0, 0));
        when(steamSpyService.buscarDetalhes(100L)).thenReturn(fakeJogo(100L, "Jogado", "action", 1000, 10));
        when(steamSpyService.buscarPorGenero("action")).thenReturn(List.of(
                fakeJogo(400L, "Jogo Bom", "action", 900, 10)));
        when(steamService.buscarDetalhesLoja(400L)).thenReturn(disponivel(90, false, "R$ 39,90"));

        RecomendacaoResponse resposta = recomendacaoService.recomendar(dono);

        var recomendado = resposta.recomendacoes().stream().filter(j -> j.appId().equals(400L)).findFirst().orElseThrow();
        assertThat(recomendado.notaMetacritic()).isEqualTo(90);
        assertThat(recomendado.precoFormatado()).isEqualTo("R$ 39,90");
        assertThat(recomendado.gratuito()).isFalse();
    }

    @Test
    void recomendacoes_completaComTop100QuandoGeneroNaoTemCandidatosSuficientes() {
        jogoRepository.save(new Jogo(contaSteam, 100L, "Jogado", "", 50.0, 0, 0));
        when(steamSpyService.buscarDetalhes(100L)).thenReturn(fakeJogo(100L, "Jogado", "action", 1000, 10));
        when(steamSpyService.buscarPorGenero("action")).thenReturn(List.of(
                fakeJogo(200L, "Unico Candidato Do Genero", "action", 900, 10)));
        List<SteamSpyService.JogoSteamSpy> top100 = new java.util.ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            top100.add(fakeJogo(1000L + i, "Popular " + i, "variado", 500, 5));
        }
        when(steamSpyService.top100Forever()).thenReturn(top100);

        RecomendacaoResponse resposta = recomendacaoService.recomendar(dono);

        assertThat(resposta.recomendacoes()).hasSize(20);
        assertThat(resposta.recomendacoes()).extracting("appId").contains(200L);
    }

    @Test
    void recomendacoes_semHistoricoDeJogo_usaGeneroPadraoAction() {
        when(steamSpyService.buscarPorGenero(eq("action"))).thenReturn(List.of(
                fakeJogo(500L, "Jogo De Acao", "action", 900, 10)));

        recomendacaoService.recomendar(dono);

        org.mockito.Mockito.verify(steamSpyService).buscarPorGenero("action");
    }
}
