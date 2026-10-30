/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.standard;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface QualificationSelectionRepository
        extends JpaRepository<QualificationSelection, Long> {

    List<QualificationSelection> findByStandardId(Long standardId);

    boolean existsByStandardIdAndDefinitionConfigKey(Long standardId, String definitionConfigKey);

    List<QualificationSelection> findByDefinitionConfigKey(String definitionConfigKey);

    @Transactional
    void deleteByDefinitionConfigKey(String definitionConfigKey);
}
