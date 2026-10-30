/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.repository;

import com.emberone.timeentry.domain.UserTimeEntry;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserTimeEntryRepository
        extends JpaRepository<UserTimeEntry, Long>, JpaSpecificationExecutor<UserTimeEntry> {

    @EntityGraph(attributePaths = {"organization", "organizationGroup"})
    List<UserTimeEntry> findByUser_IdOrderByStartTimeDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"organization", "organizationGroup"})
    Optional<UserTimeEntry> findByIdAndUser_Id(Long id, Long userId);

    @EntityGraph(attributePaths = {"organization", "organizationGroup"})
    Optional<UserTimeEntry> findByIdAndUser_IdAndOrganization_Id(
            Long id, Long userId, Long organizationId);

    List<UserTimeEntry> findByUser_IdAndStartTimeBetween(
            Long userId, Instant from, Instant toExclusive);

    @EntityGraph(attributePaths = {"user", "organization", "organizationGroup"})
    List<UserTimeEntry> findByOrganization_IdOrderByStartTimeDescIdDesc(Long organizationId);

    @EntityGraph(attributePaths = {"organization", "organizationGroup"})
    List<UserTimeEntry> findByUser_IdAndOrganization_IdOrderByStartTimeDescIdDesc(
            Long userId, Long organizationId);

    @EntityGraph(attributePaths = {"organization", "organizationGroup"})
    @Query("SELECT e FROM UserTimeEntry e WHERE e.id IN :ids")
    List<UserTimeEntry> findWithDetailsByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"user", "organization", "organizationGroup"})
    @Query("SELECT e FROM UserTimeEntry e WHERE e.id IN :ids")
    List<UserTimeEntry> findWithUserDetailsByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"user", "organization", "organizationGroup"})
    Optional<UserTimeEntry> findByIdAndOrganization_Id(Long id, Long organizationId);

    @Query(
            """
        select distinct e.organization.id, e.organization.name
        from UserTimeEntry e
        where e.user.id = :userId
        order by e.organization.name
        """)
    List<Object[]> findDistinctOrganizationRowsByUserId(@Param("userId") Long userId);

    @Query("select e.startTime from UserTimeEntry e where e.user.id = :userId")
    List<Instant> findStartTimesByUserId(@Param("userId") Long userId);

    @Query(
            """
        select distinct e.organization.id, e.organization.name
        from UserTimeEntry e
        where e.user.id = :userId and e.organization.id = :organizationId
        order by e.organization.name
        """)
    List<Object[]> findDistinctOrganizationRowsByUserIdAndOrganizationId(
            @Param("userId") Long userId, @Param("organizationId") Long organizationId);

    @Query(
            """
        select e.startTime
        from UserTimeEntry e
        where e.user.id = :userId and e.organization.id = :organizationId
        """)
    List<Instant> findStartTimesByUserIdAndOrganizationId(
            @Param("userId") Long userId, @Param("organizationId") Long organizationId);
}
