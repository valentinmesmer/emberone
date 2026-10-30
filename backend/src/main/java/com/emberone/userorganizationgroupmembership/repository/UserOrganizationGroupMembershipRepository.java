/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationgroupmembership.repository;

import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserOrganizationGroupMembershipRepository
        extends JpaRepository<UserOrganizationGroupMembership, Long>,
                JpaSpecificationExecutor<UserOrganizationGroupMembership> {

    List<UserOrganizationGroupMembership> findByUserId(Long userId);

    @Query(
            "SELECT m FROM UserOrganizationGroupMembership m JOIN FETCH m.user "
                    + "WHERE m.organizationGroup.id = :organizationGroupId")
    List<UserOrganizationGroupMembership> findByOrganizationGroup_IdWithUser(
            @Param("organizationGroupId") Long organizationGroupId);

    @Query(
            "SELECT m FROM UserOrganizationGroupMembership m JOIN FETCH m.user "
                    + "WHERE m.organizationGroup.id = :organizationGroupId "
                    + "AND m.startDate <= :at "
                    + "AND (m.endDate IS NULL OR m.endDate > :at)")
    List<UserOrganizationGroupMembership> findActiveByOrganizationGroup_IdWithUser(
            @Param("organizationGroupId") Long organizationGroupId, @Param("at") Instant at);

    @Query(
            """
            SELECT m FROM UserOrganizationGroupMembership m JOIN FETCH m.user
            WHERE m.organizationGroup.id = :organizationGroupId
            AND m.startDate <= :at
            AND (m.endDate IS NULL OR m.endDate > :at)
            ORDER BY LOWER(m.user.lastName), LOWER(m.user.firstName), m.user.id
            """)
    Page<UserOrganizationGroupMembership> findActiveByOrganizationGroup_IdWithUser(
            @Param("organizationGroupId") Long organizationGroupId,
            @Param("at") Instant at,
            Pageable pageable);

    List<UserOrganizationGroupMembership> findByUser_Id(Long userId);

    List<UserOrganizationGroupMembership> findByUser_IdAndOrganizationGroup_Organization_Id(
            Long userId, Long organizationId);

    List<UserOrganizationGroupMembership> findByUser_IdAndOrganizationGroup_Id(
            Long userId, Long organizationGroupId);

    @Query(
            "SELECT m FROM UserOrganizationGroupMembership m "
                    + "WHERE m.user.id = :userId AND m.organizationGroup.id = :organizationGroupId "
                    + "AND m.startDate <= :at "
                    + "AND (m.endDate IS NULL OR m.endDate > :at)")
    Optional<UserOrganizationGroupMembership> findActiveByUser_IdAndOrganizationGroup_Id(
            @Param("userId") Long userId,
            @Param("organizationGroupId") Long organizationGroupId,
            @Param("at") Instant at);

    @Query(
            "SELECT DISTINCT m.organizationGroup.id FROM UserOrganizationGroupMembership m "
                    + "WHERE m.user.id = :userId "
                    + "AND m.organizationGroup.organization.id = :organizationId "
                    + "AND m.startDate <= :at "
                    + "AND (m.endDate IS NULL OR m.endDate > :at)")
    List<Long> findActiveGroupIdsForUserInOrganization(
            @Param("userId") Long userId,
            @Param("organizationId") Long organizationId,
            @Param("at") Instant at);

    @Query(
            "SELECT DISTINCT m.user.id FROM UserOrganizationGroupMembership m "
                    + "WHERE m.organizationGroup.id IN :groupIds "
                    + "AND m.startDate <= :at "
                    + "AND (m.endDate IS NULL OR m.endDate > :at)")
    List<Long> findActiveUserIdsByGroupIds(
            @Param("groupIds") List<Long> groupIds, @Param("at") Instant at);

    @Query(
            "SELECT m FROM UserOrganizationGroupMembership m JOIN FETCH m.user u JOIN m.organizationGroup g "
                    + "WHERE g.organization.id = :organizationId "
                    + "AND g.type = 'LEADERSHIP' "
                    + "AND m.membershipType = :leaderType "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at) "
                    + "ORDER BY m.id ASC")
    List<UserOrganizationGroupMembership> findActiveLeadershipLeaderMemberships(
            @Param("organizationId") Long organizationId,
            @Param("leaderType") GroupMembershipType leaderType,
            @Param("at") Instant at);

    @Query(
            "SELECT m FROM UserOrganizationGroupMembership m JOIN FETCH m.user u JOIN m.organizationGroup g "
                    + "WHERE g.organization.id = :organizationId "
                    + "AND g.type = 'LEADERSHIP' "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at) "
                    + "ORDER BY m.id ASC")
    List<UserOrganizationGroupMembership> findActiveLeadershipMemberships(
            @Param("organizationId") Long organizationId, @Param("at") Instant at);

    @Query(
            "SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END "
                    + "FROM UserOrganizationGroupMembership m JOIN m.organizationGroup g "
                    + "WHERE m.user.id = :userId AND g.organization.id = :organizationId "
                    + "AND g.type = 'LEADERSHIP' "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    boolean existsActiveLeadershipMembership(
            @Param("userId") Long userId,
            @Param("organizationId") Long organizationId,
            @Param("at") Instant at);

    @Query(
            "SELECT DISTINCT g.organization.id FROM UserOrganizationGroupMembership m "
                    + "JOIN m.organizationGroup g "
                    + "WHERE g.type = 'LEADERSHIP' "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    List<Long> findOrganizationIdsWithActiveLeadershipMember(@Param("at") Instant at);

    @Query(
            "SELECT m FROM UserOrganizationGroupMembership m JOIN m.organizationGroup g "
                    + "WHERE m.user.id = :userId AND g.organization.id = :organizationId "
                    + "AND m.startDate <= :at "
                    + "AND (m.endDate IS NULL OR m.endDate > :at)")
    List<UserOrganizationGroupMembership> findActiveByUser_IdAndOrganization_Id(
            @Param("userId") Long userId,
            @Param("organizationId") Long organizationId,
            @Param("at") Instant at);

    @Query(
            "SELECT m FROM UserOrganizationGroupMembership m JOIN FETCH m.organizationGroup g "
                    + "WHERE m.user.id IN :userIds AND g.organization.id = :organizationId "
                    + "AND m.startDate <= :at AND (m.endDate IS NULL OR m.endDate > :at)")
    List<UserOrganizationGroupMembership> findActiveByUser_IdInAndOrganization_Id(
            @Param("userIds") Collection<Long> userIds,
            @Param("organizationId") Long organizationId,
            @Param("at") Instant at);

    boolean existsByOrganizationGroup_IdIn(Collection<Long> organizationGroupIds);

    @Transactional
    @Modifying
    @Query(
            "UPDATE UserOrganizationGroupMembership m SET m.endDate = :at "
                    + "WHERE m.organizationGroup.id IN :groupIds "
                    + "AND m.startDate <= :at "
                    + "AND (m.endDate IS NULL OR m.endDate > :at)")
    int endActiveMembershipsByGroupIds(
            @Param("groupIds") Collection<Long> groupIds, @Param("at") Instant at);

    @Query(
            "SELECT m FROM UserOrganizationGroupMembership m JOIN FETCH m.user JOIN FETCH m.organizationGroup "
                    + "WHERE m.endDate <= :at AND m.endProcessedAt IS NULL")
    List<UserOrganizationGroupMembership> findDueEndedUnprocessed(@Param("at") Instant at);
}
