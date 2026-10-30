/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.certification.dto.OrgCertificationCatalogDto;
import com.emberone.certification.service.CertificationCatalogService;
import com.emberone.drivinglicense.dto.OrgDrivingLicenseCatalogDto;
import com.emberone.drivinglicense.service.DrivingLicenseCatalogService;
import com.emberone.emergencycontact.repository.UserEmergencyContactRepository;
import com.emberone.emergencycontact.service.EmergencyContactService;
import com.emberone.onboarding.config.EmptyStepConfig;
import com.emberone.onboarding.config.OnboardingStepConfig;
import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import com.emberone.onboarding.domain.OnboardingInvitationStepData;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.dto.SelfOnboardingInvitationDetailDto;
import com.emberone.onboarding.handler.EmergencyContactsStepHandler;
import com.emberone.onboarding.handler.EntryDocumentsStepHandler;
import com.emberone.onboarding.handler.OrganizationEntryDocumentsStepHandler;
import com.emberone.onboarding.repository.OnboardingInvitationRepository;
import com.emberone.onboarding.repository.OnboardingInvitationStepDataRepository;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboarding.spi.OnboardingStepHandler;
import com.emberone.onboarding.spi.OnboardingStepHandlerRegistry;
import com.emberone.onboarding.standard.StandardOnboardingStep;
import com.emberone.onboardingdocument.domain.OnboardingDocumentFulfillmentMode;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.service.OnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentDefinitionService;
import com.emberone.onboardingdocument.service.OrganizationOnboardingDocumentTemplateService;
import com.emberone.onboardingdocument.service.UserEntryDocumentFulfillmentService;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinition;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.platform.i18n.LocalizationConfiguration;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.storage.service.ObjectStorageService;
import com.emberone.platform.storage.service.ObjectStorageTransactionCoordinator;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.trainingcourse.dto.OrgTrainingCourseCatalogDto;
import com.emberone.trainingcourse.service.TrainingCourseCatalogService;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowInputChange;
import com.emberone.workflow.domain.WorkflowInputChangeStatus;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.repository.WorkflowRepository;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OnboardingInvitationProgressServiceTest {

    @Mock OnboardingInvitationRepository invitationRepository;
    @Mock OnboardingInvitationStepDataRepository stepDataRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock StandardOnboardingStepService standardOnboardingStepService;
    @Mock OnboardingStepHandlerRegistry handlerRegistry;
    @Mock TenantOnboardingDocumentDefinitionRepository documentDefinitionRepository;

    @Mock
    OrganizationOnboardingDocumentDefinitionRepository organizationDocumentDefinitionRepository;

    @Mock OrganizationOnboardingDocumentDefinitionService organizationDocumentDefinitionService;
    @Mock OrganizationOnboardingDocumentTemplateService organizationDocumentTemplateService;
    @Mock OrganizationEntryDocumentsStepHandler organizationEntryDocumentsStepHandler;
    @Mock OnboardingDocumentTemplateService documentTemplateService;
    @Mock TrainingCourseCatalogService trainingCourseCatalogService;
    @Mock CertificationCatalogService certificationCatalogService;
    @Mock DrivingLicenseCatalogService drivingLicenseCatalogService;
    @Mock ObjectStorageService objectStorageService;
    @Mock ObjectStorageTransactionCoordinator objectStorageTransactionCoordinator;
    @Mock UserEmergencyContactRepository emergencyContactRepository;
    @Mock EmergencyContactService emergencyContactService;
    @Mock UserEntryDocumentFulfillmentService fulfillmentService;
    @Mock WorkflowRepository workflowRepository;
    @Mock OnboardingAttachmentPromotionService attachmentPromotionService;
    @Mock OnboardingInvitationReadinessService readinessService;
    @Mock UserOrganizationMembershipRepository membershipRepository;

    @InjectMocks OnboardingInvitationProgressService service;

    private EmergencyContactsStepHandler emergencyHandler;

    private OnboardingEntryDocumentReuseService entryDocumentReuseService;

    private OnboardingEntryDocumentDraftSupport entryDocumentDraftSupport;

    @BeforeEach
    void setUp() {
        emergencyHandler =
                new EmergencyContactsStepHandler(
                        emergencyContactRepository, emergencyContactService);
        entryDocumentReuseService = new OnboardingEntryDocumentReuseService(fulfillmentService);
        entryDocumentDraftSupport =
                new OnboardingEntryDocumentDraftSupport(
                        documentDefinitionRepository,
                        organizationDocumentDefinitionRepository,
                        documentTemplateService,
                        organizationDocumentTemplateService,
                        new LocalizationService(new LocalizationConfiguration().messageSource()));
        ReflectionTestUtils.setField(
                service, "entryDocumentReuseService", entryDocumentReuseService);
        ReflectionTestUtils.setField(
                service, "entryDocumentDraftSupport", entryDocumentDraftSupport);
        lenient()
                .when(workflowRepository.findActiveBySubject(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        OnboardingDocumentTemplateService.TemplateMetadata templateMetadata =
                new OnboardingDocumentTemplateService.TemplateMetadata(
                        "template.pdf",
                        100L,
                        "a".repeat(64),
                        1,
                        Instant.parse("2024-06-01T12:00:00Z"));
        lenient()
                .when(
                        documentTemplateService.hasCurrentTemplate(
                                any(TenantOnboardingDocumentDefinition.class)))
                .thenReturn(true);
        lenient()
                .when(
                        documentTemplateService.findCurrentTemplateMetadata(
                                any(TenantOnboardingDocumentDefinition.class)))
                .thenReturn(Optional.of(templateMetadata));
        lenient()
                .when(
                        organizationDocumentTemplateService.hasCurrentTemplate(
                                any(OrganizationOnboardingDocumentDefinition.class)))
                .thenReturn(true);
        lenient()
                .when(
                        organizationDocumentTemplateService.findCurrentTemplateMetadata(
                                any(OrganizationOnboardingDocumentDefinition.class)))
                .thenReturn(Optional.of(templateMetadata));
    }

    @Test
    void getDetailReturnsConfiguredStepsAndDraftFulfillment() {
        stubInvitationAndOrg();
        when(emergencyContactRepository.countByUser_Id(20L)).thenReturn(0);
        when(stepDataRepository.findByInvitationId(3L)).thenReturn(List.of());
        stubEmptyCatalogs();

        SelfOnboardingInvitationDetailDto detail = service.getDetail(20L, 3L);

        assertThat(detail.getId()).isEqualTo(3L);
        assertThat(detail.getTargetOrganizationName()).isEqualTo("Alpha Wehr");
        assertThat(detail.isEditable()).isTrue();
        assertThat(detail.isCompletionEnabled()).isFalse();
        assertThat(detail.isAllStepsFulfilled()).isFalse();
        assertThat(detail.getSteps()).hasSize(1);
        assertThat(detail.getSteps().get(0).getStepType())
                .isEqualTo(OnboardingStepType.EMERGENCY_CONTACTS);
        assertThat(detail.getSteps().get(0).isFulfilled()).isFalse();
        assertThat(detail.getAlreadyFulfilledStepTypes()).isEmpty();
    }

    @Test
    void getDetailExposesInputChangeMessageWhenAwaitingInputChange() {
        stubInvitationAndOrg();
        when(emergencyContactRepository.countByUser_Id(20L)).thenReturn(0);
        when(stepDataRepository.findByInvitationId(3L)).thenReturn(List.of());
        stubEmptyCatalogs();
        Workflow workflow =
                Workflow.builder()
                        .id(44L)
                        .tenantId(7L)
                        .status(WorkflowStatus.AWAITING_INPUT_CHANGE)
                        .openedByUserId(20L)
                        .inputChanges(new LinkedHashSet<>())
                        .build();
        WorkflowInputChange change =
                WorkflowInputChange.builder()
                        .tenantId(7L)
                        .workflow(workflow)
                        .requestedByUserId(9L)
                        .message("Bitte Führerschein nachreichen")
                        .status(WorkflowInputChangeStatus.OPEN)
                        .build();
        workflow.getInputChanges().add(change);
        when(workflowRepository.findActiveBySubject(any(), any(), any(), any()))
                .thenReturn(Optional.of(workflow));

        SelfOnboardingInvitationDetailDto detail = service.getDetail(20L, 3L);

        assertThat(detail.isEditable()).isTrue();
        assertThat(detail.getInputChangeMessage()).isEqualTo("Bitte Führerschein nachreichen");
    }

    @Test
    void getDetailOmitsFulfilledSteps() {
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation()));
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha Wehr").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(5L).tenantId(7L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.TRAINING_COURSES)
                                        .sortOrder(0)
                                        .config(Map.of())
                                        .build(),
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.EMERGENCY_CONTACTS)
                                        .sortOrder(1)
                                        .config(Map.of("minimumCount", 1))
                                        .build()));
        OnboardingStepHandler trainingHandler = mock(OnboardingStepHandler.class);
        when(trainingHandler.parseConfig(any())).thenReturn(EmptyStepConfig.INSTANCE);
        when(trainingHandler.isFulfilled(
                        any(OnboardingStepFulfillmentContext.class),
                        any(OnboardingStepConfig.class)))
                .thenReturn(true);
        when(trainingHandler.hasInvitationWork(any(), any())).thenReturn(false);
        when(handlerRegistry.require(OnboardingStepType.TRAINING_COURSES))
                .thenReturn(trainingHandler);
        when(handlerRegistry.require(OnboardingStepType.EMERGENCY_CONTACTS))
                .thenReturn(emergencyHandler);
        when(emergencyContactRepository.countByUser_Id(20L)).thenReturn(0);
        when(stepDataRepository.findByInvitationId(3L)).thenReturn(List.of());
        stubEmptyCatalogs();

        SelfOnboardingInvitationDetailDto detail = service.getDetail(20L, 3L);

        assertThat(detail.getSteps()).hasSize(1);
        assertThat(detail.getSteps().get(0).getStepType())
                .isEqualTo(OnboardingStepType.EMERGENCY_CONTACTS);
        assertThat(detail.isAllStepsFulfilled()).isFalse();
        assertThat(detail.getAlreadyFulfilledStepTypes())
                .containsExactly(OnboardingStepType.TRAINING_COURSES);
    }

    @Test
    void getDetailOmitsLiveFulfilledStepsWithoutInvitationWorkWhenNotEditable() {
        OnboardingInvitation invitation = invitation();
        invitation.setStatus(OnboardingInvitationStatus.SUBMITTED);
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation));
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha Wehr").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(5L).tenantId(7L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.TRAINING_COURSES)
                                        .sortOrder(0)
                                        .config(Map.of())
                                        .build(),
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.EMERGENCY_CONTACTS)
                                        .sortOrder(1)
                                        .config(Map.of("minimumCount", 1))
                                        .build()));
        OnboardingStepHandler trainingHandler = mock(OnboardingStepHandler.class);
        when(trainingHandler.parseConfig(any())).thenReturn(EmptyStepConfig.INSTANCE);
        when(trainingHandler.isFulfilled(
                        any(OnboardingStepFulfillmentContext.class),
                        any(OnboardingStepConfig.class)))
                .thenReturn(true);
        when(trainingHandler.hasInvitationWork(any(), any())).thenReturn(false);
        when(handlerRegistry.require(OnboardingStepType.TRAINING_COURSES))
                .thenReturn(trainingHandler);
        when(handlerRegistry.require(OnboardingStepType.EMERGENCY_CONTACTS))
                .thenReturn(emergencyHandler);
        when(emergencyContactRepository.countByUser_Id(20L)).thenReturn(0);
        when(stepDataRepository.findByInvitationId(3L)).thenReturn(List.of());
        stubEmptyCatalogs();

        SelfOnboardingInvitationDetailDto detail = service.getDetail(20L, 3L);

        assertThat(detail.getSteps()).hasSize(1);
        assertThat(detail.getSteps().get(0).getStepType())
                .isEqualTo(OnboardingStepType.EMERGENCY_CONTACTS);
        assertThat(detail.isEditable()).isFalse();
        assertThat(detail.getAlreadyFulfilledStepTypes())
                .containsExactly(OnboardingStepType.TRAINING_COURSES);
    }

    @Test
    void getDetailForApprovedKeepsOnlyInvitationWorkAndIgnoresLiveGaps() {
        OnboardingInvitation invitation = invitation();
        invitation.setStatus(OnboardingInvitationStatus.APPROVED);
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation));
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha Wehr").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(5L).tenantId(7L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.ENTRY_DOCUMENTS)
                                        .sortOrder(0)
                                        .config(Map.of("documentConfigKeys", List.of("DSGVO")))
                                        .build(),
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.EMERGENCY_CONTACTS)
                                        .sortOrder(1)
                                        .config(Map.of("minimumCount", 1))
                                        .build()));
        OnboardingStepHandler entryHandler = mock(OnboardingStepHandler.class);
        when(entryHandler.parseConfig(any())).thenReturn(EmptyStepConfig.INSTANCE);
        when(entryHandler.hasInvitationWork(any(), any())).thenReturn(true);
        when(handlerRegistry.require(OnboardingStepType.ENTRY_DOCUMENTS)).thenReturn(entryHandler);
        when(handlerRegistry.require(OnboardingStepType.EMERGENCY_CONTACTS))
                .thenReturn(emergencyHandler);
        when(stepDataRepository.findByInvitationId(3L))
                .thenReturn(
                        List.of(
                                OnboardingInvitationStepData.builder()
                                        .tenantId(7L)
                                        .invitationId(3L)
                                        .stepType(OnboardingStepType.ENTRY_DOCUMENTS)
                                        .payload(
                                                Map.of(
                                                        "documents",
                                                        List.of(
                                                                Map.of(
                                                                        "configKey",
                                                                        "DSGVO",
                                                                        "confirmed",
                                                                        true))))
                                        .build()));
        when(documentDefinitionRepository.findByConfigKey("DSGVO"))
                .thenReturn(
                        Optional.of(
                                TenantOnboardingDocumentDefinition.builder()
                                        .tenantId(7L)
                                        .configKey("DSGVO")
                                        .name("Datenschutz")
                                        .description("")
                                        .fulfillmentMode(
                                                OnboardingDocumentFulfillmentMode.CONFIRMED)
                                        .mandatory(true)
                                        .active(true)
                                        .archived(false)
                                        .version(1)
                                        .build()));
        stubEmptyCatalogs();

        SelfOnboardingInvitationDetailDto detail = service.getDetail(20L, 3L);

        assertThat(detail.getSteps()).hasSize(1);
        assertThat(detail.getSteps().get(0).getStepType())
                .isEqualTo(OnboardingStepType.ENTRY_DOCUMENTS);
        assertThat(detail.getSteps().get(0).isFulfilled()).isTrue();
        assertThat(detail.isAllStepsFulfilled()).isTrue();
        assertThat(detail.isEditable()).isFalse();
        assertThat(detail.isCompletionEnabled()).isFalse();
        assertThat(detail.getAlreadyFulfilledStepTypes()).isEmpty();
        assertThat(detail.getCatalog().getDocuments()).hasSize(1);
        assertThat(detail.getCatalog().getDocuments().get(0).getConfigKey()).isEqualTo("DSGVO");
    }

    @Test
    void getDetailForApprovedIgnoresNewlyAddedOrganizationDocuments() {
        OnboardingInvitation invitation = invitation();
        invitation.setStatus(OnboardingInvitationStatus.APPROVED);
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation));
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha Wehr").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(5L).tenantId(7L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS)
                                        .sortOrder(0)
                                        .config(Map.of())
                                        .build()));
        when(handlerRegistry.require(OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS))
                .thenReturn(
                        new OrganizationEntryDocumentsStepHandler(
                                organizationDocumentDefinitionService,
                                organizationDocumentDefinitionRepository,
                                entryDocumentReuseService,
                                entryDocumentDraftSupport,
                                attachmentPromotionService));
        when(stepDataRepository.findByInvitationId(3L))
                .thenReturn(
                        List.of(
                                OnboardingInvitationStepData.builder()
                                        .tenantId(7L)
                                        .invitationId(3L)
                                        .stepType(OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS)
                                        .payload(
                                                Map.of(
                                                        "documents",
                                                        List.of(
                                                                Map.of(
                                                                        "configKey",
                                                                        "OLD_DOC",
                                                                        "confirmed",
                                                                        true))))
                                        .build()));
        when(organizationDocumentDefinitionRepository.findByOrganizationIdAndConfigKey(
                        10L, "OLD_DOC"))
                .thenReturn(
                        Optional.of(
                                OrganizationOnboardingDocumentDefinition.builder()
                                        .tenantId(7L)
                                        .organizationId(10L)
                                        .configKey("OLD_DOC")
                                        .name("Altes Dokument")
                                        .description("")
                                        .fulfillmentMode(
                                                OnboardingDocumentFulfillmentMode.CONFIRMED)
                                        .mandatory(true)
                                        .active(true)
                                        .archived(false)
                                        .version(1)
                                        .build()));
        // Live catalog would include a new doc - must not appear for APPROVED.
        lenient()
                .when(organizationDocumentDefinitionService.listActiveConfigKeys(10L))
                .thenReturn(List.of("OLD_DOC", "NEW_DOC"));
        stubEmptyCatalogs();

        SelfOnboardingInvitationDetailDto detail = service.getDetail(20L, 3L);

        assertThat(detail.isAllStepsFulfilled()).isTrue();
        assertThat(detail.getSteps()).hasSize(1);
        assertThat(detail.getSteps().get(0).isFulfilled()).isTrue();
        assertThat(detail.getCatalog().getOrganizationDocuments()).hasSize(1);
        assertThat(detail.getCatalog().getOrganizationDocuments().get(0).getConfigKey())
                .isEqualTo("OLD_DOC");
    }

    @Test
    void getDetailForOpenStillListsLiveOrganizationDocuments() {
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation()));
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha Wehr").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(5L).tenantId(7L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS)
                                        .sortOrder(0)
                                        .config(Map.of())
                                        .build()));
        when(handlerRegistry.require(OnboardingStepType.ORGANIZATION_ENTRY_DOCUMENTS))
                .thenReturn(
                        new OrganizationEntryDocumentsStepHandler(
                                organizationDocumentDefinitionService,
                                organizationDocumentDefinitionRepository,
                                entryDocumentReuseService,
                                entryDocumentDraftSupport,
                                attachmentPromotionService));
        when(stepDataRepository.findByInvitationId(3L)).thenReturn(List.of());
        when(organizationDocumentDefinitionService.listActiveConfigKeys(10L))
                .thenReturn(List.of("NEW_DOC"));
        when(organizationDocumentDefinitionRepository.findByOrganizationIdAndConfigKey(
                        10L, "NEW_DOC"))
                .thenReturn(
                        Optional.of(
                                OrganizationOnboardingDocumentDefinition.builder()
                                        .tenantId(7L)
                                        .organizationId(10L)
                                        .configKey("NEW_DOC")
                                        .name("Neues Dokument")
                                        .description("")
                                        .fulfillmentMode(
                                                OnboardingDocumentFulfillmentMode.CONFIRMED)
                                        .mandatory(true)
                                        .active(true)
                                        .archived(false)
                                        .version(1)
                                        .build()));
        when(fulfillmentService.findFulfilledOrganizationConfigKeys(10L, 20L, List.of("NEW_DOC")))
                .thenReturn(Set.of());
        when(fulfillmentService.areAllOrganizationFulfilled(10L, 20L, List.of("NEW_DOC")))
                .thenReturn(false);
        stubEmptyCatalogs();
        when(readinessService.areAllStepsFulfilled(any(), any(), any())).thenReturn(false);

        SelfOnboardingInvitationDetailDto detail = service.getDetail(20L, 3L);

        assertThat(detail.getCatalog().getOrganizationDocuments()).hasSize(1);
        assertThat(detail.getCatalog().getOrganizationDocuments().get(0).getConfigKey())
                .isEqualTo("NEW_DOC");
        assertThat(detail.getSteps()).hasSize(1);
        assertThat(detail.getSteps().get(0).isFulfilled()).isFalse();
    }

    @Test
    void getDetailOmitsEmergencyContactsWhenLiveFulfilled() {
        stubInvitationAndOrg();
        when(emergencyContactRepository.countByUser_Id(20L)).thenReturn(2);
        when(stepDataRepository.findByInvitationId(3L)).thenReturn(List.of());
        stubEmptyCatalogs();
        when(readinessService.areAllStepsFulfilled(any(), any(), any())).thenReturn(true);

        SelfOnboardingInvitationDetailDto detail = service.getDetail(20L, 3L);

        assertThat(detail.getSteps()).isEmpty();
        assertThat(detail.isAllStepsFulfilled()).isTrue();
        assertThat(detail.getAlreadyFulfilledStepTypes())
                .containsExactly(OnboardingStepType.EMERGENCY_CONTACTS);
    }

    @Test
    void saveStepPersistsNormalizedPayload() {
        stubInvitationAndOrg();
        when(emergencyContactRepository.countByUser_Id(20L)).thenReturn(0);
        when(stepDataRepository.findByInvitationIdAndStepType(
                        3L, OnboardingStepType.EMERGENCY_CONTACTS))
                .thenReturn(Optional.empty());
        when(stepDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepDataRepository.findByInvitationId(3L)).thenReturn(List.of());
        stubEmptyCatalogs();

        Map<String, Object> contact = new HashMap<>();
        contact.put("relationship", "PARENT");
        contact.put("firstName", "Ada");
        contact.put("lastName", "Lovelace");
        contact.put("street", "A");
        contact.put("city", "B");
        contact.put("postalCode", "1");
        contact.put("country", "DE");
        contact.put("phone", "+49111");
        contact.put("email", "ada@example.com");

        service.saveStep(
                20L,
                3L,
                OnboardingStepType.EMERGENCY_CONTACTS,
                Map.of("contacts", List.of(contact)));

        ArgumentCaptor<OnboardingInvitationStepData> captor =
                ArgumentCaptor.forClass(OnboardingInvitationStepData.class);
        verify(stepDataRepository).save(captor.capture());
        assertThat(captor.getValue().getStepType())
                .isEqualTo(OnboardingStepType.EMERGENCY_CONTACTS);
        assertThat(captor.getValue().getPayload()).containsKey("contacts");
    }

    @Test
    void getDetailRejectsForeignInvitation() {
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(20L, 3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveEntryDocumentsStepPreservesUploadedDocumentVersion() {
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation()));
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha Wehr").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(5L).tenantId(7L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.ENTRY_DOCUMENTS)
                                        .sortOrder(0)
                                        .config(
                                                Map.of(
                                                        "documentConfigKeys",
                                                        List.of("DSGVO", "GGG")))
                                        .build()));
        EntryDocumentsStepHandler entryHandler =
                new EntryDocumentsStepHandler(
                        documentDefinitionRepository,
                        documentTemplateService,
                        entryDocumentReuseService,
                        entryDocumentDraftSupport,
                        attachmentPromotionService);
        when(handlerRegistry.require(OnboardingStepType.ENTRY_DOCUMENTS)).thenReturn(entryHandler);
        TenantOnboardingDocumentDefinition dsgvo =
                TenantOnboardingDocumentDefinition.builder()
                        .tenantId(7L)
                        .configKey("DSGVO")
                        .name("Datenschutz")
                        .description("")
                        .fulfillmentMode(OnboardingDocumentFulfillmentMode.SIGNED_UPLOAD)
                        .mandatory(true)
                        .active(true)
                        .archived(false)
                        .version(1)
                        .build();
        TenantOnboardingDocumentDefinition ggg =
                TenantOnboardingDocumentDefinition.builder()
                        .tenantId(7L)
                        .configKey("GGG")
                        .name("GGG")
                        .description("")
                        .fulfillmentMode(OnboardingDocumentFulfillmentMode.CONFIRMED)
                        .mandatory(true)
                        .active(true)
                        .archived(false)
                        .version(1)
                        .build();
        when(documentDefinitionRepository.findByConfigKey("DSGVO")).thenReturn(Optional.of(dsgvo));
        when(documentDefinitionRepository.findByConfigKey("GGG")).thenReturn(Optional.of(ggg));
        OnboardingInvitationStepData existing =
                OnboardingInvitationStepData.builder()
                        .tenantId(7L)
                        .invitationId(3L)
                        .stepType(OnboardingStepType.ENTRY_DOCUMENTS)
                        .payload(
                                Map.of(
                                        "documents",
                                        List.of(
                                                Map.of(
                                                        "configKey",
                                                        "DSGVO",
                                                        "storageKey",
                                                        "tenants/7/upload.pdf",
                                                        "fileName",
                                                        "Antragsdaten.pdf",
                                                        "documentVersion",
                                                        1,
                                                        "openedAt",
                                                        "2024-06-01T12:00:00Z"),
                                                Map.of(
                                                        "configKey",
                                                        "GGG",
                                                        "openedAt",
                                                        "2024-06-01T12:00:00Z"))))
                        .build();
        when(stepDataRepository.findByInvitationIdAndStepType(
                        3L, OnboardingStepType.ENTRY_DOCUMENTS))
                .thenReturn(Optional.of(existing));
        AtomicReference<OnboardingInvitationStepData> savedStepData =
                new AtomicReference<>(existing);
        when(stepDataRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            OnboardingInvitationStepData saved = invocation.getArgument(0);
                            savedStepData.set(saved);
                            return saved;
                        });
        when(stepDataRepository.findByInvitationId(3L))
                .thenAnswer(invocation -> List.of(savedStepData.get()));
        stubEmptyCatalogs();
        when(readinessService.areAllStepsFulfilled(any(), any(), any())).thenReturn(true);

        SelfOnboardingInvitationDetailDto detail =
                service.saveStep(
                        20L,
                        3L,
                        OnboardingStepType.ENTRY_DOCUMENTS,
                        Map.of(
                                "documents",
                                List.of(
                                        Map.of("configKey", "DSGVO", "confirmed", false),
                                        Map.of("configKey", "GGG", "confirmed", true))));

        ArgumentCaptor<OnboardingInvitationStepData> captor =
                ArgumentCaptor.forClass(OnboardingInvitationStepData.class);
        verify(stepDataRepository).save(captor.capture());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> savedDocuments =
                (List<Map<String, Object>>) captor.getValue().getPayload().get("documents");
        Map<String, Object> savedDsgvo =
                savedDocuments.stream()
                        .filter(document -> "DSGVO".equals(document.get("configKey")))
                        .findFirst()
                        .orElseThrow();
        assertThat(savedDsgvo).containsEntry("storageKey", "tenants/7/upload.pdf");
        assertThat(savedDsgvo).containsEntry("documentVersion", 1);
        assertThat(detail.getSteps()).hasSize(1);
        assertThat(detail.getSteps().get(0).isFulfilled()).isTrue();
    }

    @Test
    void saveStepRejectsSubmittedInvitation() {
        OnboardingInvitation invitation = invitation();
        invitation.setStatus(OnboardingInvitationStatus.SUBMITTED);
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(
                        () ->
                                service.saveStep(
                                        20L,
                                        3L,
                                        OnboardingStepType.EMERGENCY_CONTACTS,
                                        Map.of("contacts", List.of())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void downloadSignedDocumentReturnsStoredFile() {
        ReflectionTestUtils.setField(service, "attachmentsBucket", "attachments");
        stubEntryDocumentsConfigured();
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation()));
        when(stepDataRepository.findByInvitationIdAndStepType(
                        3L, OnboardingStepType.ENTRY_DOCUMENTS))
                .thenReturn(
                        Optional.of(
                                OnboardingInvitationStepData.builder()
                                        .tenantId(7L)
                                        .invitationId(3L)
                                        .stepType(OnboardingStepType.ENTRY_DOCUMENTS)
                                        .payload(
                                                Map.of(
                                                        "documents",
                                                        List.of(
                                                                Map.of(
                                                                        "configKey",
                                                                        "DSGVO",
                                                                        "attachments",
                                                                        List.of(
                                                                                Map.of(
                                                                                        "storageKey",
                                                                                        "tenants/7/upload.pdf",
                                                                                        "fileName",
                                                                                        "signed.pdf",
                                                                                        "contentType",
                                                                                        "application/pdf"))))))
                                        .build()));
        when(objectStorageService.downloadFile("attachments", "tenants/7/upload.pdf"))
                .thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));

        OnboardingInvitationProgressService.StoredUpload download =
                service.downloadSignedDocument(20L, 3L, "DSGVO");

        assertThat(download.fileName()).isEqualTo("signed.pdf");
        assertThat(download.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void downloadSignedDocumentMissingUploadIsNotFound() {
        stubEntryDocumentsConfigured();
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation()));
        when(stepDataRepository.findByInvitationIdAndStepType(
                        3L, OnboardingStepType.ENTRY_DOCUMENTS))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadSignedDocument(20L, 3L, "DSGVO"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void downloadSignedDocumentForOrganizationRejectsOtherOrg() {
        when(invitationRepository.findByIdAndTargetOrganizationId(3L, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadSignedDocumentForOrganization(99L, 3L, "DSGVO"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteDrivingLicenseAttachmentRemovesStoredFile() {
        ReflectionTestUtils.setField(service, "attachmentsBucket", "attachments");
        stubDrivingLicenseConfigured();
        stubDrivingLicenseDetailHandler();
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("fileName", "VM_FL.pdf");
        first.put("storageKey", "tenants/7/a.pdf");
        first.put("contentType", "application/pdf");
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("fileName", "other.pdf");
        second.put("storageKey", "tenants/7/b.pdf");
        second.put("contentType", "application/pdf");
        Map<String, Object> license = new LinkedHashMap<>();
        license.put("attachments", new ArrayList<>(List.of(first, second)));
        license.put("fileName", "VM_FL.pdf");
        license.put("storageKey", "tenants/7/a.pdf");
        Map<String, Object> payload = new HashMap<>();
        payload.put("license", license);
        OnboardingInvitationStepData data =
                OnboardingInvitationStepData.builder()
                        .tenantId(7L)
                        .invitationId(3L)
                        .stepType(OnboardingStepType.DRIVING_LICENSE)
                        .payload(payload)
                        .build();
        when(stepDataRepository.findByInvitationIdAndStepType(
                        3L, OnboardingStepType.DRIVING_LICENSE))
                .thenReturn(Optional.of(data));
        when(stepDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepDataRepository.findByInvitationId(3L)).thenReturn(List.of(data));
        stubEmptyCatalogs();

        service.deleteDrivingLicenseAttachment(20L, 3L, 0);

        verify(objectStorageTransactionCoordinator)
                .deleteFileAfterCommit("attachments", "tenants/7/a.pdf");
        ArgumentCaptor<OnboardingInvitationStepData> captor =
                ArgumentCaptor.forClass(OnboardingInvitationStepData.class);
        verify(stepDataRepository).save(captor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> savedLicense =
                (Map<String, Object>) captor.getValue().getPayload().get("license");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> savedAttachments =
                (List<Map<String, Object>>) savedLicense.get("attachments");
        assertThat(savedAttachments).hasSize(1);
        assertThat(savedAttachments.get(0)).containsEntry("fileName", "other.pdf");
        assertThat(savedLicense).containsEntry("fileName", "other.pdf");
        assertThat(savedLicense).containsEntry("storageKey", "tenants/7/b.pdf");
    }

    @Test
    void deleteDrivingLicenseAttachmentMissingIsNotFound() {
        stubDrivingLicenseConfigured();
        when(stepDataRepository.findByInvitationIdAndStepType(
                        3L, OnboardingStepType.DRIVING_LICENSE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDrivingLicenseAttachment(20L, 3L, 0))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteDrivingLicenseAttachmentRejectsSubmittedInvitation() {
        OnboardingInvitation invitation = invitation();
        invitation.setStatus(OnboardingInvitationStatus.SUBMITTED);
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.deleteDrivingLicenseAttachment(20L, 3L, 0))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteRecordAttachmentRemovesStoredFile() {
        ReflectionTestUtils.setField(service, "attachmentsBucket", "attachments");
        stubCertificationsConfigured();
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("fileName", "proof.pdf");
        attachment.put("storageKey", "tenants/7/proof.pdf");
        attachment.put("contentType", "application/pdf");
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("recordKey", "rec-1");
        record.put("attachments", new ArrayList<>(List.of(attachment)));
        Map<String, Object> payload = new HashMap<>();
        payload.put("records", new ArrayList<>(List.of(record)));
        OnboardingInvitationStepData data =
                OnboardingInvitationStepData.builder()
                        .tenantId(7L)
                        .invitationId(3L)
                        .stepType(OnboardingStepType.CERTIFICATIONS)
                        .payload(payload)
                        .build();
        when(stepDataRepository.findByInvitationIdAndStepType(
                        3L, OnboardingStepType.CERTIFICATIONS))
                .thenReturn(Optional.of(data));
        when(stepDataRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepDataRepository.findByInvitationId(3L)).thenReturn(List.of(data));
        stubEmptyCatalogs();

        service.deleteRecordAttachment(20L, 3L, OnboardingStepType.CERTIFICATIONS, "rec-1", 0);

        verify(objectStorageTransactionCoordinator)
                .deleteFileAfterCommit("attachments", "tenants/7/proof.pdf");
        ArgumentCaptor<OnboardingInvitationStepData> captor =
                ArgumentCaptor.forClass(OnboardingInvitationStepData.class);
        verify(stepDataRepository).save(captor.capture());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> savedRecords =
                (List<Map<String, Object>>) captor.getValue().getPayload().get("records");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> savedAttachments =
                (List<Map<String, Object>>) savedRecords.get(0).get("attachments");
        assertThat(savedAttachments).isEmpty();
        assertThat(savedRecords.get(0)).doesNotContainKey("fileName");
        assertThat(savedRecords.get(0)).doesNotContainKey("uploaded");
    }

    private void stubDrivingLicenseConfigured() {
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation()));
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha Wehr").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(5L).tenantId(7L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.DRIVING_LICENSE)
                                        .sortOrder(0)
                                        .config(Map.of())
                                        .build()));
    }

    private void stubDrivingLicenseDetailHandler() {
        OnboardingStepHandler handler = mock(OnboardingStepHandler.class);
        when(handler.parseConfig(any())).thenReturn(EmptyStepConfig.INSTANCE);
        when(handler.isFulfilled(any(), any())).thenReturn(false);
        when(handler.isOnboardingFulfilled(any(), any(), any())).thenReturn(false);
        when(handlerRegistry.require(OnboardingStepType.DRIVING_LICENSE)).thenReturn(handler);
    }

    private void stubCertificationsConfigured() {
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation()));
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha Wehr").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(5L).tenantId(7L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.CERTIFICATIONS)
                                        .sortOrder(0)
                                        .config(Map.of())
                                        .build()));
        OnboardingStepHandler handler = mock(OnboardingStepHandler.class);
        when(handler.parseConfig(any())).thenReturn(EmptyStepConfig.INSTANCE);
        when(handler.isFulfilled(any(), any())).thenReturn(false);
        when(handler.isOnboardingFulfilled(any(), any(), any())).thenReturn(false);
        when(handlerRegistry.require(OnboardingStepType.CERTIFICATIONS)).thenReturn(handler);
    }

    private void stubEntryDocumentsConfigured() {
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha Wehr").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(5L).tenantId(7L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.ENTRY_DOCUMENTS)
                                        .sortOrder(0)
                                        .config(Map.of("documentConfigKeys", List.of("DSGVO")))
                                        .build()));
        EntryDocumentsStepHandler entryHandler =
                new EntryDocumentsStepHandler(
                        documentDefinitionRepository,
                        documentTemplateService,
                        entryDocumentReuseService,
                        entryDocumentDraftSupport,
                        attachmentPromotionService);
        when(handlerRegistry.require(OnboardingStepType.ENTRY_DOCUMENTS)).thenReturn(entryHandler);
    }

    private void stubInvitationAndOrg() {
        when(invitationRepository.findByIdAndUserId(3L, 20L)).thenReturn(Optional.of(invitation()));
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).name("Alpha Wehr").build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        Standard standard = Standard.builder().id(5L).tenantId(7L).build();
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(standardOnboardingStepService.listSteps(standard))
                .thenReturn(
                        List.of(
                                StandardOnboardingStep.builder()
                                        .tenantId(7L)
                                        .standardId(5L)
                                        .stepType(OnboardingStepType.EMERGENCY_CONTACTS)
                                        .sortOrder(0)
                                        .config(Map.of("minimumCount", 1))
                                        .build()));
        when(handlerRegistry.require(OnboardingStepType.EMERGENCY_CONTACTS))
                .thenReturn(emergencyHandler);
    }

    private void stubEmptyCatalogs() {
        when(trainingCourseCatalogService.getAvailableTypesForOrg(10L))
                .thenReturn(
                        OrgTrainingCourseCatalogDto.builder()
                                .organizationId(10L)
                                .types(List.of())
                                .build());
        when(certificationCatalogService.getCatalog(10L))
                .thenReturn(
                        OrgCertificationCatalogDto.builder()
                                .organizationId(10L)
                                .definitions(List.of())
                                .build());
        when(drivingLicenseCatalogService.getAvailableDefinitionsForOrg(10L))
                .thenReturn(
                        OrgDrivingLicenseCatalogDto.builder()
                                .organizationId(10L)
                                .definitions(List.of())
                                .build());
    }

    private static OnboardingInvitation invitation() {
        return OnboardingInvitation.builder()
                .id(3L)
                .tenantId(7L)
                .userId(20L)
                .targetOrganizationId(10L)
                .status(OnboardingInvitationStatus.OPEN)
                .invitedAt(Instant.parse("2026-08-31T10:00:00Z"))
                .build();
    }
}
