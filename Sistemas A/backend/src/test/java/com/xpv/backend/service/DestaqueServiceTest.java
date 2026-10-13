package com.xpv.backend.service;

import com.xpv.backend.dto.DestaqueJogoRequest;
import com.xpv.backend.dto.DestaqueRequest;
import com.xpv.backend.dto.DestaqueResponse;
import com.xpv.backend.model.ContaPlataforma;
import com.xpv.backend.model.Jogo;
import com.xpv.backend.model.TipoDestaque;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.ContaPlataformaRepository;
import com.xpv.backend.repository.DestaqueJogoRepository;
import com.xpv.backend.repository.DestaqueRepository;
import com.xpv.backend.repository.JogoRepository;
import com.xpv.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Cobre a Issue #10 (personalização de perfil / destaques): os 8 tipos de
 * destaque, o limite por usuário, e o fato de que os destaques com jogos
 * escolhidos (favorito/perfeccionista/conquistas específicas) resolvem os
 * dados sempre pela biblioteca atual - sobrevivendo a uma ressincronização
 * que apaga e recria as linhas de Jogo com ids novos.
 */
@SpringBootTest
class DestaqueServiceTest {

    @Autowired
    DestaqueService destaqueService;

    @Autowired
    ContaPlataformaService contaPlataformaService;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    ContaPlataformaRepository contaPlataformaRepository;

    @Autowired
    JogoRepository jogoRepository;

    @Autowired
    DestaqueRepository destaqueRepository;

    @Autowired
    DestaqueJogoRepository destaqueJogoRepository;

    @MockitoBean
    SteamService steamService;

    Usuario dono;
    ContaPlataforma contaSteam;

    @BeforeEach
    void configurar() {
        destaqueJogoRepository.deleteAllInBatch();
        destaqueRepository.deleteAllInBatch();
        jogoRepository.deleteAllInBatch();
        contaPlataformaRepository.deleteAll();
        usuarioRepository.deleteAll();
        dono = usuarioRepository.save(new Usuario("google-sub-destaques", "destaques@gmail.com", "DonoDestaques", null));
        contaSteam = contaPlataformaService.vincularSteam(dono,
                new SteamService.PerfilSteam("76561198000000099", "Nick", "", true, false));
        jogoRepository.save(new Jogo(contaSteam, 10L, "Jogo Pouco Jogado", "", 2.0, 1, 10));
        jogoRepository.save(new Jogo(contaSteam, 20L, "Jogo Muito Jogado", "", 100.0, 10, 10));
        jogoRepository.save(new Jogo(contaSteam, 30L, "Jogo Medio", "", 50.0, 5, 10));
        jogoRepository.save(new Jogo(contaSteam, 40L, "Jogo Sem Conquistas", "", 1.0, 0, 0));
    }

    @Test
    void salvarHorasPlataforma_somaTodasAsHorasDaPlataforma() {
        var pedido = new DestaqueRequest(TipoDestaque.HORAS_PLATAFORMA, "Steam", null, null, null);

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        assertThat(destaques).hasSize(1);
        assertThat(destaques.get(0).tipo()).isEqualTo(TipoDestaque.HORAS_PLATAFORMA);
        assertThat(destaques.get(0).horasTotais()).isEqualTo(2.0 + 100.0 + 50.0 + 1.0);
    }

