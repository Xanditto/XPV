package com.xpv.backend.controller;

import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cobre os critérios de conclusão da Issue 1 (login): existe uma página de
 * login, o usuário consegue logar e os dados protegidos exigem sessão válida.
 * Chama a API real (subida numa porta aleatória) via HttpClient, do mesmo
 * jeito que o front-end faria.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    UsuarioRepository usuarioRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void limparBanco() {
        usuarioRepository.deleteAll();
    }

    private String url(String caminho) {
        return "http://localhost:" + port + caminho;
    }

    private HttpResponse<String> post(String caminho, String jsonBody, String bearerToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url(caminho)))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String caminho, String bearerToken) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url(caminho)))
                .timeout(Duration.ofSeconds(5))
                .GET();
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private Usuario criarUsuarioComSenha(String googleSub, String email, String nickname, String senha) {
        Usuario usuario = new Usuario(googleSub, email, nickname, null);
        usuario.setSenhaHash(encoder.encode(senha));
        return usuarioRepository.save(usuario);
    }

    @Test
    void loginComCredenciaisInexistentes_retorna401() throws Exception {
        var response = post("/api/auth/login", "{\"identificador\":\"naoexiste@teste.com\",\"senha\":\"qualquer123\"}", null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void loginComSenhaCorreta_devolveTokenDeSessao() throws Exception {
        criarUsuarioComSenha("google-sub-1", "teste@gmail.com", "JogadorTeste", "senha123");

        var response = post("/api/auth/login", "{\"identificador\":\"teste@gmail.com\",\"senha\":\"senha123\"}", null);
        JsonNode corpo = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(corpo.path("sessionToken").asText()).isNotBlank();
    }

    @Test
    void loginComNicknameEmVezDeEmail_tambemFunciona() throws Exception {
        criarUsuarioComSenha("google-sub-2", "outro@gmail.com", "OutroJogador", "senha123");

        var response = post("/api/auth/login", "{\"identificador\":\"OutroJogador\",\"senha\":\"senha123\"}", null);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    void loginComSenhaErrada_retorna401() throws Exception {
        criarUsuarioComSenha("google-sub-3", "terceiro@gmail.com", "TerceiroJogador", "senhaCorreta");

        var response = post("/api/auth/login", "{\"identificador\":\"terceiro@gmail.com\",\"senha\":\"senhaErrada\"}", null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void perfilSemTokenDeSessao_retorna401() throws Exception {
        var response = get("/api/profile", null);

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void perfilComSessaoValida_retornaDadosDoUsuarioLogado() throws Exception {
        criarUsuarioComSenha("google-sub-4", "quarto@gmail.com", "QuartoJogador", "senha123");

        var login = post("/api/auth/login", "{\"identificador\":\"quarto@gmail.com\",\"senha\":\"senha123\"}", null);
        String token = objectMapper.readTree(login.body()).path("sessionToken").asText();

        var response = get("/api/profile", token);
        JsonNode corpo = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(corpo.path("nickname").asText()).isEqualTo("QuartoJogador");
        assertThat(corpo.path("email").asText()).isEqualTo("quarto@gmail.com");
    }
}
