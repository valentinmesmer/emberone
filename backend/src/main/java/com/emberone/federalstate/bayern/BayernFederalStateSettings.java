/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.bayern;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.federalstate.FederalStateSettings;
import com.emberone.federalstate.baseline.BaselineValidator;
import com.emberone.federalstate.baseline.FederalStateBaselines;
import com.emberone.federalstate.baseline.OrganizationSetupBaseline;
import com.emberone.federalstate.baseline.TenantBaseline;
import com.emberone.federalstate.domain.FederalState;
import com.emberone.platform.i18n.LocalizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BayernFederalStateSettings implements FederalStateSettings {

    private final FederalStateBaselines baselines;

    public BayernFederalStateSettings(
            BaselineValidator baselineValidator, LocalizationService localizationService) {
        SupportedLocale locale = SupportedLocale.DE;
        TenantBaseline tenant =
                BayernTenantBaseline.build(tenantLabels(localizationService, locale));
        OrganizationSetupBaseline organizationSetup =
                BayernOrganizationSetupBaseline.build(
                        organizationSetupLabels(localizationService, locale));
        this.baselines =
                new FederalStateBaselines(
                        tenant, organizationSetup, BayernRetentionProfile.PROFILE);
        baselineValidator.validate(this.baselines);
        log.info(
                "Loaded Bayern federal state settings ({} certification definitions, {} training course types, {} driving license definitions, {} qualification definitions)",
                tenant.certificationDefinitions().size(),
                tenant.trainingCourseTypes().size(),
                tenant.drivingLicenseDefinitions().size(),
                tenant.qualificationDefinitions().size());
    }

    private static BayernTenantBaseline.Labels tenantLabels(
            LocalizationService localizationService, SupportedLocale locale) {
        return new BayernTenantBaseline.Labels(
                localizationService.getMessage(
                        BayernTenantBaseline.CERT_DEF_G_26_3_NAME_KEY, locale),
                localizationService.getMessage(
                        BayernTenantBaseline.CERT_DEF_RESPIRATORY_LOAD_EXERCISE_NAME_KEY, locale),
                localizationService.getMessage(
                        BayernTenantBaseline.CERT_DEF_RESPIRATORY_INSTRUCTION_NAME_KEY, locale),
                localizationService.getMessage(
                        BayernTenantBaseline.CERT_DEF_RESPIRATORY_DRILL_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.CERT_DEF_FSK_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.TC_BASIS_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.TC_MTA_ZP_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.TC_MTA_AP_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.TC_TF_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.TC_FROM2_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.TC_CSA_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.TC_MA_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.TC_GF_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.TC_SAEGE_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.TC_AGT_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.DL_B_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.DL_BE_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.DL_C1_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.DL_C1E_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.DL_C_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.DL_CE_NAME_KEY, locale),
                localizationService.getMessage(
                        BayernTenantBaseline.QUAL_OPERATIONAL_DUTY_NAME_KEY, locale),
                localizationService.getMessage(
                        BayernTenantBaseline.QUAL_ATEMSCHUTZ_NAME_KEY, locale),
                localizationService.getMessage(
                        BayernTenantBaseline.QUAL_SPRITZSCHUTZ_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.QUAL_CSA_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.QUAL_TF_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.QUAL_GF_NAME_KEY, locale),
                localizationService.getMessage(
                        BayernTenantBaseline.QUAL_MASCHINIST_NAME_KEY, locale),
                localizationService.getMessage(
                        BayernTenantBaseline.ORG_TYPE_DEPARTMENT_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.RANK_FWAN_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.RANK_FWM_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.RANK_OFWM_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.RANK_HFWM_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.RANK_LM_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.RANK_OLM_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.RANK_HLM_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.RANK_BM_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.RANK_OBM_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.RANK_HBM_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.FUNC_KDT_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.FUNC_STVKDT_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.FUNC_KBM_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.FUNC_KBI_NAME_KEY, locale),
                localizationService.getMessage(BayernTenantBaseline.FUNC_KBR_NAME_KEY, locale));
    }

    private static BayernOrganizationSetupBaseline.Labels organizationSetupLabels(
            LocalizationService localizationService, SupportedLocale locale) {
        return new BayernOrganizationSetupBaseline.Labels(
                localizationService.getMessage(
                        BayernOrganizationSetupBaseline.ORG_GROUP_LEADERSHIP_TEAM_NAME_KEY, locale),
                localizationService.getMessage(
                        BayernOrganizationSetupBaseline.ORG_GROUP_HR_NAME_KEY, locale),
                localizationService.getMessage(
                        BayernOrganizationSetupBaseline.PERM_GROUP_POWERUSER_NAME_KEY, locale));
    }

    @Override
    public FederalState federalState() {
        return FederalState.BAYERN;
    }

    @Override
    public FederalStateBaselines baselines() {
        return baselines;
    }
}
