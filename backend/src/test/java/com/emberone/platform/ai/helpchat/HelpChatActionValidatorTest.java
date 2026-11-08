/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.ai.helpchat.HelpChatActionValidator.CatalogEntry;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.WorkflowTypeCatalogDto;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class HelpChatActionValidatorTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final HelpChatActionValidator validator = new HelpChatActionValidator();

    private static WorkflowTypeCatalogDto type(WorkflowType type) {
        return WorkflowTypeCatalogDto.builder()
                .workflowTypeId(type.getId())
                .workflowTypeKey(type.name())
                .build();
    }

    private final Map<String, WorkflowTypeCatalogDto> allowed =
            Map.of(
                    "NAME_CHANGE", type(WorkflowType.NAME_CHANGE),
                    "CERTIFICATION_RECORD", type(WorkflowType.CERTIFICATION_RECORD));

    private final Map<String, Map<String, CatalogEntry>> catalogs =
            Map.of(
                    "CERTIFICATION_RECORD",
                    Map.of("G26.3", new CatalogEntry(42L, "G26.3", "Atemschutz G26.3")));

    private static JsonNode json(String value) {
        return MAPPER.readTree(value);
    }

    @Test
    void keepsAllowedTypeAndCatalogEntry() {
        List<HelpChatActionDto> actions =
                validator.validate(
                        json(
                                """
                                [{"target":"NAME_CHANGE","configKey":null},
                                 {"target":"CERTIFICATION_RECORD","configKey":"G26.3"}]
                                """),
                        allowed,
                        catalogs);

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).workflowTypeId()).isEqualTo(WorkflowType.NAME_CHANGE.getId());
        assertThat(actions.get(0).configKey()).isNull();
        assertThat(actions.get(1).catalogId()).isEqualTo(42L);
        assertThat(actions.get(1).label()).isEqualTo("Atemschutz G26.3");
    }

    @Test
    void discardsInventedType() {
        assertThat(
                        validator.validate(
                                json("[{\"target\":\"DELETE_EVERYTHING\",\"configKey\":null}]"),
                                allowed,
                                catalogs))
                .isEmpty();
    }

    @Test
    void discardsTypeTheUserMayNotStart() {
        // ADDRESS_CHANGE exists as a workflow type but is not in the user's allowed set
        assertThat(
                        validator.validate(
                                json("[{\"target\":\"ADDRESS_CHANGE\",\"configKey\":null}]"),
                                allowed,
                                catalogs))
                .isEmpty();
    }

    @Test
    void discardsForeignConfigKey() {
        assertThat(
                        validator.validate(
                                json(
                                        "[{\"target\":\"CERTIFICATION_RECORD\",\"configKey\":\"G99\"}]"),
                                allowed,
                                catalogs))
                .isEmpty();
    }

    @Test
    void ignoresMeaninglessConfigKeyOnTypeWithoutCatalog() {
        List<HelpChatActionDto> actions =
                validator.validate(
                        json("[{\"target\":\"NAME_CHANGE\",\"configKey\":\"junk\"}]"),
                        allowed,
                        catalogs);

        assertThat(actions).hasSize(1);
        assertThat(actions.getFirst().configKey()).isNull();
        assertThat(actions.getFirst().catalogId()).isNull();
    }

    @Test
    void collapsesDuplicatesAndCapsAtThree() {
        Map<String, WorkflowTypeCatalogDto> many =
                Map.of(
                        "NAME_CHANGE", type(WorkflowType.NAME_CHANGE),
                        "ADDRESS_CHANGE", type(WorkflowType.ADDRESS_CHANGE),
                        "RANK_REQUEST", type(WorkflowType.RANK_REQUEST),
                        "USER_DOCUMENT_SUBMISSION", type(WorkflowType.USER_DOCUMENT_SUBMISSION));

        List<HelpChatActionDto> actions =
                validator.validate(
                        json(
                                """
                                [{"target":"NAME_CHANGE","configKey":null},
                                 {"target":"NAME_CHANGE","configKey":null},
                                 {"target":"ADDRESS_CHANGE","configKey":null},
                                 {"target":"RANK_REQUEST","configKey":null},
                                 {"target":"USER_DOCUMENT_SUBMISSION","configKey":null}]
                                """),
                        many,
                        Map.of());

        assertThat(actions).hasSize(3);
        assertThat(actions).extracting(HelpChatActionDto::workflowTypeKey).doesNotHaveDuplicates();
    }

    @Test
    void toleratesGarbage() {
        assertThat(validator.validate(null, allowed, catalogs)).isEmpty();
        assertThat(validator.validate(json("{\"a\":1}"), allowed, catalogs)).isEmpty();
        assertThat(validator.validate(json("[1,\"x\",null,{}]"), allowed, catalogs)).isEmpty();
    }

    private static final HelpChatOrganizationDto ORG_A = new HelpChatOrganizationDto(1L, "FF A");
    private static final HelpChatOrganizationDto ORG_B = new HelpChatOrganizationDto(2L, "FF B");

    private static Map<HelpTopic, AvailableHelpTopic> topics(AvailableHelpTopic... available) {
        Map<HelpTopic, AvailableHelpTopic> map = new java.util.EnumMap<>(HelpTopic.class);
        for (AvailableHelpTopic topic : available) {
            map.put(topic.topic(), topic);
        }
        return map;
    }

    @Test
    void organizationTopicWithCreateUsesCreateOrganizations() {
        List<HelpChatActionDto> actions =
                validator.validate(
                        json("[{\"target\":\"ORG_SERVICE_EVENTS\",\"create\":true}]"),
                        allowed,
                        catalogs,
                        topics(
                                new AvailableHelpTopic(
                                        HelpTopic.ORG_SERVICE_EVENTS,
                                        List.of(ORG_A, ORG_B),
                                        List.of(ORG_B),
                                        null)));

        assertThat(actions).hasSize(1);
        assertThat(actions.getFirst().kind()).isEqualTo(HelpChatActionDto.KIND_TOPIC);
        assertThat(actions.getFirst().topicKey()).isEqualTo("ORG_SERVICE_EVENTS");
        assertThat(actions.getFirst().area()).isEqualTo(HelpTopicArea.ORGANIZATION);
        assertThat(actions.getFirst().create()).isTrue();
        assertThat(actions.getFirst().organizations()).containsExactly(ORG_B);
    }

    @Test
    void createWithoutCreatePermissionFallsBackToOpeningThePage() {
        List<HelpChatActionDto> actions =
                validator.validate(
                        json("[{\"target\":\"ORG_SERVICE_EVENTS\",\"create\":true}]"),
                        allowed,
                        catalogs,
                        topics(
                                new AvailableHelpTopic(
                                        HelpTopic.ORG_SERVICE_EVENTS,
                                        List.of(ORG_A),
                                        List.of(),
                                        null)));

        assertThat(actions.getFirst().create()).isFalse();
        assertThat(actions.getFirst().organizations()).containsExactly(ORG_A);
    }

    @Test
    void unavailableOrUnknownTopicsAreDiscarded() {
        List<HelpChatActionDto> actions =
                validator.validate(
                        json(
                                """
                                [{"target":"ORG_OPERATION_EVENTS","create":false},
                                 {"target":"ADMIN_USERS","create":false},
                                 {"target":"DROP_TABLES","create":true}]
                                """),
                        allowed,
                        catalogs,
                        topics(
                                new AvailableHelpTopic(
                                        HelpTopic.ORG_SERVICE_EVENTS,
                                        List.of(ORG_A),
                                        List.of(),
                                        null)));

        assertThat(actions).isEmpty();
    }

    @Test
    void selfAndAdminTopicsCarryNoOrganizations() {
        List<HelpChatActionDto> actions =
                validator.validate(
                        json(
                                """
                                [{"target":"MY_EMERGENCY_CONTACTS","create":true},
                                 {"target":"ADMIN_ORGANIZATIONS","create":true}]
                                """),
                        allowed,
                        catalogs,
                        topics(
                                new AvailableHelpTopic(
                                        HelpTopic.MY_EMERGENCY_CONTACTS,
                                        List.of(),
                                        List.of(),
                                        null),
                                new AvailableHelpTopic(
                                        HelpTopic.ADMIN_ORGANIZATIONS, List.of(), List.of(), 7L)));

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).create()).isTrue();
        assertThat(actions.get(0).organizations()).isEmpty();
        assertThat(actions.get(1).create()).isFalse();
        assertThat(actions.get(1).tenantId()).isEqualTo(7L);
    }
}
