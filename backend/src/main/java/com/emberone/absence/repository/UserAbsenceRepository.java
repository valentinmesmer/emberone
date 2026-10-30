/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.repository;

import com.emberone.absence.domain.AbsenceReason;
import com.emberone.absence.domain.UserAbsence;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface UserAbsenceRepository
        extends JpaRepository<UserAbsence, Long>, JpaSpecificationExecutor<UserAbsence> {

    @EntityGraph(attributePaths = {"user", "organizationLinks", "organizationLinks.organization"})
    @Query("select ua from UserAbsence ua where ua.id in :ids")
    List<UserAbsence> findWithOrganizationsByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"user", "organizationLinks", "organizationLinks.organization"})
    @Query(
            """
        select distinct ua from UserAbsence ua
        join ua.organizationLinks link
        where ua.id = :absenceId and link.organization.id = :organizationId
        """)
    Optional<UserAbsence> findByIdAndOrganizationId(
            @Param("absenceId") Long absenceId, @Param("organizationId") Long organizationId);

    @EntityGraph(attributePaths = {"organizationLinks", "organizationLinks.organization"})
    List<UserAbsence> findByUser_IdAndEndDateGreaterThanEqualOrderByStartDateDescIdDesc(
            Long userId, LocalDate minEndDate);

    @EntityGraph(attributePaths = {"organizationLinks", "organizationLinks.organization"})
    Page<UserAbsence> findByUser_IdAndEndDateGreaterThanEqualOrderByStartDateDescIdDesc(
            Long userId, LocalDate minEndDate, Pageable pageable);

    @EntityGraph(attributePaths = {"organizationLinks", "organizationLinks.organization"})
    Optional<UserAbsence> findByIdAndUser_Id(Long id, Long userId);

    @EntityGraph(attributePaths = {"organizationLinks", "organizationLinks.organization"})
    Optional<UserAbsence> findByIdAndUser_IdAndOrganizationLinks_Organization_Id(
            Long id, Long userId, Long organizationId);

    /** Absences that include the organization and overlap {@code today} (inclusive date range). */
    @EntityGraph(attributePaths = {"user", "organizationLinks", "organizationLinks.organization"})
    @Query(
            """
        select distinct ua from UserAbsence ua
        join ua.organizationLinks link
        where link.organization.id = :organizationId
        and ua.startDate <= :today
        and ua.endDate >= :today
        order by ua.startDate desc, ua.id desc
        """)
    List<UserAbsence> findActiveByOrganizationId(
            @Param("organizationId") Long organizationId, @Param("today") LocalDate today);

    @EntityGraph(attributePaths = {"user", "organizationLinks", "organizationLinks.organization"})
    @Query(
            """
        select distinct ua from UserAbsence ua
        join ua.organizationLinks link
        where link.organization.id = :organizationId
        and ua.startDate <= :today
        and ua.endDate >= :today
        order by ua.startDate desc, ua.id desc
        """)
    Page<UserAbsence> findActivePageByOrganizationId(
            @Param("organizationId") Long organizationId,
            @Param("today") LocalDate today,
            Pageable pageable);

    @EntityGraph(attributePaths = {"organizationLinks", "organizationLinks.organization"})
    @Query(
            """
        select distinct ua from UserAbsence ua
        join ua.organizationLinks link
        where ua.user.id = :userId
        and link.organization.id = :organizationId
        and ua.endDate >= :minEndDate
        order by ua.startDate desc, ua.id desc
        """)
    List<UserAbsence> findByUserIdAndOrganizationId(
            @Param("userId") Long userId,
            @Param("organizationId") Long organizationId,
            @Param("minEndDate") LocalDate minEndDate);

    @Query("select distinct ua.reason from UserAbsence ua where ua.user.id = :userId")
    List<AbsenceReason> findDistinctReasonsByUserId(@Param("userId") Long userId);

    @Query(
            """
        select distinct link.organization.id, link.organization.name
        from UserAbsence ua
        join ua.organizationLinks link
        where ua.user.id = :userId
        order by link.organization.name
        """)
    List<Object[]> findDistinctOrganizationRowsByUserId(@Param("userId") Long userId);

    @Query("select ua.startDate, ua.endDate from UserAbsence ua where ua.user.id = :userId")
    List<Object[]> findDateRangesByUserId(@Param("userId") Long userId);

    @Query(
            """
        select distinct ua.reason
        from UserAbsence ua
        join ua.organizationLinks link
        where ua.user.id = :userId and link.organization.id = :organizationId
        """)
    List<AbsenceReason> findDistinctReasonsByUserIdAndOrganizationId(
            @Param("userId") Long userId, @Param("organizationId") Long organizationId);

    @Query(
            """
        select distinct link.organization.id, link.organization.name
        from UserAbsence ua
        join ua.organizationLinks link
        where ua.user.id = :userId and link.organization.id = :organizationId
        order by link.organization.name
        """)
    List<Object[]> findDistinctOrganizationRowsByUserIdAndOrganizationId(
            @Param("userId") Long userId, @Param("organizationId") Long organizationId);

    @Query(
            """
        select ua.startDate, ua.endDate
        from UserAbsence ua
        join ua.organizationLinks link
        where ua.user.id = :userId and link.organization.id = :organizationId
        """)
    List<Object[]> findDateRangesByUserIdAndOrganizationId(
            @Param("userId") Long userId, @Param("organizationId") Long organizationId);

    @Query(
            """
        select distinct ua.reason
        from UserAbsence ua
        join ua.organizationLinks link
        where link.organization.id = :organizationId
        """)
    List<AbsenceReason> findDistinctReasonsByOrganizationId(
            @Param("organizationId") Long organizationId);

    @Query(
            """
        select ua.startDate, ua.endDate
        from UserAbsence ua
        join ua.organizationLinks link
        where link.organization.id = :organizationId
        """)
    List<Object[]> findDateRangesByOrganizationId(@Param("organizationId") Long organizationId);
}
