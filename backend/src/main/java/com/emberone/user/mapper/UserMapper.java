/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.mapper;

import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.user.domain.User;
import com.emberone.user.dto.UpdateUserDataRequest;
import com.emberone.user.dto.UserDto;
import com.emberone.user.dto.UserSummaryDto;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.mapper.UserOrganizationGroupMembershipMapper;
import com.emberone.userorganizationmembership.dto.UserOrganizationMembershipDto;
import com.emberone.userorganizationmembership.mapper.UserOrganizationMembershipMapper;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {
            UserContactInformationMapper.class,
            UserOrganizationMembershipMapper.class,
            UserOrganizationGroupMembershipMapper.class
        })
public interface UserMapper {

    private static void hideAllMembersGroupMemberships(User user, UserDto dto) {
        if (dto.getOrganizationGroupMemberships() == null
                || user.getOrganizationGroupMemberships() == null) {
            return;
        }
        Set<Long> allMembersGroupIds =
                user.getOrganizationGroupMemberships().stream()
                        .filter(
                                membership ->
                                        membership.getOrganizationGroup() != null
                                                && membership.getOrganizationGroup().getType()
                                                        == OrganizationGroupType.ALL_MEMBERS)
                        .map(membership -> membership.getOrganizationGroup().getId())
                        .collect(Collectors.toSet());
        if (allMembersGroupIds.isEmpty()) {
            return;
        }
        dto.setOrganizationGroupMemberships(
                dto.getOrganizationGroupMemberships().stream()
                        .filter(
                                membership ->
                                        !allMembersGroupIds.contains(
                                                membership.getOrganizationGroupId()))
                        .collect(Collectors.toList()));
    }

    UserDto toDto(User user);

    @AfterMapping
    default void sortMemberships(User user, @MappingTarget UserDto dto) {
        hideAllMembersGroupMemberships(user, dto);
        if (dto.getOrganizationMemberships() != null) {
            dto.getOrganizationMemberships()
                    .sort(
                            Comparator.comparing(UserOrganizationMembershipDto::getMain)
                                    .reversed()
                                    .thenComparing(
                                            membership -> membership.getOrganization().getName(),
                                            String.CASE_INSENSITIVE_ORDER)
                                    .thenComparing(UserOrganizationMembershipDto::getStartDate)
                                    .thenComparing(UserOrganizationMembershipDto::getId));
        }
        if (dto.getOrganizationGroupMemberships() != null) {
            dto.getOrganizationGroupMemberships()
                    .sort(
                            Comparator.comparing(
                                            UserOrganizationGroupMembershipDto
                                                    ::getOrganizationGroupId)
                                    .thenComparing(UserOrganizationGroupMembershipDto::getStartDate)
                                    .thenComparing(UserOrganizationGroupMembershipDto::getId));
        }
    }

    UserSummaryDto toSummaryDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "personalNumber", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "contactInformation", ignore = true)
    @Mapping(target = "organizationMemberships", ignore = true)
    @Mapping(target = "organizationGroupMemberships", ignore = true)
    @Mapping(target = "deactivatedAt", ignore = true)
    void updateFromRequest(UpdateUserDataRequest request, @MappingTarget User user);
}
