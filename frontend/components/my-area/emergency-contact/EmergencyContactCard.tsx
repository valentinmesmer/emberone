/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { GripVertical, Pencil, Trash2 } from 'lucide-react';
import { IconButton } from '@/components/ui/icon-button';
import { ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import { getEmergencyContactRelationshipLabelKey } from '@/lib/my-area/emergencyContactOptions';
import { cn } from '@/lib/utils/cn';
import type { EmergencyContactDto } from '@/types/emergencyContact';

interface EmergencyContactCardProps {
  contact: EmergencyContactDto;
  href: string;
  isReordering?: boolean;
  isDragging?: boolean;
  isDragOver?: boolean;
  reorderable?: boolean;
  onReorderPointerDown?: (e: React.PointerEvent<HTMLButtonElement>) => void;
  onEdit: () => void;
  onDelete: () => void;
  actionsDisabled?: boolean;
}

export function EmergencyContactCard({
  contact,
  href,
  isReordering = false,
  isDragging = false,
  isDragOver = false,
  reorderable = false,
  onReorderPointerDown,
  onEdit,
  onDelete,
  actionsDisabled = false,
}: EmergencyContactCardProps) {
  const t = useTranslation();
  const fullName = `${contact.firstName} ${contact.lastName}`;
  const relationship = t(getEmergencyContactRelationshipLabelKey(contact.relationship));

  return (
    <ListRow
      href={href}
      ariaLabel={fullName}
      chevron
      rootProps={{
        'data-contact-id': contact.id,
        className: cn(
          isDragging && 'opacity-50',
          isDragOver && '!bg-[var(--fire-red)]/10',
          isReordering && 'pointer-events-none opacity-70',
        ),
      }}
      dragHandle={
        reorderable ? (
          <button
            type="button"
            disabled={isReordering}
            onPointerDown={onReorderPointerDown}
            onClick={(e) => {
              e.preventDefault();
              e.stopPropagation();
            }}
            className={cn(
              'flex h-11 w-11 shrink-0 cursor-grab touch-none select-none items-center justify-center rounded-xl',
              'text-[var(--text-secondary)] hover:bg-[var(--fire-red)]/10 hover:text-[var(--fire-red)]',
              'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--fire-red)]/40',
              'active:cursor-grabbing',
              isDragging && 'cursor-grabbing bg-[var(--fire-red)]/10 text-[var(--fire-red)]',
              isReordering && 'cursor-wait',
            )}
            aria-label={t('myArea.emergencyContact.dragToReorder')}
            title={t('myArea.emergencyContact.dragToReorder')}
          >
            <GripVertical className="h-5 w-5" aria-hidden />
          </button>
        ) : undefined
      }
      leading={
        <div
          className={cn(
            'flex h-8 min-w-8 shrink-0 items-center justify-center rounded-lg px-1.5',
            'bg-[var(--fire-red)]/15 text-xs font-bold tabular-nums leading-none text-[var(--fire-red)]',
          )}
          title={t('myArea.emergencyContact.priority', { priority: contact.priority })}
        >
          <span className="block leading-none">{contact.priority}</span>
        </div>
      }
      title={fullName}
      subtitle={<span className="block truncate">{relationship}</span>}
      actions={
        <>
          <IconButton
            variant="action"
            icon={<Pencil className="h-4 w-4" aria-hidden />}
            onClick={onEdit}
            disabled={actionsDisabled}
            aria-label={t('myArea.emergencyContact.editTitle')}
            title={t('myArea.emergencyContact.editTitle')}
          />
          <IconButton
            variant="action"
            icon={<Trash2 className="h-4 w-4" aria-hidden />}
            onClick={onDelete}
            disabled={actionsDisabled}
            aria-label={t('myArea.emergencyContact.delete')}
            title={t('myArea.emergencyContact.delete')}
          />
        </>
      }
    />
  );
}
