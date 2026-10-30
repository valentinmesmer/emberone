/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.repository;

import com.emberone.appointment.domain.AppointmentRead;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface AppointmentReadRepository extends JpaRepository<AppointmentRead, Long> {

    boolean existsByAppointmentIdAndUserId(Long appointmentId, Long userId);

    @Query(
            "SELECT r.appointment.id FROM AppointmentRead r WHERE r.userId = :userId AND r.appointment.id IN :appointmentIds")
    List<Long> findAppointmentIdsByUserIdAndAppointmentIdIn(
            @Param("userId") Long userId, @Param("appointmentIds") List<Long> appointmentIds);
}
