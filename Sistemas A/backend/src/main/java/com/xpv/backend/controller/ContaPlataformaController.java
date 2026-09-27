package com.xpv.backend.controller;

import com.xpv.backend.dto.ContaPlataformaResponse;
import com.xpv.backend.dto.JogoResponse;
import com.xpv.backend.dto.RiotContaRequest;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.service.BattleNetService;
import com.xpv.backend.service.ContaPlataformaService;
import com.xpv.backend.service.CurrentUserResolver;
import com.xpv.backend.service.RiotService;
import com.xpv.backend.service.SteamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contas")
@RequiredArgsConstructor
public class ContaPlataformaController {

    private final CurrentUserResolver currentUserResolver;
    private final ContaPlataformaService contaPlataformaService;
    private final SteamService steamService;
    private final RiotService riotService;
    private final BattleNetService battleNetService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping
    public List<ContaPlataformaResponse> listar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        return contaPlataformaService.listar(usuario).stream().map(ContaPlataformaResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public void desvincular(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                             @PathVariable Long id) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        contaPlataformaService.desvincular(usuario, id);
    }

    @GetMapping("/{id}/jogos")
    public List<JogoResponse> listarJogos(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                           @PathVariable Long id) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        return contaPlataformaService.listarJogos(usuario, id).stream().map(JogoResponse::from).toList();
    }

    @PostMapping("/{id}/sincronizar")
    public List<JogoResponse> sincronizar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                           @PathVariable Long id) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        return contaPlataformaService.sincronizarJogos(usuario, id).stream().map(JogoResponse::from).toList();
    }

    // ---------------------------------------------------------------
    // Steam: login via OpenID. O front-end navega o navegador inteiro
    // para /iniciar (não é uma chamada fetch), pois o fluxo depende de
    // redirecionamentos reais entre nosso site e o da Steam.
    // ---------------------------------------------------------------

    @GetMapping("/steam/iniciar")
    public ResponseEntity<Void> iniciarSteam(@RequestParam String session) {
        return redirecionar(steamService.gerarUrlLogin(session));
    }

    @GetMapping("/steam/callback")
    public ResponseEntity<Void> callbackSteam(@RequestParam String session,
                                               @RequestParam Map<String, String> todosParametros) {
        try {
            Usuario usuario = currentUserResolver.resolver("Bearer " + session);
            SteamService.PerfilSteam perfil = steamService.processarCallback(todosParametros);
            contaPlataformaService.vincularSteam(usuario, perfil);
            return redirecionarParaFrontend("steam", true, null);
        } catch (Exception e) {
            return redirecionarParaFrontend("steam", false, e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Riot Games: sem OAuth - o usuário digita o próprio Riot ID
    // (Nome#TAG) e confirmamos consultando a API oficial da Riot.
    // ---------------------------------------------------------------

    @PostMapping("/riot")
    public ContaPlataformaResponse vincularRiot(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                 @Valid @RequestBody RiotContaRequest request) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        int posicaoTag = request.riotId().lastIndexOf('#');
        if (posicaoTag <= 0 || posicaoTag == request.riotId().length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o Riot ID no formato Nome#TAG");
        }
        String gameName = request.riotId().substring(0, posicaoTag);
        String tagLine = request.riotId().substring(posicaoTag + 1);
        RiotService.PerfilRiot perfil = riotService.buscarConta(gameName, tagLine);
        return ContaPlataformaResponse.from(contaPlataformaService.vincularRiot(usuario, perfil));
    }

    // ---------------------------------------------------------------
    // Battle.net: login via OAuth 2.0, mesma ideia da Steam/Riot em
    // termos de fluxo (redirecionamento de página inteira).
    // ---------------------------------------------------------------

    @GetMapping("/battlenet/iniciar")
    public ResponseEntity<Void> iniciarBattleNet(@RequestParam String session) {
        return redirecionar(battleNetService.gerarUrlLogin(session));
    }

    @GetMapping("/battlenet/callback")
    public ResponseEntity<Void> callbackBattleNet(@RequestParam String state, @RequestParam String code) {
        try {
            Usuario usuario = currentUserResolver.resolver("Bearer " + state);
            BattleNetService.PerfilBattleNet perfil = battleNetService.processarCallback(code);
            contaPlataformaService.vincularBattleNet(usuario, perfil);
            return redirecionarParaFrontend("battlenet", true, null);
        } catch (Exception e) {
            return redirecionarParaFrontend("battlenet", false, e.getMessage());
        }
    }

    private ResponseEntity<Void> redirecionar(String url) {
        return ResponseEntity.status(302).location(URI.create(url)).build();
    }

    private ResponseEntity<Void> redirecionarParaFrontend(String plataforma, boolean sucesso, String mensagemErro) {
        String url = frontendUrl + "/contas?plataforma=" + plataforma + "&status=" + (sucesso ? "ok" : "erro");
        if (mensagemErro != null) {
            url += "&mensagem=" + URLEncoder.encode(mensagemErro, StandardCharsets.UTF_8);
        }
        return redirecionar(url);
    }
}
