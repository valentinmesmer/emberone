/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { RepeatableSelect } from '@/components/ui/repeatable-select';
import { useTranslation } from '@/hooks/useTranslation';
import type { TrainingCourseTypeDto } from '@/lib/api/trainingCourses';

export interface TrainingCourseTypeSelectionFieldProps {
  types: TrainingCourseTypeDto[];
  selectedIds: number[];
  onChange: (selectedIds: number[]) => void;
  disabled?: boolean;
}

export function TrainingCourseTypeSelectionField({
  types,
  selectedIds,
  onChange,
  disabled = false,
}: TrainingCourseTypeSelectionFieldProps) {
  const t = useTranslation();

  const options = useMemo(
    () => types.map((type) => ({ value: String(type.id), label: type.name })),
    [types],
  );

  if (types.length === 0) {
    return (
      <p className="text-sm text-[var(--text-secondary)]">
        {t('trainingCourse.grantedTypes.empty')}
      </p>
    );
  }

  return (
    <RepeatableSelect
      label={t('trainingCourse.submit.typeLabel')}
      name="tenantTrainingCourseTypeIds"
      values={selectedIds.map(String)}
      options={options}
      onChange={(values) => onChange(values.map((value) => Number.parseInt(value, 10)))}
      placeholder={t('trainingCourse.record.chooseType')}
      addLabel={t('trainingCourse.grantedTypes.addAnother')}
      removeLabel={t('trainingCourse.grantedTypes.remove')}
      disabled={disabled}
      required
    />
  );
}

export function formatGrantedTrainingCourseTypeNames(
  typeIds: number[],
  types: TrainingCourseTypeDto[],
): string {
  const names = typeIds
    .map((id) => types.find((type) => type.id === id)?.name)
    .filter((name): name is string => Boolean(name?.trim()));
  if (names.length === 0) {
    return typeIds.map((id) => `#${id}`).join(', ');
  }
  return names.join(', ');
}
