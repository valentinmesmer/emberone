/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.service;

import com.emberone.admin.tenant.domain.Tenant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Batched tenant row deletes to avoid one large CASCADE transaction. */
@Service
@RequiredArgsConstructor
public class AdminTenantPurgeTransactionService {

    private static final List<String> TABLES_IN_DELETE_ORDER =
            List.of(
                    "action_log",
                    "appointment_reads",
                    "user_ranks",
                    "std_function_grants",
                    "std_function_selections",
                    "std_rank_selections",
                    "tenant_functions",
                    "tenant_ranks",
                    "qual_user_org_decisions",
                    "std_qualification_selections",
                    "tenant_qualification_driving_licenses",
                    "tenant_qualification_cert_definitions",
                    "tenant_qualification_training_courses",
                    "tenant_qualification_definitions",
                    "driving_license_documents",
                    "driving_license_classes",
                    "driving_licenses",
                    "std_driving_license_selections",
                    "tenant_driving_license_definitions",
                    "usr_entry_document_fulfillment_events",
                    "usr_entry_document_fulfillments",
                    "onb_invitation_step_data",
                    "onb_invitations",
                    "std_onboarding_steps",
                    "onb_org_document_definition_template_versions",
                    "onb_org_document_definitions",
                    "onb_document_definition_template_versions",
                    "onb_document_definitions",
                    "training_course_user_submission_documents",
                    "training_course_user_submission_types",
                    "training_course_user_submissions",
                    "std_training_course_type_selections",
                    "tenant_training_course_types",
                    "cert_event_grant_recipients",
                    "cert_event_grants",
                    "cert_user_record_documents",
                    "cert_user_records",
                    "cert_user_assignments",
                    "std_cert_definition_selections",
                    "tenant_workflow_definition_static_orgs",
                    "tenant_workflow_definitions",
                    "tenant_cert_definition_evidence_types",
                    "tenant_cert_definitions",
                    "std_workflow_definition_template_static_orgs",
                    "std_workflow_definition_templates",
                    "std_workflow_type_mapping_templates",
                    "std_org_group_permission_group_templates",
                    "std_permission_group_template_grants",
                    "std_permission_group_templates",
                    "std_org_group_templates",
                    "std_standard_modules",
                    "std_assignments",
                    "std_standards",
                    "todo_excluded_users",
                    "todo_assignee_groups",
                    "todo_items",
                    "ntf_user_channel_overrides",
                    "ntf_deliveries",
                    "ntf_notifications",
                    "retention_runs",
                    // obj_pending_deletions: kept until S3 outbox runs (tenant_id → NULL after
                    // purge)
                    "wf_definition_static_orgs",
                    "wf_definitions",
                    "wf_type_mappings",
                    "wf_input_changes",
                    "wf_comments",
                    "wf_decisions",
                    "wf_participants",
                    "wf_exclusivity_locks",
                    "wf_instances",
                    "idm_users",
                    "doc_document_links",
                    "doc_document_tags",
                    "doc_documents",
                    "doc_entity_attachments",
                    "doc_user_profile_images",
                    "perm_group_mappings",
                    "perm_group_permission_types",
                    "perm_groups",
                    "user_time_entries",
                    "user_absence_organizations",
                    "user_absences",
                    "appointment_series_audience_groups",
                    "appointment_responses",
                    "appointment_audience_groups",
                    "appointments",
                    "appointment_series",
                    "user_emergency_contacts",
                    "event_attendances",
                    "operation_events",
                    "service_events",
                    "user_contact_information",
                    "user_organization_group_memberships",
                    "user_organization_memberships",
                    "organization_groups",
                    "users",
                    "organizations",
                    "organization_type_definitions",
                    "tenant_personal_number_sequences");

    private final AdminTenantPurgeBatchService purgeBatchService;

    public void purgeTenantData(long tenantId) {
        for (String table : TABLES_IN_DELETE_ORDER) {
            int deleted;
            do {
                deleted = purgeBatchService.deleteBatch(tenantId, table);
            } while (deleted > 0);
        }
    }

    public void deleteTenant(Tenant tenant) {
        purgeBatchService.deleteTenant(tenant);
    }
}
