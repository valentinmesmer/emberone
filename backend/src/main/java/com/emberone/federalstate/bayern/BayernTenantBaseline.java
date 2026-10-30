/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.federalstate.bayern;

import com.emberone.federalstate.baseline.*;
import com.emberone.organizationgroup.domain.GroupMembershipType;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.qualification.domain.QualificationRequirementMode;
import com.emberone.standard.util.StandardConfigKey;
import com.emberone.workflow.domain.WorkflowType;
import java.util.List;

/**
 * Bavaria tenant-wide catalog baseline (certifications, training courses, qualifications,
 * workflows, org types).
 */
public final class BayernTenantBaseline {

    public static final String CERT_DEF_G_26_3_NAME_KEY = "certification.definition.g263";
    public static final String CERT_DEF_RESPIRATORY_LOAD_EXERCISE_NAME_KEY =
            "certification.definition.respiratoryLoadExercise";
    public static final String CERT_DEF_RESPIRATORY_INSTRUCTION_NAME_KEY =
            "certification.definition.respiratoryInstruction";
    public static final String CERT_DEF_RESPIRATORY_DRILL_NAME_KEY =
            "certification.definition.respiratoryDrill";
    public static final String CERT_DEF_FSK_NAME_KEY = "certification.definition.fsk";

    public static final String CERT_DEF_G_26_3 = "G26.3";
    public static final String CERT_DEF_RESPIRATORY_LOAD_EXERCISE = "ABÜ";
    public static final String CERT_DEF_RESPIRATORY_INSTRUCTION = "AU";
    public static final String CERT_DEF_RESPIRATORY_DRILL = "AÜE";
    public static final String CERT_DEF_FSK = "FSK";

    public static final String TC_BASIS_NAME_KEY = "trainingCourse.type.basis";
    public static final String TC_MTA_ZP_NAME_KEY = "trainingCourse.type.mtaZp";
    public static final String TC_MTA_AP_NAME_KEY = "trainingCourse.type.mtaAp";
    public static final String TC_TF_NAME_KEY = "trainingCourse.type.tf";
    public static final String TC_FROM2_NAME_KEY = "trainingCourse.type.from2";
    public static final String TC_CSA_NAME_KEY = "trainingCourse.type.csa";
    public static final String TC_MA_NAME_KEY = "trainingCourse.type.ma";
    public static final String TC_GF_NAME_KEY = "trainingCourse.type.gf";
    public static final String TC_SAEGE_NAME_KEY = "trainingCourse.type.saege";
    public static final String TC_AGT_NAME_KEY = "trainingCourse.type.agt";

    public static final String DL_B_NAME_KEY = "drivingLicense.definition.b";
    public static final String DL_BE_NAME_KEY = "drivingLicense.definition.be";
    public static final String DL_C1_NAME_KEY = "drivingLicense.definition.c1";
    public static final String DL_C1E_NAME_KEY = "drivingLicense.definition.c1e";
    public static final String DL_C_NAME_KEY = "drivingLicense.definition.c";
    public static final String DL_CE_NAME_KEY = "drivingLicense.definition.ce";

    public static final String QUAL_OPERATIONAL_DUTY_NAME_KEY =
            "qualification.definition.operational-duty";
    public static final String QUAL_ATEMSCHUTZ_NAME_KEY = "qualification.definition.atemschutz";
    public static final String QUAL_SPRITZSCHUTZ_NAME_KEY = "qualification.definition.spritzschutz";
    public static final String QUAL_CSA_NAME_KEY = "qualification.definition.csa";
    public static final String QUAL_TF_NAME_KEY = "qualification.definition.tf";
    public static final String QUAL_GF_NAME_KEY = "qualification.definition.gf";
    public static final String QUAL_MASCHINIST_NAME_KEY = "qualification.definition.maschinist";

    public static final String ORG_TYPE_DEPARTMENT_NAME_KEY = "organization.type.department";

    public static final String RANK_FWAN_NAME_KEY = "rank.fwan";
    public static final String RANK_FWM_NAME_KEY = "rank.fwm";
    public static final String RANK_OFWM_NAME_KEY = "rank.ofwm";
    public static final String RANK_HFWM_NAME_KEY = "rank.hfwm";
    public static final String RANK_LM_NAME_KEY = "rank.lm";
    public static final String RANK_OLM_NAME_KEY = "rank.olm";
    public static final String RANK_HLM_NAME_KEY = "rank.hlm";
    public static final String RANK_BM_NAME_KEY = "rank.bm";
    public static final String RANK_OBM_NAME_KEY = "rank.obm";
    public static final String RANK_HBM_NAME_KEY = "rank.hbm";

