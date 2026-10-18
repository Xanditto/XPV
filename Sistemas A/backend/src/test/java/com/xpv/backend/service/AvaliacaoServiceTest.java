package com.xpv.backend.service;

import com.xpv.backend.dto.AvaliacaoRequest;
import com.xpv.backend.dto.AvaliacaoResponse;
import com.xpv.backend.model.ContaPlataforma;
import com.xpv.backend.model.Jogo;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.AvaliacaoRepository;
import com.xpv.backend.repository.ContaPlataformaRepository;
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
 * Cobre a Issue #15 (avaliação de usuários): só é possível avaliar um jogo
 * já jogado, a nota precisa estar em passos de meia estrela entre 0,5 e 5, e
 * reenviar uma avaliação para o mesmo jogo atualiza em vez de duplicar.
 */
@SpringBootTest
class AvaliacaoServiceTest {

    @Autowired
    AvaliacaoService avaliacaoService;

    @Autowired
    ContaPlataformaService contaPlataformaService;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    ContaPlataformaRepository contaPlataformaRepository;

    @Autowired
    JogoRepository jogoRepository;

    @Autowired
    AvaliacaoRepository avaliacaoRepository;

    Usuario dono;
    Usuario outroUsuario;
    ContaPlataforma contaSteam;

    @BeforeEach
    void configurar() {
        avaliacaoRepository.deleteAllInBatch();
        jogoRepository.deleteAllInBatch();
        contaPlataformaRepository.deleteAll();
        usuarioRepository.deleteAll();
        dono = usuarioRepository.save(new Usuario("google-sub-avaliacoes", "avaliacoes@gmail.com", "DonoAvaliacoes", null));
        outroUsuario = usuarioRepository.save(new Usuario("google-sub-outro-avaliacoes", "outro-avaliacoes@gmail.com", "Outro", null));
        contaSteam = contaPlataformaService.vincularSteam(dono,
                new SteamService.PerfilSteam("76561198000000199", "Nick", "", true, false));
        jogoRepository.save(new Jogo(contaSteam, 10L, "Jogo Jogado", "", 25.0, 5, 10));
        jogoRepository.save(new Jogo(contaSteam, 20L, "Jogo Nunca Jogado", "", 0.0, 0, 10));
    }

    @Test
    void salvarAvaliacao_comJogoJogado_funciona() {
        var pedido = new AvaliacaoRequest(contaSteam.getId(), 10L, 4.5, "Muito bom!");

        AvaliacaoResponse resposta = avaliacaoService.salvar(dono, pedido);

        assertThat(resposta.nota()).isEqualTo(4.5);
        assertThat(resposta.resenha()).isEqualTo("Muito bom!");
        assertThat(resposta.jogoNome()).isEqualTo("Jogo Jogado");
    }

    @Test
    void salvarAvaliacao_semResenha_permiteResenhaNula() {
        var pedido = new AvaliacaoRequest(contaSteam.getId(), 10L, 3.0, null);

        AvaliacaoResponse resposta = avaliacaoService.salvar(dono, pedido);

        assertThat(resposta.resenha()).isNull();
    }

    @Test
    void salvarAvaliacao_comJogoNuncaJogado_lancaErro() {
        var pedido = new AvaliacaoRequest(contaSteam.getId(), 20L, 5.0, null);

        assertThatThrownBy(() -> avaliacaoService.salvar(dono, pedido))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarAvaliacao_comJogoDeOutroUsuario_lancaErro() {
        var pedido = new AvaliacaoRequest(contaSteam.getId(), 10L, 5.0, null);

        assertThatThrownBy(() -> avaliacaoService.salvar(outroUsuario, pedido))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarAvaliacao_comNotaForaDoPassoDeMeiaEstrela_lancaErro() {
        var pedido = new AvaliacaoRequest(contaSteam.getId(), 10L, 4.3, null);

        assertThatThrownBy(() -> avaliacaoService.salvar(dono, pedido))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarAvaliacao_comNotaForaDoIntervalo_lancaErro() {
        var pedido = new AvaliacaoRequest(contaSteam.getId(), 10L, 5.5, null);

        assertThatThrownBy(() -> avaliacaoService.salvar(dono, pedido))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void salvarAvaliacaoDuasVezesParaOMesmoJogo_atualizaEmVezDeDuplicar() {
        avaliacaoService.salvar(dono, new AvaliacaoRequest(contaSteam.getId(), 10L, 3.0, "Primeira impressão"));
        avaliacaoService.salvar(dono, new AvaliacaoRequest(contaSteam.getId(), 10L, 4.5, "Mudei de ideia"));

        List<AvaliacaoResponse> avaliacoes = avaliacaoService.listar(dono);

        assertThat(avaliacoes).hasSize(1);
        assertThat(avaliacoes.get(0).nota()).isEqualTo(4.5);
        assertThat(avaliacoes.get(0).resenha()).isEqualTo("Mudei de ideia");
    }

    @Test
    void listar_naoDevolveAvaliacoesDeOutroUsuario() {
        avaliacaoService.salvar(dono, new AvaliacaoRequest(contaSteam.getId(), 10L, 4.0, null));

        assertThat(avaliacaoService.listar(outroUsuario)).isEmpty();
    }

    @Test
    void remover_apagaAAvaliacao() {
        AvaliacaoResponse resposta = avaliacaoService.salvar(dono, new AvaliacaoRequest(contaSteam.getId(), 10L, 4.0, null));

        avaliacaoService.remover(dono, resposta.id());

        assertThat(avaliacaoService.listar(dono)).isEmpty();
    }

    @Test
    void remover_naoPermiteRemoverAvaliacaoDeOutroUsuario() {
        AvaliacaoResponse resposta = avaliacaoService.salvar(dono, new AvaliacaoRequest(contaSteam.getId(), 10L, 4.0, null));

        assertThatThrownBy(() -> avaliacaoService.remover(outroUsuario, resposta.id()))
                .isInstanceOf(ResponseStatusException.class);
    }
}
