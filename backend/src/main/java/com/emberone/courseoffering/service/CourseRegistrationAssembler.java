/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import com.emberone.courseoffering.domain.CourseRegistration;
import com.emberone.courseoffering.domain.CourseRegistrationStatus;
import com.emberone.courseoffering.dto.CourseRegistrationDto;
import com.emberone.courseoffering.repository.CourseRegistrationRepository;
import com.emberone.platform.navigation.AppNavigationResolver;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Maps registrations of one offering to the organization-facing DTO. */
@Component
@RequiredArgsConstructor
class CourseRegistrationAssembler {

    private final CourseRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final AppNavigationResolver appNavigationResolver;

    List<CourseRegistrationDto> toDtos(Long offeringId, List<CourseRegistration> registrations) {
        if (registrations.isEmpty()) {
            return List.of();
        }
        Map<Long, String> names = new HashMap<>();
        userRepository
                .findAllById(registrations.stream().map(CourseRegistration::getUserId).toList())
                .forEach(user -> names.put(user.getId(), displayName(user)));
        Map<Long, Integer> positions = new HashMap<>();
        if (registrations.stream()
                .anyMatch(r -> r.getStatus() == CourseRegistrationStatus.WAITLIST)) {
            positions.putAll(
                    waitlistPositions(registrationRepository.findWaitlistInOrder(offeringId)));
        }
        return registrations.stream()
                .map(r -> toDto(r, offeringId, names.get(r.getUserId()), positions.get(r.getId())))
                .toList();
    }

    /**
     * 1-based waitlist places by registration id. With quotas every audience entry has its own
     * queue; without, the whole offering shares one (audience entry null).
     */
    static Map<Long, Integer> waitlistPositions(
            List<CourseRegistrationRepository.WaitlistEntry> queue) {
        Map<Long, Integer> positions = new HashMap<>();
        Map<Long, Integer> next = new HashMap<>();
        for (CourseRegistrationRepository.WaitlistEntry entry : queue) {
            // 0L stands for "no audience entry"; real ids are positive
            int place =
                    next.merge(
                            entry.getAudienceId() == null ? 0L : entry.getAudienceId(),
                            1,
                            Integer::sum);
            positions.put(entry.getId(), place);
        }
        return positions;
    }

    private CourseRegistrationDto toDto(
            CourseRegistration registration,
            Long offeringId,
            String userDisplayName,
            Integer waitlistPosition) {
        Long workflowId = registration.getWorkflowId();
        CourseRegistrationDto.CourseRegistrationDtoBuilder<?, ?> builder =
                CourseRegistrationDto.builder()
                        .id(registration.getId())
                        .offeringId(offeringId)
                        .userId(registration.getUserId())
                        .audienceId(registration.getAudienceId())
                        .userDisplayName(userDisplayName)
                        .status(registration.getStatus())
                        .waitlistPosition(waitlistPosition)
                        .alreadyHeld(registration.isAlreadyHeld())
                        .workflowId(workflowId)
                        .registeredAt(registration.getRegisteredAt())
                        .approvedAt(registration.getApprovedAt())
                        .endedAt(registration.getEndedAt())
                        .endedBy(registration.getEndedBy())
                        .endReason(registration.getEndReason());
        if (workflowId != null) {
            builder.linkType(AppLinkType.WORKFLOW)
                    .linkId(workflowId)
                    .navigationPath(
                            appNavigationResolver.resolveNavigationPath(
                                    AppLinkType.WORKFLOW, workflowId));
        }
        return builder.build();
    }

    static String displayName(User user) {
        String name =
                ((user.getFirstName() != null ? user.getFirstName() : "")
                                + " "
                                + (user.getLastName() != null ? user.getLastName() : ""))
                        .trim();
        return StringUtils.hasText(name) ? name : user.getUsername();
    }
}
