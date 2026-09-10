package com.xpv.backend.service;

import com.xpv.backend.model.ContaPlataforma;
import com.xpv.backend.model.Plataforma;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.ContaPlataformaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContaPlataformaService {

    private final ContaPlataformaRepository contaPlataformaRepository;
    private final PlataformaService plataformaService;

    public List<ContaPlataforma> listar(Usuario usuario) {
        return contaPlataformaRepository.findByUsuario(usuario);
    }

    public void desvincular(Usuario usuario, Long id) {
        ContaPlataforma conta = contaPlataformaRepository.findByIdAndUsuario(id, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta vinculada não encontrada"));
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
        return contaPlataformaRepository.save(conta);
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
}
