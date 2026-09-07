package com.xpv.backend.service;

import com.xpv.backend.dto.AuthResponse;
import com.xpv.backend.dto.UsuarioResponse;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final SessionService sessionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponse loginComGoogle(String idToken) {
        GoogleTokenVerifier.GoogleUser googleUser = googleTokenVerifier.verify(idToken);

        Usuario usuario = usuarioRepository.findByGoogleSub(googleUser.sub())
                .orElseGet(() -> usuarioRepository.save(new Usuario(
                        googleUser.sub(),
                        googleUser.email(),
                        gerarNicknameUnico(googleUser.name().isBlank() ? googleUser.email() : googleUser.name()),
                        googleUser.picture())));

        String sessionToken = sessionService.criarSessao(usuario.getId());
        boolean precisaDefinirSenha = usuario.getSenhaHash() == null;
        return new AuthResponse(sessionToken, precisaDefinirSenha, UsuarioResponse.from(usuario));
    }

    @Transactional
    public UsuarioResponse definirSenha(Long usuarioId, String senha) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public AuthResponse loginComSenha(String identificador, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(identificador)
                .or(() -> usuarioRepository.findByNickname(identificador))
                .orElseThrow(() -> credenciaisInvalidas());

        if (usuario.getSenhaHash() == null || !passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw credenciaisInvalidas();
        }

        String sessionToken = sessionService.criarSessao(usuario.getId());
        return new AuthResponse(sessionToken, false, UsuarioResponse.from(usuario));
    }

    private ResponseStatusException credenciaisInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail/nickname ou senha inválidos");
    }

    private String gerarNicknameUnico(String base) {
        String nickname = base;
        int contador = 1;
        while (usuarioRepository.findByNickname(nickname).isPresent()) {
            nickname = base + contador++;
        }
        return nickname;
    }
}
