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
 * Vínculo entre um Usuário do XPV e a conta dele em uma plataforma externa
 * (Steam, Epic Games, ...). Um usuário só pode ter uma conta vinculada por
 * plataforma (ver UniqueConstraint).
 */
@Entity
@Table(name = "contas_plataforma", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "plataforma_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ContaPlataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plataforma_id", nullable = false)
    private Plataforma plataforma;

    /** Identificador do usuário na plataforma externa (ex.: SteamID64, Epic Account ID). */
    @Column(nullable = false)
    private String identificador;

    private String nickname;

    private String avatar;

    /**
     * Indica se o perfil está público na plataforma externa. Quando falso,
     * dados como biblioteca de jogos e horas jogadas podem não estar
     * disponíveis. Nula para plataformas onde esse conceito não se aplica
     * (ex.: Epic Games, que não expõe biblioteca de jogos via API pública).
     */
    private Boolean perfilPublico;

    /**
     * Indica se a biblioteca de jogos (horas, conquistas) está visível.
     * É uma configuração de privacidade separada do perfil geral na Steam
     * ("Detalhes do jogo") - por isso é um campo distinto de perfilPublico.
     * Nula para plataformas onde esse conceito não se aplica.
     */
    private Boolean bibliotecaPublica;

    @Column(nullable = false)
    private Instant vinculadoEm = Instant.now();

    public ContaPlataforma(Usuario usuario, Plataforma plataforma, String identificador,
                            String nickname, String avatar, Boolean perfilPublico) {
        this.usuario = usuario;
        this.plataforma = plataforma;
        this.identificador = identificador;
        this.nickname = nickname;
        this.avatar = avatar;
        this.perfilPublico = perfilPublico;
    }
}
