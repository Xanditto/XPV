package com.xpv.backend.controller;

import com.xpv.backend.dto.AvaliacaoRequest;
import com.xpv.backend.dto.AvaliacaoResponse;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.service.AvaliacaoService;
import com.xpv.backend.service.CurrentUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {

    private final CurrentUserResolver currentUserResolver;
    private final AvaliacaoService avaliacaoService;

    @GetMapping
    public List<AvaliacaoResponse> listar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        return avaliacaoService.listar(usuario);
    }

    /** Cria a avaliação de um jogo, ou atualiza a nota/resenha se o usuário já tiver avaliado esse jogo. */
    @PutMapping
    public AvaliacaoResponse salvar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                     @RequestBody AvaliacaoRequest avaliacao) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        return avaliacaoService.salvar(usuario, avaliacao);
    }

    @DeleteMapping("/{id}")
    public void remover(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                         @PathVariable Long id) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        avaliacaoService.remover(usuario, id);
    }
}
