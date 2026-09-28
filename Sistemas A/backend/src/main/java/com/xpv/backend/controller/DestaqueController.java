package com.xpv.backend.controller;

import com.xpv.backend.dto.DestaqueRequest;
import com.xpv.backend.dto.DestaqueResponse;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.service.CurrentUserResolver;
import com.xpv.backend.service.DestaqueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/destaques")
@RequiredArgsConstructor
public class DestaqueController {

    private final CurrentUserResolver currentUserResolver;
    private final DestaqueService destaqueService;

    @GetMapping
    public List<DestaqueResponse> listar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        return destaqueService.listar(usuario);
    }

    /** Substitui a lista inteira de destaques do usuário (no máximo 2). */
    @PutMapping
    public List<DestaqueResponse> salvar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                          @RequestBody List<DestaqueRequest> destaques) {
        Usuario usuario = currentUserResolver.resolver(authorization);
        return destaqueService.salvar(usuario, destaques);
    }
}
