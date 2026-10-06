package com.xpv.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Um jogo escolhido dentro de um Destaque (JOGO_FAVORITO tem 1, PERFECCIONISTA tem de 2 a 6). */
@Entity
@Table(name = "destaque_jogos")
@Getter
@Setter
@NoArgsConstructor
public class DestaqueJogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destaque_id", nullable = false)
    private Destaque destaque;

    /** Conta vinculada (Steam) à qual o jogo pertence. */
    @Column(name = "conta_plataforma_id", nullable = false)
    private Long contaPlataformaId;

    @Column(name = "app_id", nullable = false)
    private Long appId;

    @Column(nullable = false)
    private int ordem;

    /**
     * Usados só por CONQUISTAS_ESPECIFICAS: identificam qual conquista do
     * jogo foi escolhida. O nome/ícone são copiados da Steam no momento da
     * escolha (não mudam depois) para não precisar consultar a Steam de novo
     * toda vez que o perfil for exibido. Nulos nos demais tipos de destaque,
     * onde a linha representa o jogo inteiro.
     */
    private String conquistaChave;
    private String conquistaNome;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String conquistaIcone;

    public DestaqueJogo(Destaque destaque, Long contaPlataformaId, Long appId, int ordem) {
        this.destaque = destaque;
        this.contaPlataformaId = contaPlataformaId;
        this.appId = appId;
        this.ordem = ordem;
    }

    public DestaqueJogo(Destaque destaque, Long contaPlataformaId, Long appId, int ordem,
                         String conquistaChave, String conquistaNome, String conquistaIcone) {
        this(destaque, contaPlataformaId, appId, ordem);
        this.conquistaChave = conquistaChave;
        this.conquistaNome = conquistaNome;
        this.conquistaIcone = conquistaIcone;
    }
}