    public static final String FUNC_KDT_NAME_KEY = "dutyFunction.kdt";
    public static final String FUNC_STVKDT_NAME_KEY = "dutyFunction.stvkdt";
    public static final String FUNC_KBM_NAME_KEY = "dutyFunction.kbm";
    public static final String FUNC_KBI_NAME_KEY = "dutyFunction.kbi";
    public static final String FUNC_KBR_NAME_KEY = "dutyFunction.kbr";

    public static final String RANK_FA = "FA";
    public static final String RANK_FM = "FM";
    public static final String RANK_OFM = "OFM";
    public static final String RANK_HFM = "HFM";
    public static final String RANK_LM = "LM";
    public static final String RANK_OLM = "OLM";
    public static final String RANK_HLM = "HLM";
    public static final String RANK_BM = "BM";
    public static final String RANK_OBM = "OBM";
    public static final String RANK_HBM = "HBM";

    public static final String FUNC_KDT = "KDT";
    public static final String FUNC_STVKDT = "STVKDT";
    public static final String FUNC_KBM = "KBM";
    public static final String FUNC_KBI = "KBI";
    public static final String FUNC_KBR = "KBR";

    public static final String TC_BASIS = "BASIS";
    public static final String TC_MTA_ZP = "MTA ZP";
    public static final String TC_MTA_AP = "MTA AP";
    public static final String TC_TF = "TF";
    public static final String TC_FROM2 = "FROM2";
    public static final String TC_CSA = "CSA";
    public static final String TC_MA = "MA";
    public static final String TC_GF = "GF";
    public static final String TC_SAEGE = "SAEGE";
    public static final String TC_AGT = "AGT";

    public static final String DL_B = "B";
    public static final String DL_BE = "BE";
    public static final String DL_C1 = "C1";
    public static final String DL_C1E = "C1E";
    public static final String DL_C = "C";
    public static final String DL_CE = "CE";

    public static final String QUAL_OPERATIONAL_DUTY = "BASIS";
    public static final String QUAL_ATEMSCHUTZ = "AGT";
    public static final String QUAL_SPRITZSCHUTZ = "FROM2";
    public static final String QUAL_CSA = "CSA";
    public static final String QUAL_TF = "TF";
    public static final String QUAL_GF = "GF";
    public static final String QUAL_MASCHINIST = "MA";

    private BayernTenantBaseline() {}

    public static TenantBaseline build(Labels labels) {
        return new TenantBaseline(
                certificationDefinitions(labels),
                trainingCourseTypes(labels),
                drivingLicenseDefinitions(labels),
                qualificationDefinitions(labels),
                workflowDefinitions(),
                organizationTypeDefinitions(labels),
                onboardingDocumentDefinitions(),
                ranks(labels),
                functions(labels),
                functionGrants(),
                List.of());
    }

    private static List<CertificationDefinitionBaseline> certificationDefinitions(Labels labels) {
        return List.of(
                certificationDefinition(labels.certDefG263(), CERT_DEF_G_26_3, 1095, 90),
                certificationDefinition(
                        labels.certDefRespiratoryLoadExercise(),
                        CERT_DEF_RESPIRATORY_LOAD_EXERCISE,
                        365,
                        30),
                certificationDefinition(
                        labels.certDefRespiratoryInstruction(),
                        CERT_DEF_RESPIRATORY_INSTRUCTION,
                        365,
                        30),
                certificationDefinition(
                        labels.certDefRespiratoryDrill(), CERT_DEF_RESPIRATORY_DRILL, 365, 30),
                certificationDefinition(labels.certDefFsk(), CERT_DEF_FSK, 365, 30));
    }

    private static List<DrivingLicenseDefinitionBaseline> drivingLicenseDefinitions(Labels labels) {
        return List.of(
                drivingLicenseDefinition(labels.dlB(), DL_B),
                drivingLicenseDefinition(labels.dlBe(), DL_BE),
                drivingLicenseDefinition(labels.dlC1(), DL_C1),
                drivingLicenseDefinition(labels.dlC1e(), DL_C1E),
                drivingLicenseDefinition(labels.dlC(), DL_C),
                drivingLicenseDefinition(labels.dlCe(), DL_CE));
    }

    private static List<TrainingCourseTypeBaseline> trainingCourseTypes(Labels labels) {
        return List.of(
                trainingCourseType(labels.tcBasis(), TC_BASIS),
                trainingCourseType(labels.tcMtaZp(), TC_MTA_ZP),
                trainingCourseType(labels.tcMtaAp(), TC_MTA_AP),
                trainingCourseType(labels.tcTf(), TC_TF),
                trainingCourseType(labels.tcFrom2(), TC_FROM2),
                trainingCourseType(labels.tcCsa(), TC_CSA),
                trainingCourseType(labels.tcMa(), TC_MA),
                trainingCourseType(labels.tcGf(), TC_GF),
                trainingCourseType(labels.tcSaege(), TC_SAEGE),
                trainingCourseType(labels.tcAgt(), TC_AGT));
    }

