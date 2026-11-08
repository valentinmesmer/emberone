/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Turns the model's proposed actions into validated ones (concept rule R3: the model only supplies
 * keys from a fixed set; anything else is discarded). Pure function of its inputs, no I/O.
 *
 * <p>Each proposed action has a {@code target}: either a workflow type key or a {@link HelpTopic}
 * key.
 *
 * <ul>
 *   <li>Workflow: must be one of the allowed types (beginnable for the user, not blocked, with help
 *       text). For types with a catalog (certification, training course, driving license) a given
 *       {@code configKey} must be in that catalog, otherwise the action is discarded; for other
 *       types a {@code configKey} is ignored (small models fill it with junk).
 *   <li>Topic: must be available to the user. {@code create} only survives when the user may create
 *       there; the organizations come from the server (create organizations when creating).
 *   <li>Violating actions are discarded; duplicates collapse; at most {@value #MAX_ACTIONS}.
 * </ul>
 */
@Component
public class HelpChatActionValidator {

    static final int MAX_ACTIONS = 3;

    /** Catalog entry as loaded in the user's context. */
    public record CatalogEntry(long id, String configKey, String name) {}

    /** Without topics; for tests of the workflow rules. */
    List<HelpChatActionDto> validate(
            JsonNode proposed,
            Map<String, WorkflowTypeCatalogDto> allowedTypes,
            Map<String, Map<String, CatalogEntry>> catalogsByType) {
        return validate(proposed, allowedTypes, catalogsByType, Map.of());
    }

    /**
     * @param proposed the {@code actions} array from the model (may be null/invalid)
     * @param allowedTypes workflow type key to catalog row, only startable types
     * @param catalogsByType workflow type key to {@code configKey -> entry}; key present means the
     *     type is catalog-based
     * @param topics topics available to the user
     */
    public List<HelpChatActionDto> validate(
            JsonNode proposed,
            Map<String, WorkflowTypeCatalogDto> allowedTypes,
            Map<String, Map<String, CatalogEntry>> catalogsByType,
            Map<HelpTopic, AvailableHelpTopic> topics) {
        if (proposed == null || !proposed.isArray()) {
            return List.of();
        }
        List<HelpChatActionDto> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode node : proposed) {
            if (result.size() >= MAX_ACTIONS) {
                break;
            }
            JsonNode targetNode = node == null ? null : node.get("target");
            if (targetNode == null || !targetNode.isString()) {
                continue;
            }
            String target = targetNode.asString();
            HelpChatActionDto action =
                    allowedTypes.containsKey(target)
                            ? workflowAction(node, allowedTypes.get(target), catalogsByType)
                            : topicAction(node, target, topics);
            if (action != null && seen.add(dedupKey(action))) {
                result.add(action);
            }
        }
        return List.copyOf(result);
    }

    private static HelpChatActionDto workflowAction(
            JsonNode node,
            WorkflowTypeCatalogDto type,
            Map<String, Map<String, CatalogEntry>> catalogsByType) {
        JsonNode keyNode = node.get("configKey");
        String configKey =
                keyNode != null && keyNode.isString() && !keyNode.asString().isBlank()
                        ? keyNode.asString().trim()
                        : null;
        Map<String, CatalogEntry> catalog = catalogsByType.get(type.getWorkflowTypeKey());
        CatalogEntry entry = null;
        if (configKey != null && catalog != null) {
            entry = catalog.get(configKey);
            if (entry == null) {
                return null;
            }
        }
        return HelpChatActionDto.workflow(
                type.getWorkflowTypeId(),
                type.getWorkflowTypeKey(),
                entry == null ? null : entry.configKey(),
                entry == null ? null : entry.id(),
                entry == null ? null : entry.name());
    }

    private static HelpChatActionDto topicAction(
            JsonNode node, String target, Map<HelpTopic, AvailableHelpTopic> topics) {
        HelpTopic topic;
        try {
            topic = HelpTopic.valueOf(target);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        AvailableHelpTopic available = topics.get(topic);
        if (available == null) {
            return null;
        }
        JsonNode createNode = node.get("create");
        boolean create =
                createNode != null
                        && createNode.isBoolean()
                        && createNode.asBoolean()
                        && available.canCreate();
        List<HelpChatOrganizationDto> organizations =
                topic.getArea() != HelpTopicArea.ORGANIZATION
                        ? List.of()
                        : create ? available.createOrganizations() : available.readOrganizations();
        return HelpChatActionDto.topic(available, create, organizations);
    }

    private static String dedupKey(HelpChatActionDto action) {
        return HelpChatActionDto.KIND_WORKFLOW.equals(action.kind())
                ? action.kind() + "|" + action.workflowTypeKey() + "|" + action.configKey()
                : action.kind() + "|" + action.topicKey() + "|" + action.create();
    }
}
