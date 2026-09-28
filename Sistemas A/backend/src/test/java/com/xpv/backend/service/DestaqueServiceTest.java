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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobre a Issue #10 (personalização de perfil / destaques): as 4 opções de
 * destaque, o limite de 2 por usuário, e o fato de que os destaques com
 * jogos escolhidos (favorito/perfeccionista) resolvem os dados sempre pela
 * biblioteca atual - sobrevivendo a uma ressincronização que apaga e recria
 * as linhas de Jogo com ids novos.
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
        var pedido = new DestaqueRequest(TipoDestaque.HORAS_PLATAFORMA, "Steam", null);

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        assertThat(destaques).hasSize(1);
        assertThat(destaques.get(0).tipo()).isEqualTo(TipoDestaque.HORAS_PLATAFORMA);
        assertThat(destaques.get(0).horasTotais()).isEqualTo(2.0 + 100.0 + 50.0 + 1.0);
    }

    @Test
    void salvarHorasPlataforma_semContaNaPlataforma_lancaErro() {
        var pedido = new DestaqueRequest(TipoDestaque.HORAS_PLATAFORMA, "Battle.net", null);

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(pedido)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarJogoFavorito_comUmJogo_resolveDadosDoJogo() {
        var pedido = new DestaqueRequest(TipoDestaque.JOGO_FAVORITO, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L)));

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        assertThat(destaques.get(0).jogos()).hasSize(1);
        assertThat(destaques.get(0).jogos().get(0).nome()).isEqualTo("Jogo Muito Jogado");
        assertThat(destaques.get(0).jogos().get(0).horasJogadas()).isEqualTo(100.0);
    }

    @Test
    void salvarJogoFavorito_comZeroOuMaisDeUmJogo_lancaErro() {
        var semJogo = new DestaqueRequest(TipoDestaque.JOGO_FAVORITO, null, List.of());
        var doisJogos = new DestaqueRequest(TipoDestaque.JOGO_FAVORITO, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L), new DestaqueJogoRequest(contaSteam.getId(), 30L)));

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(semJogo))).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(doisJogos))).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarPerfeccionista_comJogosNaoPlatinados_lancaErro() {
        // "Jogo Muito Jogado" (10/10) é platina, "Jogo Medio" (5/10) não é.
        var pedido = new DestaqueRequest(TipoDestaque.PERFECCIONISTA, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L), new DestaqueJogoRequest(contaSteam.getId(), 30L)));

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(pedido)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarPerfeccionista_comMenosDeDoisOuMaisDeSeisJogos_lancaErro() {
        var umJogo = new DestaqueRequest(TipoDestaque.PERFECCIONISTA, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L)));

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(umJogo)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarPerfeccionista_comJogosPlatinados_resolveTodos() {
        jogoRepository.save(new Jogo(contaSteam, 50L, "Jogo Platina 2", "", 20.0, 3, 3));
        var pedido = new DestaqueRequest(TipoDestaque.PERFECCIONISTA, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L), new DestaqueJogoRequest(contaSteam.getId(), 50L)));

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        assertThat(destaques.get(0).jogos()).hasSize(2);
    }

    @Test
    void salvarMaisJogados_resolveTop3PorHoras() {
        var pedido = new DestaqueRequest(TipoDestaque.MAIS_JOGADOS, null, null);

        List<DestaqueResponse> destaques = destaqueService.salvar(dono, List.of(pedido));

        List<String> nomes = destaques.get(0).jogos().stream().map(j -> j.nome()).toList();
        assertThat(nomes).containsExactly("Jogo Muito Jogado", "Jogo Medio", "Jogo Pouco Jogado");
    }

    @Test
    void salvarMaisDeDoisDestaques_lancaErro() {
        var d1 = new DestaqueRequest(TipoDestaque.MAIS_JOGADOS, null, null);
        var d2 = new DestaqueRequest(TipoDestaque.HORAS_PLATAFORMA, "Steam", null);
        var d3 = new DestaqueRequest(TipoDestaque.JOGO_FAVORITO, null, List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L)));

        assertThatThrownBy(() -> destaqueService.salvar(dono, List.of(d1, d2, d3)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarSubstituiDestaquesAnteriores() {
        destaqueService.salvar(dono, List.of(new DestaqueRequest(TipoDestaque.MAIS_JOGADOS, null, null)));

        List<DestaqueResponse> destaques = destaqueService.salvar(dono,
                List.of(new DestaqueRequest(TipoDestaque.HORAS_PLATAFORMA, "Steam", null)));

        assertThat(destaques).hasSize(1);
        assertThat(destaques.get(0).tipo()).isEqualTo(TipoDestaque.HORAS_PLATAFORMA);
    }

    @Test
    void jogoFavorito_sobreviveAResincronizacaoQueTrocaOsIdsInternos() {
        var pedido = new DestaqueRequest(TipoDestaque.JOGO_FAVORITO, null,
                List.of(new DestaqueJogoRequest(contaSteam.getId(), 20L)));
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
