package com.ztech.crm.opportunities.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ztech.crm.access.domain.Role;
import com.ztech.crm.catalogs.domain.enums.StageKind;
import com.ztech.crm.catalogs.dto.response.StageResponse;
import com.ztech.crm.catalogs.service.StageService;
import com.ztech.crm.opportunities.domain.Opportunity;
import com.ztech.crm.opportunities.domain.StageHistory;
import com.ztech.crm.opportunities.domain.enums.OpportunityStatus;
import com.ztech.crm.opportunities.dto.request.ChangeStageRequest;
import com.ztech.crm.opportunities.mapper.OpportunityMapper;
import com.ztech.crm.opportunities.repository.OpportunityRepository;
import com.ztech.crm.opportunities.repository.StageHistoryRepository;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.security.AuthenticatedUser;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit test puro (sin Spring, sin Testcontainers) — el objetivo específico es probar
 * que {@link ChangeStageService} NO se traga una excepción del insert de historial,
 * que es lo que le permite a {@code @Transactional} revertir también el cambio de
 * etapa ya aplicado en memoria (design.md §9.1, Fase 4 de tasks.md).
 */
class ChangeStageServiceTest {

    private static final Long TENANT_ID = 10L;
    private static final Long OPPORTUNITY_ID = 999L;
    private static final Long FROM_STAGE_ID = 200L;
    private static final Long TO_STAGE_ID = 300L;

    private final OpportunityRepository opportunityRepository = mock(OpportunityRepository.class);
    private final OpportunityAccessService opportunityAccessService = mock(OpportunityAccessService.class);
    private final StageHistoryRepository stageHistoryRepository = mock(StageHistoryRepository.class);
    private final StageService stageService = mock(StageService.class);
    private final OpportunityMapper opportunityMapper = new OpportunityMapper();

    private final ChangeStageService changeStageService = new ChangeStageService(
            opportunityRepository, opportunityAccessService, stageHistoryRepository, stageService, opportunityMapper);

    private Opportunity opportunity;

    @BeforeEach
    void setUp() {
        AuthenticatedUser user = new AuthenticatedUser(1L, TENANT_ID, "admin@ztech.local", null, Role.ADMIN, true);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        opportunity = new Opportunity(TENANT_ID, "Título", 5L, null, 1L, 100L, FROM_STAGE_ID, Instant.now(), 20);
        ReflectionTestUtils.setField(opportunity, "id", OPPORTUNITY_ID);

        when(opportunityRepository.findByIdAndTenantId(OPPORTUNITY_ID, TENANT_ID)).thenReturn(Optional.of(opportunity));
        when(opportunityAccessService.getReadableOrThrow(OPPORTUNITY_ID)).thenReturn(opportunity);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void changesStageAndSavesHistoryTogether() {
        when(stageService.assertOpenAndActive(TO_STAGE_ID))
                .thenReturn(new StageResponse(TO_STAGE_ID, "Negociación", 5, StageKind.OPEN, true));

        changeStageService.changeStage(OPPORTUNITY_ID, new ChangeStageRequest(TO_STAGE_ID, "Avanza tras la reunión"));

        assertThat(opportunity.getStageId()).isEqualTo(TO_STAGE_ID);
        verify(opportunityRepository).save(opportunity);

        ArgumentCaptor<StageHistory> captor = ArgumentCaptor.forClass(StageHistory.class);
        verify(stageHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getFromStageId()).isEqualTo(FROM_STAGE_ID);
        assertThat(captor.getValue().getToStageId()).isEqualTo(TO_STAGE_ID);
        assertThat(captor.getValue().getChangedBy()).isEqualTo(1L);
        assertThat(captor.getValue().getNote()).isEqualTo("Avanza tras la reunión");
    }

    @Test
    void propagatesExceptionWhenHistoryInsertFailsSoTransactionCanRollBack() {
        when(stageService.assertOpenAndActive(TO_STAGE_ID))
                .thenReturn(new StageResponse(TO_STAGE_ID, "Negociación", 5, StageKind.OPEN, true));
        when(stageHistoryRepository.save(any())).thenThrow(new DataIntegrityViolationException("boom"));

        assertThatThrownBy(() -> changeStageService.changeStage(OPPORTUNITY_ID,
                new ChangeStageRequest(TO_STAGE_ID, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
        // La propagación sin catch es la que le permite a @Transactional revertir el
        // cambio de stageId ya aplicado sobre la entidad en la misma transacción.
    }

    @Test
    void rejectsChangeToTheSameStage() {
        assertThatThrownBy(() -> changeStageService.changeStage(OPPORTUNITY_ID,
                new ChangeStageRequest(FROM_STAGE_ID, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsChangeOnAClosedOpportunity() {
        ReflectionTestUtils.setField(opportunity, "status", OpportunityStatus.GANADA);

        assertThatThrownBy(() -> changeStageService.changeStage(OPPORTUNITY_ID,
                new ChangeStageRequest(TO_STAGE_ID, null)))
                .isInstanceOf(ConflictException.class);

        verify(stageHistoryRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void rejectsChangeToAClosedStage() {
        when(stageService.assertOpenAndActive(eq(TO_STAGE_ID)))
                .thenThrow(new BusinessException("STAGE_NOT_OPEN", "Una oportunidad sólo puede crearse en una etapa abierta."));

        assertThatThrownBy(() -> changeStageService.changeStage(OPPORTUNITY_ID,
                new ChangeStageRequest(TO_STAGE_ID, null)))
                .isInstanceOf(BusinessException.class);

        verify(opportunityRepository, org.mockito.Mockito.never()).save(any());
    }
}
