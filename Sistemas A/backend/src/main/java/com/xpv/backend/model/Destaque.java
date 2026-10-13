package com.xpv.backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Um destaque de perfil escolhido pelo usuário (Issue #10) - no máximo
 * DestaqueService.MAXIMO_DESTAQUES por usuário. Os jogos escolhidos
 * (JOGO_FAVORITO, PERFECCIONISTA, CONQUISTAS_ESPECIFICAS) são referenciados
 * por appId, não pelo id interno de Jogo:
 * a sincronização da Steam apaga e recria todas as linhas de Jogo a cada
 * sincronização, então uma referência por id ficaria órfã. O appId da Steam
 * é estável e sobrevive a ressincronizações.
 */
@Entity
@Table(name = "destaques")
@Getter
@Setter
@NoArgsConstructor
public class Destaque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDestaque tipo;

    @Column(nullable = false)
    private int posicao;

    /** Usado só por HORAS_PLATAFORMA (ex.: "Steam"). Nulo nos demais tipos. */
    private String plataforma;

    /** Usada só por IMAGEM_PERSONALIZADA (base64, mesmo esquema do avatar do usuário). Nula nos demais tipos. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String imagem;

    /** Usado só por CAIXA_TEXTO. Nulo nos demais tipos. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String texto;

    @OneToMany(mappedBy = "destaque", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem asc")
    private List<DestaqueJogo> jogos = new ArrayList<>();

    public Destaque(Usuario usuario, TipoDestaque tipo, int posicao, String plataforma) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.posicao = posicao;
        this.plataforma = plataforma;
    }
}
