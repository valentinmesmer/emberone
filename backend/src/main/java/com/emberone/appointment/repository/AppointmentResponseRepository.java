/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.repository;

import com.emberone.appointment.domain.AppointmentResponse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface AppointmentResponseRepository extends JpaRepository<AppointmentResponse, Long> {

    List<AppointmentResponse> findByAppointmentId(Long appointmentId);

    Optional<AppointmentResponse> findByAppointmentIdAndUserId(Long appointmentId, Long userId);

    List<AppointmentResponse> findByUserIdAndAppointmentIdIn(
            Long userId, List<Long> appointmentIds);
}
