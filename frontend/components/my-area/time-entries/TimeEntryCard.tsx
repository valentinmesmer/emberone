/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { IconButton } from '@/components/ui/icon-button';
import { ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import { formatTimeEntryListSubtitle } from '@/lib/utils/date';
import type { UserTimeEntryDto } from '@/types/timeEntry';
import { TimeEntryStampOutButton } from './TimeEntryStampOutButton';

interface TimeEntryCardProps {
  entry: UserTimeEntryDto;
  href: string;
  onStampOut?: () => void;
  isStampOutPending?: boolean;
  onEdit?: () => void;
  onDelete: () => void;
  actionsDisabled?: boolean;
  showActions?: boolean;
}

export function TimeEntryCard({
  entry,
  href,
  onStampOut,
  isStampOutPending = false,
  onEdit,
  onDelete,
  actionsDisabled = false,
  showActions = true,
}: TimeEntryCardProps) {
  const t = useTranslation();
  const isOpen = !entry.endTime;

  const title = entry.organizationName;
  const subtitle = formatTimeEntryListSubtitle(entry.startTime, entry.endTime);

  return (
    <ListRow
      tone={isOpen ? 'active' : 'default'}
      href={href}
      title={title}
      subtitle={<span className="tabular-nums text-[var(--text-secondary)]">{subtitle}</span>}
      badges={
        isOpen ? (
          <Badge tone="accent" pill className="px-2">
            {t('myArea.timeEntries.running')}
          </Badge>
        ) : null
      }
      actions={
        showActions ? (
          <>
            {onEdit ? (
              <IconButton
                variant="action"
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={onEdit}
                disabled={actionsDisabled}
                aria-label={t('myArea.timeEntries.editTitle')}
                title={t('myArea.timeEntries.editTitle')}
              />
            ) : null}
            <IconButton
              variant="action"
              icon={<Trash2 className="h-4 w-4" aria-hidden />}
              onClick={onDelete}
              disabled={actionsDisabled}
              aria-label={t('myArea.timeEntries.delete')}
              title={t('myArea.timeEntries.delete')}
            />
          </>
        ) : undefined
      }
      below={
        showActions && isOpen && onStampOut ? (
          <TimeEntryStampOutButton
            onClick={onStampOut}
            disabled={actionsDisabled}
            isLoading={isStampOutPending}
          />
        ) : undefined
      }
    />
  );
}
