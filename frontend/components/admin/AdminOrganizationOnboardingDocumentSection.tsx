/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useState } from 'react';
import { AdminOrganizationOnboardingDocumentManagement } from '@/components/admin/AdminOrganizationOnboardingDocumentManagement';
import { standardsApi } from '@/lib/api/standards';

interface AdminOrganizationOnboardingDocumentSectionProps {
  tenantId: number;
  organizationId: number;
}

export function AdminOrganizationOnboardingDocumentSection({
  tenantId,
  organizationId,
}: AdminOrganizationOnboardingDocumentSectionProps) {
  const [stepEnabled, setStepEnabled] = useState<boolean | null>(null);

  const loadStepEnabled = useCallback(async () => {
    try {
      const orgStandard = await standardsApi.getOrganizationStandard(tenantId, organizationId);
      const effectiveStandardId = orgStandard.effectiveStandardId;
      if (effectiveStandardId == null) {
        setStepEnabled(false);
        return;
      }
      const stepsResponse = await standardsApi.getOnboardingSteps(tenantId, effectiveStandardId);
      setStepEnabled(
        stepsResponse.steps.some((step) => step.stepType === 'ORGANIZATION_ENTRY_DOCUMENTS'),
      );
    } catch {
      setStepEnabled(false);
    }
  }, [tenantId, organizationId]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadStepEnabled();
    });
  }, [loadStepEnabled]);

  if (stepEnabled !== true) {
    return null;
  }

  return (
    <AdminOrganizationOnboardingDocumentManagement
      tenantId={tenantId}
      organizationId={organizationId}
    />
  );
}
