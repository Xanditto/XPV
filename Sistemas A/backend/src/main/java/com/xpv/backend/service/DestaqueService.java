package com.xpv.backend.service;

import com.xpv.backend.dto.ConquistaDestaqueResponse;
import com.xpv.backend.dto.DestaqueJogoRequest;
import com.xpv.backend.dto.DestaqueRequest;
import com.xpv.backend.dto.DestaqueResponse;
import com.xpv.backend.dto.JogoDestaqueResponse;
import com.xpv.backend.dto.TempoPlataformaResponse;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Destaques de perfil (Issue #10): cada usuário escolhe entre horas totais
 * numa plataforma, tempo por plataforma (top 3), um jogo favorito, de 2 a 6
 * jogos platinados (perfeccionista), o top 3 de mais jogados, uma imagem
 * própria, um texto livre ou até 20 conquistas específicas (de jogos
 * diferentes). Cada tipo tem seu próprio limite de quantos destaques desse
 * tipo podem coexistir (ver LIMITE_POR_TIPO) - a maioria permite só 1, mas
 * imagem/texto permitem 3 e perfeccionista/conquistas permitem 2. Os dados
 * exibidos vêm sempre resolvidos contra a biblioteca sincronizada atual -
 * nada fica congelado no momento da escolha, exceto o nome/ícone de uma
 * conquista específica (ver DestaqueJogo).
 */
@Service
@RequiredArgsConstructor
public class DestaqueService {

    private static final Map<TipoDestaque, Integer> LIMITE_POR_TIPO = new EnumMap<>(TipoDestaque.class);

    static {
        for (TipoDestaque tipo : TipoDestaque.values()) {
            LIMITE_POR_TIPO.put(tipo, 1);
        }
        LIMITE_POR_TIPO.put(TipoDestaque.IMAGEM_PERSONALIZADA, 3);
        LIMITE_POR_TIPO.put(TipoDestaque.CAIXA_TEXTO, 3);
        LIMITE_POR_TIPO.put(TipoDestaque.PERFECCIONISTA, 2);
        LIMITE_POR_TIPO.put(TipoDestaque.CONQUISTAS_ESPECIFICAS, 2);
    }

    private static final int MINIMO_JOGOS_PERFECCIONISTA = 2;
    private static final int MAXIMO_JOGOS_PERFECCIONISTA = 6;
    private static final int MINIMO_CONQUISTAS_ESPECIFICAS = 1;
    private static final int MAXIMO_CONQUISTAS_ESPECIFICAS = 20;
    private static final int MAXIMO_TAMANHO_TEXTO = 500;

    private final DestaqueRepository destaqueRepository;
    private final DestaqueJogoRepository destaqueJogoRepository;
    private final ContaPlataformaRepository contaPlataformaRepository;
    private final JogoRepository jogoRepository;
    private final SteamService steamService;

    @Transactional(readOnly = true)
    public List<DestaqueResponse> listar(Usuario usuario) {
        return destaqueRepository.findByUsuarioId(usuario.getId()).stream()
                .map(this::resolver)
                .toList();
    }

    @Transactional
    public List<DestaqueResponse> salvar(Usuario usuario, List<DestaqueRequest> pedidos) {
        validarLimitePorTipo(pedidos);

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

    private void validarLimitePorTipo(List<DestaqueRequest> pedidos) {
        Map<TipoDestaque, Integer> contagem = new EnumMap<>(TipoDestaque.class);
        for (DestaqueRequest pedido : pedidos) {
            if (pedido.tipo() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de destaque é obrigatório");
            }
            int atual = contagem.merge(pedido.tipo(), 1, Integer::sum);
            int limite = LIMITE_POR_TIPO.get(pedido.tipo());
            if (atual > limite) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No máximo " + limite + " destaque(s) do tipo \"" + pedido.tipo() + "\" por perfil");
            }
        }
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
            case MAIS_JOGADOS, TEMPO_POR_PLATAFORMA -> new Destaque(usuario, tipo, posicao, null);
            case IMAGEM_PERSONALIZADA -> criarDestaqueImagem(usuario, pedido, posicao);
            case CAIXA_TEXTO -> criarDestaqueTexto(usuario, pedido, posicao);
            case CONQUISTAS_ESPECIFICAS -> criarDestaqueConquistas(usuario, pedido, posicao);
        };
    }

    private Destaque criarDestaqueHorasPlataforma(Usuario usuario, DestaqueRequest pedido, int posicao) {
        if (pedido.plataforma() == null || pedido.plataforma().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma plataforma para o destaque de horas");
        }
        buscarContaDoUsuarioPorPlataformaObrigatoria(usuario, pedido.plataforma());
        return new Destaque(usuario, TipoDestaque.HORAS_PLATAFORMA, posicao, pedido.plataforma());
    }

    private Destaque criarDestaqueImagem(Usuario usuario, DestaqueRequest pedido, int posicao) {
        if (pedido.imagem() == null || pedido.imagem().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem para esse destaque");
        }
        Destaque destaque = new Destaque(usuario, TipoDestaque.IMAGEM_PERSONALIZADA, posicao, null);
        destaque.setImagem(pedido.imagem());
        return destaque;
    }

    private Destaque criarDestaqueTexto(Usuario usuario, DestaqueRequest pedido, int posicao) {
        if (pedido.texto() == null || pedido.texto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva um texto para esse destaque");
        }
        if (pedido.texto().length() > MAXIMO_TAMANHO_TEXTO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O texto deve ter no máximo " + MAXIMO_TAMANHO_TEXTO + " caracteres");
        }
        Destaque destaque = new Destaque(usuario, TipoDestaque.CAIXA_TEXTO, posicao, null);
        destaque.setTexto(pedido.texto());
        return destaque;
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
            Jogo jogo = buscarJogoDoUsuario(usuario, jogoPedido.contaPlataformaId(), jogoPedido.appId());
            if (exigirPlatina && !platinado(jogo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O jogo \"" + jogo.getNome() + "\" ainda não está 100% platinado");
            }
            destaque.getJogos().add(new DestaqueJogo(destaque, jogoPedido.contaPlataformaId(), jogoPedido.appId(), ordem++));
        }
        return destaque;
    }

    private Destaque criarDestaqueConquistas(Usuario usuario, DestaqueRequest pedido, int posicao) {
        List<DestaqueJogoRequest> escolhas = pedido.jogos() == null ? List.of() : pedido.jogos();
        if (escolhas.size() < MINIMO_CONQUISTAS_ESPECIFICAS || escolhas.size() > MAXIMO_CONQUISTAS_ESPECIFICAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Escolha entre " + MINIMO_CONQUISTAS_ESPECIFICAS + " e " + MAXIMO_CONQUISTAS_ESPECIFICAS
                            + " conquistas para esse destaque");
        }

        Destaque destaque = new Destaque(usuario, TipoDestaque.CONQUISTAS_ESPECIFICAS, posicao, null);
        int ordem = 1;
        for (DestaqueJogoRequest escolha : escolhas) {
            if (escolha.conquistaChave() == null || escolha.conquistaChave().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma conquista para cada jogo");
            }
            ContaPlataforma conta = buscarContaDoUsuario(usuario, escolha.contaPlataformaId());
            buscarJogoDoUsuario(usuario, escolha.contaPlataformaId(), escolha.appId());

            SteamService.ConquistaDetalhada conquista = steamService.buscarConquistaPorChave(
                    conta.getIdentificador(), escolha.appId(), escolha.conquistaChave());
            if (conquista == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Essa conquista não foi encontrada entre as que você já obteve nesse jogo");
            }

            destaque.getJogos().add(new DestaqueJogo(destaque, escolha.contaPlataformaId(), escolha.appId(), ordem++,
                    conquista.chave(), conquista.nome(), conquista.icone()));
        }
        return destaque;
    }

    private boolean platinado(Jogo jogo) {
        return jogo.getConquistasTotais() > 0 && jogo.getConquistasObtidas() == jogo.getConquistasTotais();
    }

    private Jogo buscarJogoDoUsuario(Usuario usuario, Long contaPlataformaId, Long appId) {
        ContaPlataforma conta = buscarContaDoUsuario(usuario, contaPlataformaId);
        return jogoRepository.findByContaPlataformaIdAndAppId(conta.getId(), appId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jogo não encontrado na biblioteca"));
    }

    private ContaPlataforma buscarContaDoUsuario(Usuario usuario, Long contaPlataformaId) {
        return contaPlataformaRepository.findByIdAndUsuario(contaPlataformaId, usuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conta vinculada não encontrada"));
    }

    private DestaqueResponse resolver(Destaque destaque) {
        Long id = destaque.getId();
        TipoDestaque tipo = destaque.getTipo();
        int posicao = destaque.getPosicao();

        return switch (tipo) {
            case HORAS_PLATAFORMA -> new DestaqueResponse(id, tipo, posicao, destaque.getPlataforma(),
                    somarHorasDaConta(buscarContaDoUsuarioPorPlataforma(destaque.getUsuario(), destaque.getPlataforma())),
                    List.of(), null, null, null, null);
            case JOGO_FAVORITO, PERFECCIONISTA -> new DestaqueResponse(id, tipo, posicao, null, null,
                    resolverJogosEscolhidos(destaque), null, null, null, null);
            case MAIS_JOGADOS -> new DestaqueResponse(id, tipo, posicao, null, null,
                    resolverMaisJogados(destaque), null, null, null, null);
            case IMAGEM_PERSONALIZADA -> new DestaqueResponse(id, tipo, posicao, null, null,
                    List.of(), destaque.getImagem(), null, null, null);
            case CAIXA_TEXTO -> new DestaqueResponse(id, tipo, posicao, null, null,
                    List.of(), null, destaque.getTexto(), null, null);
            case TEMPO_POR_PLATAFORMA -> new DestaqueResponse(id, tipo, posicao, null, null,
                    List.of(), null, null, resolverTempoPorPlataforma(destaque), null);
            case CONQUISTAS_ESPECIFICAS -> new DestaqueResponse(id, tipo, posicao, null, null,
                    List.of(), null, null, null, resolverConquistasEspecificas(destaque));
        };
    }

    private double somarHorasDaConta(Optional<ContaPlataforma> conta) {
        return conta.map(c -> jogoRepository.findByContaPlataformaId(c.getId()).stream()
                        .mapToDouble(Jogo::getHorasJogadas).sum())
                .orElse(0.0);
    }

    private List<TempoPlataformaResponse> resolverTempoPorPlataforma(Destaque destaque) {
        return contaPlataformaRepository.findByUsuario(destaque.getUsuario()).stream()
                .map(conta -> new TempoPlataformaResponse(conta.getPlataforma().getNome(),
                        jogoRepository.findByContaPlataformaId(conta.getId()).stream()
                                .mapToDouble(Jogo::getHorasJogadas).sum()))
                .sorted(Comparator.comparingDouble(TempoPlataformaResponse::horasTotais).reversed())
                .limit(3)
                .toList();
    }

    private List<JogoDestaqueResponse> resolverJogosEscolhidos(Destaque destaque) {
        return destaque.getJogos().stream()
                .flatMap(dj -> jogoRepository.findByContaPlataformaIdAndAppId(dj.getContaPlataformaId(), dj.getAppId()).stream())
                .map(JogoDestaqueResponse::from)
                .toList();
    }

    private List<ConquistaDestaqueResponse> resolverConquistasEspecificas(Destaque destaque) {
        return destaque.getJogos().stream()
                .map(dj -> new ConquistaDestaqueResponse(
                        dj.getAppId(),
                        jogoRepository.findByContaPlataformaIdAndAppId(dj.getContaPlataformaId(), dj.getAppId())
                                .map(Jogo::getNome)
                                .orElse("Jogo não encontrado"),
                        dj.getConquistaChave(),
                        dj.getConquistaNome(),
                        dj.getConquistaIcone()))
                .toList();
    }

    private List<JogoDestaqueResponse> resolverMaisJogados(Destaque destaque) {
        return contaPlataformaRepository.findByUsuario(destaque.getUsuario()).stream()
                .filter(c -> PlataformaService.STEAM.equals(c.getPlataforma().getNome()))
                .findFirst()
                .map(conta -> jogoRepository.findByContaPlataformaId(conta.getId()).stream()
                        .sorted(Comparator.comparingDouble(Jogo::getHorasJogadas).reversed())
                        .limit(3)
                        .map(JogoDestaqueResponse::from)
                        .toList())
                .orElse(List.of());
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
