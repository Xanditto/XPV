package com.xpv.backend.repository;

import com.xpv.backend.model.ContaPlataforma;
import com.xpv.backend.model.Plataforma;
import com.xpv.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContaPlataformaRepository extends JpaRepository<ContaPlataforma, Long> {
    List<ContaPlataforma> findByUsuario(Usuario usuario);

    Optional<ContaPlataforma> findByUsuarioAndPlataforma(Usuario usuario, Plataforma plataforma);

    Optional<ContaPlataforma> findByIdAndUsuario(Long id, Usuario usuario);
}
