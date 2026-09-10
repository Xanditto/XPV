package com.xpv.backend.repository;

import com.xpv.backend.model.Plataforma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlataformaRepository extends JpaRepository<Plataforma, Long> {
    Optional<Plataforma> findByNome(String nome);
}
