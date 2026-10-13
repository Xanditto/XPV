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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Cobre a Issue #16 (recomendação de jogos): "Dê uma chance" são os jogos já
 * possuídos com 0 horas (no máximo 5), e "Recomendações" são sempre 20 jogos
 * não possuídos, escolhidos pelo gênero mais jogado do usuário e completados
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
    }

    private SteamSpyService.JogoSteamSpy fakeJogo(long appId, String nome, String genero, long positivas, long negativas) {
        return new SteamSpyService.JogoSteamSpy(appId, nome, List.of(genero), positivas, negativas);
    }

    @Test
    void deUmaChance_contemApenasJogosNuncaJogados_noMaximoCinco() {
        for (int i = 1; i <= 7; i++) {
            jogoRepository.save(new Jogo(contaSteam, (long) i, "Nunca Jogado " + i, "", 0.0, 0, 0));
        }
        jogoRepository.save(new Jogo(contaSteam, 100L, "Jogado", "", 10.0, 0, 0));

        RecomendacaoResponse resposta = recomendacaoService.recomendar(dono);

        assertThat(resposta.deUmaChance()).hasSize(5);
        assertThat(resposta.deUmaChance()).allSatisfy(jogo -> assertThat(jogo.horasJogadas()).isZero());
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
