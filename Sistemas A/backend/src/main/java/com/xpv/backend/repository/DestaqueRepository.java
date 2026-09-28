package com.xpv.backend.repository;

import com.xpv.backend.model.Destaque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DestaqueRepository extends JpaRepository<Destaque, Long> {

    @Query("select d from Destaque d where d.usuario.id = :usuarioId order by d.posicao asc")
    List<Destaque> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("delete from Destaque d where d.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Long usuarioId);
}
