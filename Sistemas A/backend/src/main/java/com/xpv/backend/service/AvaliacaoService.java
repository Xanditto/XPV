package com.xpv.backend.service;

import com.xpv.backend.dto.AvaliacaoRequest;
import com.xpv.backend.dto.AvaliacaoResponse;
import com.xpv.backend.model.Avaliacao;
import com.xpv.backend.model.ContaPlataforma;
import com.xpv.backend.model.Jogo;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.AvaliacaoRepository;
import com.xpv.backend.repository.ContaPlataformaRepository;
import com.xpv.backend.repository.JogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Avaliações pessoais de jogos (Issue #15): o usuário só pode avaliar um
 * jogo que já tenha jogado (horasJogadas > 0), com nota de 0,5 a 5 estrelas
 * em passos de meia estrela e uma resenha opcional em texto livre. Reenviar
 * uma avaliação para um jogo já avaliado atualiza a nota/resenha existente
 * em vez de criar uma duplicata.
 */
@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private static final double NOTA_MINIMA = 0.5;
    private static final double NOTA_MAXIMA = 5.0;
    private static final double PASSO_NOTA = 0.5;

    private final AvaliacaoRepository avaliacaoRepository;
    private final ContaPlataformaRepository contaPlataformaRepository;
    private final JogoRepository jogoRepository;

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listar(Usuario usuario) {
        return avaliacaoRepository.findByUsuarioId(usuario.getId()).stream()
                .map(this::resolver)
                .toList();
    }

    @Transactional
    public AvaliacaoResponse salvar(Usuario usuario, AvaliacaoRequest pedido) {
        Jogo jogo = buscarJogoDoUsuario(usuario, pedido.contaPlataformaId(), pedido.appId());
        if (jogo.getHorasJogadas() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Você precisa ter jogado \"" + jogo.getNome() + "\" para poder avaliá-lo");
        }
        validarNota(pedido.nota());

        Avaliacao avaliacao = avaliacaoRepository
                .findByUsuarioIdAndContaPlataformaIdAndAppId(usuario.getId(), pedido.contaPlataformaId(), pedido.appId())
                .orElseGet(() -> new Avaliacao(usuario, pedido.contaPlataformaId(), pedido.appId(), pedido.nota(), null));
        avaliacao.setNota(pedido.nota());
        avaliacao.setResenha(pedido.resenha());

        return resolver(avaliacaoRepository.save(avaliacao));
    }

    @Transactional
    public void remover(Usuario usuario, Long id) {
        Avaliacao avaliacao = avaliacaoRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avaliação não encontrada"));
        avaliacaoRepository.delete(avaliacao);
    }

    private void validarNota(Double nota) {
        if (nota == null || nota < NOTA_MINIMA || nota > NOTA_MAXIMA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A nota deve ser entre " + NOTA_MINIMA + " e " + NOTA_MAXIMA + " estrelas");
        }
        double passos = nota / PASSO_NOTA;
        if (Math.abs(passos - Math.round(passos)) > 1e-9) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nota deve ser em passos de meia estrela");
        }
    }

    private Jogo buscarJogoDoUsuario(Usuario usuario, Long contaPlataformaId, Long appId) {
        ContaPlataforma conta = contaPlataformaRepository.findByIdAndUsuario(contaPlataformaId, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conta vinculada não encontrada"));
        return jogoRepository.findByContaPlataformaIdAndAppId(conta.getId(), appId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jogo não encontrado na biblioteca"));
    }

    private AvaliacaoResponse resolver(Avaliacao avaliacao) {
        Jogo jogo = jogoRepository
                .findByContaPlataformaIdAndAppId(avaliacao.getContaPlataformaId(), avaliacao.getAppId())
                .orElse(null);
        String nome = jogo != null ? jogo.getNome() : "Jogo não encontrado";
        String imagem = jogo != null ? jogo.getImagem() : null;
        return AvaliacaoResponse.from(avaliacao, nome, imagem);
    }
}
