package com.xpv.backend.service;

import com.xpv.backend.dto.AuthResponse;
import com.xpv.backend.dto.UsuarioResponse;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final SessionService sessionService;

    @Transactional
    public AuthResponse loginComGoogle(String idToken) {
        GoogleTokenVerifier.GoogleUser googleUser = googleTokenVerifier.verify(idToken);

        Usuario usuario = usuarioRepository.findByGoogleSub(googleUser.sub())
                .orElseGet(() -> usuarioRepository.save(new Usuario(
                        googleUser.sub(),
                        googleUser.email(),
                        googleUser.name().isBlank() ? googleUser.email() : googleUser.name(),
                        googleUser.picture())));

        String sessionToken = sessionService.criarSessao(usuario.getId());
        return new AuthResponse(sessionToken, UsuarioResponse.from(usuario));
    }
}
