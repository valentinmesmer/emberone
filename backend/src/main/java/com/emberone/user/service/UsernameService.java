/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.util.UsernamePartNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsernameService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    public String generateUniqueUsername(long tenantId, String firstName, String lastName) {
        Tenant tenant =
                tenantRepository
                        .findById(tenantId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Tenant not found: " + tenantId));
        String tenantCode = tenant.getAbbreviation();
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new IllegalStateException("Tenant has no code configured: " + tenantId);
        }

        String nameBase =
                UsernamePartNormalizer.normalize(firstName)
                        + "."
                        + UsernamePartNormalizer.normalize(lastName);
        String baseUsername = nameBase + "." + tenantCode;
        if (!userRepository.existsByUsername(baseUsername)) {
            return baseUsername;
        }

        int suffix = 2;
        String candidate;
        do {
            candidate = nameBase + suffix + "." + tenantCode;
            suffix++;
        } while (userRepository.existsByUsername(candidate));

        return candidate;
    }
}
