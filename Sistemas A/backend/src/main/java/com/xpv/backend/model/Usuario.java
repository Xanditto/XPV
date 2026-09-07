package com.xpv.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String googleSub;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String avatar;

    /** Hash bcrypt da senha escolhida pelo usuário após o primeiro login com Google. Nulo até então. */
    private String senhaHash;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    public Usuario(String googleSub, String email, String nickname, String avatar) {
        this.googleSub = googleSub;
        this.email = email;
        this.nickname = nickname;
        this.avatar = avatar;
    }
}
