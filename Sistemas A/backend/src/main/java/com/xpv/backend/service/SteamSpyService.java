package com.xpv.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SteamSpy (steamspy.com) - API gratuita de terceiros usada para recomendar
 * jogos (Issue #16), já que a Steam não tem endpoint público de
 * "recomendações"/"jogos parecidos". Dá os gêneros/avaliações de um jogo
 * (appdetails) e listas de jogos por gênero (genre) sem precisar de chave.
 * Os parâmetros de gênero/tag só funcionam em minúsculo - testado
 * manualmente, "Indie" devolve vazio mas "indie" funciona. Tem um limite de
 * aproximadamente 1 requisição por segundo, por isso todo acesso HTTP passa
 * por {@link #respeitarRateLimit()}, e os resultados ficam em cache em
 * memória (compartilhado entre todos os usuários - gênero de um jogo não
 * muda, e a lista de populares de um gênero não precisa ser atualizada a
 * cada requisição).
 */
@Service
public class SteamSpyService {

    private static final Logger log = LoggerFactory.getLogger(SteamSpyService.class);
    private static final Duration TTL_LISTA_POR_GENERO = Duration.ofHours(6);
    private static final Duration INTERVALO_MINIMO_ENTRE_CHAMADAS = Duration.ofMillis(1100);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<Long, JogoSteamSpy> cacheDetalhes = new ConcurrentHashMap<>();
    private final Map<String, List<JogoSteamSpy>> cacheListaPorGenero = new ConcurrentHashMap<>();
    private final Map<String, Instant> validadeCacheListaPorGenero = new ConcurrentHashMap<>();
    private List<JogoSteamSpy> cacheTop100Forever;
    private Instant validadeCacheTop100Forever;

    private final Object travaRequisicao = new Object();
    private Instant ultimaChamada = Instant.EPOCH;

    public record JogoSteamSpy(long appId, String nome, List<String> generos, long positivas, long negativas) {
    }

    /** Gêneros/avaliações de um jogo específico, com cache permanente (gênero não muda). Null se a consulta falhar. */
    public JogoSteamSpy buscarDetalhes(long appId) {
        JogoSteamSpy emCache = cacheDetalhes.get(appId);
        if (emCache != null) {
            return emCache;
        }
        JogoSteamSpy resultado = consultar("appdetails&appid=" + appId);
        if (resultado != null) {
            cacheDetalhes.put(appId, resultado);
        }
        return resultado;
    }

    /** Jogos de um gênero (ex.: "action", "indie", "rpg" - sempre minúsculo), com cache por algumas horas. */
    public List<JogoSteamSpy> buscarPorGenero(String genero) {
        String chave = genero.toLowerCase();
        Instant validade = validadeCacheListaPorGenero.get(chave);
        if (validade != null && validade.isAfter(Instant.now())) {
            return cacheListaPorGenero.get(chave);
        }
        List<JogoSteamSpy> lista = consultarLista("genre&genre=" + urlEncode(chave));
        cacheListaPorGenero.put(chave, lista);
        validadeCacheListaPorGenero.put(chave, Instant.now().plus(TTL_LISTA_POR_GENERO));
        return lista;
    }

    /** Os 100 jogos mais jogados de todos os tempos na Steam - usado como reforço quando faltam recomendações por gênero. */
    public List<JogoSteamSpy> top100Forever() {
        if (cacheTop100Forever != null && validadeCacheTop100Forever.isAfter(Instant.now())) {
            return cacheTop100Forever;
        }
        List<JogoSteamSpy> lista = consultarLista("top100forever");
        cacheTop100Forever = lista;
        validadeCacheTop100Forever = Instant.now().plus(TTL_LISTA_POR_GENERO);
        return lista;
    }

    private JogoSteamSpy consultar(String parametros) {
        try {
            JsonNode raiz = chamarApi(parametros);
            return raiz == null ? null : paraJogo(raiz);
        } catch (Exception e) {
            log.warn("Falha ao consultar SteamSpy ({}): {}", parametros, e.getMessage());
            return null;
        }
    }

    private List<JogoSteamSpy> consultarLista(String parametros) {
        try {
            JsonNode raiz = chamarApi(parametros);
            if (raiz == null) {
                return List.of();
            }
            List<JogoSteamSpy> lista = new ArrayList<>();
            Iterator<String> chaves = raiz.propertyNames().iterator();
            while (chaves.hasNext()) {
                lista.add(paraJogo(raiz.path(chaves.next())));
            }
            return lista;
        } catch (Exception e) {
            log.warn("Falha ao consultar SteamSpy ({}): {}", parametros, e.getMessage());
            return List.of();
        }
    }

    private JsonNode chamarApi(String parametros) throws Exception {
        respeitarRateLimit();
        String url = "https://steamspy.com/api.php?request=" + parametros;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0")
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }
        return objectMapper.readTree(response.body());
    }

    private JogoSteamSpy paraJogo(JsonNode dados) {
        List<String> generos = Arrays.stream(dados.path("genre").asText("").split(","))
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .toList();
        return new JogoSteamSpy(dados.path("appid").asLong(), dados.path("name").asText(""), generos,
                dados.path("positive").asLong(0), dados.path("negative").asLong(0));
    }

    private void respeitarRateLimit() {
        synchronized (travaRequisicao) {
            Duration desdeAUltima = Duration.between(ultimaChamada, Instant.now());
            if (desdeAUltima.compareTo(INTERVALO_MINIMO_ENTRE_CHAMADAS) < 0) {
                try {
                    Thread.sleep(INTERVALO_MINIMO_ENTRE_CHAMADAS.minus(desdeAUltima).toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ultimaChamada = Instant.now();
        }
    }

    private String urlEncode(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
