/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useMemo } from 'react';
import {
  buildStandardTitleByKey,
  standardOrgKeyLabel,
  standardWorkflowTypeId,
  standardWorkflowTypeLabel,
} from '@/components/admin/standards/standardContentUtils';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { Badge } from '@/components/ui/badge';
import { ListRow } from '@/components/ui/list';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { WorkflowOpenScopeListSections } from '@/components/workflow/WorkflowOpenScopeListSections';
import { useTranslation } from '@/hooks/useTranslation';
import { getAdminStandardWorkflowPath } from '@/lib/navigation/adminWorkspace';

interface StandardWorkflowManagementProps {
  state: StandardContentState;
  embedded?: boolean;
}

function workflowBadges(attributes: Record<string, string>, t: (key: string) => string) {
  const badges = [];
  if (attributes.enabled === 'false') {
    badges.push(
      <Badge key="disabled" tone="neutral">
        {t('admin.standards.fields.disabled')}
      </Badge>,
    );
  }
  if (attributes.autoApprove === 'true') {
    badges.push(
      <Badge key="auto-approve" tone="neutral">
        {t('admin.standards.fields.autoApprove')}
      </Badge>,
    );
  }
  return badges;
}

export function StandardWorkflowManagement({
  state,
  embedded = false,
}: StandardWorkflowManagementProps) {
  const t = useTranslation();
  const { tenantId, standardId, items } = state;

  const workflowItems = useMemo(
    () => items.filter((i) => i.domainKey === 'workflow-type-mappings'),
    [items],
  );
  const titleByKey = useMemo(() => buildStandardTitleByKey(items), [items]);

  return (
    <OrganizationManageSection
      embedded={embedded}
      title={t('admin.standards.sections.workflowTypeMappings')}
    >
      {workflowItems.length === 0 ? (
        <EmptyStateCard>{t('admin.standards.detail.empty')}</EmptyStateCard>
      ) : (
        <WorkflowOpenScopeListSections
          items={workflowItems}
          getWorkflowTypeId={standardWorkflowTypeId}
          getItemKey={(item) => item.configKey}
          listLabel={t('admin.standards.sections.workflowTypeMappings')}
          renderRow={(item) => {
            const a = item.attributes ?? {};
            const badges = workflowBadges(a, t);

            return (
              <ListRow
                compact
                href={getAdminStandardWorkflowPath(tenantId, standardId, item.configKey)}
                title={standardWorkflowTypeLabel(t, item.title)}
                subtitle={
                  <span className="line-clamp-1">
                    {t('admin.standards.approverLabel')}:{' '}
                    {standardOrgKeyLabel(
                      t,
                      a.approverOrgGroupConfigKey ?? item.subtitle ?? '',
                      titleByKey,
                    )}
                  </span>
                }
                badges={badges.length > 0 ? badges : undefined}
              />
            );
          }}
        />
      )}
    </OrganizationManageSection>
  );
}
