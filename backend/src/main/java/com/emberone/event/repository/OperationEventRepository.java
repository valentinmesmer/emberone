/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.repository;

import com.emberone.event.domain.OperationEvent;
import java.time.Instant;
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
public interface OperationEventRepository
        extends JpaRepository<OperationEvent, Long>, JpaSpecificationExecutor<OperationEvent> {

    @Query("SELECT e FROM OperationEvent e WHERE e.organization.id = :organizationId")
    List<OperationEvent> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query(
            "SELECT e FROM OperationEvent e WHERE e.organization.id = :organizationId "
                    + "AND e.start >= :from AND e.start < :to ORDER BY e.start DESC, e.id DESC")
    Page<OperationEvent> findByOrganizationIdAndStartBetweenOrderByStartDescIdDesc(
            @Param("organizationId") Long organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    Optional<OperationEvent> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query(
            "SELECT e.id FROM OperationEvent e WHERE e.organization.id = :organizationId "
                    + "AND e.id IN :eventIds")
    List<Long> findIdsByOrganizationIdAndIdIn(
            @Param("organizationId") Long organizationId, @Param("eventIds") List<Long> eventIds);

    @Query(
            "SELECT COUNT(o) FROM OperationEvent o WHERE o.organization.id IN "
                    + "(SELECT m.organization.id FROM UserOrganizationMembership m WHERE m.user.id = :userId "
                    + "AND m.startDate <= CURRENT_DATE AND (m.endDate IS NULL OR m.endDate > CURRENT_DATE))")
    long countForUserMemberOrganizations(@Param("userId") Long userId);

    @Query("SELECT e FROM OperationEvent e LEFT JOIN FETCH e.leader WHERE e.id IN :ids")
    List<OperationEvent> findWithLeaderByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT e.start FROM OperationEvent e WHERE e.organization.id = :organizationId")
    List<Instant> findStartsByOrganizationId(@Param("organizationId") Long organizationId);
}
