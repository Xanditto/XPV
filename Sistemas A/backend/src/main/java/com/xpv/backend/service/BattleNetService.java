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
import java.util.Base64;

/**
 * Login com a conta Battle.net (OAuth 2.0), oficial e sem necessidade de
 * aprovação especial - só registrar um client em develop.battle.net.
 *
 * Nesta entrega guardamos apenas a identidade da conta (battletag), no
 * mesmo nível de profundidade da vinculação da Steam. Dados específicos de
 * um jogo (personagens/conquistas de World of Wow, por exemplo) exigiriam
 * escopos adicionais (ex.: "wow.profile") e ficam para uma história futura
 * de "estatísticas", assim como já é o caso da Steam.
 */
@Service
public class BattleNetService {

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${battlenet.client-id:}")
    private String clientId;

    @Value("${battlenet.client-secret:}")
    private String clientSecret;

    @Value("${battlenet.region:us}")
    private String region;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public record PerfilBattleNet(String accountId, String battletag) {
    }

    private String redirectUri() {
        return baseUrl + "/api/contas/battlenet/callback";
    }

    public String gerarUrlLogin(String sessionToken) {
        return "https://" + region + ".battle.net/oauth/authorize"
                + "?client_id=" + urlEncode(clientId)
                + "&response_type=code"
                + "&redirect_uri=" + urlEncode(redirectUri())
                + "&scope=" + urlEncode("openid")
                + "&state=" + urlEncode(sessionToken);
    }

    public PerfilBattleNet processarCallback(String code) {
        try {
            String accessToken = trocarCodePorToken(code);
            return buscarUserInfo(accessToken);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao autenticar com a Battle.net", e);
        }
    }

    private String trocarCodePorToken(String code) throws Exception {
        String corpo = "grant_type=authorization_code"
                + "&code=" + urlEncode(code)
                + "&redirect_uri=" + urlEncode(redirectUri());
        String credenciais = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + region + ".battle.net/oauth/token"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + credenciais)
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível autenticar com a Battle.net: " + response.body());
        }
        String token = objectMapper.readTree(response.body()).path("access_token").asText(null);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta da Battle.net sem access_token");
        }
        return token;
    }

    private PerfilBattleNet buscarUserInfo(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + region + ".battle.net/oauth/userinfo"))
                .timeout(Duration.ofSeconds(8))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível obter o perfil da Battle.net");
        }

        JsonNode corpo = objectMapper.readTree(response.body());
        String accountId = corpo.path("id").asText(null);
        if (accountId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta da Battle.net sem identificador de conta");
        }
        String battletag = corpo.path("battletag").asText(accountId);
        return new PerfilBattleNet(accountId, battletag);
    }

    private static String urlEncode(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }
}
