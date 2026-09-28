package com.xpv.backend.service;

import com.xpv.backend.dto.DestaqueJogoRequest;
import com.xpv.backend.dto.DestaqueRequest;
import com.xpv.backend.dto.DestaqueResponse;
import com.xpv.backend.dto.JogoDestaqueResponse;
import com.xpv.backend.model.ContaPlataforma;
import com.xpv.backend.model.Destaque;
import com.xpv.backend.model.DestaqueJogo;
import com.xpv.backend.model.Jogo;
import com.xpv.backend.model.TipoDestaque;
import com.xpv.backend.model.Usuario;
import com.xpv.backend.repository.ContaPlataformaRepository;
import com.xpv.backend.repository.DestaqueJogoRepository;
import com.xpv.backend.repository.DestaqueRepository;
import com.xpv.backend.repository.JogoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Destaques de perfil (Issue #10): cada usuário escolhe no máximo 2, entre
 * horas totais numa plataforma, um jogo favorito, até 6 jogos platinados
 * (perfeccionista) ou o top 3 de mais jogados. Os dados exibidos vêm sempre
 * resolvidos contra a biblioteca sincronizada atual - nada fica congelado
 * no momento da escolha.
 */
@Service
@RequiredArgsConstructor
public class DestaqueService {

    private static final int MAXIMO_DESTAQUES = 2;
    private static final int MINIMO_JOGOS_PERFECCIONISTA = 2;
    private static final int MAXIMO_JOGOS_PERFECCIONISTA = 6;

    private final DestaqueRepository destaqueRepository;
    private final DestaqueJogoRepository destaqueJogoRepository;
    private final ContaPlataformaRepository contaPlataformaRepository;
    private final JogoRepository jogoRepository;

    @Transactional(readOnly = true)
    public List<DestaqueResponse> listar(Usuario usuario) {
        return destaqueRepository.findByUsuarioId(usuario.getId()).stream()
                .map(this::resolver)
                .toList();
    }

    @Transactional
    public List<DestaqueResponse> salvar(Usuario usuario, List<DestaqueRequest> pedidos) {
        if (pedidos.size() > MAXIMO_DESTAQUES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No máximo " + MAXIMO_DESTAQUES + " destaques por perfil");
        }

        List<Destaque> novos = new ArrayList<>();
        int posicao = 1;
        for (DestaqueRequest pedido : pedidos) {
            novos.add(validarECriar(usuario, pedido, posicao));
            posicao++;
        }

        // Bulk delete via JPQL não passa pelo cascade/orphanRemoval da entidade,
        // por isso os jogos escolhidos são apagados explicitamente primeiro.
        destaqueJogoRepository.deleteByDestaqueUsuarioId(usuario.getId());
        destaqueRepository.deleteByUsuarioId(usuario.getId());
        destaqueRepository.saveAll(novos);

        return listar(usuario);
    }

    private Destaque validarECriar(Usuario usuario, DestaqueRequest pedido, int posicao) {
        TipoDestaque tipo = pedido.tipo();
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de destaque é obrigatório");
        }

        return switch (tipo) {
            case HORAS_PLATAFORMA -> criarDestaqueHorasPlataforma(usuario, pedido, posicao);
            case JOGO_FAVORITO -> criarDestaqueComJogos(usuario, pedido, posicao, 1, 1, false);
            case PERFECCIONISTA -> criarDestaqueComJogos(usuario, pedido, posicao,
                    MINIMO_JOGOS_PERFECCIONISTA, MAXIMO_JOGOS_PERFECCIONISTA, true);
            case MAIS_JOGADOS -> new Destaque(usuario, tipo, posicao, null);
        };
    }

    private Destaque criarDestaqueHorasPlataforma(Usuario usuario, DestaqueRequest pedido, int posicao) {
        if (pedido.plataforma() == null || pedido.plataforma().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma plataforma para o destaque de horas");
        }
        buscarContaDoUsuarioPorPlataformaObrigatoria(usuario, pedido.plataforma());
        return new Destaque(usuario, TipoDestaque.HORAS_PLATAFORMA, posicao, pedido.plataforma());
    }

    private Destaque criarDestaqueComJogos(Usuario usuario, DestaqueRequest pedido, int posicao,
                                            int minimo, int maximo, boolean exigirPlatina) {
        List<DestaqueJogoRequest> jogos = pedido.jogos() == null ? List.of() : pedido.jogos();
        if (jogos.size() < minimo || jogos.size() > maximo) {
            String faixa = minimo == maximo ? ("exatamente " + minimo) : ("entre " + minimo + " e " + maximo);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha " + faixa + " jogo(s) para esse destaque");
        }

        Destaque destaque = new Destaque(usuario, pedido.tipo(), posicao, null);
        int ordem = 1;
        for (DestaqueJogoRequest jogoPedido : jogos) {
            ContaPlataforma conta = contaPlataformaRepository.findByIdAndUsuario(jogoPedido.contaPlataformaId(), usuario)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conta vinculada não encontrada"));
            Jogo jogo = jogoRepository.findByContaPlataformaIdAndAppId(conta.getId(), jogoPedido.appId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jogo não encontrado na biblioteca"));
            if (exigirPlatina && !platinado(jogo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O jogo \"" + jogo.getNome() + "\" ainda não está 100% platinado");
            }
            destaque.getJogos().add(new DestaqueJogo(destaque, conta.getId(), jogoPedido.appId(), ordem++));
        }
        return destaque;
    }

    private boolean platinado(Jogo jogo) {
        return jogo.getConquistasTotais() > 0 && jogo.getConquistasObtidas() == jogo.getConquistasTotais();
    }

    private DestaqueResponse resolver(Destaque destaque) {
        return switch (destaque.getTipo()) {
            case HORAS_PLATAFORMA -> resolverHorasPlataforma(destaque);
            case JOGO_FAVORITO, PERFECCIONISTA -> resolverJogosEscolhidos(destaque);
            case MAIS_JOGADOS -> resolverMaisJogados(destaque);
        };
    }

    private DestaqueResponse resolverHorasPlataforma(Destaque destaque) {
        double total = buscarContaDoUsuarioPorPlataforma(destaque.getUsuario(), destaque.getPlataforma())
                .map(conta -> jogoRepository.findByContaPlataformaId(conta.getId()).stream()
                        .mapToDouble(Jogo::getHorasJogadas).sum())
                .orElse(0.0);
        return new DestaqueResponse(destaque.getId(), destaque.getTipo(), destaque.getPosicao(),
                destaque.getPlataforma(), total, List.of());
    }

    private DestaqueResponse resolverJogosEscolhidos(Destaque destaque) {
        List<JogoDestaqueResponse> jogos = destaque.getJogos().stream()
                .flatMap(dj -> jogoRepository.findByContaPlataformaIdAndAppId(dj.getContaPlataformaId(), dj.getAppId()).stream())
                .map(JogoDestaqueResponse::from)
                .toList();
        return new DestaqueResponse(destaque.getId(), destaque.getTipo(), destaque.getPosicao(), null, null, jogos);
    }

    private DestaqueResponse resolverMaisJogados(Destaque destaque) {
        List<JogoDestaqueResponse> top3 = contaPlataformaRepository.findByUsuario(destaque.getUsuario()).stream()
                .filter(c -> PlataformaService.STEAM.equals(c.getPlataforma().getNome()))
                .findFirst()
                .map(conta -> jogoRepository.findByContaPlataformaId(conta.getId()).stream()
                        .sorted(Comparator.comparingDouble(Jogo::getHorasJogadas).reversed())
                        .limit(3)
                        .map(JogoDestaqueResponse::from)
                        .toList())
                .orElse(List.of());
        return new DestaqueResponse(destaque.getId(), destaque.getTipo(), destaque.getPosicao(), null, null, top3);
    }

    private Optional<ContaPlataforma> buscarContaDoUsuarioPorPlataforma(Usuario usuario, String plataforma) {
        return contaPlataformaRepository.findByUsuario(usuario).stream()
                .filter(c -> plataforma.equals(c.getPlataforma().getNome()))
                .findFirst();
    }

    private ContaPlataforma buscarContaDoUsuarioPorPlataformaObrigatoria(Usuario usuario, String plataforma) {
        return buscarContaDoUsuarioPorPlataforma(usuario, plataforma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Você não tem uma conta " + plataforma + " vinculada"));
    }
}
