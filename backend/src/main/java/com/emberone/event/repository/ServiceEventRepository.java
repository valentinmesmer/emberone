/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.event.repository;

import com.emberone.event.domain.ServiceEvent;
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
public interface ServiceEventRepository
        extends JpaRepository<ServiceEvent, Long>, JpaSpecificationExecutor<ServiceEvent> {

    @Query("SELECT e FROM ServiceEvent e WHERE e.organization.id = :organizationId")
    List<ServiceEvent> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Query(
            "SELECT e FROM ServiceEvent e WHERE e.organization.id = :organizationId "
                    + "AND e.start >= :from AND e.start < :to ORDER BY e.start DESC, e.id DESC")
    Page<ServiceEvent> findByOrganizationIdAndStartBetweenOrderByStartDescIdDesc(
            @Param("organizationId") Long organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    Optional<ServiceEvent> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query(
            "SELECT e.id FROM ServiceEvent e WHERE e.organization.id = :organizationId "
                    + "AND e.id IN :eventIds")
    List<Long> findIdsByOrganizationIdAndIdIn(
            @Param("organizationId") Long organizationId, @Param("eventIds") List<Long> eventIds);

    @Query("SELECT e FROM ServiceEvent e LEFT JOIN FETCH e.leader WHERE e.id IN :ids")
    List<ServiceEvent> findWithLeaderByIdIn(@Param("ids") List<Long> ids);

    @Query("SELECT e.start FROM ServiceEvent e WHERE e.organization.id = :organizationId")
    List<Instant> findStartsByOrganizationId(@Param("organizationId") Long organizationId);
}
