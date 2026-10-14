package com.ztech.crm.opportunities.repository;

import com.ztech.crm.opportunities.domain.StageHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Append-only (BE-ACT-03/04): se bloquean los métodos de borrado más obvios de
 * {@link JpaRepository} para que un error de programación no viole la regla, no sólo
 * para no exponerla por HTTP. No es exhaustivo (quedan variantes como
 * {@code deleteAll()}), pero cubre los casos que realmente se podrían invocar por error.
 */
public interface StageHistoryRepository extends JpaRepository<StageHistory, Long> {
    boolean existsByTenantIdAndFromStageIdOrTenantIdAndToStageId(Long tenantId, Long fromStageId, Long otherTenantId, Long toStageId);


    List<StageHistory> findAllByTenantIdAndOpportunityIdOrderByChangedAtAscIdAsc(Long tenantId, Long opportunityId);

    @Override
    default void deleteById(Long id) {
        throw new UnsupportedOperationException("El historial de etapas es append-only.");
    }

    @Override
    default void delete(StageHistory entity) {
        throw new UnsupportedOperationException("El historial de etapas es append-only.");
    }
}
