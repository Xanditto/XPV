package com.xpv.backend.repository;

import com.xpv.backend.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    @Query("select a from Avaliacao a where a.usuario.id = :usuarioId order by a.dataCriacao desc")
    List<Avaliacao> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("select a from Avaliacao a where a.usuario.id = :usuarioId and a.contaPlataformaId = :contaId and a.appId = :appId")
    Optional<Avaliacao> findByUsuarioIdAndContaPlataformaIdAndAppId(@Param("usuarioId") Long usuarioId,
                                                                     @Param("contaId") Long contaId,
                                                                     @Param("appId") Long appId);

    @Query("select a from Avaliacao a where a.id = :id and a.usuario.id = :usuarioId")
    Optional<Avaliacao> findByIdAndUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
