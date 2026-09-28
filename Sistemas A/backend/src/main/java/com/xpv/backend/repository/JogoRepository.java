package com.xpv.backend.repository;

import com.xpv.backend.model.Jogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JogoRepository extends JpaRepository<Jogo, Long> {

    @Query("select j from Jogo j where j.contaPlataforma.id = :contaId")
    List<Jogo> findByContaPlataformaId(@Param("contaId") Long contaId);

    @Query("select j from Jogo j where j.contaPlataforma.id = :contaId and j.appId = :appId")
    Optional<Jogo> findByContaPlataformaIdAndAppId(@Param("contaId") Long contaId, @Param("appId") Long appId);

    @Modifying
    @Query("delete from Jogo j where j.contaPlataforma.id = :contaId")
    void deleteByContaPlataformaId(@Param("contaId") Long contaId);
}
