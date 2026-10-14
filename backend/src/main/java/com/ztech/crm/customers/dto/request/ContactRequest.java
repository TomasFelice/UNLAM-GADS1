package com.ztech.crm.customers.dto.request;

import com.ztech.crm.customers.domain.enums.PartyStatus;
import com.ztech.crm.shared.validation.OptionalText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Se usa tanto para alta como para edición (BE-CUS-04/05), igual que {@code CompanyRequest}. */
public record ContactRequest(

        @Schema(description = "Id de la empresa relacionada. Vacío si es cliente individual (Módulo 1 de la consigna).")
        Long companyId,

        @Schema(example = "María")
        @NotBlank(message = "El nombre es obligatorio.")
        String firstName,

        @Schema(example = "Gómez")
        @NotBlank(message = "El apellido es obligatorio.")
        String lastName,

        @Schema(description = "Opcional", example = "30123456")
        String document,

        @Schema(description = "Cargo, si corresponde", example = "Responsable de compras")
        String position,

        @Schema(example = "maria.gomez@empresa.com")
        @Email(message = "El email no tiene un formato válido.")
        String email,

        @Schema(example = "+54 11 4000-0000")
        String phone,

        @NotNull(message = "El estado es obligatorio.")
        PartyStatus status,

        @Schema(description = "Id del usuario responsable comercial, si ya está asignado")
        Long salesRepId,

        @Schema(description = "Id del origen comercial")
        Long originId,

        String notes
) {
    public ContactRequest {
        document = OptionalText.normalize(document);
        position = OptionalText.normalize(position);
        email = OptionalText.normalize(email);
        phone = OptionalText.normalize(phone);
        notes = OptionalText.normalize(notes);
    }
}
