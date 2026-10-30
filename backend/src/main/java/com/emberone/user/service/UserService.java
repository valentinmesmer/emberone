/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import com.emberone.identity.dto.ProvisionIdentityUserRequest;
import com.emberone.identity.dto.UpdateIdentityUserRequest;
import com.emberone.identity.service.IdentityUserService;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.onboarding.service.OnboardingInvitationService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.dto.OrganizationDto;
import com.emberone.organization.dto.OrganizationTreeDto;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.mapper.OrganizationMapper;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.dto.GroupTreeDto;
import com.emberone.organizationgroup.dto.OrganizationGroupDto;
import com.emberone.organizationgroup.dto.OrganizationGroupJoinTargetDto;
import com.emberone.organizationgroup.dto.OrganizationGroupLeaveTargetDto;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.organizationgroup.service.AllMembersGroupService;
import com.emberone.organizationgroup.service.OrganizationGroupService;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.domain.User;
import com.emberone.user.domain.UserContactInformation;
import com.emberone.user.dto.*;
import com.emberone.user.event.NotificationEmailChangeRequestedEvent;
import com.emberone.user.event.UserCreatedEvent;
import com.emberone.user.exception.UserAlreadyDeactivatedException;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.list.OrganizationMemberListDefinition;
import com.emberone.user.list.OrganizationMemberListSpecs;
import com.emberone.user.mapper.UserMapper;
import com.emberone.user.repository.UserContactInformationRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.util.UsernamePartNormalizer;
import com.emberone.userorganizationgroupmembership.dto.CreateUserOrganizationGroupMembershipRequest;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.dto.CreateUserOrganizationMembershipRequest;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import com.emberone.userorganizationmembership.mapper.UserOrganizationMembershipMapper;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.repository.WorkflowTypeMappingRepository;
import com.emberone.workflow.service.WorkflowOpenConcurrencyService;
import com.emberone.workflow.spi.WorkflowOpenConcurrencyKeys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final int EMAIL_VERIFICATION_TOKEN_BYTES = 32;
    private static final int EMAIL_VERIFICATION_TTL_HOURS = 24;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final IdentityUserService identityUserService;
    private final OrganizationGroupService organizationGroupService;
    private final AllMembersGroupService allMembersGroupService;
    private final OrganizationGroupRepository organizationGroupRepository;
    private final UserOrganizationGroupMembershipService userOrganizationGroupMembershipService;
    private final OrganizationRepository organizationRepository;
    private final UserOrganizationMembershipMapper userOrganizationMembershipMapper;
    private final UserOrganizationMembershipRepository userOrganizationMembershipRepository;
    private final UserOrganizationMembershipService userOrganizationMembershipService;
    private final UserContactInformationRepository userContactInformationRepository;
    private final UsernameService usernameService;
    private final UserPersonalNumberService userPersonalNumberService;
    private final ApplicationEventPublisher eventPublisher;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowTypeMappingRepository workflowTypeMappingRepository;
    private final WorkflowOpenConcurrencyService workflowOpenConcurrencyService;
    private final OrganizationMapper organizationMapper;
    private final OnboardingInvitationRepository onboardingInvitationRepository;
    private final OnboardingInvitationService onboardingInvitationService;
    private final TokenService tokenService;

    private static String generateVerificationToken() {
        byte[] bytes = new byte[EMAIL_VERIFICATION_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Transactional
    public UserDto createInvitedUser(
            Long tenantId, Long targetOrganizationId, CreateUserRequest request) {
        Organization organization =
                organizationRepository
                        .findById(targetOrganizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(targetOrganizationId));
        if (!tenantId.equals(organization.getTenantId())) {
            throw new IllegalArgumentException("Organization does not belong to tenant");
        }

        User saved = saveNewUserWithContact(tenantId, request);
        log.info(
                "Created invited user {} for organization {}", saved.getId(), targetOrganizationId);

        provisionIdentityForNewUser(saved);

        Long invitedByUserId = tokenService.getReferencedUserId();
        if (onboardingInvitationService.recordInvitationForAdmission(
                tenantId, saved.getId(), targetOrganizationId, invitedByUserId)) {
            ensureOrganizationMembershipAndAllMembersGroup(
                    tenantId, saved.getId(), targetOrganizationId);
        }

        return mapper.toDto(saved);
    }

    private User saveNewUserWithContact(Long tenantId, CreateUserRequest request) {
        return userRepository.save(buildNewUserWithContact(tenantId, request));
    }

    private User buildNewUserWithContact(Long tenantId, CreateUserRequest request) {
        User user =
                User.builder()
                        .tenantId(tenantId)
                        .personalNumber(userPersonalNumberService.allocateNext(tenantId))
                        .salutation(request.getSalutation())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .username(
                                usernameService.generateUniqueUsername(
                                        tenantId, request.getFirstName(), request.getLastName()))
                        .dateOfBirth(request.getDateOfBirth())
                        .build();

        UserContactInformation contactInformation =
                UserContactInformation.builder()
                        .tenantId(tenantId)
                        .user(user)
                        .privateEmail(request.getPrivateEmail())
                        .street(request.getStreet())
                        .city(request.getCity())
                        .postalCode(request.getPostalCode())
                        .country(request.getCountry())
                        .build();

        user.setContactInformation(contactInformation);
        return user;
    }

    private void provisionIdentityForNewUser(User saved) {
        identityUserService.provisionUser(
                ProvisionIdentityUserRequest.builder()
                        .userId(saved.getId())
                        .username(saved.getUsername())
                        .firstName(saved.getFirstName())
                        .lastName(saved.getLastName())
                        .privateEmail(saved.getContactInformation().getPrivateEmail())
                        .role(EmberOneAppRole.USER.getRoleName())
                        .tenantId(saved.getTenantId())
                        .build());
    }

    @Transactional
    public UserDto createInitialTenantMember(Long tenantId, SetupUserRequest request) {
        try {
            TenantContextHolder.setTenantId(tenantId);
            Organization organization =
                    organizationRepository
                            .findById(request.getMainOrganizationId())
                            .orElseThrow(
                                    () ->
                                            new OrganizationNotFoundException(
                                                    request.getMainOrganizationId()));
            if (!tenantId.equals(organization.getTenantId())) {
                throw new IllegalArgumentException("Organization does not belong to tenant");
            }

            User user =
                    User.builder()
                            .tenantId(tenantId)
                            .personalNumber(userPersonalNumberService.allocateNext(tenantId))
                            .salutation(request.getSalutation())
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .username(
                                    usernameService.generateUniqueUsername(
                                            tenantId,
                                            request.getFirstName(),
                                            request.getLastName()))
                            .dateOfBirth(request.getDateOfBirth())
                            .build();

            UserContactInformation contactInformation =
                    UserContactInformation.builder()
                            .tenantId(tenantId)
                            .user(user)
                            .privateEmail(request.getPrivateEmail())
                            .street(request.getStreet())
                            .city(request.getCity())
                            .postalCode(request.getPostalCode())
                            .country(request.getCountry())
                            .build();
            user.setContactInformation(contactInformation);

            UserOrganizationMembership mainMembership =
                    UserOrganizationMembership.builder()
                            .tenantId(tenantId)
                            .user(user)
                            .organization(organization)
                            .main(true)
                            .startDate(Instant.now())
                            .build();
            user.getOrganizationMemberships().add(mainMembership);

            User saved = userRepository.save(user);
            eventPublisher.publishEvent(
                    new UserCreatedEvent(saved.getTenantId(), saved.getId(), organization.getId()));

            identityUserService.provisionUser(
                    ProvisionIdentityUserRequest.builder()
                            .userId(saved.getId())
                            .username(saved.getUsername())
                            .firstName(saved.getFirstName())
                            .lastName(saved.getLastName())
                            .privateEmail(saved.getContactInformation().getPrivateEmail())
                            .role(EmberOneAppRole.USER.getRoleName())
                            .tenantId(saved.getTenantId())
                            .build());

            return mapper.toDto(saved);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Transactional
    public void ensureOrganizationMembershipAndAllMembersGroup(
            long tenantId, long userId, long organizationId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getTenantId() != tenantId) {
            throw new IllegalArgumentException("User does not belong to tenant");
        }
        Organization organization =
                organizationRepository
                        .findById(organizationId)
                        .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (organization.getTenantId() != tenantId) {
            throw new IllegalArgumentException("Organization does not belong to tenant");
        }
        Instant now = Instant.now();
        boolean hasActiveMembership =
                user.getOrganizationMemberships().stream()
                        .anyMatch(
                                m ->
                                        m.getOrganization().getId().equals(organizationId)
                                                && m.isActiveAt(now));
        if (!hasActiveMembership) {
            boolean main =
                    user.getOrganizationMemberships().stream()
                            .noneMatch(m -> Boolean.TRUE.equals(m.getMain()) && m.isActiveAt(now));
            userOrganizationMembershipService.createMembership(
                    CreateUserOrganizationMembershipRequest.builder()
                            .userId(userId)
                            .organizationId(organizationId)
                            .startDate(now)
                            .main(main)
                            .build());
            log.info(
                    "Added organization membership for user {} in organization {} (main={})",
                    userId,
                    organizationId,
                    main);
        }
        onboardingInvitationService.ensureSucceededInvitationWhenLiveFulfilled(
                tenantId, userId, organizationId);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        return mapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public List<OrganizationTreeDto> getOrganizationTreeForUser(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        List<UserOrganizationMembership> orgMemberships =
                user.getOrganizationMemberships().stream()
                        .filter(m -> m.isActiveAt(Instant.now()))
                        .toList();

        Map<Long, OrganizationTreeDto> orgMap = new java.util.HashMap<>();
        Map<Long, Long> parentOrgMap = new java.util.HashMap<>();

        for (UserOrganizationMembership membership : orgMemberships) {
            Organization org = membership.getOrganization();
            Long orgId = org.getId();

            List<GroupTreeDto> groups =
                    organizationGroupService
                            .getGroupsForUserInOrganization(userId, orgId, false)
                            .stream()
                            .map(
                                    groupDto ->
                                            GroupTreeDto.builder()
                                                    .id(groupDto.getId())
                                                    .name(groupDto.getName())
                                                    .description(groupDto.getDescription())
                                                    .type(groupDto.getType())
                                                    .build())
                            .collect(Collectors.toList());

            Long parentOrgId =
                    org.getParentOrganization() != null
                            ? org.getParentOrganization().getId()
                            : null;
            parentOrgMap.put(orgId, parentOrgId);

            OrganizationTreeDto orgTree =
                    OrganizationTreeDto.builder()
                            .id(orgId)
                            .name(org.getName())
                            .organizationCode(org.getOrganizationCode())
                            .main(membership.getMain())
                            .groups(groups)
                            .children(new ArrayList<>())
                            .build();

            orgMap.put(orgId, orgTree);
        }

        List<OrganizationTreeDto> rootOrganizations = new ArrayList<>();
        for (Map.Entry<Long, OrganizationTreeDto> entry : orgMap.entrySet()) {
            Long orgId = entry.getKey();
            OrganizationTreeDto orgTree = entry.getValue();
            Long parentOrgId = parentOrgMap.get(orgId);

            if (parentOrgId == null) {
                rootOrganizations.add(orgTree);
            } else {
                OrganizationTreeDto parent = orgMap.get(parentOrgId);
                if (parent != null) {
                    parent.getChildren().add(orgTree);
                } else {
                    rootOrganizations.add(orgTree);
                }
            }
        }

        sortOrganizationTree(rootOrganizations);
        return rootOrganizations;
    }

    private void sortOrganizationTree(List<OrganizationTreeDto> organizations) {
        organizations.sort(
                Comparator.comparing(OrganizationTreeDto::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(OrganizationTreeDto::getId));
        organizations.forEach(organization -> sortOrganizationTree(organization.getChildren()));
    }

    @Transactional
    public UserDto updateContactInformationSelf(
            Long userId, UpdateContactInformationRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        String requestedEmail = request.getPrivateEmail().trim();
        UserContactInformation contactInformation = user.getContactInformation();
        if (contactInformation == null
                || !StringUtils.hasText(contactInformation.getPrivateEmail())) {
            throw new IllegalStateException("User has no notification email");
        }

        if (requestedEmail.equalsIgnoreCase(contactInformation.getPrivateEmail())) {
            clearPendingEmailVerification(contactInformation);
            User saved = userRepository.save(user);
            log.info(
                    "Cleared pending notification-email change for user {} (same address)", userId);
            return mapper.toDto(saved);
        }

        String rawToken = beginPendingEmailVerification(contactInformation, requestedEmail);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(
                new NotificationEmailChangeRequestedEvent(
                        saved.getTenantId(),
                        saved.getId(),
                        saved.getFirstName(),
                        requestedEmail,
                        rawToken));
        log.info("Requested notification-email change for user {}", userId);
        return mapper.toDto(saved);
    }

    @Transactional
    public UserDto verifyNotificationEmailSelf(Long userId, String rawToken) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        return mapper.toDto(applyNotificationEmailVerification(user, rawToken));
    }

    /**
     * Public verification by token. Caller must bind the tenant ({@code TenantScopeGuard}) before
     * entering, because the pending contact row is looked up within the tenant scope.
     */
    @Transactional
    public void verifyNotificationEmailByToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            throw new IllegalArgumentException("Invalid notification email verification token");
        }
        UserContactInformation contactInformation =
                userContactInformationRepository
                        .findByEmailVerificationTokenHash(hashToken(rawToken.trim()))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Invalid notification email verification token"));
        User user = contactInformation.getUser();
        if (user == null) {
            throw new IllegalArgumentException("Invalid notification email verification token");
        }
        if (user.getContactInformation() == null) {
            user.setContactInformation(contactInformation);
        }
        applyNotificationEmailVerification(user, rawToken.trim());
    }

    private User applyNotificationEmailVerification(User user, String rawToken) {
        UserContactInformation contactInformation = user.getContactInformation();
        if (contactInformation == null
                || !StringUtils.hasText(contactInformation.getPendingEmail())
                || !StringUtils.hasText(contactInformation.getEmailVerificationTokenHash())
                || contactInformation.getEmailVerificationExpiresAt() == null) {
            throw new IllegalArgumentException("No pending notification email verification");
        }
        if (contactInformation.getEmailVerificationExpiresAt().isBefore(Instant.now())) {
            clearPendingEmailVerification(contactInformation);
            userRepository.save(user);
            throw new IllegalArgumentException("Notification email verification token expired");
        }
        if (!MessageDigest.isEqual(
                hashToken(rawToken).getBytes(StandardCharsets.UTF_8),
                contactInformation
                        .getEmailVerificationTokenHash()
                        .getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Invalid notification email verification token");
        }

        contactInformation.setPrivateEmail(contactInformation.getPendingEmail().trim());
        clearPendingEmailVerification(contactInformation);

        User saved = userRepository.save(user);
        identityUserService.updateUser(
                UpdateIdentityUserRequest.builder()
                        .userId(saved.getId())
                        .username(saved.getUsername())
                        .firstName(saved.getFirstName())
                        .lastName(saved.getLastName())
                        .privateEmail(saved.getContactInformation().getPrivateEmail())
                        .tenantId(saved.getTenantId())
                        .build());
        log.info("Verified notification-email change for user {}", saved.getId());
        return saved;
    }

    @Transactional
    public UserDto resendNotificationEmailVerificationSelf(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        UserContactInformation contactInformation = user.getContactInformation();
        if (contactInformation == null
                || !StringUtils.hasText(contactInformation.getPendingEmail())) {
            throw new IllegalArgumentException("No pending notification email verification");
        }

        String pendingEmail = contactInformation.getPendingEmail().trim();
        String rawToken = beginPendingEmailVerification(contactInformation, pendingEmail);
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(
                new NotificationEmailChangeRequestedEvent(
                        saved.getTenantId(),
                        saved.getId(),
                        saved.getFirstName(),
                        pendingEmail,
                        rawToken));
        log.info("Resent notification-email verification for user {}", userId);
        return mapper.toDto(saved);
    }

    @Transactional
    public UserDto cancelPendingNotificationEmailSelf(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        UserContactInformation contactInformation = user.getContactInformation();
        if (contactInformation != null) {
            clearPendingEmailVerification(contactInformation);
        }
        User saved = userRepository.save(user);
        log.info("Cancelled pending notification-email change for user {}", userId);
        return mapper.toDto(saved);
    }

    private String beginPendingEmailVerification(
            UserContactInformation contactInformation, String pendingEmail) {
        String rawToken = generateVerificationToken();
        contactInformation.setPendingEmail(pendingEmail);
        contactInformation.setEmailVerificationTokenHash(hashToken(rawToken));
        contactInformation.setEmailVerificationExpiresAt(
                Instant.now().plus(Duration.ofHours(EMAIL_VERIFICATION_TTL_HOURS)));
        return rawToken;
    }

    private void clearPendingEmailVerification(UserContactInformation contactInformation) {
        contactInformation.setPendingEmail(null);
        contactInformation.setEmailVerificationTokenHash(null);
        contactInformation.setEmailVerificationExpiresAt(null);
    }

    @Transactional(readOnly = true)
    public List<UserOrganizationMembershipDto> getOrganizationMembershipsForUser(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        return user.getOrganizationMemberships().stream()
                .filter(m -> m.isActiveAt(Instant.now()))
                .map(userOrganizationMembershipMapper::toDto)
                .sorted(
                        Comparator.comparing(UserOrganizationMembershipDto::getMain)
                                .reversed()
                                .thenComparing(
                                        membership -> membership.getOrganization().getName(),
                                        String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(UserOrganizationMembershipDto::getId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizationDto getOrganizationForMember(Long userId, Long organizationId) {
        return userOrganizationMembershipRepository
                .findActiveByUser_IdAndOrganization_Id(userId, organizationId, Instant.now())
                .map(userOrganizationMembershipMapper::toDto)
                .map(UserOrganizationMembershipDto::getOrganization)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    /**
     * Organizations the user may request onboarding for: workflow enabled for the type, mapping
     * present, not the main org, no active membership, and no pending onboarding yet.
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> getJoinableOrganizationsForUser(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        Long tenantId = user.getTenantId();
        Instant now = Instant.now();
        long workflowTypeId = WorkflowType.ORGANIZATION_JOIN_REQUEST.getId();

        Long mainOrgId =
                userOrganizationMembershipRepository
                        .findActiveByUser_IdAndMainTrue(userId, now)
                        .map(membership -> membership.getOrganization().getId())
                        .orElse(null);

        Set<Long> activeMemberOrgIds =
                user.getOrganizationMemberships().stream()
                        .filter(membership -> membership.isActiveAt(now))
                        .map(membership -> membership.getOrganization().getId())
                        .collect(Collectors.toSet());
        Set<Long> openJoinTargetOrgIds =
                workflowOpenConcurrencyService.findOpenKeys(userId, workflowTypeId).stream()
                        .flatMap(
                                key ->
                                        WorkflowOpenConcurrencyKeys.parseOrganizationId(key)
                                                .stream())
                        .collect(Collectors.toSet());
        Set<Long> pendingOnboardingOrgIds =
                onboardingInvitationRepository
                        .findByUserIdAndStatusIn(userId, OnboardingInvitationStatus.pending())
                        .stream()
                        .map(OnboardingInvitation::getTargetOrganizationId)
                        .collect(Collectors.toSet());

        return workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId).stream()
                .filter(WorkflowDefinition::isEnabled)
                .map(WorkflowDefinition::getOrganizationId)
                .filter(orgId -> mainOrgId == null || !mainOrgId.equals(orgId))
                .filter(orgId -> !activeMemberOrgIds.contains(orgId))
                .filter(orgId -> !openJoinTargetOrgIds.contains(orgId))
                .filter(orgId -> !pendingOnboardingOrgIds.contains(orgId))
                .filter(
                        orgId ->
                                workflowTypeMappingRepository
                                        .existsByOrganizationIdAndWorkflowTypeId(
                                                orgId, workflowTypeId))
                .map(organizationRepository::findById)
                .flatMap(Optional::stream)
                .filter(org -> tenantId.equals(org.getTenantId()))
                .filter(org -> !org.isDeactivated())
                .map(organizationMapper::toDto)
                .sorted(
                        Comparator.comparing(
                                OrganizationDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Organizations the user may request as new main organization: active non-main memberships when
     * a main org exists, or any active membership when none exists; workflow enabled for the type,
     * mapping present.
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> getMainOrganizationChangeTargetsForUser(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        Long tenantId = user.getTenantId();
        Instant now = Instant.now();
        long workflowTypeId = WorkflowType.MAIN_ORGANIZATION_CHANGE.getId();

        Long mainOrgId =
                userOrganizationMembershipRepository
                        .findActiveByUser_IdAndMainTrue(userId, now)
                        .map(membership -> membership.getOrganization().getId())
                        .orElse(null);

        Set<Long> candidateOrgIds =
                user.getOrganizationMemberships().stream()
                        .filter(membership -> membership.isActiveAt(now))
                        .filter(
                                membership ->
                                        mainOrgId == null
                                                || !Boolean.TRUE.equals(membership.getMain()))
                        .map(membership -> membership.getOrganization().getId())
                        .collect(Collectors.toSet());

        return workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId).stream()
                .filter(WorkflowDefinition::isEnabled)
                .map(WorkflowDefinition::getOrganizationId)
                .filter(candidateOrgIds::contains)
                .filter(
                        orgId ->
                                workflowTypeMappingRepository
                                        .existsByOrganizationIdAndWorkflowTypeId(
                                                orgId, workflowTypeId))
                .map(organizationRepository::findById)
                .flatMap(Optional::stream)
                .filter(org -> tenantId.equals(org.getTenantId()))
                .filter(org -> !org.isDeactivated())
                .map(organizationMapper::toDto)
                .sorted(
                        Comparator.comparing(
                                OrganizationDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Organizations a member may be switched to as main organization when requested from org
     * user-management (workflow type 14). Callable from the member's current main org or from any
     * other active non-main membership context.
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> getMainOrganizationChangeTargetsForMember(
            long contextOrganizationId, long targetUserId) {
        User user =
                userRepository
                        .findById(targetUserId)
                        .orElseThrow(() -> new UserNotFoundException(targetUserId));
        Long tenantId = user.getTenantId();
        Instant now = Instant.now();
        long workflowTypeId = WorkflowType.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE.getId();

        Optional<UserOrganizationMembership> mainMembership =
                userOrganizationMembershipRepository.findActiveByUser_IdAndMainTrue(
                        targetUserId, now);
        if (mainMembership.isEmpty()) {
            return List.of();
        }
        long mainOrgId = mainMembership.get().getOrganization().getId();
        if (mainOrgId != contextOrganizationId
                && userOrganizationMembershipRepository
                        .findActiveByUser_IdAndOrganization_Id(
                                targetUserId, contextOrganizationId, now)
                        .isEmpty()) {
            return List.of();
        }

        Set<Long> candidateOrgIds =
                userOrganizationMembershipRepository.findActiveByUser_Id(targetUserId, now).stream()
                        .filter(membership -> !Boolean.TRUE.equals(membership.getMain()))
                        .filter(membership -> membership.getEndDate() == null)
                        .map(membership -> membership.getOrganization().getId())
                        .collect(Collectors.toSet());

        return workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId).stream()
                .filter(WorkflowDefinition::isEnabled)
                .map(WorkflowDefinition::getOrganizationId)
                .filter(candidateOrgIds::contains)
                .filter(
                        orgId ->
                                workflowTypeMappingRepository
                                        .existsByOrganizationIdAndWorkflowTypeId(
                                                orgId, workflowTypeId))
                .map(organizationRepository::findById)
                .flatMap(Optional::stream)
                .filter(org -> tenantId.equals(org.getTenantId()))
                .filter(org -> !org.isDeactivated())
                .map(organizationMapper::toDto)
                .sorted(
                        Comparator.comparing(
                                OrganizationDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Organizations the user may request to leave: active membership, workflow enabled for the
     * type, mapping present. Organizations where the user is in the leadership group or already has
     * an open leave request are excluded.
     */
    @Transactional(readOnly = true)
    public List<OrganizationDto> getOrganizationLeaveTargetsForUser(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        Long tenantId = user.getTenantId();
        Instant now = Instant.now();
        long workflowTypeId = WorkflowType.ORGANIZATION_LEAVE_REQUEST.getId();

        Set<Long> enabledMappedOrgIds = enabledMappedOrganizationIds(workflowTypeId);
        if (enabledMappedOrgIds.isEmpty()) {
            return List.of();
        }

        Set<Long> openLeaveTargetOrgIds =
                workflowOpenConcurrencyService.findOpenKeys(userId, workflowTypeId).stream()
                        .flatMap(
                                key ->
                                        WorkflowOpenConcurrencyKeys.parseOrganizationId(key)
                                                .stream())
                        .collect(Collectors.toSet());

        return userOrganizationMembershipRepository.findActiveByUser_Id(userId, now).stream()
                .map(UserOrganizationMembership::getOrganization)
                .filter(org -> tenantId.equals(org.getTenantId()))
                .filter(org -> !org.isDeactivated())
                .filter(org -> enabledMappedOrgIds.contains(org.getId()))
                .filter(org -> !openLeaveTargetOrgIds.contains(org.getId()))
                .filter(
                        org ->
                                !allMembersGroupService.hasActiveLeadershipMembership(
                                        userId, org.getId()))
                .map(organizationMapper::toDto)
                .sorted(
                        Comparator.comparing(
                                OrganizationDto::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Organization groups the user may request to leave: active group membership in a UI-visible
     * custom group (not all-members or leadership), workflow enabled for the type, mapping present
     * on the owning org. Groups with an open leave request are excluded.
     */
    @Transactional(readOnly = true)
    public List<OrganizationGroupLeaveTargetDto> getOrganizationGroupLeaveTargetsForUser(
            Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        Long tenantId = user.getTenantId();
        long workflowTypeId = WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST.getId();

        Set<Long> enabledMappedOrgIds = enabledMappedOrganizationIds(workflowTypeId);
        if (enabledMappedOrgIds.isEmpty()) {
            return List.of();
        }

        Set<Long> openLeaveTargetGroupIds =
                workflowOpenConcurrencyService.findOpenKeys(userId, workflowTypeId).stream()
                        .flatMap(
                                key ->
                                        WorkflowOpenConcurrencyKeys.parseOrganizationGroupId(key)
                                                .stream())
                        .collect(Collectors.toSet());

        List<OrganizationGroupLeaveTargetDto> targets = new ArrayList<>();
        for (Long organizationId : enabledMappedOrgIds) {
            Organization organization =
                    organizationRepository.findById(organizationId).orElse(null);
            if (organization == null || !tenantId.equals(organization.getTenantId())) {
                continue;
            }
            for (OrganizationGroupDto group :
                    organizationGroupService.getGroupsForUserInOrganization(
                            userId, organizationId, false)) {
                if (group.getType() == OrganizationGroupType.LEADERSHIP
                        || group.getType() == OrganizationGroupType.ALL_MEMBERS) {
                    continue;
                }
                if (openLeaveTargetGroupIds.contains(group.getId())) {
                    continue;
                }
                targets.add(
                        OrganizationGroupLeaveTargetDto.builder()
                                .organizationId(organizationId)
                                .organizationName(organization.getName())
                                .organizationGroupId(group.getId())
                                .organizationGroupName(group.getName())
                                .organizationGroupType(group.getType())
                                .build());
            }
        }
        targets.sort(
                Comparator.comparing(
                                OrganizationGroupLeaveTargetDto::getOrganizationName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                OrganizationGroupLeaveTargetDto::getOrganizationGroupName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(OrganizationGroupLeaveTargetDto::getOrganizationGroupId));
        return targets;
    }

    /**
     * Organization groups the user may request to join: active org membership, UI-visible custom
     * group they are not already in (not all-members or leadership), workflow enabled for the type,
     * mapping present on the owning org.
     */
    @Transactional(readOnly = true)
    public List<OrganizationGroupJoinTargetDto> getOrganizationGroupJoinTargetsForUser(
            Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        Long tenantId = user.getTenantId();
        Instant now = Instant.now();
        long workflowTypeId = WorkflowType.ORGANIZATION_GROUP_JOIN_REQUEST.getId();

        Set<Long> enabledMappedOrgIds = enabledMappedOrganizationIds(workflowTypeId);
        if (enabledMappedOrgIds.isEmpty()) {
            return List.of();
        }

        Set<Long> activeOrgIds =
                userOrganizationMembershipRepository.findActiveByUser_Id(userId, now).stream()
                        .map(membership -> membership.getOrganization().getId())
                        .collect(Collectors.toSet());
        Set<Long> openJoinTargetGroupIds =
                workflowOpenConcurrencyService.findOpenKeys(userId, workflowTypeId).stream()
                        .flatMap(
                                key ->
                                        WorkflowOpenConcurrencyKeys.parseOrganizationGroupId(key)
                                                .stream())
                        .collect(Collectors.toSet());

        List<OrganizationGroupJoinTargetDto> targets = new ArrayList<>();
        for (Long organizationId : enabledMappedOrgIds) {
            if (!activeOrgIds.contains(organizationId)) {
                continue;
            }
            Organization organization =
                    organizationRepository.findById(organizationId).orElse(null);
            if (organization == null || !tenantId.equals(organization.getTenantId())) {
                continue;
            }
            Set<Long> memberGroupIds =
                    organizationGroupService
                            .getGroupsForUserInOrganization(userId, organizationId, false)
                            .stream()
                            .map(OrganizationGroupDto::getId)
                            .collect(Collectors.toSet());
            for (OrganizationGroupDto group :
                    organizationGroupService.getAssignableGroupsByOrganizationId(organizationId)) {
                if (group.getType() == OrganizationGroupType.LEADERSHIP
                        || group.getType() == OrganizationGroupType.ALL_MEMBERS) {
                    continue;
                }
                if (memberGroupIds.contains(group.getId())) {
                    continue;
                }
                if (openJoinTargetGroupIds.contains(group.getId())) {
                    continue;
                }
                targets.add(
                        OrganizationGroupJoinTargetDto.builder()
                                .organizationId(organizationId)
                                .organizationName(organization.getName())
                                .organizationGroupId(group.getId())
                                .organizationGroupName(group.getName())
                                .organizationGroupType(group.getType())
                                .build());
            }
        }
        targets.sort(
                Comparator.comparing(
                                OrganizationGroupJoinTargetDto::getOrganizationName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                OrganizationGroupJoinTargetDto::getOrganizationGroupName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(OrganizationGroupJoinTargetDto::getOrganizationGroupId));
        return targets;
    }

    private Set<Long> enabledMappedOrganizationIds(long workflowTypeId) {
        return workflowDefinitionRepository.findByWorkflowTypeId(workflowTypeId).stream()
                .filter(WorkflowDefinition::isEnabled)
                .map(WorkflowDefinition::getOrganizationId)
                .filter(
                        orgId ->
                                workflowTypeMappingRepository
                                        .existsByOrganizationIdAndWorkflowTypeId(
                                                orgId, workflowTypeId))
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public UserPageDto listUsersForOrganization(Long organizationId, ListQuery query) {
        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        List<Long> organizationGroupIds =
                organizationGroupRepository.findIdsByOrganizationIdAndTypeIn(
                        organizationId, OrganizationGroupType.userInterfaceVisibleTypes());

        Pageable pageable = OrganizationMemberListDefinition.INSTANCE.pageable(query);
        Specification<User> spec =
                Specification.allOf(
                        OrganizationMemberListSpecs.activeInOrganization(organizationId),
                        OrganizationMemberListSpecs.notDeactivated(),
                        OrganizationMemberListDefinition.INSTANCE.searchSpecification(query));
        Page<User> page = userRepository.findAll(spec, pageable);
        List<UserDto> content =
                page.getContent().stream()
                        .map(user -> toOrganizationUserDto(user, organizationGroupIds))
                        .toList();
        return UserPageDto.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsersByOrganizationId(Long organizationId) {
        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        List<Long> organizationGroupIds =
                organizationGroupRepository.findIdsByOrganizationIdAndTypeIn(
                        organizationId, OrganizationGroupType.userInterfaceVisibleTypes());

        List<UserOrganizationMembership> memberships =
                userOrganizationMembershipRepository.findActiveByOrganization_Id(
                        organizationId, Instant.now());

        return memberships.stream()
                .map(UserOrganizationMembership::getUser)
                .filter(user -> user.getDeactivatedAt() == null)
                .map(user -> toOrganizationUserDto(user, organizationGroupIds))
                .sorted(
                        Comparator.comparing(UserDto::getLastName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(UserDto::getFirstName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(UserDto::getId))
                .collect(Collectors.toList());
    }

    private UserDto toOrganizationUserDto(User user, List<Long> organizationGroupIds) {
        UserDto dto = mapper.toDto(user);
        if (dto.getOrganizationGroupMemberships() != null) {
            List<UserOrganizationGroupMembershipDto> filteredMemberships =
                    dto.getOrganizationGroupMemberships().stream()
                            .filter(
                                    membership ->
                                            organizationGroupIds.contains(
                                                    membership.getOrganizationGroupId()))
                            .collect(Collectors.toList());
            dto.setOrganizationGroupMemberships(filteredMemberships);
        }
        return dto;
    }

    /**
     * Applies a name change after a {@link com.emberone.workflow.domain.WorkflowType#NAME_CHANGE}
     * workflow was fully approved. Updates core user, regenerates username, and syncs identity -
     * same rules as {@link #updateUserData} but without requiring an admin org context.
     */
    @Transactional
    public void applyApprovedNameChange(
            Long userId, Long tenantId, String firstName, String lastName) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        if (!user.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("User does not belong to the tenant");
        }
        String previousFirstName = user.getFirstName();
        String previousLastName = user.getLastName();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        regenerateUsernameIfNameChanged(user, previousFirstName, previousLastName);

        User saved = userRepository.save(user);
        log.info("Applied workflow-approved name change for user {}", userId);

        identityUserService.updateUser(
                UpdateIdentityUserRequest.builder()
                        .userId(saved.getId())
                        .username(saved.getUsername())
                        .firstName(saved.getFirstName())
                        .lastName(saved.getLastName())
                        .privateEmail(
                                saved.getContactInformation() != null
                                        ? saved.getContactInformation().getPrivateEmail()
                                        : null)
                        .tenantId(saved.getTenantId())
                        .build());
    }

    /**
     * Applies an address change after a {@link
     * com.emberone.workflow.domain.WorkflowType#ADDRESS_CHANGE} workflow was fully approved.
     */
    @Transactional
    public void applyApprovedAddressChange(
            Long userId,
            Long tenantId,
            String street,
            String city,
            String postalCode,
            String country) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        if (!user.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("User does not belong to the tenant");
        }

        UserContactInformation contactInformation = user.getContactInformation();
        if (contactInformation == null) {
            throw new IllegalStateException("User has no contact information");
        }

        contactInformation.setStreet(street.trim());
        contactInformation.setCity(city.trim());
        contactInformation.setPostalCode(postalCode.trim());
        contactInformation.setCountry(country.trim());

        userRepository.save(user);
        log.info("Applied workflow-approved address change for user {}", userId);
    }

    /** Checks the user's tenant and active organization membership. */
    @Transactional(readOnly = true)
    public void assertUserBelongsToOrganization(Long userId, Long organizationId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !user.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("User does not belong to the tenant");
        }
        if (!userOrganizationMembershipRepository.existsActiveByUser_IdAndOrganization_Id(
                userId, organizationId, Instant.now())) {
            throw new IllegalArgumentException(
                    "User does not belong to the specified organization");
        }
    }

    /**
     * Keeps the username stable unless the name behind it changed: regenerating it for an unchanged
     * name would collide with the user's own current username and append a numeric suffix.
     */
    private void regenerateUsernameIfNameChanged(
            User user, String previousFirstName, String previousLastName) {
        if (UsernamePartNormalizer.normalize(previousFirstName)
                        .equals(UsernamePartNormalizer.normalize(user.getFirstName()))
                && UsernamePartNormalizer.normalize(previousLastName)
                        .equals(UsernamePartNormalizer.normalize(user.getLastName()))) {
            return;
        }
        user.setUsername(
                usernameService.generateUniqueUsername(
                        user.getTenantId(), user.getFirstName(), user.getLastName()));
    }

    @Transactional
    public UserDto updateUserData(Long userId, Long organizationId, UpdateUserDataRequest request) {
        assertUserBelongsToOrganization(userId, organizationId);
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getDeactivatedAt() != null) {
            throw new UserAlreadyDeactivatedException(userId);
        }

        String previousFirstName = user.getFirstName();
        String previousLastName = user.getLastName();
        mapper.updateFromRequest(request, user);
        regenerateUsernameIfNameChanged(user, previousFirstName, previousLastName);

        User saved = userRepository.save(user);
        log.info("Updated personal data for user {} by organization {}", userId, organizationId);

        identityUserService.updateUser(
                UpdateIdentityUserRequest.builder()
                        .userId(saved.getId())
                        .username(saved.getUsername())
                        .firstName(saved.getFirstName())
                        .lastName(saved.getLastName())
                        .privateEmail(
                                saved.getContactInformation() != null
                                        ? saved.getContactInformation().getPrivateEmail()
                                        : null)
                        .tenantId(saved.getTenantId())
                        .build());

        return mapper.toDto(saved);
    }

    @Transactional
    public void addUserToLeadershipGroupAsLeader(Long userId, Long organizationId) {
        List<OrganizationGroupDto> groups =
                organizationGroupService.getGroupsByOrganizationId(organizationId);
        Optional<OrganizationGroupDto> leadershipGroup =
                groups.stream()
                        .filter(group -> group.getType() == OrganizationGroupType.LEADERSHIP)
                        .findFirst();
        if (leadershipGroup.isEmpty()) {
            throw new IllegalStateException(
                    "Leadership group not found for organization " + organizationId);
        }
        CreateUserOrganizationGroupMembershipRequest groupMembershipRequest =
                CreateUserOrganizationGroupMembershipRequest.builder()
                        .userId(userId)
                        .organizationGroupId(leadershipGroup.get().getId())
                        .membershipType(GroupMembershipType.LEADER)
                        .startDate(Instant.now())
                        .build();
        userOrganizationGroupMembershipService.createSystemManagedMembership(
                groupMembershipRequest);
    }
}
