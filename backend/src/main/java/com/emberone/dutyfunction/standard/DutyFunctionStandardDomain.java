/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.standard;

import com.emberone.dutyfunction.tenant.TenantFunction;
import com.emberone.dutyfunction.tenant.TenantFunctionRepository;
import com.emberone.federalstate.baseline.FunctionGrantBaseline;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.standard.OrgGroupTemplate;
import com.emberone.organizationgroup.standard.OrgGroupTemplateRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.spi.StandardContentItem;
import com.emberone.standard.spi.StandardDomain;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(55)
@RequiredArgsConstructor
public class DutyFunctionStandardDomain implements StandardDomain {

    private final FunctionGrantRepository grantRepository;
    private final FunctionSelectionRepository selectionRepository;
    private final TenantFunctionRepository functionRepository;
    private final OrgGroupTemplateRepository orgGroupTemplateRepository;

    @Override
    public String domainKey() {
        return "duty-functions";
    }

    @Override
    @Transactional
    public void copyTemplates(Standard source, Standard target) {
        for (FunctionGrant grant : grantRepository.findByStandardId(source.getId())) {
            if (existsGrant(
                    target.getId(),
                    grant.getOrganizationGroupConfigKey(),
                    grant.getMembershipType())) {
                continue;
            }
            grantRepository.save(
                    FunctionGrant.builder()
                            .tenantId(target.getTenantId())
                            .standardId(target.getId())
                            .organizationGroupConfigKey(grant.getOrganizationGroupConfigKey())
                            .membershipType(grant.getMembershipType())
                            .functionConfigKey(grant.getFunctionConfigKey())
                            .build());
        }
    }

    @Override
    @Transactional
    public void materializeForOrg(Long organizationId, Standard standard) {}

    @Override
    @Transactional
    public void reassignForOrg(Long organizationId, Standard standard) {}

    @Override
    @Transactional(readOnly = true)
    public List<StandardContentItem> describeContent(Standard standard) {
        List<StandardContentItem> items = new ArrayList<>();
        for (FunctionGrant grant : grantRepository.findByStandardId(standard.getId())) {
            functionRepository
                    .findByConfigKey(grant.getFunctionConfigKey())
                    .ifPresent(
                            function ->
                                    items.add(
                                            new StandardContentItem(
                                                    domainKey(),
                                                    grantKey(grant),
                                                    function.getName(),
                                                    function.getAbbreviation(),
                                                    List.of(grant.getOrganizationGroupConfigKey()),
                                                    Map.of(
                                                            "organizationGroupConfigKey",
                                                                    grant
                                                                            .getOrganizationGroupConfigKey(),
                                                            "membershipType",
                                                                    grant.getMembershipType()
                                                                            .name(),
                                                            "functionConfigKey",
                                                                    grant.getFunctionConfigKey(),
                                                            "functionArchived",
                                                                    String.valueOf(
                                                                            function.isArchived())),
                                                    function.isArchived())));
        }
        items.sort(Comparator.comparing(StandardContentItem::configKey));
        return items;
    }

    @Transactional
    public void replaceGrants(Standard standard, List<FunctionGrantBaseline> grants) {
        Long tenantId = standard.getTenantId();
        Long standardId = standard.getId();
        Set<String> keys = new HashSet<>();
        for (FunctionGrantBaseline baseline : grants) {
            if (baseline.membershipType() == null) {
                throw new ConflictException(
                        "Function grant membership type is required for org group: "
                                + baseline.orgGroupConfigKey());
            }
            validateOrgGroupForFunctionGrant(standardId, baseline.orgGroupConfigKey());
            validateFunction(standardId, baseline.functionConfigKey());
            String key = grantKey(baseline.orgGroupConfigKey(), baseline.membershipType());
            if (!keys.add(key)) {
                throw new ConflictException("Duplicate function grant: " + key);
            }
            FunctionGrant grant =
                    findGrant(standardId, baseline.orgGroupConfigKey(), baseline.membershipType())
                            .orElseGet(
                                    () ->
                                            FunctionGrant.builder()
                                                    .tenantId(tenantId)
                                                    .standardId(standardId)
                                                    .organizationGroupConfigKey(
                                                            baseline.orgGroupConfigKey())
                                                    .membershipType(baseline.membershipType())
                                                    .build());
            grant.setFunctionConfigKey(baseline.functionConfigKey());
            grantRepository.save(grant);
        }
        for (FunctionGrant existing : grantRepository.findByStandardId(standardId)) {
            String slotKey =
                    grantKey(
                            existing.getOrganizationGroupConfigKey(), existing.getMembershipType());
            if (!keys.contains(slotKey)) {
                grantRepository.delete(existing);
            }
        }
    }

    @Transactional
    public void replaceGrantsFromRequests(
            Standard standard, List<FunctionGrantReplacement> replacements) {
        List<FunctionGrantBaseline> baselines =
                replacements.stream()
                        .map(
                                r ->
                                        new FunctionGrantBaseline(
                                                r.organizationGroupConfigKey(),
                                                r.membershipType(),
                                                r.functionConfigKey()))
                        .toList();
        replaceGrants(standard, baselines);
    }

    private void validateOrgGroupForFunctionGrant(long standardId, String orgGroupConfigKey) {
        OrgGroupTemplate orgGroup =
                orgGroupTemplateRepository
                        .findByStandardIdAndConfigKey(standardId, orgGroupConfigKey)
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                "Unknown organization group: "
                                                        + orgGroupConfigKey));
        OrganizationGroupType type = orgGroup.getGroupType();
        if (type == null || !type.supportsFunctionGrants()) {
            throw new ConflictException(
                    "Function grants are not allowed for organization group type: "
                            + (type != null ? type.name() : orgGroupConfigKey));
        }
    }

    private void validateFunction(long standardId, String functionConfigKey) {
        TenantFunction function =
                functionRepository
                        .findByConfigKey(functionConfigKey)
                        .orElseThrow(
                                () ->
                                        new ConflictException(
                                                "Unknown tenant function: " + functionConfigKey));
        if (function.isArchived()) {
            throw new ConflictException("Tenant function is archived: " + functionConfigKey);
        }
        if (!selectionRepository.existsByStandardIdAndFunctionConfigKey(
                standardId, functionConfigKey)) {
            throw new ConflictException(
                    "Function is not selected for standard: " + functionConfigKey);
        }
    }

    private Optional<FunctionGrant> findGrant(
            Long standardId, String orgGroupConfigKey, GroupMembershipType membershipType) {
        return grantRepository.findByStandardIdAndOrganizationGroupConfigKeyAndMembershipType(
                standardId, orgGroupConfigKey, membershipType);
    }

    private boolean existsGrant(
            Long standardId, String orgGroupConfigKey, GroupMembershipType membershipType) {
        return findGrant(standardId, orgGroupConfigKey, membershipType).isPresent();
    }

    private static String grantKey(FunctionGrant grant) {
        return grantKey(grant.getOrganizationGroupConfigKey(), grant.getMembershipType())
                + ":"
                + grant.getFunctionConfigKey();
    }

    private static String grantKey(String orgGroupConfigKey, GroupMembershipType membershipType) {
        return orgGroupConfigKey + "|" + membershipType.name();
    }

    public record FunctionGrantReplacement(
            String organizationGroupConfigKey,
            GroupMembershipType membershipType,
            String functionConfigKey) {}
}
