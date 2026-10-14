package com.ztech.crm.customers.mapper;

import com.ztech.crm.customers.domain.Company;
import com.ztech.crm.customers.dto.response.CompanyDetailResponse;
import com.ztech.crm.customers.dto.response.CompanyResponse;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {

    public CompanyResponse toResponse(Company company, String salesRepName) {
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
                company.getSalesRepId(),
                salesRepName
        );
    }

    public CompanyDetailResponse toDetailResponse(Company company, String salesRepName) {
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
                salesRepName,
                company.getOriginId(),
                company.getNotes()
        );
    }

}
