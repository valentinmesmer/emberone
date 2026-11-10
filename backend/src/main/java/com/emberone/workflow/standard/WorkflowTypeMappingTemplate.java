/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.standard;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.*;

/**
 * Standard-library template for a workflow type mapping (which org group approves a workflow type).
 * The template keeps the {@code enabled} flag; group and approval flags are per slot role in {@link
 * WorkflowTypeMappingTemplateSlot}. {@code config_key} is the workflow type id.
 */
@Entity
@Table(
        name = "std_workflow_type_mapping_templates",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tenant_id", "standard_id", "config_key"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WorkflowTypeMappingTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "standard_id", nullable = false)
    private Long standardId;

    @Column(name = "config_key", nullable = false)
    private String configKey;

    @Column(name = "workflow_type_id", nullable = false)
    private Long workflowTypeId;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** One row per approver slot role of the type; group and approval flags live here. */
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkflowTypeMappingTemplateSlot> slots = new ArrayList<>();

    public Optional<WorkflowTypeMappingTemplateSlot> findSlot(String slotRole) {
        return slots.stream().filter(slot -> slot.getSlotRole().equals(slotRole)).findFirst();
    }
}
