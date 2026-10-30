/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { Checkbox } from '@/components/ui/checkbox';
import { DatePicker } from '@/components/ui/date-picker';
import { useTranslation } from '@/hooks/useTranslation';
import type {
  DrivingLicenseClassInput,
  DrivingLicenseDefinitionDto,
} from '@/lib/api/drivingLicenses';

export interface DrivingLicenseClassesEditorProps {
  definitions: DrivingLicenseDefinitionDto[];
  value: DrivingLicenseClassInput[];
  onChange: (value: DrivingLicenseClassInput[]) => void;
  disabled?: boolean;
  /** When true, explains that deselected classes are marked invalid (org edit only). */
  showArchiveHint?: boolean;
}

export function DrivingLicenseClassesEditor({
  definitions,
  value,
  onChange,
  disabled = false,
  showArchiveHint = false,
}: DrivingLicenseClassesEditorProps) {
  const t = useTranslation();
  const selectedIds = useMemo(
    () => new Set(value.map((entry) => entry.tenantDrivingLicenseDefinitionId)),
    [value],
  );

  const updateClass = (definitionId: number, patch: Partial<DrivingLicenseClassInput>) => {
    onChange(
      value.map((entry) =>
        entry.tenantDrivingLicenseDefinitionId === definitionId ? { ...entry, ...patch } : entry,
      ),
    );
  };

  const toggleClass = (definitionId: number, checked: boolean) => {
    if (checked) {
      onChange([
        ...value,
        { tenantDrivingLicenseDefinitionId: definitionId, grantDate: null, expirationDate: null },
      ]);
      return;
    }
    onChange(value.filter((entry) => entry.tenantDrivingLicenseDefinitionId !== definitionId));
  };

  if (definitions.length === 0) {
    return (
      <p className="text-sm text-[var(--text-secondary)]">
        {t('drivingLicense.grantedClasses.empty')}
      </p>
    );
  }

  return (
    <div className="space-y-3">
      <p className="text-sm font-medium text-[var(--text-primary)]">
        {t('drivingLicense.classes.label')}
      </p>
      {showArchiveHint ? (
        <p className="text-sm text-[var(--text-secondary)]">
          {t('drivingLicense.classes.archiveHint')}
        </p>
      ) : null}
      {definitions.map((definition) => {
        const selected = selectedIds.has(definition.id);
        const classInput = value.find(
          (entry) => entry.tenantDrivingLicenseDefinitionId === definition.id,
        );
        return (
          <div
            key={definition.id}
            className="rounded-lg border border-[var(--border-color)]/40 p-3 space-y-3"
          >
            <Checkbox
              label={definition.name}
              checked={selected}
              onCheckedChange={(checked) => toggleClass(definition.id, checked === true)}
              disabled={disabled}
            />
            {selected && classInput ? (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <DatePicker
                  label={t('drivingLicense.classes.grantDateLabel')}
                  value={classInput.grantDate ?? ''}
                  onChange={(event) =>
                    updateClass(definition.id, {
                      grantDate: event.target.value.trim() || null,
                    })
                  }
                  disabled={disabled}
                />
                <DatePicker
                  label={t('drivingLicense.classes.expirationDateLabel')}
                  value={classInput.expirationDate ?? ''}
                  onChange={(event) =>
                    updateClass(definition.id, {
                      expirationDate: event.target.value.trim() || null,
                    })
                  }
                  disabled={disabled}
                />
              </div>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}
