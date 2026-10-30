/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.retention.service;

import com.emberone.admin.retention.dto.RetentionCategoryPeriodDto;
import com.emberone.admin.retention.dto.TenantRetentionProfileDto;
import com.emberone.retention.RetentionPolicyService;
import com.emberone.retention.RetentionRegistry;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.profile.RetentionPeriod;
import com.emberone.retention.profile.RetentionProfile;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRetentionService {

    private final RetentionPolicyService retentionPolicyService;
    private final RetentionRegistry retentionRegistry;

    @Transactional(readOnly = true)
    public TenantRetentionProfileDto getProfileForTenant(long tenantId) {
        RetentionProfile profile = retentionPolicyService.profileForTenant(tenantId);
        List<RetentionCategoryPeriodDto> periods =
                Arrays.stream(RetentionCategory.values())
                        .map(
                                category -> {
                                    RetentionPeriod period = profile.period(category);
                                    List<String> tables =
                                            retentionRegistry.require(category).tables().stream()
                                                    .sorted(Comparator.naturalOrder())
                                                    .toList();
                                    return RetentionCategoryPeriodDto.builder()
                                            .category(category)
                                            .amount(period.quantity())
                                            .unit(period.unit())
                                            .tables(tables)
                                            .build();
                                })
                        .toList();
        return TenantRetentionProfileDto.builder()
                .profileKey(profile.key())
                .periods(periods)
                .build();
    }
}
