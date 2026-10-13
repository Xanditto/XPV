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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Avaliação pessoal de um jogo (Issue #15): nota de 0,5 a 5 estrelas (em
 * passos de meia estrela) e uma resenha opcional. Referencia o jogo por
 * contaPlataformaId + appId, não pelo id interno de Jogo - mesmo motivo do
 * DestaqueJogo: a sincronização da Steam apaga e recria as linhas de Jogo a
 * cada sincronização, então uma referência por id ficaria órfã.
 */
@Entity
@Table(name = "avaliacoes", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "conta_plataforma_id", "app_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "conta_plataforma_id", nullable = false)
    private Long contaPlataformaId;

    @Column(name = "app_id", nullable = false)
    private Long appId;

    @Column(nullable = false)
    private double nota;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String resenha;

    @Column(nullable = false)
    private Instant dataCriacao;

    public Avaliacao(Usuario usuario, Long contaPlataformaId, Long appId, double nota, String resenha) {
        this.usuario = usuario;
        this.contaPlataformaId = contaPlataformaId;
        this.appId = appId;
        this.nota = nota;
        this.resenha = resenha;
        this.dataCriacao = Instant.now();
    }
}
