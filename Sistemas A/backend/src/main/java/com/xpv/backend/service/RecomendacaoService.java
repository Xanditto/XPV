package com.xpv.backend.service;

import com.xpv.backend.dto.JogoRecomendadoResponse;
import com.xpv.backend.dto.JogoResponse;
import com.xpv.backend.dto.RecomendacaoResponse;
import com.xpv.backend.model.ContaPlataforma;
import com.xpv.backend.model.Jogo;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.ContaPlataformaRepository;
import com.xpv.backend.repository.JogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Recomendação de jogos (Issue #16). "Dê uma chance" é simples: jogos que o
 * usuário já tem na biblioteca mas nunca jogou (horasJogadas = 0). Já
 * "Recomendações" (sempre 20 jogos que o usuário não possui) usa o SteamSpy
 * (ver SteamSpyService) porque a Steam não tem endpoint público de
 * recomendação: descobrimos os gêneros favoritos olhando os jogos mais
 * jogados do usuário, buscamos os jogos mais bem avaliados desses gêneros
 * que ele não possui, e completamos com os mais jogados da Steam em geral
 * se não der pra fechar 20 (ex.: usuário novo sem histórico, ou gênero raro).
 */
@Service
@RequiredArgsConstructor
public class RecomendacaoService {

    private static final int MAXIMO_DE_UMA_CHANCE = 5;
    private static final int TOTAL_RECOMENDACOES = 20;
    private static final int JOGOS_PARA_PERFIL_DE_GENERO = 15;
    private static final int GENEROS_NO_PERFIL = 3;
    private static final int CANDIDATOS_POR_GENERO = 50;
    private static final int MINIMO_DE_AVALIACOES = 50;
    private static final double NOTA_MINIMA_POSITIVAS = 0.7;
    private static final String GENERO_PADRAO = "action";

    private final ContaPlataformaRepository contaPlataformaRepository;
    private final JogoRepository jogoRepository;
    private final SteamSpyService steamSpyService;

    public RecomendacaoResponse recomendar(Usuario usuario) {
        ContaPlataforma conta = contaPlataformaRepository.findByUsuario(usuario).stream()
                .filter(c -> PlataformaService.STEAM.equals(c.getPlataforma().getNome()))
                .findFirst()
                .orElse(null);
        if (conta == null) {
            return new RecomendacaoResponse(List.of(), List.of());
        }

        List<Jogo> todos = jogoRepository.findByContaPlataformaId(conta.getId());

        List<JogoResponse> deUmaChance = todos.stream()
                .filter(jogo -> jogo.getHorasJogadas() <= 0)
                .limit(MAXIMO_DE_UMA_CHANCE)
                .map(JogoResponse::from)
                .toList();

        Set<Long> possuidos = todos.stream().map(Jogo::getAppId).collect(Collectors.toSet());
        List<String> generosFavoritos = calcularGenerosFavoritos(todos);
        List<JogoRecomendadoResponse> recomendacoes = montarRecomendacoes(generosFavoritos, possuidos);

        return new RecomendacaoResponse(deUmaChance, recomendacoes);
    }

    /** Pondera cada gênero pelas horas jogadas nos jogos mais jogados do usuário, e devolve os GENEROS_NO_PERFIL mais fortes. */
    private List<String> calcularGenerosFavoritos(List<Jogo> todos) {
        List<Jogo> maisJogados = todos.stream()
                .filter(jogo -> jogo.getHorasJogadas() > 0)
                .sorted(Comparator.comparingDouble(Jogo::getHorasJogadas).reversed())
                .limit(JOGOS_PARA_PERFIL_DE_GENERO)
                .toList();

        Map<String, Double> pesoPorGenero = new HashMap<>();
        for (Jogo jogo : maisJogados) {
            SteamSpyService.JogoSteamSpy detalhes = steamSpyService.buscarDetalhes(jogo.getAppId());
            if (detalhes == null) {
                continue;
            }
            for (String genero : detalhes.generos()) {
                pesoPorGenero.merge(genero.toLowerCase(), jogo.getHorasJogadas(), Double::sum);
            }
        }

        if (pesoPorGenero.isEmpty()) {
            return List.of(GENERO_PADRAO);
        }

        return pesoPorGenero.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(GENEROS_NO_PERFIL)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Alterna entre os gêneros favoritos (pra ter variedade) e completa com os mais jogados da Steam se faltar. */
    private List<JogoRecomendadoResponse> montarRecomendacoes(List<String> generosFavoritos, Set<Long> possuidos) {
        List<Deque<SteamSpyService.JogoSteamSpy>> filasPorGenero = new ArrayList<>();
        for (String genero : generosFavoritos) {
            List<SteamSpyService.JogoSteamSpy> candidatos = steamSpyService.buscarPorGenero(genero).stream()
                    .filter(jogo -> !possuidos.contains(jogo.appId()))
                    .filter(this::temBoasAvaliacoes)
                    .sorted(Comparator.comparingLong(SteamSpyService.JogoSteamSpy::positivas).reversed())
                    .limit(CANDIDATOS_POR_GENERO)
                    .toList();
            filasPorGenero.add(new ArrayDeque<>(candidatos));
        }

        List<JogoRecomendadoResponse> resultado = new ArrayList<>();
        Set<Long> adicionados = new HashSet<>();

        boolean aindaTemCandidato = true;
        while (resultado.size() < TOTAL_RECOMENDACOES && aindaTemCandidato) {
            aindaTemCandidato = false;
            for (Deque<SteamSpyService.JogoSteamSpy> fila : filasPorGenero) {
                if (resultado.size() >= TOTAL_RECOMENDACOES) {
                    break;
                }
                SteamSpyService.JogoSteamSpy proximo = proximoNaoAdicionado(fila, adicionados);
                if (proximo != null) {
                    adicionados.add(proximo.appId());
                    resultado.add(new JogoRecomendadoResponse(proximo.appId(), proximo.nome()));
                    aindaTemCandidato = true;
                }
            }
        }

        if (resultado.size() < TOTAL_RECOMENDACOES) {
            for (SteamSpyService.JogoSteamSpy extra : steamSpyService.top100Forever()) {
                if (resultado.size() >= TOTAL_RECOMENDACOES) {
                    break;
                }
                if (possuidos.contains(extra.appId()) || adicionados.contains(extra.appId())) {
                    continue;
                }
                adicionados.add(extra.appId());
                resultado.add(new JogoRecomendadoResponse(extra.appId(), extra.nome()));
            }
        }

        return resultado;
    }

    private boolean temBoasAvaliacoes(SteamSpyService.JogoSteamSpy jogo) {
        long total = jogo.positivas() + jogo.negativas();
        return total >= MINIMO_DE_AVALIACOES && (double) jogo.positivas() / total >= NOTA_MINIMA_POSITIVAS;
    }

    private SteamSpyService.JogoSteamSpy proximoNaoAdicionado(Deque<SteamSpyService.JogoSteamSpy> fila, Set<Long> adicionados) {
        while (!fila.isEmpty()) {
            SteamSpyService.JogoSteamSpy candidato = fila.poll();
            if (!adicionados.contains(candidato.appId())) {
                return candidato;
            }
        }
        return null;
    }
}
