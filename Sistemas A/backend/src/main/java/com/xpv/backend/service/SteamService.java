package com.xpv.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Integração com a Steam via OpenID 2.0 (login "Entrar com Steam", sem
 * OAuth/senha própria) e a Steam Web API (dados públicos do perfil).
 */
@Service
public class SteamService {

    private static final Logger log = LoggerFactory.getLogger(SteamService.class);
    private static final String OPENID_ENDPOINT = "https://steamcommunity.com/openid/login";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    // Biblotecas grandes têm centenas de jogos com estatísticas - disparar
    // todas as chamadas de uma vez esbarra no rate limit da Steam Web API,
    // fazendo algumas falharem silenciosamente e ficarem sem contador de
    // conquistas. Limitar a concorrência reduz bastante isso.
    private final ExecutorService executorConquistas = Executors.newFixedThreadPool(6);

    @Value("${steam.api-key:}")
    private String apiKey;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public record PerfilSteam(String steamId, String nickname, String avatar, boolean perfilPublico,
                               boolean bibliotecaPublica) {
    }

    public String gerarUrlLogin(String sessionToken) {
        String returnTo = baseUrl + "/api/contas/steam/callback?session=" + urlEncode(sessionToken);
        String identifierSelect = "http://specs.openid.net/auth/2.0/identifier_select";
        return OPENID_ENDPOINT
                + "?openid.ns=" + urlEncode("http://specs.openid.net/auth/2.0")
                + "&openid.mode=checkid_setup"
                + "&openid.return_to=" + urlEncode(returnTo)
                + "&openid.realm=" + urlEncode(baseUrl)
                + "&openid.identity=" + urlEncode(identifierSelect)
                + "&openid.claimed_id=" + urlEncode(identifierSelect);
    }

