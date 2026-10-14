package com.ztech.crm.offerings.service;

import com.ztech.crm.offerings.domain.Venue;
import com.ztech.crm.offerings.dto.response.VenueResponse;
import com.ztech.crm.offerings.dto.request.VenueRequest;
import com.ztech.crm.offerings.mapper.VenueMapper;
import com.ztech.crm.offerings.repository.VenueRepository;
import com.ztech.crm.offerings.repository.specification.VenueSpecifications;
import com.ztech.crm.offerings.domain.enums.VenueStatus;
import com.ztech.crm.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.ztech.crm.shared.exception.BusinessException;
import com.ztech.crm.shared.exception.ConflictException;
import com.ztech.crm.shared.exception.NotFoundException;
import com.ztech.crm.shared.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/** BE-OFF-01/02: consulta comercial y administración de salones del tenant. */
@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;
    private final VenueReservationPort reservations;

    public VenueService(VenueRepository venueRepository, VenueMapper venueMapper, VenueReservationPort reservations) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
        this.reservations = reservations;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public VenueResponse create(VenueRequest request) {
        Venue venue = new Venue(TenantContext.currentTenantId(), request.name(), request.capacity());
        apply(venue, request);
        return venueMapper.toResponse(venueRepository.save(venue));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public VenueResponse update(Long id, VenueRequest request) {
        Venue venue = venueRepository.lockOwned(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el salón solicitado."));
        if (request.capacity() < venue.getCapacity()
                && reservations.hasReservationAboveCapacity(TenantContext.currentTenantId(), id, request.capacity())) {
            throw new ConflictException("VENUE_RESERVED_CAPACITY",
                    "No se puede reducir la capacidad por debajo de los asistentes de una reserva confirmada.");
        }
        apply(venue, request);
        return venueMapper.toResponse(venueRepository.save(venue));
    }

    private void apply(Venue venue, VenueRequest request) {
        venue.updateDetails(request.name(), request.capacity(), request.rate(), request.address(),
                request.locality(), request.description(), request.equipment(), request.status());
    }

    @Transactional(readOnly = true)
    public PageResponse<VenueResponse> list(String q, VenueStatus status, Pageable pageable) {
        var filters = VenueSpecifications.tenant(TenantContext.currentTenantId());
        if (q != null && !q.isBlank()) filters = filters.and(VenueSpecifications.contains(q));
        if (status != null) filters = filters.and(VenueSpecifications.status(status));
        var stable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().and(Sort.by("id")));
        return PageResponse.from(venueRepository.findAll(filters, stable), venueMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public VenueResponse getDetail(Long id) {
        return venueMapper.toResponse(getOwnedOrThrow(id));
    }

    /** Exige disponibilidad para una asociación nueva; conserva la anterior del tenant. */
    @Transactional(readOnly = true)
    public VenueInfo getForAssociation(Long id, Long previousId) {
        Venue venue = getOwnedOrThrow(id);
        if (!id.equals(previousId) && !venue.isAvailable()) {
            throw new BusinessException("VENUE_UNAVAILABLE", "El salón seleccionado no está disponible.");
        }
        return info(venue);
    }

    @Transactional(readOnly = true)
    public VenueInfo getInfo(Long id) {
        return info(getOwnedOrThrow(id));
    }

    /** El bloqueo compartido admite confirmaciones paralelas, pero serializa cambios de capacidad/estado. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public VenueInfo getForConfirmation(Long id) {
        return info(venueRepository.lockReadable(id, TenantContext.currentTenantId())
                .orElseThrow(() -> new NotFoundException("No se encontró el salón solicitado.")));
    }

    private VenueInfo info(Venue venue) {
        return new VenueInfo(venue.getId(), venue.getName(), venue.getCapacity(), venue.getRate(),
                venue.getAddress(), venue.getLocality(), venue.getDescription(), venue.getEquipment(), venue.getStatus().name());
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, String> getNames(java.util.Set<Long> ids) {
        if (ids.isEmpty()) return java.util.Map.of();
        return venueRepository.findAllByTenantIdAndIdIn(TenantContext.currentTenantId(), ids).stream()
                .collect(java.util.stream.Collectors.toMap(Venue::getId, Venue::getName));
    }

    private Venue getOwnedOrThrow(Long id) {
        Long tenantId = TenantContext.currentTenantId();
        return venueRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("No se encontró el salón solicitado."));
    }
}
