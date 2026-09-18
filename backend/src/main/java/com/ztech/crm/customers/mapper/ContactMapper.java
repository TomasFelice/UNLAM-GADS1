package com.ztech.crm.customers.mapper;

import com.ztech.crm.customers.domain.Company;
import com.ztech.crm.customers.domain.Contact;
import com.ztech.crm.customers.dto.response.CompanySummaryResponse;
import com.ztech.crm.customers.dto.response.ContactDetailResponse;
import com.ztech.crm.customers.dto.response.ContactResponse;
import org.springframework.stereotype.Component;

@Component
public class ContactMapper {

    public ContactResponse toResponse(Contact contact) {
        Company company = contact.getCompany();
        return new ContactResponse(
                contact.getId(),
                company == null ? null : company.getId(),
                company == null ? null : company.getBusinessName(),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getStatus()
        );
    }

    public ContactDetailResponse toDetailResponse(Contact contact) {
        Company company = contact.getCompany();
        return new ContactDetailResponse(
                contact.getId(),
                company == null ? null : new CompanySummaryResponse(company.getId(), company.getLegalName(),
                        company.getBusinessName(), company.getSalesRepId()),
                contact.getFirstName(),
                contact.getLastName(),
                contact.getDocument(),
                contact.getPosition(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getStatus(),
                contact.getSalesRepId(),
                contact.getOriginId(),
                contact.getNotes()
        );
    }
}
