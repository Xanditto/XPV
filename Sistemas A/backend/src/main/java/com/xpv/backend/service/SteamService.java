package com.xpv.backend.service;

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
import java.util.Map;

/**
 * Integração com a Steam via OpenID 2.0 (login "Entrar com Steam", sem
 * OAuth/senha própria) e a Steam Web API (dados públicos do perfil).
 */
@Service
public class SteamService {

    private static final String OPENID_ENDPOINT = "https://steamcommunity.com/openid/login";

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        try {
            String url = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/"
                    + "?key=" + urlEncode(apiKey) + "&steamid=" + urlEncode(steamId)
                    + "&include_appinfo=false&format=json";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }
            JsonNode resposta = objectMapper.readTree(response.body()).path("response");
            return resposta.has("game_count");
        } catch (Exception e) {
            return false;
        }
    }

    private static String urlEncode(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }
}
