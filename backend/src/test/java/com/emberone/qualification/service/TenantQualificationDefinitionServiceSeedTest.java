/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinition;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.federalstate.baseline.QualificationDefinitionBaseline;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.qualification.domain.QualificationRequirementMode;
import com.emberone.qualification.event.QualificationDefinitionChangedEvent;
import com.emberone.qualification.repository.UserQualificationOrgDecisionRepository;
import com.emberone.qualification.standard.QualificationSelectionRepository;
import com.emberone.qualification.tenant.TenantQualificationCertDefinition;
import com.emberone.qualification.tenant.TenantQualificationDefinition;
import com.emberone.qualification.tenant.TenantQualificationDefinitionRepository;
import com.emberone.qualification.tenant.TenantQualificationDrivingLicense;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TenantQualificationDefinitionServiceSeedTest {

    private static final long TENANT_ID = 7L;
    private static final String QUAL_KEY = "MASCH";
    private static final String CERT_FSK = "FSK";
    private static final String DL_C = "C";

    @Mock TenantQualificationDefinitionRepository definitionRepository;
    @Mock QualificationSelectionRepository selectionRepository;
    @Mock UserQualificationOrgDecisionRepository orgDecisionRepository;
    @Mock TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    @Mock TenantDrivingLicenseDefinitionRepository drivingLicenseDefinitionRepository;
    @Mock TenantCertDefinitionRepository certDefinitionRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    private TenantQualificationDefinitionService service;

    private static TenantQualificationDefinition maschinist(
            List<String> certKeys, List<String> drivingLicenseKeys) {
        TenantQualificationDefinition definition =
                TenantQualificationDefinition.builder()
                        .id(11L)
                        .tenantId(TENANT_ID)
                        .configKey(QUAL_KEY)
                        .name("Maschinist")
                        .requiresOrgDecision(true)
                        .trainingCourseRequirementMode(QualificationRequirementMode.ALL)
                        .certRequirementMode(QualificationRequirementMode.ALL)
                        .drivingLicenseRequirementMode(QualificationRequirementMode.ALL)
                        .active(true)
                        .trainingCourses(new ArrayList<>())
                        .certDefinitions(new ArrayList<>())
                        .drivingLicenses(new ArrayList<>())
                        .build();
        for (String key : certKeys) {
            TenantQualificationCertDefinition link =
                    TenantQualificationCertDefinition.builder()
                            .tenantId(TENANT_ID)
                            .qualificationDefinition(definition)
                            .certDefinitionConfigKey(key)
                            .build();
            definition.getCertDefinitions().add(link);
        }
        for (String key : drivingLicenseKeys) {
            TenantQualificationDrivingLicense link =
                    TenantQualificationDrivingLicense.builder()
                            .tenantId(TENANT_ID)
                            .qualificationDefinition(definition)
                            .drivingLicenseConfigKey(key)
                            .build();
            definition.getDrivingLicenses().add(link);
        }
        return definition;
    }

    private static QualificationDefinitionBaseline maschinistBaseline() {
        return new QualificationDefinitionBaseline(
                QUAL_KEY,
                "Maschinist",
                null,
                true,
                QualificationRequirementMode.ALL,
                QualificationRequirementMode.ALL,
                QualificationRequirementMode.ALL,
                List.of(),
                List.of(CERT_FSK),
                List.of(DL_C),
                true);
    }

    private static TenantDrivingLicenseDefinition drivingLicense(String configKey) {
        return TenantDrivingLicenseDefinition.builder()
                .id(5L)
                .tenantId(TENANT_ID)
                .configKey(configKey)
                .name("Klasse " + configKey)
                .active(true)
                .archived(false)
                .build();
    }

    private static TenantCertDefinition cert(String configKey) {
        return TenantCertDefinition.builder()
                .id(9L)
                .tenantId(TENANT_ID)
                .configKey(configKey)
                .name("Führerschein-Kontrolle C")
                .active(true)
                .archived(false)
                .build();
    }

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        service =
                new TenantQualificationDefinitionService(
                        definitionRepository,
                        selectionRepository,
                        orgDecisionRepository,
                        trainingCourseTypeRepository,
                        drivingLicenseDefinitionRepository,
                        certDefinitionRepository,
                        eventPublisher);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void seedAddsMissingDrivingLicenseRequirementToExistingDefinition() {
        TenantQualificationDefinition existing = maschinist(List.of(CERT_FSK), List.of());

        when(definitionRepository.findByConfigKey(QUAL_KEY)).thenReturn(Optional.of(existing));
        when(drivingLicenseDefinitionRepository.findByConfigKey(DL_C))
                .thenReturn(Optional.of(drivingLicense(DL_C)));
        when(definitionRepository.save(existing)).thenReturn(existing);

        service.seedBaselineDefinitionsIfMissing(TENANT_ID, List.of(maschinistBaseline()));

        assertThat(existing.getDrivingLicenses())
                .extracting(TenantQualificationDrivingLicense::getDrivingLicenseConfigKey)
                .containsExactly(DL_C);
        assertThat(existing.getCertDefinitions())
                .extracting(TenantQualificationCertDefinition::getCertDefinitionConfigKey)
                .containsExactly(CERT_FSK);
        verify(definitionRepository).save(existing);
        verify(eventPublisher)
                .publishEvent(new QualificationDefinitionChangedEvent(TENANT_ID, 11L));
    }

    @Test
    void seedKeepsTenantAddedRequirementsAndAddsMissingBaselineKeys() {
        TenantQualificationDefinition existing = maschinist(List.of("EXTRA"), List.of(DL_C));

        when(definitionRepository.findByConfigKey(QUAL_KEY)).thenReturn(Optional.of(existing));
        when(certDefinitionRepository.findByConfigKey(CERT_FSK))
                .thenReturn(Optional.of(cert(CERT_FSK)));
        when(definitionRepository.save(existing)).thenReturn(existing);

        service.seedBaselineDefinitionsIfMissing(TENANT_ID, List.of(maschinistBaseline()));

        assertThat(existing.getCertDefinitions())
                .extracting(TenantQualificationCertDefinition::getCertDefinitionConfigKey)
                .containsExactlyInAnyOrder("EXTRA", CERT_FSK);
        assertThat(existing.getDrivingLicenses())
                .extracting(TenantQualificationDrivingLicense::getDrivingLicenseConfigKey)
                .containsExactly(DL_C);
        verify(definitionRepository).save(existing);
    }

    @Test
    void seedSkipsUnchangedExistingDefinition() {
        TenantQualificationDefinition existing = maschinist(List.of(CERT_FSK), List.of(DL_C));

        when(definitionRepository.findByConfigKey(QUAL_KEY)).thenReturn(Optional.of(existing));

        service.seedBaselineDefinitionsIfMissing(TENANT_ID, List.of(maschinistBaseline()));

        verify(definitionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
