/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { useTranslation } from '@/hooks/useTranslation';
import { grantSlotKey } from '@/lib/admin/standardFunctionGrants';
import type { FunctionGrantRequest } from '@/lib/api/standards';

interface Props {
  grants: FunctionGrantRequest[];
  orgGroupTitleByKey: Map<string, string>;
  show: boolean;
}

export function StandardFunctionGrantTargets({ grants, orgGroupTitleByKey, show }: Props) {
  const t = useTranslation();

  if (!show) {
    return null;
  }

  if (grants.length === 0) {
    return (
      <span className="text-xs text-[var(--text-secondary)]">
        {t('admin.standards.functions.grantTargetsUnset')}
      </span>
    );
  }

  return (
    <div className="flex flex-wrap gap-1.5 pl-7 sm:justify-end sm:pl-0">
      {grants.map((grant) => {
        const groupLabel =
          orgGroupTitleByKey.get(grant.organizationGroupConfigKey) ??
          grant.organizationGroupConfigKey;
        const roleLabel = t(`admin.standards.functions.slots.${grant.membershipType}`);
        const label = t('admin.standards.functions.grantTargetSummary', {
          group: groupLabel,
          role: roleLabel,
        });
        return (
          <Badge
            key={grantSlotKey(grant.organizationGroupConfigKey, grant.membershipType)}
            tone="neutral"
            className="max-w-full truncate"
          >
            {label}
          </Badge>
        );
      })}
    </div>
  );
}
