package com.xpv.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Um jogo que o usuário possui numa conta de plataforma vinculada (por
 * enquanto, só a Steam sincroniza esses dados). Cada linha é o jogo de UMA
 * conta específica - não é um catálogo compartilhado entre usuários.
 */
@Entity
@Table(name = "jogos", uniqueConstraints = @UniqueConstraint(columnNames = {"conta_plataforma_id", "app_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Jogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conta_plataforma_id", nullable = false)
    private ContaPlataforma contaPlataforma;

    /** Identificador do jogo na plataforma externa (ex.: AppID da Steam). */
    @Column(name = "app_id", nullable = false)
    private Long appId;

    private String nome;

    private String imagem;

    @Column(nullable = false)
    private double horasJogadas;

    /**
     * columnDefinition com DEFAULT 0 é necessário para que o Hibernate consiga
     * adicionar essa coluna via ALTER TABLE em bancos que já têm jogos
     * salvos de antes dessas colunas existirem - o SQLite rejeita "ADD COLUMN
     * NOT NULL" sem um valor padrão quando a tabela já tem linhas.
     */
    @Column(nullable = false, columnDefinition = "integer not null default 0")
    private int conquistasObtidas;

    @Column(nullable = false, columnDefinition = "integer not null default 0")
    private int conquistasTotais;

    /** Quando o usuário jogou essa pela última vez. Nulo se a Steam nunca informou (jogo nunca aberto, por exemplo). */
    private Instant ultimoAcesso;

    @Column(nullable = false, columnDefinition = "float not null default 0")
    private double horasWindows;

    @Column(nullable = false, columnDefinition = "float not null default 0")
    private double horasMac;

    @Column(nullable = false, columnDefinition = "float not null default 0")
    private double horasLinux;

    @Column(nullable = false, columnDefinition = "float not null default 0")
    private double horasDeck;

    public Jogo(ContaPlataforma contaPlataforma, Long appId, String nome, String imagem, double horasJogadas,
                int conquistasObtidas, int conquistasTotais) {
        this(contaPlataforma, appId, nome, imagem, horasJogadas, conquistasObtidas, conquistasTotais, null, 0, 0, 0, 0);
    }

    public Jogo(ContaPlataforma contaPlataforma, Long appId, String nome, String imagem, double horasJogadas,
                int conquistasObtidas, int conquistasTotais, Instant ultimoAcesso,
                double horasWindows, double horasMac, double horasLinux, double horasDeck) {
        this.contaPlataforma = contaPlataforma;
        this.appId = appId;
        this.nome = nome;
        this.imagem = imagem;
        this.horasJogadas = horasJogadas;
        this.conquistasObtidas = conquistasObtidas;
        this.conquistasTotais = conquistasTotais;
        this.ultimoAcesso = ultimoAcesso;
        this.horasWindows = horasWindows;
        this.horasMac = horasMac;
        this.horasLinux = horasLinux;
        this.horasDeck = horasDeck;
    }
}
