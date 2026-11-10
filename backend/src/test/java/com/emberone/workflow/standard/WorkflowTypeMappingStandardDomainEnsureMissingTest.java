/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.standard;

import static com.emberone.workflow.spi.WorkflowApproverSlotRoles.ADDITIONAL;
import static com.emberone.workflow.spi.WorkflowApproverSlotRoles.CONTEXT;
import static com.emberone.workflow.spi.WorkflowApproverSlotRoles.NEW_MAIN_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.federalstate.baseline.WorkflowTypeMappingBaseline;
import com.emberone.organization.domain.Organization;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.organizationgroup.standard.OrgGroupTemplateRepository;
import com.emberone.platform.exception.ValidationException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.domain.StandardKind;
import com.emberone.standard.domain.StandardSource;
import com.emberone.workflow.domain.WorkflowDefinition;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.domain.WorkflowTypeMapping;
import com.emberone.workflow.domain.WorkflowTypeMappingSlot;
import com.emberone.workflow.event.WorkflowConfigurationChangedEvent;
import com.emberone.workflow.repository.WorkflowDefinitionRepository;
import com.emberone.workflow.repository.WorkflowTypeMappingRepository;
import com.emberone.workflow.spi.WorkflowHandler;
import com.emberone.workflow.spi.WorkflowHandlerRegistry;
import com.emberone.workflow.standard.WorkflowTypeMappingStandardDomain.SlotSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowTypeMappingStandardDomainEnsureMissingTest {

    private static final long TENANT_ID = 1L;
    private static final long STANDARD_ID = 10L;
    private static final long TYPE_ID = WorkflowType.TRAINING_COURSE_RECORD.getId();

    @Mock WorkflowTypeMappingTemplateRepository templateRepository;
    @Mock WorkflowTypeMappingRepository mappingRepository;
    @Mock WorkflowDefinitionRepository definitionRepository;
    @Mock OrganizationGroupRepository organizationGroupRepository;
    @Mock OrgGroupTemplateRepository orgGroupTemplateRepository;
    @Mock WorkflowHandlerRegistry handlerRegistry;
    @Mock WorkflowHandler<Object> handler;
    @Mock ApplicationEventPublisher eventPublisher;

    private WorkflowTypeMappingStandardDomain domain;

    @BeforeEach
    void setUp() {
        domain =
                new WorkflowTypeMappingStandardDomain(
                        templateRepository,
                        mappingRepository,
                        definitionRepository,
                        organizationGroupRepository,
                        orgGroupTemplateRepository,
                        handlerRegistry,
                        eventPublisher);
        doReturn(handler).when(handlerRegistry).require(TYPE_ID);
        when(handler.workflowTypeId()).thenReturn(TYPE_ID);
        when(handler.approverSlotRoles()).thenReturn(List.of(CONTEXT, NEW_MAIN_ORG));
    }

    @Test
    void ensureMissingFromBaselineCreatesOneSlotPerRoleAndRespectsEnabledFlag() {
        Standard standard = customStandard();
        WorkflowTypeMappingBaseline item =
                new WorkflowTypeMappingBaseline("5", TYPE_ID, "hr-group", false, true, false);

        when(templateRepository.findByStandardIdAndConfigKey(STANDARD_ID, "5"))
                .thenReturn(Optional.empty());
        when(orgGroupTemplateRepository.existsByStandardIdAndConfigKey(STANDARD_ID, "hr-group"))
                .thenReturn(true);
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        int added = domain.ensureMissingFromBaseline(standard, List.of(item));

        assertThat(added).isEqualTo(4); // template + CONTEXT + NEW_MAIN_ORG + ADDITIONAL
        ArgumentCaptor<WorkflowTypeMappingTemplate> captor =
                ArgumentCaptor.forClass(WorkflowTypeMappingTemplate.class);
        verify(templateRepository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(captor.getValue().getWorkflowTypeId()).isEqualTo(TYPE_ID);
        assertThat(captor.getValue().getSlots())
                .extracting(
                        WorkflowTypeMappingTemplateSlot::getSlotRole,
                        WorkflowTypeMappingTemplateSlot::getOrgGroupConfigKey)
                .containsExactly(
                        tuple(CONTEXT, "hr-group"),
                        tuple(NEW_MAIN_ORG, "hr-group"),
                        tuple(ADDITIONAL, "hr-group"));
    }

    @Test
    void ensureMissingFromBaselineSkipsCompleteExistingTemplate() {
        WorkflowTypeMappingBaseline item =
                new WorkflowTypeMappingBaseline("5", TYPE_ID, "hr-group", false, true, false);
        WorkflowTypeMappingTemplate template = template(3L, "5", true);
        addTemplateSlot(template, CONTEXT, "a", false, false);
        addTemplateSlot(template, NEW_MAIN_ORG, "a", false, false);
        addTemplateSlot(template, ADDITIONAL, "a", false, false);
        when(templateRepository.findByStandardIdAndConfigKey(STANDARD_ID, "5"))
                .thenReturn(Optional.of(template));

        int added = domain.ensureMissingFromBaseline(customStandard(), List.of(item));

        assertThat(added).isZero();
        verify(templateRepository, never()).save(any());
    }

    @Test
    void ensureMissingFromBaselineAddsMissingRoleCopyingTheContextRow() {
        WorkflowTypeMappingBaseline item =
                new WorkflowTypeMappingBaseline("5", TYPE_ID, "hr-group", false, true, false);
        // Pre-V3 shape: CONTEXT and ADDITIONAL only, with settings that differ from the baseline.
        WorkflowTypeMappingTemplate template = template(3L, "5", true);
        addTemplateSlot(template, CONTEXT, "leadership", true, true);
        addTemplateSlot(template, ADDITIONAL, "other", false, false);
        when(templateRepository.findByStandardIdAndConfigKey(STANDARD_ID, "5"))
                .thenReturn(Optional.of(template));

        int added = domain.ensureMissingFromBaseline(customStandard(), List.of(item));

        assertThat(added).isEqualTo(1);
        assertThat(template.getSlots())
                .extracting(
                        WorkflowTypeMappingTemplateSlot::getSlotRole,
                        WorkflowTypeMappingTemplateSlot::getOrgGroupConfigKey,
                        WorkflowTypeMappingTemplateSlot::isAutoApprove,
                        WorkflowTypeMappingTemplateSlot::isSelfApproveAllowed)
                .containsExactlyInAnyOrder(
                        tuple(CONTEXT, "leadership", true, true),
                        tuple(ADDITIONAL, "other", false, false),
                        tuple(NEW_MAIN_ORG, "leadership", true, true));
        verify(templateRepository).save(template);
    }

    @Test
    void materializeForOrgCreatesMappingWithGroupPerSlot() {
        WorkflowTypeMappingTemplate template = template(3L, "4", false);
        addTemplateSlot(template, CONTEXT, "hr-group", false, true);
        addTemplateSlot(template, ADDITIONAL, "lead-group", true, false);
        when(templateRepository.findByStandardId(STANDARD_ID)).thenReturn(List.of(template));
        when(mappingRepository.findByOrganizationIdAndConfigKey(20L, "4"))
                .thenReturn(Optional.empty());
        stubGroup(20L, "hr-group", 99L);
        stubGroup(20L, "lead-group", 98L);
        when(mappingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(definitionRepository.findByOrganizationIdAndConfigKey(20L, "4"))
                .thenReturn(Optional.empty());

        domain.materializeForOrg(20L, customStandard());

        ArgumentCaptor<WorkflowTypeMapping> captor =
                ArgumentCaptor.forClass(WorkflowTypeMapping.class);
        verify(mappingRepository).save(captor.capture());
        assertThat(captor.getValue().getSlots())
                .extracting(
                        WorkflowTypeMappingSlot::getSlotRole,
                        WorkflowTypeMappingSlot::getOrganizationGroupId,
                        WorkflowTypeMappingSlot::isAutoApprove,
                        WorkflowTypeMappingSlot::isSelfApproveAllowed)
                .containsExactly(
                        tuple(CONTEXT, 99L, false, true), tuple(ADDITIONAL, 98L, true, false));
    }

    @Test
    void materializeForOrgSkipsMappingWhenAnApproverGroupIsMissing() {
        WorkflowTypeMappingTemplate template = template(3L, "4", false);
        addTemplateSlot(template, CONTEXT, "hr-group", false, false);
        addTemplateSlot(template, ADDITIONAL, "gone", false, false);
        when(templateRepository.findByStandardId(STANDARD_ID)).thenReturn(List.of(template));
        when(mappingRepository.findByOrganizationIdAndConfigKey(20L, "4"))
                .thenReturn(Optional.empty());
        stubGroup(20L, "hr-group", 99L);
        when(organizationGroupRepository.findByOrganizationIdAndConfigKey(20L, "gone"))
                .thenReturn(Optional.empty());

        domain.materializeForOrg(20L, customStandard());

        verify(mappingRepository, never()).save(any());
    }

    @Test
    void materializeForOrgAddsMissingRoleToExistingMappingFromTheTemplate() {
        // After the baseline sync added NEW_MAIN_ORG to the template, the org mapping gets it too.
        WorkflowTypeMappingTemplate template = template(3L, "4", true);
        addTemplateSlot(template, CONTEXT, "hr-group", false, false);
        addTemplateSlot(template, NEW_MAIN_ORG, "hr-group", false, false);
        WorkflowTypeMapping existing = mapping(1L, 20L, "4");
        addMappingSlot(existing, CONTEXT, 99L, false, false);
        when(templateRepository.findByStandardId(STANDARD_ID)).thenReturn(List.of(template));
        when(mappingRepository.findByOrganizationIdAndConfigKey(20L, "4"))
                .thenReturn(Optional.of(existing));
        stubGroup(20L, "hr-group", 99L);

        domain.materializeForOrg(20L, customStandard());

        assertThat(existing.getSlots())
                .extracting(WorkflowTypeMappingSlot::getSlotRole)
                .containsExactly(CONTEXT, NEW_MAIN_ORG);
        verify(mappingRepository).save(existing);
    }

    @Test
    void reassignForOrgAppliesEverySlotAndPublishesConfigurationChanged() {
        Standard standard = customStandard();
        WorkflowTypeMappingTemplate template = template(3L, "5", true);
        addTemplateSlot(template, CONTEXT, "hr-group", true, true);
        WorkflowTypeMapping existing = mapping(1L, 20L, "5");
        addMappingSlot(existing, CONTEXT, 50L, false, false);
        addMappingSlot(existing, "OBSOLETE", 51L, false, false);

        when(templateRepository.findByStandardId(STANDARD_ID)).thenReturn(List.of(template));
        when(mappingRepository.findByOrganizationIdAndConfigKey(20L, "5"))
                .thenReturn(Optional.of(existing));
        stubGroup(20L, "hr-group", 99L);
        when(mappingRepository.findByOrganizationIdAndStandardOriginIdIsNotNull(20L))
                .thenReturn(List.of());
        when(definitionRepository.findByOrganizationIdAndConfigKey(20L, "5"))
                .thenReturn(Optional.empty());

        domain.reassignForOrg(20L, standard);

        assertThat(existing.getSlots())
                .extracting(
                        WorkflowTypeMappingSlot::getSlotRole,
                        WorkflowTypeMappingSlot::getOrganizationGroupId,
                        WorkflowTypeMappingSlot::isAutoApprove,
                        WorkflowTypeMappingSlot::isSelfApproveAllowed)
                .containsExactly(tuple(CONTEXT, 99L, true, true));
        ArgumentCaptor<WorkflowConfigurationChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(WorkflowConfigurationChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        WorkflowConfigurationChangedEvent event = eventCaptor.getValue();
        assertThat(event.tenantId()).isEqualTo(TENANT_ID);
        assertThat(event.workflowTypeId()).isEqualTo(TYPE_ID);
        assertThat(event.organizationIds()).containsExactly(20L);
    }

    @Test
    void updateMappingTemplateWritesEveryRoleAndOnlyPropagatesEnabledToTheDefinition() {
        Standard standard = customStandard();
        WorkflowTypeMappingTemplate template = template(3L, "5", true);
        addTemplateSlot(template, CONTEXT, "a", false, false);
        addTemplateSlot(template, ADDITIONAL, "a", false, false);
        when(templateRepository.findByStandardIdAndConfigKey(STANDARD_ID, "5"))
                .thenReturn(Optional.of(template));
        WorkflowTypeMapping existing = mapping(1L, 20L, "5");
        addMappingSlot(existing, CONTEXT, 50L, false, false);
        when(mappingRepository.findByOrganizationIdAndConfigKey(20L, "5"))
                .thenReturn(Optional.of(existing));
        stubGroup(20L, "b", 60L);
        stubGroup(20L, "c", 61L);
        stubGroup(20L, "d", 62L);
        WorkflowDefinition definition =
                WorkflowDefinition.builder().organizationId(20L).enabled(true).build();
        when(definitionRepository.findByOrganizationIdAndConfigKey(20L, "5"))
                .thenReturn(Optional.of(definition));

        domain.updateMappingTemplate(
                standard,
                "5",
                false,
                List.of(
                        new SlotSettings(CONTEXT, "b", true, false),
                        new SlotSettings(NEW_MAIN_ORG, "c", false, true),
                        new SlotSettings(ADDITIONAL, "d", false, false)),
                List.of(Organization.builder().id(20L).tenantId(TENANT_ID).build()));

        assertThat(template.isEnabled()).isFalse();
        assertThat(template.getSlots())
                .extracting(
                        WorkflowTypeMappingTemplateSlot::getSlotRole,
                        WorkflowTypeMappingTemplateSlot::getOrgGroupConfigKey,
                        WorkflowTypeMappingTemplateSlot::isAutoApprove,
                        WorkflowTypeMappingTemplateSlot::isSelfApproveAllowed)
                .containsExactlyInAnyOrder(
                        tuple(CONTEXT, "b", true, false),
                        tuple(NEW_MAIN_ORG, "c", false, true),
                        tuple(ADDITIONAL, "d", false, false));
        assertThat(existing.getSlots())
                .extracting(
                        WorkflowTypeMappingSlot::getSlotRole,
                        WorkflowTypeMappingSlot::getOrganizationGroupId,
                        WorkflowTypeMappingSlot::isAutoApprove,
                        WorkflowTypeMappingSlot::isSelfApproveAllowed)
                .containsExactlyInAnyOrder(
                        tuple(CONTEXT, 60L, true, false),
                        tuple(NEW_MAIN_ORG, 61L, false, true),
                        tuple(ADDITIONAL, 62L, false, false));
        assertThat(definition.isEnabled()).isFalse();
        verify(eventPublisher).publishEvent(any(WorkflowConfigurationChangedEvent.class));
    }

    @Test
    void updateMappingTemplateRejectsUnknownMissingAndDuplicateRoles() {
        Standard standard = customStandard();
        WorkflowTypeMappingTemplate template = template(3L, "5", true);
        addTemplateSlot(template, CONTEXT, "a", false, false);
        when(templateRepository.findByStandardIdAndConfigKey(STANDARD_ID, "5"))
                .thenReturn(Optional.of(template));

        assertThatThrownBy(() ->
                        domain.updateMappingTemplate(
                                standard,
                                "5",
                                true,
                                List.of(
                                        new SlotSettings(CONTEXT, "a", false, false),
                                        new SlotSettings(NEW_MAIN_ORG, "a", false, false),
                                        new SlotSettings(ADDITIONAL, "a", false, false),
                                        new SlotSettings("BOGUS", "a", false, false)),
                                List.of()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("BOGUS");
        assertThatThrownBy(() ->
                        domain.updateMappingTemplate(
                                standard,
                                "5",
                                true,
                                List.of(new SlotSettings(CONTEXT, "a", false, false)),
                                List.of()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("required");
        assertThatThrownBy(() ->
                        domain.updateMappingTemplate(
                                standard,
                                "5",
                                true,
                                List.of(
                                        new SlotSettings(CONTEXT, "a", false, false),
                                        new SlotSettings(CONTEXT, "a", false, false),
                                        new SlotSettings(NEW_MAIN_ORG, "a", false, false),
                                        new SlotSettings(ADDITIONAL, "a", false, false)),
                                List.of()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("more than once");
        verify(templateRepository, never()).save(any());
    }

    @Test
    void copyTemplatesDeepCopiesTheSlots() {
        WorkflowTypeMappingTemplate template = template(3L, "5", true);
        addTemplateSlot(template, CONTEXT, "a", true, false);
        addTemplateSlot(template, ADDITIONAL, "b", false, true);
        when(templateRepository.findByStandardId(STANDARD_ID)).thenReturn(List.of(template));
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Standard target = Standard.builder().id(77L).tenantId(TENANT_ID).name("Copy").build();

        domain.copyTemplates(customStandard(), target);

        ArgumentCaptor<WorkflowTypeMappingTemplate> captor =
                ArgumentCaptor.forClass(WorkflowTypeMappingTemplate.class);
        verify(templateRepository).save(captor.capture());
        WorkflowTypeMappingTemplate copy = captor.getValue();
        assertThat(copy.getId()).isNull();
        assertThat(copy.getStandardId()).isEqualTo(77L);
        assertThat(copy.getSlots()).isNotSameAs(template.getSlots()).hasSize(2);
        assertThat(copy.getSlots())
                .extracting(
                        WorkflowTypeMappingTemplateSlot::getSlotRole,
                        WorkflowTypeMappingTemplateSlot::getOrgGroupConfigKey,
                        WorkflowTypeMappingTemplateSlot::isAutoApprove,
                        WorkflowTypeMappingTemplateSlot::isSelfApproveAllowed)
                .containsExactly(
                        tuple(CONTEXT, "a", true, false), tuple(ADDITIONAL, "b", false, true));
        assertThat(copy.getSlots()).allMatch(slot -> slot.getTemplate() == copy);
    }

    @Test
    void describeContentSummarizesTheContextRole() {
        WorkflowTypeMappingTemplate template = template(3L, "5", true);
        addTemplateSlot(template, CONTEXT, "leadership", true, false);
        addTemplateSlot(template, ADDITIONAL, "other", false, true);
        when(templateRepository.findByStandardId(STANDARD_ID)).thenReturn(List.of(template));

        var items = domain.describeContent(customStandard());

        assertThat(items).hasSize(1);
        assertThat(items.get(0).subtitle()).isEqualTo("leadership");
        assertThat(items.get(0).attributes())
                .containsEntry("approverOrgGroupConfigKey", "leadership")
                .containsEntry("autoApprove", "true")
                .containsEntry("selfApproveAllowed", "false")
                .containsEntry("enabled", "true");
    }

    @Test
    void isOrgGroupUsedAsApproverChecksTheSlots() {
        when(templateRepository.existsByStandardIdAndSlotOrgGroupConfigKey(STANDARD_ID, "g"))
                .thenReturn(true);

        assertThat(domain.isOrgGroupUsedAsApprover(customStandard(), "g")).isTrue();
        assertThat(domain.isOrgGroupUsedAsApprover(customStandard(), "other")).isFalse();
    }

    private void stubGroup(Long organizationId, String configKey, Long groupId) {
        when(organizationGroupRepository.findByOrganizationIdAndConfigKey(
                        organizationId, configKey))
                .thenReturn(
                        Optional.of(
                                OrganizationGroup.builder()
                                        .id(groupId)
                                        .tenantId(TENANT_ID)
                                        .configKey(configKey)
                                        .build()));
    }

    private static Standard customStandard() {
        return Standard.builder()
                .id(STANDARD_ID)
                .tenantId(TENANT_ID)
                .kind(StandardKind.ORGANIZATION_SETUP)
                .source(StandardSource.CUSTOM)
                .name("Custom")
                .build();
    }

    private static WorkflowTypeMappingTemplate template(
            Long id, String configKey, boolean enabled) {
        return WorkflowTypeMappingTemplate.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .standardId(STANDARD_ID)
                .configKey(configKey)
                .workflowTypeId(TYPE_ID)
                .enabled(enabled)
                .slots(new ArrayList<>())
                .build();
    }

    private static void addTemplateSlot(
            WorkflowTypeMappingTemplate template,
            String role,
            String groupKey,
            boolean autoApprove,
            boolean selfApprove) {
        template.getSlots()
                .add(
                        WorkflowTypeMappingTemplateSlot.builder()
                                .tenantId(TENANT_ID)
                                .template(template)
                                .slotRole(role)
                                .orgGroupConfigKey(groupKey)
                                .autoApprove(autoApprove)
                                .selfApproveAllowed(selfApprove)
                                .build());
    }

    private static WorkflowTypeMapping mapping(Long id, Long organizationId, String configKey) {
        return WorkflowTypeMapping.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .organizationId(organizationId)
                .workflowTypeId(TYPE_ID)
                .configKey(configKey)
                .standardOriginId(3L)
                .build();
    }

    private static void addMappingSlot(
            WorkflowTypeMapping mapping,
            String role,
            Long groupId,
            boolean autoApprove,
            boolean selfApprove) {
        mapping.getSlots()
                .add(
                        WorkflowTypeMappingSlot.builder()
                                .tenantId(TENANT_ID)
                                .mapping(mapping)
                                .slotRole(role)
                                .organizationGroupId(groupId)
                                .autoApprove(autoApprove)
                                .selfApproveAllowed(selfApprove)
                                .build());
    }
}
