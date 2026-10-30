/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import type { QualificationPrerequisiteDetailDto } from '@/lib/api/qualifications';
import type { QualificationRequirementMode } from '@/lib/api/tenantQualifications';

export interface QualificationPrerequisiteSectionProps {
  title: string;
  items: QualificationPrerequisiteDetailDto[];
  requirementMode: QualificationRequirementMode;
  groupMet: boolean;
  resolveHref?: (detail: QualificationPrerequisiteDetailDto) => string | null;
  resolveAction?: (detail: QualificationPrerequisiteDetailDto) => ReactNode;
  resolveStatusBadge?: (detail: QualificationPrerequisiteDetailDto) => ReactNode;
  resolveInProgress?: (detail: QualificationPrerequisiteDetailDto) => boolean;
}

/**
 * Renders prerequisite rows for training courses or certifications.
 * ANY mode shows an explicit “one of” hint and treats unmet alternatives as optional once the group is satisfied.
 */
export function QualificationPrerequisiteSection({
  title,
  items,
  requirementMode,
  groupMet,
  resolveHref,
  resolveAction,
  resolveStatusBadge,
  resolveInProgress,
}: QualificationPrerequisiteSectionProps) {
  const t = useTranslation();
  const isAny = requirementMode === 'ANY' && items.length > 1;
  const modeHint = isAny
    ? t('qualifications.prerequisite.modeAnyHint')
    : items.length > 1
      ? t('qualifications.prerequisite.modeAllHint')
      : null;
  const displayItems =
    isAny && groupMet
      ? [...items].sort((left, right) => Number(right.met) - Number(left.met))
      : items;

  return (
    <div>
      <div className="mb-1.5 flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
        <h2 className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
          {title}
        </h2>
        {modeHint ? (
          <Badge tone="neutral" pill>
            {modeHint}
          </Badge>
        ) : null}
      </div>
      <ListContainer label={title} rowSeparator="inset">
        {displayItems.map((detail, index) => {
          const optionalAlternative = isAny && groupMet && !detail.met;
          const inProgress =
            !detail.met && !optionalAlternative && (resolveInProgress?.(detail) ?? false);
          const tone = detail.met
            ? 'success'
            : optionalAlternative
              ? 'neutral'
              : inProgress
                ? 'warning'
                : 'danger';
          const href = resolveHref?.(detail) ?? undefined;
          const action = optionalAlternative ? null : resolveAction?.(detail);

          return (
            <ListRow
              key={detail.configKey}
              compact
              tone={tone}
              href={href}
              title={
                <span className="flex min-w-0 flex-col gap-0.5">
                  {isAny && index > 0 ? (
                    <span className="text-[10px] font-semibold uppercase tracking-wider text-[var(--text-secondary)]">
                      {t('qualifications.prerequisite.orSeparator')}
                    </span>
                  ) : null}
                  <span
                    className={optionalAlternative ? 'text-[var(--text-secondary)]' : undefined}
                  >
                    {detail.name}
                  </span>
                </span>
              }
              badges={
                optionalAlternative ? (
                  <Badge tone="neutral" pill>
                    {t('qualifications.prerequisite.alternativeNotNeeded')}
                  </Badge>
                ) : (
                  resolveStatusBadge?.(detail)
                )
              }
              below={action ?? undefined}
            />
          );
        })}
      </ListContainer>
    </div>
  );
}
