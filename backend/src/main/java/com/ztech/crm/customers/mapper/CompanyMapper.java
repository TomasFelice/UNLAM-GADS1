package com.ztech.crm.customers.mapper;

import com.ztech.crm.customers.domain.Company;
import com.ztech.crm.customers.domain.Contact;
import com.ztech.crm.customers.dto.response.CompanyDetailResponse;
import com.ztech.crm.customers.dto.response.CompanyResponse;
import com.ztech.crm.customers.dto.response.ContactSummaryResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getLegalName(),
                company.getBusinessName(),
                company.getCuit(),
                company.getIndustry(),
                company.getEmail(),
                company.getPhone(),
                company.getLocality(),
                company.getStatus(),
                company.getSalesRepId()
        );
    }

    public CompanyDetailResponse toDetailResponse(Company company, List<Contact> contacts) {
        return new CompanyDetailResponse(
                company.getId(),
                company.getLegalName(),
                company.getBusinessName(),
                company.getCuit(),
                company.getIndustry(),
                company.getEmail(),
                company.getPhone(),
                company.getAddress(),
                company.getLocality(),
                company.getWebsite(),
                company.getStatus(),
                company.getSalesRepId(),
                company.getOriginId(),
                company.getNotes(),
                contacts.stream().map(this::toContactSummary).toList()
        );
    }

    private ContactSummaryResponse toContactSummary(Contact contact) {
        return new ContactSummaryResponse(
                contact.getId(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getStatus(),
                contact.getCompany() == null ? null : contact.getCompany().getId(),
                contact.getSalesRepId()
        );
    }
}
