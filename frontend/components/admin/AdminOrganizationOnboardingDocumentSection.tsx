/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { AdminOrganizationOnboardingDocumentManagement } from '@/components/admin/AdminOrganizationOnboardingDocumentManagement';
import { useAdminOrganizationStandard, useStandardOnboardingSteps } from '@/lib/store';

interface AdminOrganizationOnboardingDocumentSectionProps {
  tenantId: number;
  organizationId: number;
}

export function AdminOrganizationOnboardingDocumentSection({
  tenantId,
  organizationId,
}: AdminOrganizationOnboardingDocumentSectionProps) {
  const orgStandard = useAdminOrganizationStandard(tenantId, organizationId);
  const effectiveStandardId = orgStandard.data?.effectiveStandardId ?? null;
  const steps = useStandardOnboardingSteps(tenantId, effectiveStandardId);

  const stepEnabled =
    steps.data?.steps.some((step) => step.stepType === 'ORGANIZATION_ENTRY_DOCUMENTS') ?? false;

  if (!stepEnabled) {
    return null;
  }

  return (
    <AdminOrganizationOnboardingDocumentManagement
      tenantId={tenantId}
      organizationId={organizationId}
    />
  );
}
