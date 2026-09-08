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
 * Cobre os critérios de conclusão da Issue 2 (edição de perfil): o usuário
 * consegue alterar nickname e foto, e essas alterações continuam salvas
 * mesmo depois de encerrar a sessão e entrar de novo (novo login).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileControllerTest {

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

    private HttpResponse<String> put(String caminho, String jsonBody, String bearerToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url(caminho)))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String caminho, String bearerToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url(caminho)))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + bearerToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String logar(String identificador, String senha) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/api/auth/login")))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"identificador\":\"" + identificador + "\",\"senha\":\"" + senha + "\"}"))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readTree(response.body()).path("sessionToken").asText();
    }

    private Usuario criarUsuarioComSenha(String googleSub, String email, String nickname, String senha) {
        Usuario usuario = new Usuario(googleSub, email, nickname, null);
        usuario.setSenhaHash(encoder.encode(senha));
        return usuarioRepository.save(usuario);
    }

    @Test
    void atualizarPerfilSemSessao_retorna401() throws Exception {
        var response = put("/api/profile", "{\"nickname\":\"NovoNick\",\"avatar\":\"\"}", "token-invalido");

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void usuarioConsegueAlterarNicknameEFoto() throws Exception {
        criarUsuarioComSenha("google-sub-10", "quinto@gmail.com", "NicknameAntigo", "senha123");
        String token = logar("quinto@gmail.com", "senha123");

        var response = put("/api/profile",
                "{\"nickname\":\"NicknameNovo\",\"avatar\":\"data:image/png;base64,ABC123\"}", token);
        JsonNode corpo = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(corpo.path("nickname").asText()).isEqualTo("NicknameNovo");
        assertThat(corpo.path("avatar").asText()).isEqualTo("data:image/png;base64,ABC123");
    }

    @Test
    void alteracoesContinuamSalvasAposNovoLogin() throws Exception {
        criarUsuarioComSenha("google-sub-11", "sexto@gmail.com", "NicknameOriginal", "senha123");
        String primeiraSessao = logar("sexto@gmail.com", "senha123");

        put("/api/profile", "{\"nickname\":\"NicknameEditado\",\"avatar\":\"foto-nova.png\"}", primeiraSessao);

        // Simula logout (o token antigo é só esquecido pelo front-end) e um novo login.
        String novaSessao = logar("sexto@gmail.com", "senha123");
        var perfilDepoisDoRelogin = get("/api/profile", novaSessao);
        JsonNode corpo = objectMapper.readTree(perfilDepoisDoRelogin.body());

        assertThat(perfilDepoisDoRelogin.statusCode()).isEqualTo(200);
        assertThat(corpo.path("nickname").asText()).isEqualTo("NicknameEditado");
        assertThat(corpo.path("avatar").asText()).isEqualTo("foto-nova.png");
    }

    @Test
    void nicknameMuitoCurto_retorna400() throws Exception {
        criarUsuarioComSenha("google-sub-12", "setimo@gmail.com", "NicknameValido", "senha123");
        String token = logar("setimo@gmail.com", "senha123");

        var response = put("/api/profile", "{\"nickname\":\"ab\",\"avatar\":\"\"}", token);

        assertThat(response.statusCode()).isEqualTo(400);
    }
}
