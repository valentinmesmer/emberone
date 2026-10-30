/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.repository;

import com.emberone.appointment.domain.Appointment;
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
public interface AppointmentRepository
        extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {

    @Query(
            "SELECT a FROM Appointment a WHERE a.organization.id = :organizationId "
                    + "AND a.archived = false AND a.start < :to AND a.end > :from "
                    + "ORDER BY a.start ASC, a.id ASC")
    List<Appointment> findByOrganizationIdAndDateRange(
            @Param("organizationId") Long organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(
            "SELECT DISTINCT a FROM Appointment a "
                    + "LEFT JOIN FETCH a.organization "
                    + "LEFT JOIN FETCH a.audienceGroups "
                    + "WHERE a.id IN :ids")
    List<Appointment> findWithOrganizationAndAudienceByIdIn(@Param("ids") List<Long> ids);

    Optional<Appointment> findByIdAndOrganization_Id(Long id, Long orgId);

    @Query(
            """
        select a.start
        from Appointment a
        where a.organization.id = :organizationId
        and a.archived = false
        """)
    List<Instant> findStartsByOrganizationId(@Param("organizationId") Long organizationId);

    @Query(
            "SELECT a FROM Appointment a WHERE a.organization.id = :organizationId "
                    + "AND a.archived = false AND a.start >= :from "
                    + "ORDER BY a.start ASC, a.id ASC")
    List<Appointment> findUpcomingByOrganizationId(
            @Param("organizationId") Long organizationId, @Param("from") Instant from);

    List<Appointment> findBySeries_IdAndOccurrenceStartGreaterThanEqual(
            Long seriesId, Instant occurrenceStart);

    List<Appointment> findBySeries_Id(Long seriesId);

    Optional<Appointment> findByIdAndOrganization_IdAndArchivedIsFalse(
            Long id, Long organizationId);

    long countBySeries_Id(Long seriesId);

    @Query("SELECT MAX(a.occurrenceStart) FROM Appointment a WHERE a.series.id = :seriesId")
    Optional<Instant> findLatestOccurrenceStartBySeriesId(@Param("seriesId") Long seriesId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "UPDATE Appointment a SET a.archived = true, a.archivedAt = :archivedAt "
                    + "WHERE a.archived = false AND a.end <= :cutoff")
    int archiveWhereEndedLongEnoughAgo(
            @Param("cutoff") Instant cutoff, @Param("archivedAt") Instant archivedAt);

    String VISIBLE_UPCOMING_PREDICATE =
            "a.organization.id IN :organizationIds "
                    + "AND a.archived = false "
                    + "AND a.start >= :from "
                    + "AND a.start < :horizonEnd "
                    + "AND (a.audienceScope = com.emberone.appointment.domain.AudienceScope.ALL "
                    + "OR EXISTS ("
                    + "  SELECT 1 FROM AppointmentAudienceGroup aag "
                    + "  WHERE aag.appointment = a "
                    + "  AND aag.id.organizationGroupId IN ("
                    + "    SELECT gum.organizationGroup.id FROM UserOrganizationGroupMembership gum "
                    + "    WHERE gum.user.id = :userId "
                    + "    AND gum.organizationGroup.organization.id = a.organization.id "
                    + "    AND gum.startDate <= :membershipAt "
                    + "    AND (gum.endDate IS NULL OR gum.endDate > :membershipAt)"
                    + "  )"
                    + "))";

    @Query(
            "SELECT a FROM Appointment a WHERE "
                    + VISIBLE_UPCOMING_PREDICATE
                    + " ORDER BY a.start ASC, a.id ASC")
    Page<Appointment> findVisibleUpcomingAcrossOrganizations(
            @Param("userId") Long userId,
            @Param("organizationIds") Collection<Long> organizationIds,
            @Param("from") Instant from,
            @Param("horizonEnd") Instant horizonEnd,
            @Param("membershipAt") Instant membershipAt,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE " + VISIBLE_UPCOMING_PREDICATE)
    long countVisibleUpcomingAcrossOrganizations(
            @Param("userId") Long userId,
            @Param("organizationIds") Collection<Long> organizationIds,
            @Param("from") Instant from,
            @Param("horizonEnd") Instant horizonEnd,
            @Param("membershipAt") Instant membershipAt);

    @Query(
            "SELECT a.organization.id AS organizationId, COUNT(a) AS unreadCount FROM Appointment a "
                    + "WHERE "
                    + VISIBLE_UPCOMING_PREDICATE
                    + " AND NOT EXISTS ("
                    + "  SELECT 1 FROM AppointmentRead r "
                    + "  WHERE r.appointment = a AND r.userId = :userId"
                    + ") GROUP BY a.organization.id")
    List<AppointmentOrganizationUnreadCount> countUnreadVisibleUpcomingByOrganization(
            @Param("userId") Long userId,
            @Param("organizationIds") Collection<Long> organizationIds,
            @Param("from") Instant from,
            @Param("horizonEnd") Instant horizonEnd,
            @Param("membershipAt") Instant membershipAt);

    String VISIBLE_IN_ORG_DATE_RANGE_PREDICATE =
            "a.organization.id = :organizationId "
                    + "AND a.archived = false "
                    + "AND a.start < :to AND a.end > :from "
                    + "AND (a.audienceScope = com.emberone.appointment.domain.AudienceScope.ALL "
                    + "OR EXISTS ("
                    + "  SELECT 1 FROM AppointmentAudienceGroup aag "
                    + "  WHERE aag.appointment = a "
                    + "  AND aag.id.organizationGroupId IN ("
                    + "    SELECT gum.organizationGroup.id FROM UserOrganizationGroupMembership gum "
                    + "    WHERE gum.user.id = :userId "
                    + "    AND gum.organizationGroup.organization.id = :organizationId "
                    + "    AND gum.startDate <= :membershipAt "
                    + "    AND (gum.endDate IS NULL OR gum.endDate > :membershipAt)"
                    + "  )"
                    + "))";

    @Query(
            "SELECT a FROM Appointment a WHERE "
                    + VISIBLE_IN_ORG_DATE_RANGE_PREDICATE
                    + " ORDER BY a.start ASC, a.id ASC")
    Page<Appointment> findVisibleByOrganizationIdAndDateRange(
            @Param("organizationId") Long organizationId,
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("membershipAt") Instant membershipAt,
            Pageable pageable);
}
