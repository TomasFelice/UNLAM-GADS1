package com.ztech.crm.opportunities.mapper;

import com.ztech.crm.catalogs.dto.response.StageResponse;
import com.ztech.crm.customers.dto.response.CompanySummaryResponse;
import com.ztech.crm.customers.dto.response.ContactSummaryResponse;
import com.ztech.crm.offerings.dto.response.VenueResponse;
import com.ztech.crm.opportunities.domain.Opportunity;
import com.ztech.crm.opportunities.dto.response.OpportunityDetailResponse;
import com.ztech.crm.opportunities.dto.response.OpportunityResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper puro: no depende de ningún `service` de otro módulo. Resolver los datos
 * anidados del detalle (empresa/contacto/salón/etapa) es responsabilidad de
 * {@code OpportunityService}, que ya los necesita para validar — acá sólo se ensambla
 * el DTO final.
 */
@Component
public class OpportunityMapper {

    public OpportunityResponse toResponse(Opportunity opportunity) {
        return toResponse(opportunity, null, null, null, null, null);
    }

    public OpportunityResponse toResponse(Opportunity opportunity, String companyName, String contactName,
                                           String salesRepName, String venueName, String stageName) {
        return new OpportunityResponse(
                opportunity.getId(),
                opportunity.getTitle(),
                opportunity.getCompanyId(),
                opportunity.getContactId(),
                opportunity.getSalesRepId(),
                opportunity.getVenueId(),
                opportunity.getStageId(),
                opportunity.getEventTypeId(),
                opportunity.getStatus(),
                opportunity.getEstimatedValue(),
                opportunity.getEventStart(),
                opportunity.getEventEnd(),
                opportunity.getAttendeeCount(),
                opportunity.getEventServiceIds(),
                companyName, contactName, salesRepName, venueName, stageName
        );
    }

    public OpportunityDetailResponse toDetailResponse(Opportunity opportunity, CompanySummaryResponse company,
                                                        ContactSummaryResponse contact, VenueResponse venue,
                                                        StageResponse stage) {
        return new OpportunityDetailResponse(
                opportunity.getId(),
                opportunity.getTitle(),
                company,
                contact,
                opportunity.getSalesRepId(),
                venue,
                stage,
                opportunity.getEventTypeId(),
                opportunity.getStatus(),
                opportunity.getEstimatedValue(),
                opportunity.getFinalValue(),
                opportunity.getProbability(),
                opportunity.getEventStart(),
                opportunity.getEventEnd(),
                opportunity.getAttendeeCount(),
                opportunity.getEstimatedCloseDate(),
                opportunity.getClosedAt(),
                opportunity.getOriginId(),
                opportunity.getLossReasonId(),
                opportunity.getEventServiceIds(),
                opportunity.getNotes()
        );
    }
}
