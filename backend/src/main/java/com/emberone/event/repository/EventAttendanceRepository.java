/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.repository;

import com.emberone.event.domain.EventAttendance;
import com.emberone.event.domain.EventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface EventAttendanceRepository
        extends JpaRepository<EventAttendance, Long>, JpaSpecificationExecutor<EventAttendance> {

    @Query(
            "SELECT a FROM EventAttendance a JOIN FETCH a.user WHERE a.eventType = :eventType AND a.eventId = :eventId ORDER BY a.user.lastName, a.user.firstName, a.id")
    List<EventAttendance> findByEventTypeAndEventIdWithUser(
            @Param("eventType") EventType eventType, @Param("eventId") Long eventId);

    @Query(
            "SELECT a FROM EventAttendance a JOIN a.user u WHERE a.eventType = :eventType AND a.eventId = :eventId "
                    + "ORDER BY u.lastName ASC, u.firstName ASC, a.id ASC")
    Page<EventAttendance> findByEventTypeAndEventIdOrderByUserName(
            @Param("eventType") EventType eventType,
            @Param("eventId") Long eventId,
            Pageable pageable);

    boolean existsByEventTypeAndEventIdAndUser_Id(EventType eventType, Long eventId, Long userId);

    Optional<EventAttendance> findByIdAndEventTypeAndEventId(
            Long id, EventType eventType, Long eventId);

    @Transactional
    void deleteByEventTypeAndEventId(EventType eventType, Long eventId);

    List<EventAttendance> findByUser_Id(Long userId);

    @Query(
            "SELECT a.eventId FROM EventAttendance a WHERE a.eventType = :eventType AND a.user.id = :userId "
                    + "AND a.eventId IN :eventIds")
    List<Long> findAttendedEventIds(
            @Param("eventType") EventType eventType,
            @Param("userId") Long userId,
            @Param("eventIds") List<Long> eventIds);

    @Query(
            "SELECT COUNT(a) FROM EventAttendance a WHERE a.eventType = :eventType AND a.user.id = :userId AND "
                    + "a.eventId IN (SELECT o.id FROM OperationEvent o WHERE o.organization.id IN "
                    + "(SELECT m.organization.id FROM UserOrganizationMembership m WHERE m.user.id = :userId "
                    + "AND m.startDate <= CURRENT_DATE AND (m.endDate IS NULL OR m.endDate > CURRENT_DATE)))")
    long countAttendanceForUserMemberOrganizations(
            @Param("userId") Long userId, @Param("eventType") EventType eventType);

    @Query("SELECT a FROM EventAttendance a JOIN FETCH a.user WHERE a.id IN :ids")
    List<EventAttendance> findWithUserByIdIn(@Param("ids") List<Long> ids);
}