    public PerfilSteam processarCallback(Map<String, String> parametrosOpenId) {
        if (!verificarComSteam(parametrosOpenId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não foi possível verificar a resposta da Steam");
        }
        String claimedId = parametrosOpenId.get("openid.claimed_id");
        if (claimedId == null || claimedId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resposta da Steam sem identificador de usuário");
        }
        String steamId = claimedId.substring(claimedId.lastIndexOf('/') + 1);
        return buscarPerfil(steamId);
    }

    private boolean verificarComSteam(Map<String, String> parametrosOpenId) {
        try {
            StringBuilder corpo = new StringBuilder();
            for (var entrada : parametrosOpenId.entrySet()) {
                if (!corpo.isEmpty()) {
                    corpo.append('&');
                }
                String valor = "openid.mode".equals(entrada.getKey()) ? "check_authentication" : entrada.getValue();
                corpo.append(urlEncode(entrada.getKey())).append('=').append(urlEncode(valor));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENID_ENDPOINT))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(corpo.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().contains("is_valid:true");
        } catch (Exception e) {
            return false;
        }
    }

    public PerfilSteam buscarPerfil(String steamId) {
        try {
            String url = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/"
                    + "?key=" + urlEncode(apiKey) + "&steamids=" + urlEncode(steamId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar a Steam");
            }

            JsonNode jogadores = objectMapper.readTree(response.body()).path("response").path("players");
            if (!jogadores.isArray() || jogadores.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil Steam não encontrado");
            }

            JsonNode jogador = jogadores.get(0);
            int visibilidade = jogador.path("communityvisibilitystate").asInt(1);
            // 1 = privado, 2 = somente amigos, 3 = público (documentação da Steam Web API)
            boolean publico = visibilidade == 3;

            return new PerfilSteam(
                    steamId,
                    jogador.path("personaname").asText(steamId),
                    jogador.path("avatarfull").asText(""),
                    publico,
                    consultarBibliotecaPublica(steamId));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao consultar a Steam", e);
        }
    }

    /**
     * A Steam separa a privacidade do "perfil" da privacidade dos "detalhes
     * do jogo" (biblioteca, horas jogadas). Não existe um campo booleano
     * explícito para essa segunda configuração — a forma documentada de
     * detectá-la é consultar GetOwnedGames: se os detalhes do jogo
     * estiverem privados, a resposta vem sem a lista de jogos.
     */
    private boolean consultarBibliotecaPublica(String steamId) {
        JsonNode resposta = consultarBibliotecaBruta(steamId);
        return resposta != null && resposta.has("game_count");
    }

    public record JogoSteam(long appId, String nome, String imagem, double horasJogadas,
                             int conquistasObtidas, int conquistasTotais) {
    }

    /**
     * Biblioteca de jogos do usuário (nome, ícone, horas jogadas e
     * conquistas). A Steam não devolve conquistas junto da lista de jogos -
     * é preciso uma chamada extra por jogo (só para os que têm estatísticas
     * visíveis), então elas são feitas em paralelo para não demorar demais
     * quando a biblioteca tem muitos jogos.
     */
    public List<JogoSteam> buscarJogos(String steamId) {
        JsonNode resposta = consultarBibliotecaBruta(steamId);
        if (resposta == null || !resposta.has("games")) {
            return List.of();
        }

        List<CompletableFuture<JogoSteam>> futuros = new ArrayList<>();
        for (JsonNode jogo : resposta.path("games")) {
            long appId = jogo.path("appid").asLong();
            String iconHash = jogo.path("img_icon_url").asText("");
            String imagem = iconHash.isBlank()
                    ? ""
                    : "https://cdn.steamstatic.com/steamcommunity/public/images/apps/" + appId + "/" + iconHash + ".jpg";
            String nome = jogo.path("name").asText("Jogo " + appId);
            double horasJogadas = jogo.path("playtime_forever").asLong() / 60.0;
            boolean temEstatisticas = jogo.path("has_community_visible_stats").asBoolean(false);

            futuros.add(CompletableFuture.supplyAsync(() -> {
                int[] conquistas = temEstatisticas ? consultarConquistasComRetentativa(steamId, appId, nome) : new int[] {0, 0};
                return new JogoSteam(appId, nome, imagem, horasJogadas, conquistas[0], conquistas[1]);
            }, executorConquistas));
        }

        return futuros.stream().map(CompletableFuture::join).toList();
    }

    private static final int TENTATIVAS_CONQUISTAS = 3;

    /** @return {conquistas obtidas, total de conquistas do jogo} */
    private int[] consultarConquistasComRetentativa(String steamId, long appId, String nomeJogo) {
        for (int tentativa = 1; tentativa <= TENTATIVAS_CONQUISTAS; tentativa++) {
            try {
                int[] resultado = consultarConquistas(steamId, appId);
                if (resultado != null) {
                    return resultado;
                }
            } catch (Exception e) {
                log.warn("Falha ao consultar conquistas de '{}' (appid {}), tentativa {}/{}: {}",
                        nomeJogo, appId, tentativa, TENTATIVAS_CONQUISTAS, e.getMessage());
            }
            try {
                Thread.sleep(300L * tentativa);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("Não foi possível obter conquistas de '{}' (appid {}) após {} tentativas - ficará sem contador.",
                nomeJogo, appId, TENTATIVAS_CONQUISTAS);
        return new int[] {0, 0};
    }

    /** @return {conquistas obtidas, total de conquistas do jogo}, ou null se a chamada falhou (para permitir nova tentativa) */
    private int[] consultarConquistas(String steamId, long appId) throws Exception {
        String url = "https://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v0001/"
                + "?appid=" + appId + "&key=" + urlEncode(apiKey) + "&steamid=" + urlEncode(steamId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 429) {
            // Rate limit da Steam - vale tentar de novo depois de esperar.
            throw new IllegalStateException("Rate limit da Steam (HTTP 429)");
        }
        if (response.statusCode() != 200) {
            // Outros códigos (ex.: 400 "jogo sem estatísticas") não melhoram
            // com retentativa - trata como "sem conquistas" direto.
            return new int[] {0, 0};
        }
        JsonNode conquistas = objectMapper.readTree(response.body()).path("playerstats").path("achievements");
        if (!conquistas.isArray()) {
            return new int[] {0, 0};
        }
        int total = conquistas.size();
        int obtidas = 0;
        for (JsonNode conquista : conquistas) {
            if (conquista.path("achieved").asInt() == 1) {
                obtidas++;
            }
        }
        return new int[] {obtidas, total};
    }

    private JsonNode consultarBibliotecaBruta(String steamId) {
        try {
            String url = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/"
                    + "?key=" + urlEncode(apiKey) + "&steamid=" + urlEncode(steamId)
                    + "&include_appinfo=true&format=json";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            return objectMapper.readTree(response.body()).path("response");
        } catch (Exception e) {
            return null;
        }
    }

    private static String urlEncode(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }
}