    private static List<QualificationDefinitionBaseline> qualificationDefinitions(Labels labels) {
        return List.of(
                qualificationDefinition(
                        labels.qualOperationalDuty(),
                        QUAL_OPERATIONAL_DUTY,
                        false,
                        QualificationRequirementMode.ANY,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        List.of(TC_BASIS, TC_MTA_ZP, TC_MTA_AP),
                        List.of(),
                        List.of()),
                qualificationDefinition(
                        labels.qualAtemschutz(),
                        QUAL_ATEMSCHUTZ,
                        false,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        List.of(TC_AGT),
                        List.of(
                                CERT_DEF_G_26_3,
                                CERT_DEF_RESPIRATORY_LOAD_EXERCISE,
                                CERT_DEF_RESPIRATORY_INSTRUCTION,
                                CERT_DEF_RESPIRATORY_DRILL),
                        List.of()),
                qualificationDefinition(
                        labels.qualSpritzschutz(),
                        QUAL_SPRITZSCHUTZ,
                        false,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        List.of(TC_FROM2),
                        List.of(CERT_DEF_G_26_3),
                        List.of()),
                qualificationDefinition(
                        labels.qualCsa(),
                        QUAL_CSA,
                        false,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        List.of(TC_CSA),
                        List.of(CERT_DEF_G_26_3),
                        List.of()),
                qualificationDefinition(
                        labels.qualTf(),
                        QUAL_TF,
                        true,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        List.of(TC_TF),
                        List.of(),
                        List.of()),
                qualificationDefinition(
                        labels.qualGf(),
                        QUAL_GF,
                        true,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        List.of(TC_GF),
                        List.of(CERT_DEF_G_26_3),
                        List.of()),
                qualificationDefinition(
                        labels.qualMaschinist(),
                        QUAL_MASCHINIST,
                        true,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        QualificationRequirementMode.ALL,
                        List.of(TC_MA),
                        List.of(CERT_DEF_FSK),
                        List.of(DL_C)));
    }

    private static List<OnboardingDocumentDefinitionBaseline> onboardingDocumentDefinitions() {
        return List.of();
    }

    private static QualificationDefinitionBaseline qualificationDefinition(
            String name,
            String abbreviation,
            boolean requiresOrgDecision,
            QualificationRequirementMode trainingCourseRequirementMode,
            QualificationRequirementMode certRequirementMode,
            QualificationRequirementMode drivingLicenseRequirementMode,
            List<String> trainingCourseConfigKeys,
            List<String> certDefinitionConfigKeys,
            List<String> drivingLicenseDefinitionConfigKeys) {
        return new QualificationDefinitionBaseline(
                abbreviation,
                name,
                null,
                requiresOrgDecision,
                trainingCourseRequirementMode,
                certRequirementMode,
                drivingLicenseRequirementMode,
                trainingCourseConfigKeys,
                certDefinitionConfigKeys,
                drivingLicenseDefinitionConfigKeys,
                true);
    }

    private static TrainingCourseTypeBaseline trainingCourseType(String name, String abbreviation) {
        return new TrainingCourseTypeBaseline(abbreviation, name, true);
    }

    private static DrivingLicenseDefinitionBaseline drivingLicenseDefinition(
            String name, String abbreviation) {
        return new DrivingLicenseDefinitionBaseline(abbreviation, name, true);
    }

    private static List<WorkflowDefinitionBaseline> workflowDefinitions() {
        return List.of(
                workflowDefinition(WorkflowType.NAME_CHANGE),
                workflowDefinition(WorkflowType.ADDRESS_CHANGE),
                workflowDefinition(WorkflowType.CERTIFICATION_RECORD),
                workflowDefinition(WorkflowType.TRAINING_COURSE_RECORD),
                workflowDefinition(WorkflowType.DRIVING_LICENSE_RECORD),
                workflowDefinition(WorkflowType.ORGANIZATION_JOIN_REQUEST),
                workflowDefinition(WorkflowType.MAIN_ORGANIZATION_CHANGE),
                workflowDefinition(WorkflowType.ORGANIZATION_LEAVE_REQUEST),
                workflowDefinition(WorkflowType.ORGANIZATION_GROUP_LEAVE_REQUEST),
                workflowDefinition(WorkflowType.ORGANIZATION_GROUP_JOIN_REQUEST),
                workflowDefinition(WorkflowType.ONBOARDING_SUBMISSION),
                workflowDefinition(WorkflowType.ORGANIZATION_MEMBERSHIP_END_REQUEST),
                workflowDefinition(WorkflowType.ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE),
                workflowDefinition(WorkflowType.USER_DOCUMENT_SUBMISSION),
                workflowDefinition(WorkflowType.RANK_REQUEST),
                workflowDefinition(WorkflowType.COURSE_OFFERING_REGISTRATION),
                workflowDefinition(WorkflowType.COURSE_REGISTRATION_CANCELLATION));
    }

