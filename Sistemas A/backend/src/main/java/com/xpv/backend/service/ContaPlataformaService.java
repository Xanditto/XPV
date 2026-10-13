package com.xpv.backend.service;

import com.xpv.backend.model.ContaPlataforma;
import com.xpv.backend.model.Jogo;
import com.xpv.backend.model.Plataforma;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.ContaPlataformaRepository;
import com.xpv.backend.repository.JogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContaPlataformaService {

    private final ContaPlataformaRepository contaPlataformaRepository;
    private final JogoRepository jogoRepository;
    private final PlataformaService plataformaService;
    private final SteamService steamService;

    public List<ContaPlataforma> listar(Usuario usuario) {
        return contaPlataformaRepository.findByUsuario(usuario);
    }

    @Transactional
    public void desvincular(Usuario usuario, Long id) {
        ContaPlataforma conta = contaPlataformaRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta vinculada não encontrada"));
        jogoRepository.deleteByContaPlataformaId(conta.getId());
        contaPlataformaRepository.delete(conta);
    }

    @Transactional
    public ContaPlataforma vincularSteam(Usuario usuario, SteamService.PerfilSteam perfil) {
        Plataforma steam = plataformaService.obterOuCriar(PlataformaService.STEAM);
        ContaPlataforma conta = contaPlataformaRepository.findByUsuarioAndPlataforma(usuario, steam)
                .orElseGet(() -> new ContaPlataforma(usuario, steam, perfil.steamId(), null, null, null));
        conta.setIdentificador(perfil.steamId());
        conta.setNickname(perfil.nickname());
        conta.setAvatar(perfil.avatar());
        conta.setPerfilPublico(perfil.perfilPublico());
        conta.setBibliotecaPublica(perfil.bibliotecaPublica());
        conta = contaPlataformaRepository.save(conta);

        if (perfil.bibliotecaPublica()) {
            sincronizarJogosSteam(conta, perfil.steamId());
        }
        return conta;
    }

    @Transactional
    public ContaPlataforma vincularRiot(Usuario usuario, RiotService.PerfilRiot perfil) {
        Plataforma riot = plataformaService.obterOuCriar(PlataformaService.RIOT_GAMES);
        ContaPlataforma conta = contaPlataformaRepository.findByUsuarioAndPlataforma(usuario, riot)
                .orElseGet(() -> new ContaPlataforma(usuario, riot, perfil.puuid(), null, null, null));
        conta.setIdentificador(perfil.puuid());
        conta.setNickname(perfil.riotId());
        return contaPlataformaRepository.save(conta);
    }

    @Transactional
    public ContaPlataforma vincularBattleNet(Usuario usuario, BattleNetService.PerfilBattleNet perfil) {
        Plataforma battleNet = plataformaService.obterOuCriar(PlataformaService.BATTLE_NET);
        ContaPlataforma conta = contaPlataformaRepository.findByUsuarioAndPlataforma(usuario, battleNet)
                .orElseGet(() -> new ContaPlataforma(usuario, battleNet, perfil.accountId(), null, null, null));
        conta.setIdentificador(perfil.accountId());
        conta.setNickname(perfil.battletag());
        return contaPlataformaRepository.save(conta);
    }

    public List<Jogo> listarJogos(Usuario usuario, Long contaId) {
        ContaPlataforma conta = buscarContaDoUsuario(usuario, contaId);
        return ordenarPorHorasJogadas(jogoRepository.findByContaPlataformaId(conta.getId()));
    }

    @Transactional
    public List<Jogo> sincronizarJogos(Usuario usuario, Long contaId) {
        ContaPlataforma conta = buscarContaDoUsuario(usuario, contaId);
        if (!PlataformaService.STEAM.equals(conta.getPlataforma().getNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sincronização de biblioteca ainda não é suportada para " + conta.getPlataforma().getNome());
        }
        sincronizarJogosSteam(conta, conta.getIdentificador());
        return ordenarPorHorasJogadas(jogoRepository.findByContaPlataformaId(conta.getId()));
    }

    public List<SteamService.ConquistaDetalhada> listarConquistasDoJogo(Usuario usuario, Long contaId, Long appId) {
        ContaPlataforma conta = buscarContaDoUsuario(usuario, contaId);
        if (!PlataformaService.STEAM.equals(conta.getPlataforma().getNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Conquistas detalhadas ainda não são suportadas para " + conta.getPlataforma().getNome());
        }
        return steamService.buscarConquistasDetalhadas(conta.getIdentificador(), appId);
    }

    public SteamService.DetalhesLoja buscarDetalhesLojaDoJogo(Usuario usuario, Long contaId, Long appId) {
        ContaPlataforma conta = buscarContaDoUsuario(usuario, contaId);
        if (!PlataformaService.STEAM.equals(conta.getPlataforma().getNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Detalhes da loja ainda não são suportados para " + conta.getPlataforma().getNome());
        }
        SteamService.DetalhesLoja detalhes = steamService.buscarDetalhesLoja(appId);
        if (detalhes == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar a loja da Steam");
        }
        return detalhes;
    }

    private List<Jogo> ordenarPorHorasJogadas(List<Jogo> jogos) {
        return jogos.stream()
                .sorted(Comparator.comparingDouble(Jogo::getHorasJogadas).reversed())
                .toList();
    }

    private void sincronizarJogosSteam(ContaPlataforma conta, String steamId) {
        jogoRepository.deleteByContaPlataformaId(conta.getId());
        for (SteamService.JogoSteam jogoSteam : steamService.buscarJogos(steamId)) {
            jogoRepository.save(new Jogo(conta, jogoSteam.appId(), jogoSteam.nome(), jogoSteam.imagem(),
                    jogoSteam.horasJogadas(), jogoSteam.conquistasObtidas(), jogoSteam.conquistasTotais(),
                    jogoSteam.ultimoAcesso(), jogoSteam.horasWindows(), jogoSteam.horasMac(),
                    jogoSteam.horasLinux(), jogoSteam.horasDeck()));
        }
    }

    private ContaPlataforma buscarContaDoUsuario(Usuario usuario, Long contaId) {
        return contaPlataformaRepository.findByIdAndUsuario(contaId, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta vinculada não encontrada"));
    }
}
