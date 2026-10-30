/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.mapper;

import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowDecision;
import com.emberone.workflow.domain.WorkflowInputChange;
import com.emberone.workflow.domain.WorkflowParticipant;
import com.emberone.workflow.dto.WorkflowDecisionDto;
import com.emberone.workflow.dto.WorkflowDto;
import com.emberone.workflow.dto.WorkflowInputChangeDto;
import com.emberone.workflow.dto.WorkflowParticipantDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WorkflowMapper {

    @Mapping(target = "openConcurrencyKeys", ignore = true)
    @Mapping(target = "openedByDisplayName", ignore = true)
    @Mapping(target = "selfApproveAllowed", ignore = true)
    @Mapping(target = "canEditInput", ignore = true)
    @Mapping(target = "canRequestInputChange", ignore = true)
    @Mapping(target = "canWithdrawInputChange", ignore = true)
    @Mapping(target = "mapsRejectToInputChange", ignore = true)
    @Mapping(target = "activeInputChange", ignore = true)
    WorkflowDto toDto(Workflow workflow);

    @Mapping(target = "organizationDisplayName", ignore = true)
    @Mapping(target = "principalDisplayName", ignore = true)
    @Mapping(target = "claimedByDisplayName", ignore = true)
    WorkflowParticipantDto toDto(WorkflowParticipant participant);

    @Mapping(target = "userDisplayName", ignore = true)
    WorkflowDecisionDto toDto(WorkflowDecision decision);

    WorkflowInputChangeDto toDto(WorkflowInputChange inputChange);

    List<WorkflowDto> toDtoList(List<Workflow> workflows);
}
