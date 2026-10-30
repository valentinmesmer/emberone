/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.standard.repository;

import com.emberone.standard.domain.Standard;
import com.emberone.standard.domain.StandardSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface StandardRepository extends JpaRepository<Standard, Long> {

    List<Standard> findAllByOrderByNameAsc();

    Optional<Standard> findBySource(StandardSource source);

    boolean existsByNameIgnoreCase(String name);

    /**
     * Drops clone lineage ({@code base_standard_id}) for standards derived from {@code
     * baseStandardId}.
     */
    @Transactional
    @Modifying
    @Query(
            """
        UPDATE Standard s
        SET s.baseStandardId = null
        WHERE s.baseStandardId = :baseStandardId
        """)
    int detachCloneLineage(@Param("baseStandardId") Long baseStandardId);
}