    private static List<RankBaseline> ranks(Labels labels) {
        return List.of(
                rank(labels.rankFwan(), RANK_FA, RANK_FA, 0),
                rank(labels.rankFwm(), RANK_FM, RANK_FM, 1),
                rank(labels.rankOfwm(), RANK_OFM, RANK_OFM, 2),
                rank(labels.rankHfwm(), RANK_HFM, RANK_HFM, 3),
                rank(labels.rankLm(), RANK_LM, RANK_LM, 4),
                rank(labels.rankOlm(), RANK_OLM, RANK_OLM, 5),
                rank(labels.rankHlm(), RANK_HLM, RANK_HLM, 6),
                rank(labels.rankBm(), RANK_BM, RANK_BM, 7),
                rank(labels.rankObm(), RANK_OBM, RANK_OBM, 8),
                rank(labels.rankHbm(), RANK_HBM, RANK_HBM, 9));
    }

    private static List<FunctionBaseline> functions(Labels labels) {
        return List.of(
                function(labels.funcKdt(), FUNC_KDT, FUNC_KDT, 0),
                function(labels.funcStvkdt(), FUNC_STVKDT, FUNC_STVKDT, 1),
                function(labels.funcKbm(), FUNC_KBM, FUNC_KBM, 2),
                function(labels.funcKbi(), FUNC_KBI, FUNC_KBI, 3),
                function(labels.funcKbr(), FUNC_KBR, FUNC_KBR, 4));
    }

    private static List<FunctionGrantBaseline> functionGrants() {
        return List.of(
                new FunctionGrantBaseline(
                        OrganizationGroupType.LEADERSHIP.name(),
                        GroupMembershipType.LEADER,
                        FUNC_KDT),
                new FunctionGrantBaseline(
                        OrganizationGroupType.LEADERSHIP.name(),
                        GroupMembershipType.DEPUTY_LEADER,
                        FUNC_STVKDT));
    }

    private static RankBaseline rank(
            String name, String configKey, String abbreviation, int sortOrder) {
        return new RankBaseline(configKey, name, abbreviation, sortOrder);
    }

    private static FunctionBaseline function(
            String name, String configKey, String abbreviation, int sortOrder) {
        return new FunctionBaseline(configKey, name, abbreviation, sortOrder);
    }

    private static List<TenantBaseline.OrganizationTypeDefinitionBaseline>
            organizationTypeDefinitions(Labels labels) {
        return List.of(
                new TenantBaseline.OrganizationTypeDefinitionBaseline(
                        configKey(ORG_TYPE_DEPARTMENT_NAME_KEY), labels.orgTypeDepartment()));
    }

    private static CertificationDefinitionBaseline certificationDefinition(
            String name, String abbreviation, int intervalDays, int reminderDaysBeforeExpiry) {
        return new CertificationDefinitionBaseline(
                abbreviation, name, null, intervalDays, reminderDaysBeforeExpiry, true);
    }

    private static WorkflowDefinitionBaseline workflowDefinition(WorkflowType workflowType) {
        return new WorkflowDefinitionBaseline(
                String.valueOf(workflowType.getId()), workflowType.getId(), false);
    }

    private static String configKey(String nameKey) {
        return StandardConfigKey.customFromMessageKey(nameKey);
    }

    public record Labels(
            String certDefG263,
            String certDefRespiratoryLoadExercise,
            String certDefRespiratoryInstruction,
            String certDefRespiratoryDrill,
            String certDefFsk,
            String tcBasis,
            String tcMtaZp,
            String tcMtaAp,
            String tcTf,
            String tcFrom2,
            String tcCsa,
            String tcMa,
            String tcGf,
            String tcSaege,
            String tcAgt,
            String dlB,
            String dlBe,
            String dlC1,
            String dlC1e,
            String dlC,
            String dlCe,
            String qualOperationalDuty,
            String qualAtemschutz,
            String qualSpritzschutz,
            String qualCsa,
            String qualTf,
            String qualGf,
            String qualMaschinist,
            String orgTypeDepartment,
            String rankFwan,
            String rankFwm,
            String rankOfwm,
            String rankHfwm,
            String rankLm,
            String rankOlm,
            String rankHlm,
            String rankBm,
            String rankObm,
            String rankHbm,
            String funcKdt,
            String funcStvkdt,
            String funcKbm,
            String funcKbi,
            String funcKbr) {}
}
