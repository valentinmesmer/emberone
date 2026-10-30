/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { IconButton } from '@/components/ui/icon-button';
import { ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import { getAbsenceReasonLabelKey } from '@/lib/my-area/absenceReasonOptions';
import { formatDateShort } from '@/lib/utils/date';
import type { UserAbsenceDto } from '@/types/absence';

interface AbsenceCardProps {
  absence: UserAbsenceDto;
  href: string;
  onEdit: () => void;
  onDelete: () => void;
  actionsDisabled?: boolean;
  showActions?: boolean;
}

export function AbsenceCard({
  absence,
  href,
  onEdit,
  onDelete,
  actionsDisabled = false,
  showActions = true,
}: AbsenceCardProps) {
  const t = useTranslation();
  const reasonLabel = t(getAbsenceReasonLabelKey(absence.reason));
  const dateRange = `${formatDateShort(absence.startDate)} - ${formatDateShort(absence.endDate)}`;

  return (
    <ListRow
      href={href}
      title={reasonLabel}
      subtitle={<span className="tabular-nums">{dateRange}</span>}
      actions={
        showActions ? (
          <>
            <IconButton
              variant="action"
              icon={<Pencil className="h-4 w-4" aria-hidden />}
              onClick={onEdit}
              disabled={actionsDisabled}
              aria-label={t('myArea.absence.editTitle')}
              title={t('myArea.absence.editTitle')}
            />
            <IconButton
              variant="action"
              icon={<Trash2 className="h-4 w-4" aria-hidden />}
              onClick={onDelete}
              disabled={actionsDisabled}
              aria-label={t('myArea.absence.delete')}
              title={t('myArea.absence.delete')}
            />
          </>
        ) : undefined
      }
    />
  );
}
