package com.ztech.crm.catalogs.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Alta/edición completa; active=false expresa la baja lógica. */
public record CatalogEntryRequest(
        @NotBlank(message = "El nombre es obligatorio.") String name,
        @NotNull(message = "Indicá si la entrada está activa.") Boolean active
) {
    public CatalogEntryRequest { name = name == null ? null : name.strip(); }
}
