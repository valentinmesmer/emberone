/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.rank.standard;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface RankSelectionRepository extends JpaRepository<RankSelection, Long> {

    List<RankSelection> findByStandardId(Long standardId);

    boolean existsByStandardIdAndRankConfigKey(Long standardId, String rankConfigKey);
}
