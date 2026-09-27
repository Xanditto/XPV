package com.xpv.backend.service;

import com.xpv.backend.model.Plataforma;
import com.xpv.backend.repository.PlataformaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlataformaService {

    public static final String STEAM = "Steam";
    public static final String RIOT_GAMES = "Riot Games";
    public static final String BATTLE_NET = "Battle.net";

    private final PlataformaRepository plataformaRepository;

    public Plataforma obterOuCriar(String nome) {
        return plataformaRepository.findByNome(nome)
                .orElseGet(() -> plataformaRepository.save(new Plataforma(nome)));
    }
}
