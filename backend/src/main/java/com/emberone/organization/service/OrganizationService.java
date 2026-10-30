/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.dto.*;
import com.emberone.organization.dto.MoveOrganizationRequest;
import com.emberone.organization.event.OrganizationCreatedEvent;
import com.emberone.organization.exception.OrganizationCodeAlreadyExistsException;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.mapper.OrganizationMapper;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.standard.service.StandardReadinessService;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper mapper;
    private final StandardReadinessService standardReadinessService;
    private final OrganizationTypeDefinitionService organizationTypeDefinitionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrganizationDto createRootOrganizationForTenant(
            Long tenantId, SetupOrganizationRequest request) {
        OrganizationTypeDefinition type =
                organizationTypeDefinitionService.requireActiveType(
                        request.getOrganizationTypeId());
        standardReadinessService.requireReadyForOrganizationCreation(tenantId, type.getId());
        if (organizationRepository
                .findByOrganizationCode(request.getOrganizationCode())
                .isPresent()) {
            throw new OrganizationCodeAlreadyExistsException(request.getOrganizationCode());
        }
        try {
            TenantContextHolder.setTenantId(tenantId);
            Organization organization =
                    Organization.builder()
                            .tenantId(tenantId)
                            .organizationCode(request.getOrganizationCode())
                            .name(request.getName())
                            .type(type)
                            .parentOrganization(null)
                            .build();
            Organization saved = organizationRepository.save(organization);
            log.info(
                    "Created root organization {} with type {}",
                    saved.getId(),
                    saved.getType().getId());

            eventPublisher.publishEvent(
                    new OrganizationCreatedEvent(
                            saved.getTenantId(), saved.getId(), saved.getType().getId()));

            return mapper.toDto(saved);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Transactional(readOnly = true)
    public List<OrganizationDto> getChildOrganizations(Long parentOrganizationId) {
        List<Organization> children =
                organizationRepository.findByParentOrganizationId(parentOrganizationId);
        return children.stream()
                .sorted(
                        Comparator.comparing(Organization::getName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(Organization::getId))
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrganizationDto updateOrganization(Long id, UpdateOrganizationRequest request) {
        Organization organization =
                organizationRepository
                        .findById(id)
                        .orElseThrow(() -> new OrganizationNotFoundException(id));
        OrganizationDeactivationService.requireActive(organization);

        // Check if organization code already exists (excluding current organization)
        organizationRepository
                .findByOrganizationCode(request.getOrganizationCode())
                .ifPresent(
                        existing -> {
                            if (!existing.getId().equals(id)) {
                                throw new OrganizationCodeAlreadyExistsException(
                                        request.getOrganizationCode());
                            }
                        });

        mapper.updateFromRequest(request, organization);
        Organization saved = organizationRepository.save(organization);
        log.info("Updated organization {}", saved.getId());
        return mapper.toDto(saved);
    }

    @Transactional
    public OrganizationDto createChildOrganization(
            Long parentOrganizationId, CreateChildOrganizationRequest request) {
        Organization parent =
                organizationRepository
                        .findById(parentOrganizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(parentOrganizationId));
        OrganizationDeactivationService.requireActive(parent);

        // Check if organization code already exists
        if (organizationRepository
                .findByOrganizationCode(request.getOrganizationCode())
                .isPresent()) {
            throw new OrganizationCodeAlreadyExistsException(request.getOrganizationCode());
        }

        OrganizationTypeDefinition type =
                organizationTypeDefinitionService.requireActiveType(
                        request.getOrganizationTypeId());
        standardReadinessService.requireReadyForOrganizationCreation(
                parent.getTenantId(), type.getId());
        Organization child =
                Organization.builder()
                        .tenantId(parent.getTenantId())
                        .organizationCode(request.getOrganizationCode())
                        .name(request.getName())
                        .type(type)
                        .parentOrganization(parent)
                        .build();

        Organization saved = organizationRepository.save(child);
        log.info(
                "Created child organization {} under parent {}",
                saved.getId(),
                parentOrganizationId);

        eventPublisher.publishEvent(
                new OrganizationCreatedEvent(
                        saved.getTenantId(), saved.getId(), saved.getType().getId()));
        return mapper.toDto(saved);
    }

    @Transactional
    public OrganizationDto moveOrganization(
            long tenantId, long organizationId, MoveOrganizationRequest request) {
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (!organization.getTenantId().equals(tenantId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Organization does not belong to this tenant");
        }
        OrganizationDeactivationService.requireActive(organization);

        Long newParentId = request.getNewParentOrganizationId();
        if (newParentId != null && newParentId.equals(organizationId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Organization cannot be its own parent");
        }

        List<Organization> tenantOrganizations = organizationRepository.findAllWithParentFetch();
        if (newParentId != null) {
            Organization newParent =
                    tenantOrganizations.stream()
                            .filter(org -> org.getId().equals(newParentId))
                            .findFirst()
                            .orElseThrow(() -> new OrganizationNotFoundException(newParentId));
            if (!newParent.getTenantId().equals(tenantId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Parent organization does not belong to this tenant");
            }
            OrganizationDeactivationService.requireActive(newParent);
            if (OrganizationSubtreeSupport.isDescendantOf(
                    newParentId, organizationId, tenantOrganizations)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot move organization under one of its descendants");
            }
            organization.setParentOrganization(newParent);
        } else {
            organization.setParentOrganization(null);
        }

        Organization saved = organizationRepository.save(organization);
        log.info(
                "Moved organization {} under parent {}",
                saved.getId(),
                newParentId == null ? "root" : Objects.toString(newParentId));
        return mapper.toDto(saved);
    }
}
