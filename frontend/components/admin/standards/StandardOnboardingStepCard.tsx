/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { GripVertical, Pencil, Trash2 } from 'lucide-react';
import {
  type LocalOnboardingStep,
  onboardingStepTypeKey,
} from '@/components/admin/standards/onboardingStepUtils';
import { IconButton } from '@/components/ui/icon-button';
import { ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

interface StandardOnboardingStepCardProps {
  step: LocalOnboardingStep;
  sortIndex: number;
  href: string;
  configurable?: boolean;
  isReordering?: boolean;
  isDragging?: boolean;
  isDragOver?: boolean;
  reorderable?: boolean;
  onReorderPointerDown?: (e: React.PointerEvent<HTMLButtonElement>) => void;
  onEdit?: () => void;
  onDelete: () => void;
  actionsDisabled?: boolean;
}

export function StandardOnboardingStepCard({
  step,
  sortIndex,
  href,
  configurable = false,
  isReordering = false,
  isDragging = false,
  isDragOver = false,
  reorderable = false,
  onReorderPointerDown,
  onEdit,
  onDelete,
  actionsDisabled = false,
}: StandardOnboardingStepCardProps) {
  const t = useTranslation();
  const stepKey = onboardingStepTypeKey(step.stepType);
  const title = t(`admin.standards.onboardingSteps.${stepKey}.title`);

  return (
    <ListRow
      href={href}
      ariaLabel={title}
      chevron
      rootProps={{
        'data-step-type': step.stepType,
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
            aria-label={t('admin.standards.onboardingSteps.dragToReorder')}
            title={t('admin.standards.onboardingSteps.dragToReorder')}
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
          title={t('admin.standards.onboardingSteps.sortOrder', { order: sortIndex + 1 })}
        >
          <span className="block leading-none">{sortIndex + 1}</span>
        </div>
      }
      title={title}
      subtitle={t(`admin.standards.onboardingSteps.${stepKey}.subtitle`)}
      actions={
        <>
          {configurable && onEdit ? (
            <IconButton
              variant="action"
              icon={<Pencil className="h-4 w-4" aria-hidden />}
              onClick={onEdit}
              disabled={actionsDisabled}
              aria-label={t('admin.standards.onboardingSteps.editConfig')}
              title={t('admin.standards.onboardingSteps.editConfig')}
            />
          ) : null}
          <IconButton
            variant="action"
            icon={<Trash2 className="h-4 w-4" aria-hidden />}
            onClick={onDelete}
            disabled={actionsDisabled}
            aria-label={t('admin.standards.onboardingSteps.removeStep')}
            title={t('admin.standards.onboardingSteps.removeStep')}
          />
        </>
      }
    />
  );
}
