/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.emergencycontact.repository;

import com.emberone.emergencycontact.domain.UserEmergencyContact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface UserEmergencyContactRepository extends JpaRepository<UserEmergencyContact, Long> {

    List<UserEmergencyContact> findByUser_IdOrderByPriorityAscIdAsc(Long userId);

    Optional<UserEmergencyContact> findByIdAndUser_Id(Long id, Long userId);

    @Query(
            "SELECT COALESCE(MAX(c.priority), 0) FROM UserEmergencyContact c WHERE c.user.id = :userId")
    int findMaxPriorityByUserId(@Param("userId") Long userId);

    int countByUser_Id(Long userId);

    @Query(
            """
        select distinct c from UserEmergencyContact c
        join UserOrganizationMembership m on m.user.id = c.user.id
        where m.organization.id = :organizationId
        and m.startDate <= CURRENT_DATE
        and (m.endDate is null or m.endDate > CURRENT_DATE)
        order by c.user.id asc, c.priority asc, c.id asc
        """)
    List<UserEmergencyContact> findByOrganizationId(@Param("organizationId") Long organizationId);
}
