/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.appointment.repository;

import com.emberone.appointment.domain.AppointmentSeries;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface AppointmentSeriesRepository extends JpaRepository<AppointmentSeries, Long> {

    @Query("SELECT s FROM AppointmentSeries s WHERE s.seriesEnd IS NULL OR s.seriesEnd > :now")
    List<AppointmentSeries> findActiveSeries(@Param("now") Instant now);
}