    @Test
    void salvarHorasPlataforma_semContaNaPlataforma_lancaErro() {
        var pedido = new DestaqueRequest(TipoDestaque.HORAS_PLATAFORMA, "Battle.net", null, null, null);

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(pedido)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarTempoPorPlataforma_mostraTop3PlataformasPorHoras() {
        contaPlataformaService.vincularRiot(dono, new RiotService.PerfilRiot("puuid-destaques-1", "Nick#BR1"));
        var pedido = new DestaqueRequest(TipoDestaque.TEMPO_POR_PLATAFORMA, null, null, null, null);

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        List<String> plataformas = destaques.get(0).temposPorPlataforma().stream()
                .map(t -> t.plataforma())
                .toList();
        assertThat(plataformas).containsExactly("Steam", "Riot Games");
        assertThat(destaques.get(0).temposPorPlataforma().get(0).horasTotais()).isEqualTo(153.0);
        assertThat(destaques.get(0).temposPorPlataforma().get(1).horasTotais()).isEqualTo(0.0);
    }

    @Test
    void salvarJogoFavorito_comUmJogo_resolveDadosDoJogo() {
        var pedido = new DestaqueRequest(TipoDestaque.JOGO_FAVORITO, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L, null)), null, null);

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        assertThat(destaques.get(0).jogos()).hasSize(1);
        assertThat(destaques.get(0).jogos().get(0).nome()).isEqualTo("Jogo Muito Jogado");
        assertThat(destaques.get(0).jogos().get(0).horasJogadas()).isEqualTo(100.0);
    }

    @Test
    void salvarJogoFavorito_comZeroOuMaisDeUmJogo_lancaErro() {
        var semJogo = new DestaqueRequest(TipoDestaque.JOGO_FAVORITO, null, List.of(), null, null);
        var doisJogos = new DestaqueRequest(TipoDestaque.JOGO_FAVORITO, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L, null), new DestaqueJogoRequest(contaSteam.getId(), 30L, null)),
                null, null);

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(semJogo))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(doisJogos))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarPerfeccionista_comJogosNaoPlatinados_lancaErro() {
        // "Jogo Muito Jogado" (10/10) é platina, "Jogo Medio" (5/10) não é.
        var pedido = new DestaqueRequest(TipoDestaque.PERFECCIONISTA, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L, null), new DestaqueJogoRequest(contaSteam.getId(), 30L, null)),
                null, null);

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(pedido)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarPerfeccionista_comMenosDeDoisOuMaisDeSeisJogos_lancaErro() {
        var umJogo = new DestaqueRequest(TipoDestaque.PERFECCIONISTA, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L, null)), null, null);

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(umJogo)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarPerfeccionista_comJogosPlatinados_resolveTodos() {
        jogoRepository.save(new Jogo(contaSteam, 50L, "Jogo Platina 2", "", 20.0, 3, 3));
        var pedido = new DestaqueRequest(TipoDestaque.PERFECCIONISTA, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L, null), new DestaqueJogoRequest(contaSteam.getId(), 50L, null)),
                null, null);

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        assertThat(destaques.get(0).jogos()).hasSize(2);
    }

    @Test
    void salvarMaisJogados_resolveTop3PorHoras() {
        var pedido = new DestaqueRequest(TipoDestaque.MAIS_JOGADOS, null, null, null, null);

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        List<String> nomes = destaques.get(0).jogos().stream().map(j -> j.nome()).toList();
        assertThat(nomes).containsExactly("Jogo Muito Jogado", "Jogo Medio", "Jogo Pouco Jogado");
    }

    @Test
    void salvarImagemPersonalizada_guardaEResolveAImagem() {
        var pedido = new DestaqueRequest(TipoDestaque.IMAGEM_PERSONALIZADA, null, null,
                "data:image/png;base64,ABC123", null);

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        assertThat(destaques.get(0).imagem()).isEqualTo("data:image/png;base64,ABC123");
    }

    @Test
    void salvarImagemPersonalizada_semImagem_lancaErro() {
        var pedido = new DestaqueRequest(TipoDestaque.IMAGEM_PERSONALIZADA, null, null, null, null);

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(pedido)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarCaixaTexto_guardaEResolveOTexto() {
        var pedido = new DestaqueRequest(TipoDestaque.CAIXA_TEXTO, null, null, null, "Gamer desde sempre!");

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        assertThat(destaques.get(0).texto()).isEqualTo("Gamer desde sempre!");
    }

    @Test
    void salvarCaixaTexto_vazioOuMuitoLongo_lancaErro() {
        var vazio = new DestaqueRequest(TipoDestaque.CAIXA_TEXTO, null, null, null, "");
        var longoDemais = new DestaqueRequest(TipoDestaque.CAIXA_TEXTO, null, null, null, "a".repeat(501));

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(vazio))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(longoDemais))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarConquistasEspecificas_comConquistaObtida_resolveNomeEIcone() {
        when(steamService.buscarConquistaPorChave(eq("76561198000000099"), eq(20L), eq("ACH_PRIMEIRA_VITORIA")))
                .thenReturn(new SteamService.ConquistaDetalhada(
                        "ACH_PRIMEIRA_VITORIA", "Primeira Vitória", "Vença sua primeira partida.", "https://icon"));

        var pedido = new DestaqueRequest(TipoDestaque.CONQUISTAS_ESPECIFICAS, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L, "ACH_PRIMEIRA_VITORIA")), null, null);

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        assertThat(destaques.get(0).conquistas()).hasSize(1);
        assertThat(destaques.get(0).conquistas().get(0).jogoNome()).isEqualTo("Jogo Muito Jogado");
        assertThat(destaques.get(0).conquistas().get(0).conquistaNome()).isEqualTo("Primeira Vitória");
        assertThat(destaques.get(0).conquistas().get(0).conquistaIcone()).isEqualTo("https://icon");
    }

    @Test
    void salvarConquistasEspecificas_comConquistaNaoObtida_lancaErro() {
        when(steamService.buscarConquistaPorChave(anyString(), anyLong(), anyString())).thenReturn(null);

        var pedido = new DestaqueRequest(TipoDestaque.CONQUISTAS_ESPECIFICAS, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L, "ACH_NUNCA_OBTIDA")), null, null);

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(pedido)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarConquistasEspecificas_comMaisDeVinte_lancaErro() {
        List<DestaqueJogoRequest> vinteEUma = java.util.stream.IntStream.range(0, 21)
                .mapToObj(i -> new DestaqueJogoRequest(contaSteam.getId(), 20L, "ACH_" + i))
                .toList();
        var pedido = new DestaqueRequest(TipoDestaque.CONQUISTAS_ESPECIFICAS, null, vinteEUma, null, null);

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(pedido)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarDoisDestaquesDoMesmoTipoComLimiteUm_lancaErro() {
        List<DestaqueRequest> dois = List.of(
                new DestaqueRequest(TipoDestaque.MAIS_JOGADOS, null, null, null, null),
                new DestaqueRequest(TipoDestaque.MAIS_JOGADOS, null, null, null, null));

        assertThatThrownBy(() -> destaqueService.salvar(dono, dois))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarTresCaixasDeTexto_permitido() {
        List<DestaqueRequest> tres = List.of(
                new DestaqueRequest(TipoDestaque.CAIXA_TEXTO, null, null, null, "Texto 1"),
                new DestaqueRequest(TipoDestaque.CAIXA_TEXTO, null, null, null, "Texto 2"),
                new DestaqueRequest(TipoDestaque.CAIXA_TEXTO, null, null, null, "Texto 3"));

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, tres);

        assertThat(destaques).hasSize(3);
    }

    @Test
    void salvarQuatroCaixasDeTexto_lancaErro() {
        List<DestaqueRequest> quatro = java.util.stream.IntStream.range(0, 4)
                .mapToObj(i -> new DestaqueRequest(TipoDestaque.CAIXA_TEXTO, null, null, null, "Texto " + i))
                .toList();

        assertThatThrownBy(() -> destaqueService.salvar(dono, quatro))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarDoisPerfeccionistas_permitido() {
        jogoRepository.save(new Jogo(contaSteam, 50L, "Jogo Platina 2", "", 20.0, 3, 3));
        jogoRepository.save(new Jogo(contaSteam, 60L, "Jogo Platina 3", "", 15.0, 2, 2));
        List<DestaqueRequest> dois = List.of(
                new DestaqueRequest(TipoDestaque.PERFECCIONISTA, null,
                        List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L, null), new DestaqueJogoRequest(contaSteam.getId(), 50L, null)),
                        null, null),
                new DestaqueRequest(TipoDestaque.PERFECCIONISTA, null,
                        List.of(new DestaqueJogoRequest(contaSteam.getId(), 60L, null), new DestaqueJogoRequest(contaSteam.getId(), 50L, null)),
                        null, null));

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, dois);

        assertThat(destaques).hasSize(2);
    }

    @Test
    void salvarTresPerfeccionistas_lancaErro() {
        jogoRepository.save(new Jogo(contaSteam, 50L, "Jogo Platina 2", "", 20.0, 3, 3));
        List<DestaqueJogoRequest> jogosValidos = List.of(
                new DestaqueJogoRequest(contaSteam.getId(), 20L, null), new DestaqueJogoRequest(contaSteam.getId(), 50L, null));
        List<DestaqueRequest> tres = java.util.stream.IntStream.range(0, 3)
                .mapToObj(i -> new DestaqueRequest(TipoDestaque.PERFECCIONISTA, null, jogosValidos, null, null))
                .toList();

        assertThatThrownBy(() -> destaqueService.salvar(dono, tres))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarSubstituiDestaquesAnteriores() {
        destaqueService.salvar(dono, List.of(new DestaqueRequest(TipoDestaque.MAIS_JOGADOS, null, null, null, null)));

        List<DestaqueResponse> destaques = destaqueService.salvar(dono,
                List.of(new DestaqueRequest(TipoDestaque.HORAS_PLATAFORMA, "Steam", null, null, null)));

        assertThat(destaques).hasSize(1);
        assertThat(destaques.get(0).tipo()).isEqualTo(TipoDestaque.HORAS_PLATAFORMA);
    }

    @Test
    void jogoFavorito_sobreviveAResincronizacaoQueTrocaOsIdsInternos() {
        var pedido = new DestaqueRequest(TipoDestaque.JOGO_FAVORITO, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L, null)), null, null);
        destaqueService.salvar(dono, List.of(pedido));

        // Simula uma ressincronização: apaga todas as linhas de Jogo e recria
        // com o mesmo appId, mas com um id interno novo (autoincrement). Usa
        // deleteAll (auto-transacional) em vez do @Modifying deleteByContaPlataformaId,
        // que exigiria uma transação já aberta no chamador.
        jogoRepository.deleteAll(jogoRepository.findByContaPlataformaId(contaSteam.getId()));
        jogoRepository.save(new Jogo(contaSteam, 20L, "Jogo Muito Jogado", "", 120.0, 10, 10));

        List<DestaqueResponse> destaques = destaqueService.listar(dono);

        assertThat(destaques.get(0).jogos()).hasSize(1);
        assertThat(destaques.get(0).jogos().get(0).horasJogadas()).isEqualTo(120.0);
    }
}
