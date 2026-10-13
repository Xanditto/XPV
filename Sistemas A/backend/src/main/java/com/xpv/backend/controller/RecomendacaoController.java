package com.xpv.backend.controller;

import com.xpv.backend.dto.RecomendacaoResponse;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.service.CurrentUserResolver;
import com.xpv.backend.service.RecomendacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recomendacoes")
@RequiredArgsConstructor
public class RecomendacaoController {

    private final CurrentUserResolver currentUserResolver;
    private final RecomendacaoService recomendacaoService;

    @GetMapping
    public RecomendacaoResponse listar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        return recomendacaoService.recomendar(usuario);
    }
}
