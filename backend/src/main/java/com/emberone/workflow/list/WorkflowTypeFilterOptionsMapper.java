/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.list;

import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.workflow.domain.WorkflowType;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowTypeFilterOptionsMapper {

    private final MessageSource messageSource;

    public List<ListFilterOptionDto> toTypeOptions(List<Long> workflowTypeIds) {
        return workflowTypeIds.stream().map(this::toTypeOption).toList();
    }

    private ListFilterOptionDto toTypeOption(Long workflowTypeId) {
        WorkflowType type = WorkflowType.fromId(workflowTypeId);
        String key = type != null ? type.name() : String.valueOf(workflowTypeId);
        String label = messageSource.getMessage("workflow.type." + key, null, key, Locale.GERMAN);
        return new ListFilterOptionDto(String.valueOf(workflowTypeId), label);
    }
}
