/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.dto.OnboardingInvitationDto;
import com.emberone.onboarding.dto.OnboardingInvitationPageDto;
import com.emberone.onboarding.dto.SelfOnboardingInvitationDto;
import com.emberone.onboarding.list.OnboardingInvitationListDefinition;
import com.emberone.onboarding.list.OnboardingInvitationListSpecs;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.onboarding.repository.OnboardingInvitationStepDataRepository;
import com.emberone.onboardingdocument.domain.EntryDocumentFulfillmentChannel;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.domain.User;
import com.emberone.user.domain.UserContactInformation;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.userdocument.service.UserDocumentService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingInvitationService {

    private final OrganizationRepository organizationRepository;
    private final OnboardingInvitationRepository onboardingInvitationRepository;
    private final OnboardingInvitationStepDataRepository stepDataRepository;
    private final UserRepository userRepository;
    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    private final UserDocumentService userDocumentService;
    private final OnboardingPromotionService onboardingPromotionService;
    private final OnboardingInvitationReadinessService onboardingInvitationReadinessService;
    private final OnboardingInvitationDraftStorageService invitationDraftStorageService;

    @Transactional(readOnly = true)
    public boolean hasPendingForUserAndOrganization(Long userId, Long organizationId) {
        return onboardingInvitationRepository.existsByUserIdAndTargetOrganizationIdAndStatusIn(
                userId, organizationId, OnboardingInvitationStatus.pending());
    }

    /** Whether any invitation already exists for this user and target organization. */
    @Transactional(readOnly = true)
    public boolean hasInvitationForUserAndOrganization(Long userId, Long organizationId) {
        return onboardingInvitationRepository
                .findByUserIdAndTargetOrganizationId(userId, organizationId)
                .isPresent();
    }

    /**
     * Creates the onboarding invitation for admission: empty {@code APPROVED} when all configured
     * steps are live-fulfilled (including no steps configured), otherwise {@code OPEN}. Matches
     * join-workflow direct admission semantics.
     *
     * @return {@code true} when live-fulfilled (direct admission invitation); {@code false} when
     *     onboarding must be completed
     */
    @Transactional
    public boolean recordInvitationForAdmission(
            long tenantId, long userId, long targetOrganizationId, Long invitedByUserId) {
        if (onboardingInvitationReadinessService.areAllStepsLiveFulfilled(
                tenantId, userId, targetOrganizationId)) {
            createSucceededEmptyForExistingUser(
                    tenantId, userId, targetOrganizationId, invitedByUserId);
            return true;
        }
        createForExistingUser(tenantId, userId, targetOrganizationId, invitedByUserId);
        return false;
    }

    /**
     * Records an empty {@code APPROVED} invitation when live fulfillment allows direct admission
     * and no invitation exists yet. Does not create membership.
     */
    @Transactional
    public void ensureSucceededInvitationWhenLiveFulfilled(
            long tenantId, long userId, long organizationId) {
        if (hasInvitationForUserAndOrganization(userId, organizationId)) {
            return;
        }
        if (!onboardingInvitationReadinessService.areAllStepsLiveFulfilled(
                tenantId, userId, organizationId)) {
            return;
        }
        createSucceededEmptyForExistingUser(tenantId, userId, organizationId, null);
    }

    /** Opens onboarding for an existing user and target organization. */
    @Transactional
    public OnboardingInvitation createForExistingUser(
            Long tenantId, Long userId, Long targetOrganizationId, Long invitedByUserId) {
        return createForExistingUser(
                tenantId,
                userId,
                targetOrganizationId,
                invitedByUserId,
                OnboardingInvitationStatus.OPEN);
    }

    /**
     * Creates an empty {@code APPROVED} invitation when the user already fulfills every configured
     * onboarding step. Callers must create the organization membership separately.
     */
    @Transactional
    public OnboardingInvitation createSucceededEmptyForExistingUser(
            Long tenantId, Long userId, Long targetOrganizationId, Long invitedByUserId) {
        return createForExistingUser(
                tenantId,
                userId,
                targetOrganizationId,
                invitedByUserId,
                OnboardingInvitationStatus.APPROVED);
    }

    private OnboardingInvitation createForExistingUser(
            Long tenantId,
            Long userId,
            Long targetOrganizationId,
            Long invitedByUserId,
            OnboardingInvitationStatus status) {
        if (onboardingInvitationRepository
                .findByUserIdAndTargetOrganizationId(userId, targetOrganizationId)
                .isPresent()) {
            throw new ConflictException(
                    "Onboarding invitation already exists for this organization");
        }

        Organization organization =
                organizationRepository
                        .findById(targetOrganizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(targetOrganizationId));
        if (!tenantId.equals(organization.getTenantId())) {
            throw new IllegalArgumentException("Organization does not belong to tenant");
        }
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        OnboardingInvitation saved =
                onboardingInvitationRepository.save(
                        OnboardingInvitation.builder()
                                .tenantId(tenantId)
                                .userId(userId)
                                .targetOrganizationId(targetOrganizationId)
                                .status(status)
                                .invitedByUserId(invitedByUserId)
                                .build());
        log.info(
                "Created onboarding invitation {} with status {} for user {} in organization {}",
                saved.getId(),
                status,
                userId,
                targetOrganizationId);
        return saved;
    }

    @Transactional(readOnly = true)
    public OnboardingInvitationPageDto listForOrganization(Long organizationId, ListQuery query) {
        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        Pageable pageable = OnboardingInvitationListDefinition.INSTANCE.pageable(query);
        Specification<OnboardingInvitation> spec =
                Specification.allOf(
                        OnboardingInvitationListSpecs.forOrganization(organizationId),
                        OnboardingInvitationListSpecs.pendingStatuses(),
                        OnboardingInvitationListSpecs.withActiveUser(),
                        OnboardingInvitationListSpecs.withoutActiveOrganizationMembership(
                                organizationId),
                        OnboardingInvitationListDefinition.INSTANCE.searchSpecification(query));
        Page<OnboardingInvitation> page = onboardingInvitationRepository.findAll(spec, pageable);
        List<OnboardingInvitationDto> content =
                page.getContent().stream()
                        .map(invitation -> toOrgDto(invitation, invitation.getUser()))
                        .filter(Objects::nonNull)
                        .toList();
        return OnboardingInvitationPageDto.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SelfOnboardingInvitationDto> listAllForUser(Long userId) {
        List<OnboardingInvitation> invitations =
                onboardingInvitationRepository.findByUserId(userId);
        Map<Long, Organization> organizationsById =
                organizationRepository
                        .findAllById(
                                invitations.stream()
                                        .map(OnboardingInvitation::getTargetOrganizationId)
                                        .toList())
                        .stream()
                        .collect(Collectors.toMap(Organization::getId, Function.identity()));

        Instant now = Instant.now();
        return invitations.stream()
                .filter(
                        invitation ->
                                invitation.getStatus() != OnboardingInvitationStatus.OPEN
                                        || !userOrganizationMembershipRepository
                                                .existsActiveByUser_IdAndOrganization_Id(
                                                        userId,
                                                        invitation.getTargetOrganizationId(),
                                                        now))
                .map(
                        invitation ->
                                toSelfDto(
                                        invitation,
                                        organizationsById.get(
                                                invitation.getTargetOrganizationId())))
                .filter(Objects::nonNull)
                .sorted(
                        Comparator.comparing(
                                        SelfOnboardingInvitationDto::getTargetOrganizationName,
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(SelfOnboardingInvitationDto::getId))
                .toList();
    }

    @Transactional
    public void markSubmitted(Long tenantId, Long userId, Long invitationId) {
        OnboardingInvitation invitation = requireOpenInvitation(tenantId, userId, invitationId);
        invitation.setStatus(OnboardingInvitationStatus.SUBMITTED);
        onboardingInvitationRepository.save(invitation);
    }

    @Transactional
    public void markApproved(Long tenantId, Long invitationId) {
        OnboardingInvitation invitation =
                onboardingInvitationRepository
                        .findById(invitationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Onboarding invitation not found: "
                                                        + invitationId));
        if (!invitation.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Invitation does not belong to tenant");
        }
        if (invitation.getStatus() != OnboardingInvitationStatus.SUBMITTED) {
            throw new ConflictException("Onboarding invitation is not submitted");
        }
        invitation.setStatus(OnboardingInvitationStatus.APPROVED);
        onboardingInvitationRepository.save(invitation);
    }

    /**
     * Completes an open invitation for a user who already has org membership (admin-recorded
     * onboarding). Promotes drafts and marks {@code APPROVED} without opening a workflow.
     */
    @Transactional
    public void recordCompletedOnboarding(
            Long tenantId, Long invitationId, Long recordingAdminUserId, String adminRecordReason) {
        OnboardingInvitation invitation =
                onboardingInvitationRepository
                        .findById(invitationId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Onboarding invitation not found: "
                                                        + invitationId));
        if (!invitation.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Invitation does not belong to tenant");
        }
        if (invitation.getStatus() != OnboardingInvitationStatus.OPEN) {
            throw new ConflictException("Onboarding invitation is not open");
        }
        if (!userOrganizationMembershipRepository.existsActiveByUser_IdAndOrganization_Id(
                invitation.getUserId(), invitation.getTargetOrganizationId(), Instant.now())) {
            throw new ConflictException(
                    "User must already be an active member of the target organization");
        }
        if (!onboardingInvitationReadinessService.areAllStepsFulfilled(invitation)) {
            throw new ConflictException("Onboarding steps are not complete");
        }
        if (adminRecordReason == null || adminRecordReason.isBlank()) {
            throw new ConflictException("Admin onboarding recording requires a reason");
        }
        onboardingPromotionService.promoteAllSteps(
                invitation,
                recordingAdminUserId,
                EntryDocumentFulfillmentChannel.ADMIN_RECORDED,
                adminRecordReason.trim());
        invitation.setStatus(OnboardingInvitationStatus.APPROVED);
        onboardingInvitationRepository.save(invitation);
        log.info("Recorded completed onboarding invitation {}", invitation.getId());
    }

    @Transactional
    public void reopen(Long tenantId, Long userId, Long invitationId) {
        OnboardingInvitation invitation =
                onboardingInvitationRepository
                        .findByIdAndUserId(invitationId, userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Onboarding invitation not found: "
                                                        + invitationId));
        if (!invitation.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Invitation does not belong to tenant");
        }
        if (invitation.getStatus() != OnboardingInvitationStatus.SUBMITTED) {
            throw new ConflictException("Onboarding invitation is not submitted");
        }
        invitation.setStatus(OnboardingInvitationStatus.OPEN);
        onboardingInvitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public OnboardingInvitation requireInvitationForOrganization(
            Long organizationId, Long invitationId) {
        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        return onboardingInvitationRepository
                .findByIdAndTargetOrganizationId(invitationId, organizationId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Onboarding invitation not found: " + invitationId));
    }

    @Transactional(readOnly = true)
    public OnboardingInvitation requireInvitationForOrganizationUser(
            Long organizationId, Long userId) {
        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        return onboardingInvitationRepository
                .findByUserIdAndTargetOrganizationId(userId, organizationId)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Onboarding invitation not found for user: " + userId));
    }

    private OnboardingInvitation requireOpenInvitation(
            Long tenantId, Long userId, Long invitationId) {
        OnboardingInvitation invitation =
                onboardingInvitationRepository
                        .findByIdAndUserId(invitationId, userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Onboarding invitation not found: "
                                                        + invitationId));
        if (!invitation.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Invitation does not belong to tenant");
        }
        if (invitation.getStatus() != OnboardingInvitationStatus.OPEN) {
            throw new ConflictException("Onboarding invitation is not open");
        }
        return invitation;
    }

    /**
     * Removes pending ({@code OPEN} / {@code SUBMITTED}) invitations and their stored draft uploads
     * for a user leaving. {@code APPROVED} invitations stay until the {@code ONBOARDING} retention
     * class deletes them after exit.
     */
    @Transactional
    public void deletePendingForUser(Long userId) {
        for (OnboardingInvitation invitation :
                onboardingInvitationRepository.findByUserIdAndStatusIn(
                        userId, OnboardingInvitationStatus.pending())) {
            userDocumentService.deleteDocumentsForLinkTargets(
                    invitation.getTenantId(),
                    AppLinkType.ONBOARDING_INVITATION,
                    List.of(invitation.getId()));
            invitationDraftStorageService.deleteDraftFilesForInvitation(invitation.getId());
            onboardingInvitationRepository.delete(invitation);
            log.info("Deleted onboarding invitation {}", invitation.getId());
        }
    }

    private static OnboardingInvitationDto toOrgDto(OnboardingInvitation invitation, User user) {
        if (user == null) {
            return null;
        }
        UserContactInformation contact = user.getContactInformation();
        return OnboardingInvitationDto.builder()
                .id(invitation.getId())
                .userId(user.getId())
                .personalNumber(user.getPersonalNumber())
                .salutation(user.getSalutation())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .privateEmail(contact != null ? contact.getPrivateEmail() : null)
                .street(contact != null ? contact.getStreet() : null)
                .city(contact != null ? contact.getCity() : null)
                .postalCode(contact != null ? contact.getPostalCode() : null)
                .country(contact != null ? contact.getCountry() : null)
                .status(invitation.getStatus())
                .invitedAt(invitation.getInvitedAt())
                .build();
    }

    private static SelfOnboardingInvitationDto toSelfDto(
            OnboardingInvitation invitation, Organization organization) {
        if (organization == null) {
            return null;
        }
        return SelfOnboardingInvitationDto.builder()
                .id(invitation.getId())
                .targetOrganizationId(organization.getId())
                .targetOrganizationName(organization.getName())
                .status(invitation.getStatus())
                .invitedAt(invitation.getInvitedAt())
                .build();
    }
}
