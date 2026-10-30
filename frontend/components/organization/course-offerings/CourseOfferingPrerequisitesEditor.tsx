/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Card } from '@/components/ui/card';
import { ListContainer, ListRow } from '@/components/ui/list';
import { SectionHeader } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import { getOrganizationCoursePrerequisiteDetailPath } from '@/lib/utils/courseOffering';
import type { CourseOfferingDto, CoursePrerequisiteKind } from '@/types/courseOffering';

interface Props {
  organizationId: number;
  offering: CourseOfferingDto;
}

const KIND_ROWS: {
  kind: CoursePrerequisiteKind;
  titleKey: string;
}[] = [
  { kind: 'TRAINING_COURSE', titleKey: 'courseOfferings.prerequisites.trainingCourses' },
  { kind: 'CERTIFICATION', titleKey: 'courseOfferings.prerequisites.certifications' },
  { kind: 'DRIVING_LICENSE', titleKey: 'courseOfferings.prerequisites.drivingLicenses' },
];

export function CourseOfferingPrerequisitesEditor({ organizationId, offering }: Props) {
  const t = useTranslation();
  const entries = offering.prerequisites?.entries ?? [];

  return (
    <Card className="p-3 sm:p-4">
      <SectionHeader
        title={t('courseOfferings.prerequisites.title')}
        subtitle={t('courseOfferings.prerequisites.subtitle')}
      />
      <ListContainer
        className="mt-3"
        variant="inset"
        label={t('courseOfferings.prerequisites.title')}
      >
        {KIND_ROWS.map(({ kind, titleKey }) => {
          const count = entries.filter((entry) => entry.kind === kind).length;
          return (
            <ListRow
              key={kind}
              accent={false}
              href={getOrganizationCoursePrerequisiteDetailPath(organizationId, offering.id, kind)}
              title={t(titleKey)}
              subtitle={
                count > 0
                  ? t('courseOfferings.prerequisites.count', { count })
                  : t('courseOfferings.prerequisites.empty')
              }
            />
          );
        })}
      </ListContainer>
    </Card>
  );
}
