package com.xpv.backend.repository;

import com.xpv.backend.model.DestaqueJogo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DestaqueJogoRepository extends JpaRepository<DestaqueJogo, Long> {

    /**
     * Apaga os jogos escolhidos de todos os destaques do usuário. Precisa
     * ser chamado antes de apagar os Destaque em si: um delete em massa via
     * JPQL não passa pelo ciclo de vida da entidade, então o
     * cascade/orphanRemoval declarado em Destaque.jogos não entra em ação.
     */
    @Modifying
    @Query("delete from DestaqueJogo dj where dj.destaque.usuario.id = :usuarioId")
    void deleteByDestaqueUsuarioId(@Param("usuarioId") Long usuarioId);
}
