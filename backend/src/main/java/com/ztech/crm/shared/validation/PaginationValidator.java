package com.ztech.crm.shared.validation;

import com.ztech.crm.shared.exception.BadRequestException;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Construye paginación acotada y evita ordenar por propiedades JPA arbitrarias. */
public final class PaginationValidator {

    private PaginationValidator() {
    }

    public static Pageable of(int page, int size, String sort, Set<String> allowedFields,
                              String defaultField) {
        if (page < 0) {
            throw new BadRequestException("INVALID_PAGE", "La página no puede ser negativa.");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("INVALID_PAGE_SIZE", "El tamaño de página debe estar entre 1 y 100.");
        }
        String effective = sort == null || sort.isBlank() ? defaultField + ",asc" : sort;
        String[] parts = effective.split(",", -1);
        if (parts.length > 2 || parts[0].isBlank() || !allowedFields.contains(parts[0])) {
            throw new BadRequestException("INVALID_SORT", "El campo de orden no es válido.");
        }
        Sort.Direction direction;
        try {
            direction = parts.length == 1 ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("INVALID_SORT", "La dirección de orden debe ser asc o desc.");
        }
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }
}
