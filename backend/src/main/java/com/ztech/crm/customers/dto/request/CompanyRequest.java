package com.ztech.crm.customers.dto.request;

import com.ztech.crm.customers.domain.enums.PartyStatus;
import com.ztech.crm.shared.validation.ValidCuit;
import com.ztech.crm.shared.validation.ValidWebsite;
import com.ztech.crm.shared.validation.OptionalText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Se usa tanto para alta como para edición (BE-CUS-01/02): los campos editables son los
 * mismos en ambos casos, así que no hace falta un {@code UpdateCompanyRequest} aparte.
 */
public record CompanyRequest(

        @Schema(example = "Eventos del Sur SA")
        @NotBlank(message = "La razón social es obligatoria.")
        String legalName,

        @Schema(example = "Eventos del Sur")
        @NotBlank(message = "El nombre comercial es obligatorio.")
        String businessName,

        @Schema(description = "Opcional. 11 dígitos con verificador válido, con o sin guiones; se almacena sin guiones.", example = "30-71234567-1")
        @ValidCuit
        String cuit,

        @Schema(example = "Turismo y eventos")
        String industry,

        @Schema(example = "contacto@eventosdelsur.com")
        @Email(message = "El email no tiene un formato válido.")
        String email,

        @Schema(example = "+54 11 4000-0000")
        String phone,

        @Schema(example = "Av. Rivadavia 4500, San Justo")
        String address,

        @Schema(example = "San Justo")
        String locality,

        @Schema(example = "https://eventosdelsur.com")
        @ValidWebsite
        String website,

        @NotNull(message = "El estado es obligatorio.")
        PartyStatus status,

        @Schema(description = "Id del responsable. Si se omite, el backend aplica la herencia de DP-02")
        Long salesRepId,

        @Schema(description = "Id del origen comercial")
        Long originId,

        String notes
) {
    public CompanyRequest {
        cuit = OptionalText.normalize(cuit);
        industry = OptionalText.normalize(industry);
        email = OptionalText.normalize(email);
        phone = OptionalText.normalize(phone);
        address = OptionalText.normalize(address);
        locality = OptionalText.normalize(locality);
        website = OptionalText.normalize(website);
        notes = OptionalText.normalize(notes);
    }

    /** Compatibilidad de construcción interna con los tests de E1; el contrato HTTP usa los campos finales. */
    public CompanyRequest(String name, String cuit, String industry, String email, String phone,
                          String address, String website, PartyStatus status, Long salesRepId,
                          Long originId, String notes) {
        this(name, name, cuit, industry, email, phone, address, null, website, status,
                salesRepId, originId, notes);
    }
}
