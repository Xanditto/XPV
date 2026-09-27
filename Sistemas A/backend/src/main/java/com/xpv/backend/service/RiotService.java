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

/**
 * Integração com a API oficial da Riot Games (League of Legends, Valorant,
 * TFT). Diferente da Steam/Battle.net, não existe login OAuth aqui: o
 * usuário digita o próprio Riot ID (Nome#TAG) e nós verificamos que ele
 * existe consultando a API com uma chave de desenvolvedor.
 *
 * Observação: a chave de API "pessoal" da Riot expira a cada 24h e precisa
 * ser gerada de novo manualmente em developer.riotgames.com — é uma
 * limitação da própria Riot para chaves de teste/desenvolvimento.
 */
@Service
public class RiotService {

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${riot.api-key:}")
    private String apiKey;

    @Value("${riot.routing:americas}")
    private String routing;

    public record PerfilRiot(String puuid, String riotId) {
    }

    public PerfilRiot buscarConta(String gameName, String tagLine) {
        try {
            String url = "https://" + routing + ".api.riotgames.com/riot/account/v1/accounts/by-riot-id/"
                    + urlEncode(gameName) + "/" + urlEncode(tagLine);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("X-Riot-Token", apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Riot ID não encontrado");
            }
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Chave de API da Riot inválida ou expirada (gere uma nova em developer.riotgames.com)");
            }
            if (response.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Não foi possível consultar a Riot Games");
            }

            JsonNode corpo = objectMapper.readTree(response.body());
            String puuid = corpo.path("puuid").asText();
            String gameNameResp = corpo.path("gameName").asText(gameName);
            String tagLineResp = corpo.path("tagLine").asText(tagLine);
            return new PerfilRiot(puuid, gameNameResp + "#" + tagLineResp);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao consultar a Riot Games", e);
        }
    }

    private static String urlEncode(String valor) {
        return URLEncoder.encode(valor == null ? "" : valor, StandardCharsets.UTF_8);
    }
}
