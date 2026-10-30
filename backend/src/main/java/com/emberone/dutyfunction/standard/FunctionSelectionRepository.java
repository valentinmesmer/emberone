/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.dutyfunction.standard;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface FunctionSelectionRepository extends JpaRepository<FunctionSelection, Long> {

    List<FunctionSelection> findByStandardId(Long standardId);

    boolean existsByStandardIdAndFunctionConfigKey(Long standardId, String functionConfigKey);
}
