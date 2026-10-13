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
 * Recomendação de jogos (Issue #16). "Dê uma chance" são jogos que o usuário
 * já possui mas nunca jogou, escolhidos pra cobrir até 5 gêneros diferentes
 * (dá mais variedade de opção do que simplesmente os 5 primeiros da
 * biblioteca). Já "Recomendações" (sempre 20 jogos que o usuário não
 * possui) usa o SteamSpy (ver SteamSpyService) porque a Steam não tem
 * endpoint público de recomendação: descobrimos os gêneros favoritos
 * olhando os jogos mais jogados do usuário, buscamos os jogos mais bem
 * avaliados desses gêneros que ele não possui, confirmamos na própria loja
 * da Steam que ainda estão disponíveis pra compra (um jogo pode ter saído
 * de venda, como aconteceu com o Rocket League, mesmo continuando jogável
 * por quem já tinha) e aproveitamos essa mesma consulta pra trazer nota do
 * Metacritic e preço. Completamos com os mais jogados da Steam em geral se
 * não der pra fechar 20.
 */
@Service
@RequiredArgsConstructor
public class RecomendacaoService {

    private static final int MAXIMO_DE_UMA_CHANCE = 5;
    private static final int MAXIMO_CANDIDATOS_DE_UMA_CHANCE = 60;
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
    private final SteamService steamService;

    public RecomendacaoResponse recomendar(Usuario usuario) {
        ContaPlataforma conta = contaPlataformaRepository.findByUsuario(usuario).stream()
                .filter(c -> PlataformaService.STEAM.equals(c.getPlataforma().getNome()))
                .findFirst()
                .orElse(null);
        if (conta == null) {
            return new RecomendacaoResponse(List.of(), List.of());
        }

        List<Jogo> todos = jogoRepository.findByContaPlataformaId(conta.getId());

        List<JogoResponse> deUmaChance = escolherDeUmaChance(todos);

        Set<Long> possuidos = todos.stream().map(Jogo::getAppId).collect(Collectors.toSet());
        List<String> generosFavoritos = calcularGenerosFavoritos(todos);
        List<JogoRecomendadoResponse> recomendacoes = montarRecomendacoes(generosFavoritos, possuidos);

        return new RecomendacaoResponse(deUmaChance, recomendacoes);
    }

    /**
     * Pega até MAXIMO_DE_UMA_CHANCE jogos nunca jogados, priorizando um de cada gênero diferente.
     * Considera TODOS os gêneros do jogo (não só o primeiro) pra decidir se ele traz diversidade -
     * muitos clássicos da Valve (Counter-Strike, Half-Life, Portal...) são listados como "Action"
     * primeiro no SteamSpy, e olhar só o primeiro gênero fazia eles colidirem entre si e sobrarem
     * poucos gêneros realmente diferentes.
     */
    private List<JogoResponse> escolherDeUmaChance(List<Jogo> todos) {
        List<Jogo> naoJogados = todos.stream()
                .filter(jogo -> jogo.getHorasJogadas() <= 0)
                .limit(MAXIMO_CANDIDATOS_DE_UMA_CHANCE)
                .toList();

        List<Jogo> escolhidos = new ArrayList<>();
        Set<String> generosUsados = new HashSet<>();
        for (Jogo jogo : naoJogados) {
            if (escolhidos.size() >= MAXIMO_DE_UMA_CHANCE) {
                break;
            }
            SteamSpyService.JogoSteamSpy detalhes = steamSpyService.buscarDetalhes(jogo.getAppId());
            List<String> generos = detalhes == null ? List.of() : detalhes.generos();
            boolean trazGeneroNovo = generos.isEmpty()
                    || generos.stream().map(String::toLowerCase).anyMatch(g -> !generosUsados.contains(g));
            if (trazGeneroNovo) {
                escolhidos.add(jogo);
                generos.forEach(g -> generosUsados.add(g.toLowerCase()));
            }
        }

        // Se não deu pra achar 5 generos diferentes, completa com o que faltar (repetindo gênero se precisar).
        for (Jogo jogo : naoJogados) {
            if (escolhidos.size() >= MAXIMO_DE_UMA_CHANCE) {
                break;
            }
            if (!escolhidos.contains(jogo)) {
                escolhidos.add(jogo);
            }
        }

        return escolhidos.stream().map(JogoResponse::from).toList();
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
                JogoRecomendadoResponse aceito = null;
                while (!fila.isEmpty() && aceito == null) {
                    aceito = tentarAceitar(fila.poll(), possuidos, adicionados);
                }
                if (aceito != null) {
                    resultado.add(aceito);
                    aindaTemCandidato = true;
                }
            }
        }

        if (resultado.size() < TOTAL_RECOMENDACOES) {
            for (SteamSpyService.JogoSteamSpy extra : steamSpyService.top100Forever()) {
                if (resultado.size() >= TOTAL_RECOMENDACOES) {
                    break;
                }
                JogoRecomendadoResponse aceito = tentarAceitar(extra, possuidos, adicionados);
                if (aceito != null) {
                    resultado.add(aceito);
                }
            }
        }

        return resultado;
    }

    /**
     * Confirma na própria loja da Steam que o candidato ainda está disponível pra compra (filtra jogos
     * removidos de venda, como o Rocket League) e já aproveita a consulta pra trazer Metacritic e preço.
     */
    private JogoRecomendadoResponse tentarAceitar(SteamSpyService.JogoSteamSpy candidato, Set<Long> possuidos,
                                                    Set<Long> adicionados) {
        if (possuidos.contains(candidato.appId()) || adicionados.contains(candidato.appId())) {
            return null;
        }
        SteamService.DetalhesLoja detalhes = steamService.buscarDetalhesLoja(candidato.appId());
        if (detalhes == null || !detalhes.disponivelParaCompra()) {
            return null;
        }
        adicionados.add(candidato.appId());
        return new JogoRecomendadoResponse(candidato.appId(), candidato.nome(), detalhes.notaMetacritic(),
                detalhes.gratuito(), detalhes.precoFormatado());
    }

    private boolean temBoasAvaliacoes(SteamSpyService.JogoSteamSpy jogo) {
        long total = jogo.positivas() + jogo.negativas();
        return total >= MINIMO_DE_AVALIACOES && (double) jogo.positivas() / total >= NOTA_MINIMA_POSITIVAS;
    }
}
