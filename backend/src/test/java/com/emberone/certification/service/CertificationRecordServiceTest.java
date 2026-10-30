/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.certification.domain.CertEvidenceType;
import com.emberone.certification.domain.UserCertificationRecord;
import com.emberone.certification.dto.CreateUserCertificationRecordRequest;
import com.emberone.certification.event.CertificationAssignmentChangedEvent;
import com.emberone.certification.event.CertificationEventGrantNavigationResolver;
import com.emberone.certification.repository.UserCertificationRecordDocumentRepository;
import com.emberone.certification.repository.UserCertificationRecordRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.i18n.TenantLocaleService;
import com.emberone.platform.navigation.AppNavigationResolver;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.platform.security.TokenService;
import com.emberone.userdocument.domain.UserDocument;
import com.emberone.userdocument.repository.UserDocumentRepository;
import com.emberone.userdocument.service.UserDocumentService;
import com.emberone.userdocument.service.UserDocumentSystemWriter;
import com.emberone.workflow.service.LocalizedWorkflowDocumentPromotionSupport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CertificationRecordServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 42L;
    private static final Long DEFINITION_ID = 7L;

    @Mock UserCertificationRecordRepository recordRepository;
    @Mock UserCertificationRecordDocumentRepository recordDocumentRepository;
    @Mock TenantCertDefinitionRepository tenantDefinitionRepository;
    @Mock UserDocumentRepository userDocumentRepository;
    @Mock UserDocumentService userDocumentService;
    @Mock TokenService tokenService;
    @Mock CertificationAssignmentService assignmentService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TenantLocaleService tenantLocaleService;
    @Mock LocalizationService localizationService;
    @Mock TenantCertDefinitionEvidenceTypeService evidenceTypeService;
    @Mock AppNavigationResolver appNavigationResolver;
    @Mock CertificationEventGrantNavigationResolver grantNavigationResolver;

    CertificationRecordService service;

    @BeforeEach
    void setUp() {
        LocalizedWorkflowDocumentPromotionSupport documentPromotionMetadataSupport =
                new LocalizedWorkflowDocumentPromotionSupport(
                        tenantLocaleService, localizationService, new UserDocumentSystemWriter());
        service =
                new CertificationRecordService(
                        recordRepository,
                        recordDocumentRepository,
                        tenantDefinitionRepository,
                        userDocumentRepository,
                        userDocumentService,
                        tokenService,
                        assignmentService,
                        eventPublisher,
                        documentPromotionMetadataSupport,
                        evidenceTypeService,
                        appNavigationResolver,
                        grantNavigationResolver);
    }

    @Test
    void createManualRejectsWhenDefinitionNotAssignedToUser() {
        TenantCertDefinition definition =
                TenantCertDefinition.builder()
                        .id(DEFINITION_ID)
                        .tenantId(TENANT_ID)
                        .active(true)
                        .archived(false)
                        .build();
        CreateUserCertificationRecordRequest request = new CreateUserCertificationRecordRequest();
        request.setTenantCertDefinitionId(DEFINITION_ID);
        request.setCompletedDate(LocalDate.of(2026, 7, 2));

        when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        when(tenantDefinitionRepository.findById(DEFINITION_ID))
                .thenReturn(Optional.of(definition));
        when(assignmentService.isDefinitionAssignedToUser(USER_ID, DEFINITION_ID))
                .thenReturn(false);

        assertThatThrownBy(() -> service.createManual(USER_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Certification is not assigned to this user");
    }

    @Test
    void createManualAppliesCertificationMetadataToLinkedDocument() {
        TenantCertDefinition definition =
                TenantCertDefinition.builder()
                        .id(DEFINITION_ID)
                        .tenantId(TENANT_ID)
                        .name("Atemschutz")
                        .active(true)
                        .archived(false)
                        .build();
        UserDocument document =
                UserDocument.builder()
                        .id(100L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .fileName("proof.pdf")
                        .contentType("application/pdf")
                        .sizeBytes(123L)
                        .storageKey("k")
                        .build();
        CreateUserCertificationRecordRequest request = new CreateUserCertificationRecordRequest();
        request.setTenantCertDefinitionId(DEFINITION_ID);
        request.setCompletedDate(LocalDate.of(2026, 7, 2));
        request.setUserDocumentIds(List.of(100L));

        when(tokenService.requireTenantId()).thenReturn(TENANT_ID);
        when(tenantDefinitionRepository.findById(DEFINITION_ID))
                .thenReturn(Optional.of(definition));
        when(tenantDefinitionRepository.findById(DEFINITION_ID))
                .thenReturn(Optional.of(definition));
        when(assignmentService.isDefinitionAssignedToUser(USER_ID, DEFINITION_ID)).thenReturn(true);
        when(evidenceTypeService.getEvidenceTypes(DEFINITION_ID))
                .thenReturn(Set.of(CertEvidenceType.DOCUMENT));
        when(tenantLocaleService.resolveTenantLocale(TENANT_ID)).thenReturn(SupportedLocale.DE);
        when(recordRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            UserCertificationRecord record = invocation.getArgument(0);
                            record.setId(55L);
                            return record;
                        });
        when(userDocumentRepository.findByIdAndUserId(100L, USER_ID))
                .thenReturn(Optional.of(document));
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.title", SupportedLocale.DE))
                .thenReturn("Nachweis {name}");
        when(localizationService.getInterpolatedMessage(
                        "workflow.documentPromotion.certificationRecord.title",
                        Map.of("name", "Atemschutz"),
                        SupportedLocale.DE))
                .thenReturn("Nachweis Atemschutz");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.description",
                        SupportedLocale.DE))
                .thenReturn("Nachweis aus einer Einreichung");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.tag", SupportedLocale.DE))
                .thenReturn("Nachweis");
        when(appNavigationResolver.resolveNavigationPath(null, null)).thenReturn(null);

        service.createManual(USER_ID, request);

        assertThat(document.getTitle()).isEqualTo("Nachweis Atemschutz");
        assertThat(document.getFileName()).isEqualTo("Nachweis Atemschutz.pdf");
        assertThat(document.getDescription()).isEqualTo("Nachweis aus einer Einreichung");
        assertThat(document.getTags()).hasSize(1);
        assertThat(document.getTags().get(0).getTag()).isEqualTo("Nachweis");
        assertThat(document.getLinks()).hasSize(1);
        assertThat(document.getLinks().get(0).getLinkType())
                .isEqualTo(AppLinkType.CERTIFICATION_RECORD);
        assertThat(document.getLinks().get(0).getLinkId()).isEqualTo(DEFINITION_ID);
        org.mockito.Mockito.verify(eventPublisher)
                .publishEvent(new CertificationAssignmentChangedEvent(TENANT_ID, USER_ID));
    }

    @Test
    void createFromOnboardingAppliesMetadataToLinkedDocument() {
        TenantCertDefinition definition =
                TenantCertDefinition.builder()
                        .id(DEFINITION_ID)
                        .tenantId(TENANT_ID)
                        .name("Atemschutz")
                        .active(true)
                        .archived(false)
                        .build();
        UserDocument document =
                UserDocument.builder()
                        .id(100L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .fileName("proof.pdf")
                        .contentType("application/pdf")
                        .sizeBytes(123L)
                        .storageKey("k")
                        .build();

        when(tenantDefinitionRepository.findById(DEFINITION_ID))
                .thenReturn(Optional.of(definition));
        when(tenantLocaleService.resolveTenantLocale(TENANT_ID)).thenReturn(SupportedLocale.DE);
        when(recordRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            UserCertificationRecord record = invocation.getArgument(0);
                            record.setId(55L);
                            return record;
                        });
        when(userDocumentRepository.findByIdAndUserId(100L, USER_ID))
                .thenReturn(Optional.of(document));
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.title", SupportedLocale.DE))
                .thenReturn("Nachweis {name}");
        when(localizationService.getInterpolatedMessage(
                        "workflow.documentPromotion.certificationRecord.title",
                        Map.of("name", "Atemschutz"),
                        SupportedLocale.DE))
                .thenReturn("Nachweis Atemschutz");
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.description",
                        SupportedLocale.DE))
                .thenReturn(null);
        when(localizationService.getMessageOrNull(
                        "workflow.documentPromotion.certificationRecord.tag", SupportedLocale.DE))
                .thenReturn("Nachweis");

        service.createFromOnboarding(
                TENANT_ID, USER_ID, DEFINITION_ID, LocalDate.of(2026, 6, 1), null, null, 9L, 100L);

        assertThat(document.getTitle()).isEqualTo("Nachweis Atemschutz");
        assertThat(document.getFileName()).isEqualTo("Nachweis Atemschutz.pdf");
        assertThat(document.getTags()).hasSize(1);
        assertThat(document.getTags().get(0).getTag()).isEqualTo("Nachweis");
    }

    @Test
    void deleteFromEventGrantPublishesAssignmentChangedEvent() {
        UserCertificationRecord record =
                UserCertificationRecord.builder()
                        .id(55L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .tenantCertDefinitionId(DEFINITION_ID)
                        .completedDate(LocalDate.of(2026, 8, 12))
                        .appLinkType(AppLinkType.CERTIFICATION_EVENT_GRANT)
                        .appLinkId(9L)
                        .build();

        when(recordDocumentRepository.findByRecord_Id(55L)).thenReturn(List.of());

        service.deleteFromEventGrant(record);

        org.mockito.Mockito.verify(userDocumentService).deleteDocumentsByIds(TENANT_ID, List.of());
        org.mockito.Mockito.verify(recordRepository).delete(record);
        org.mockito.Mockito.verify(eventPublisher)
                .publishEvent(new CertificationAssignmentChangedEvent(TENANT_ID, USER_ID));
    }

    @Test
    void createFromWorkflowWritesOneRecordPerDefinitionAndPublishesOnce() {
        TenantCertDefinition first =
                TenantCertDefinition.builder()
                        .id(DEFINITION_ID)
                        .tenantId(TENANT_ID)
                        .active(true)
                        .archived(false)
                        .intervalDays(365)
                        .build();
        TenantCertDefinition second =
                TenantCertDefinition.builder()
                        .id(8L)
                        .tenantId(TENANT_ID)
                        .active(true)
                        .archived(false)
                        .intervalDays(365)
                        .build();
        LocalDate completedDate = LocalDate.of(2026, 1, 15);

        when(tenantDefinitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(first));
        when(tenantDefinitionRepository.findById(8L)).thenReturn(Optional.of(second));
        when(recordRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            UserCertificationRecord record = invocation.getArgument(0);
                            record.setId(record.getTenantCertDefinitionId());
                            return record;
                        });

        List<UserCertificationRecord> created =
                service.createFromWorkflow(
                        TENANT_ID, USER_ID, List.of(DEFINITION_ID, 8L), completedDate, "ok", 50L);

        assertThat(created).hasSize(2);
        assertThat(created)
                .extracting(UserCertificationRecord::getTenantCertDefinitionId)
                .containsExactly(DEFINITION_ID, 8L);
        assertThat(created).allMatch(record -> record.getAppLinkType() == AppLinkType.WORKFLOW);
        assertThat(created).allMatch(record -> Long.valueOf(50L).equals(record.getAppLinkId()));
        org.mockito.Mockito.verify(eventPublisher)
                .publishEvent(new CertificationAssignmentChangedEvent(TENANT_ID, USER_ID));
    }

    @Test
    void linkDocumentsFromWorkflowAttachesToEveryRecordOfTheWorkflow() {
        UserCertificationRecord first =
                UserCertificationRecord.builder()
                        .id(11L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .tenantCertDefinitionId(DEFINITION_ID)
                        .build();
        UserCertificationRecord second =
                UserCertificationRecord.builder()
                        .id(12L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .tenantCertDefinitionId(8L)
                        .build();
        TenantCertDefinition firstDef =
                TenantCertDefinition.builder().id(DEFINITION_ID).tenantId(TENANT_ID).build();
        TenantCertDefinition secondDef =
                TenantCertDefinition.builder().id(8L).tenantId(TENANT_ID).build();
        UserDocument document =
                UserDocument.builder()
                        .id(100L)
                        .tenantId(TENANT_ID)
                        .userId(USER_ID)
                        .fileName("proof.pdf")
                        .build();

        when(recordRepository.findAllByAppLinkTypeAndAppLinkId(AppLinkType.WORKFLOW, 50L))
                .thenReturn(List.of(first, second));
        when(tenantDefinitionRepository.findById(DEFINITION_ID)).thenReturn(Optional.of(firstDef));
        when(tenantDefinitionRepository.findById(8L)).thenReturn(Optional.of(secondDef));
        when(userDocumentRepository.findByIdAndUserId(100L, USER_ID))
                .thenReturn(Optional.of(document));
        when(recordDocumentRepository.existsByRecord_IdAndDocumentId(11L, 100L)).thenReturn(false);
        when(recordDocumentRepository.existsByRecord_IdAndDocumentId(12L, 100L)).thenReturn(false);

        service.linkDocumentsFromWorkflow(TENANT_ID, 50L, List.of(100L));

        org.mockito.Mockito.verify(recordDocumentRepository, org.mockito.Mockito.times(2))
                .save(any());
        assertThat(document.getLinks()).hasSize(2);
        assertThat(document.getLinks())
                .extracting(link -> link.getLinkId())
                .containsExactly(DEFINITION_ID, 8L);
    }
}
