package com.xpv.backend.service;

import com.xpv.backend.model.ContaPlataforma;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.ContaPlataformaRepository;
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
 * Cobre a Issue #03 (vincular Steam/Riot Games/Battle.net) e parte da #09
 * (perfil privado): a persistência do vínculo, a atualização em vez de
 * duplicação ao revincular, e as regras de posse/isolamento entre usuários.
 */
@SpringBootTest
class ContaPlataformaServiceTest {

    @Autowired
    ContaPlataformaService contaPlataformaService;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    ContaPlataformaRepository contaPlataformaRepository;

    Usuario dono;
    Usuario outroUsuario;

    @BeforeEach
    void configurar() {
        contaPlataformaRepository.deleteAll();
        usuarioRepository.deleteAll();
        dono = usuarioRepository.save(new Usuario("google-sub-dono", "dono@gmail.com", "Dono", null));
        outroUsuario = usuarioRepository.save(new Usuario("google-sub-outro", "outro@gmail.com", "Outro", null));
    }

    @Test
    void vincularSteam_criaContaComPerfilPublico() {
        var perfil = new SteamService.PerfilSteam("76561198000000001", "JogadorX", "http://avatar", true, true);

        ContaPlataforma conta = contaPlataformaService.vincularSteam(dono, perfil);

        assertThat(conta.getId()).isNotNull();
        assertThat(conta.getPlataforma().getNome()).isEqualTo("Steam");
        assertThat(conta.getIdentificador()).isEqualTo("76561198000000001");
        assertThat(conta.getPerfilPublico()).isTrue();
        assertThat(conta.getBibliotecaPublica()).isTrue();
    }

    @Test
    void vincularSteamComPerfilPrivado_marcaPerfilPublicoComoFalso() {
        var perfil = new SteamService.PerfilSteam("76561198000000002", "JogadorPrivado", "", false, false);

        ContaPlataforma conta = contaPlataformaService.vincularSteam(dono, perfil);

        assertThat(conta.getPerfilPublico()).isFalse();
    }

    @Test
    void vincularSteamComPerfilPublicoMasBibliotecaPrivada_marcaCamposSeparadamente() {
        var perfil = new SteamService.PerfilSteam("76561198000000010", "JogadorBibliotecaPrivada", "", true, false);

        ContaPlataforma conta = contaPlataformaService.vincularSteam(dono, perfil);

        assertThat(conta.getPerfilPublico()).isTrue();
        assertThat(conta.getBibliotecaPublica()).isFalse();
    }

    @Test
    void vincularSteamDuasVezes_atualizaEmVezDeDuplicar() {
        contaPlataformaService.vincularSteam(dono, new SteamService.PerfilSteam("76561198000000003", "Antigo", "", true, true));
        contaPlataformaService.vincularSteam(dono, new SteamService.PerfilSteam("76561198000000003", "NovoNick", "", false, false));

        List<ContaPlataforma> contas = contaPlataformaService.listar(dono);

        assertThat(contas).hasSize(1);
        assertThat(contas.get(0).getNickname()).isEqualTo("NovoNick");
        assertThat(contas.get(0).getPerfilPublico()).isFalse();
    }

    @Test
    void vincularRiot_criaContaComRiotIdComoNickname() {
        var perfil = new RiotService.PerfilRiot("puuid-1", "JogadorX#BR1");

        ContaPlataforma conta = contaPlataformaService.vincularRiot(dono, perfil);

        assertThat(conta.getPlataforma().getNome()).isEqualTo("Riot Games");
        assertThat(conta.getIdentificador()).isEqualTo("puuid-1");
        assertThat(conta.getNickname()).isEqualTo("JogadorX#BR1");
        assertThat(conta.getPerfilPublico()).isNull();
    }

    @Test
    void vincularBattleNet_criaContaSemConceitoDePerfilPublico() {
        var perfil = new BattleNetService.PerfilBattleNet("battlenet-account-1", "Jogador#1234");

        ContaPlataforma conta = contaPlataformaService.vincularBattleNet(dono, perfil);

        assertThat(conta.getPlataforma().getNome()).isEqualTo("Battle.net");
        assertThat(conta.getNickname()).isEqualTo("Jogador#1234");
        assertThat(conta.getPerfilPublico()).isNull();
    }

    @Test
    void steamRiotEBattleNetDoMesmoUsuario_saoContasSeparadas() {
        contaPlataformaService.vincularSteam(dono, new SteamService.PerfilSteam("76561198000000004", "Nick", "", true, true));
        contaPlataformaService.vincularRiot(dono, new RiotService.PerfilRiot("puuid-2", "Nick#BR1"));
        contaPlataformaService.vincularBattleNet(dono, new BattleNetService.PerfilBattleNet("battlenet-account-2", "Nick#1234"));

        assertThat(contaPlataformaService.listar(dono)).hasSize(3);
    }

    @Test
    void desvincular_removeAConta() {
        ContaPlataforma conta = contaPlataformaService.vincularSteam(dono,
                new SteamService.PerfilSteam("76561198000000005", "Nick", "", true, true));

        contaPlataformaService.desvincular(dono, conta.getId());

        assertThat(contaPlataformaService.listar(dono)).isEmpty();
    }

    @Test
    void desvincular_naoPermiteRemoverContaDeOutroUsuario() {
        ContaPlataforma conta = contaPlataformaService.vincularSteam(dono,
                new SteamService.PerfilSteam("76561198000000006", "Nick", "", true, true));

        assertThatThrownBy(() -> contaPlataformaService.desvincular(outroUsuario, conta.getId()))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(contaPlataformaService.listar(dono)).hasSize(1);
    }
}
