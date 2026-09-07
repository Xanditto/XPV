package com.xpv.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Valida o ID Token do Google Identity Services chamando o endpoint
 * tokeninfo do Google, que já verifica assinatura e expiração.
 */
@Service
public class GoogleTokenVerifier {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.client-id:}")
    private String googleClientId;

    public record GoogleUser(String sub, String email, String name, String picture) {
    }

    public GoogleUser verify(String idToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do Google inválido ou expirado");
            }

            JsonNode json = objectMapper.readTree(response.body());

            if (!googleClientId.isBlank() && !googleClientId.equals(json.path("aud").asText())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do Google não pertence a este app");
            }

            return new GoogleUser(
                    json.path("sub").asText(),
                    json.path("email").asText(),
                    json.path("name").asText(""),
                    json.path("picture").asText(""));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falha ao validar token do Google", e);
        }
    }
}
