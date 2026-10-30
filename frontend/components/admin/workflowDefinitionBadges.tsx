/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { Badge } from '@/components/ui/badge';
import type { TenantWorkflowDefinitionDto } from '@/types/workflow';

interface WorkflowDefinitionBadgeOptions {
  definition: TenantWorkflowDefinitionDto;
  t: (key: string, params?: Record<string, string | number>) => string;
  variant?: 'tenantDefault' | 'orgEffective';
  hasOrgException?: boolean;
}

export function workflowDefinitionBadges({
  definition,
  t,
  variant = 'tenantDefault',
  hasOrgException,
}: WorkflowDefinitionBadgeOptions) {
  const badges = [];

  if (variant === 'orgEffective' && hasOrgException === false) {
    badges.push(
      <Badge key="tenant-default" tone="neutral">
        {t('admin.workflowDefinitions.badgeTenantDefault')}
      </Badge>,
    );
  }

  if (variant === 'orgEffective' && hasOrgException) {
    badges.push(
      <Badge key="override" tone="warning">
        {t('admin.workflowDefinitions.badgeOrgExceptionActive')}
      </Badge>,
    );
  }

  const staticCount = definition.staticApproverOrganizationIds.length;
  if (staticCount > 0) {
    badges.push(
      <Badge key="static" tone="neutral">
        {t('admin.workflowDefinitions.badgeStaticOrgs', { count: staticCount })}
      </Badge>,
    );
  }

  if (definition.staticApproversExclusive && staticCount > 0) {
    badges.push(
      <Badge key="exclusive" tone="neutral">
        {t('admin.workflowDefinitions.badgeExclusive')}
      </Badge>,
    );
  }

  if (variant === 'tenantDefault' && definition.customized) {
    badges.push(
      <Badge key="customized" tone="warning">
        {t('admin.workflowDefinitions.badgeCustomized')}
      </Badge>,
    );
  }

  return badges;
}
