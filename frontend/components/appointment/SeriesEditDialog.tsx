/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Button } from '@/components/ui/button';
import { Modal } from '@/components/ui/modal';
import { useTranslation } from '@/hooks/useTranslation';
import { SeriesEditScope } from '@/types/appointment';

interface SeriesEditDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (scope: SeriesEditScope) => void;
  mode: 'edit' | 'delete';
}

export function SeriesEditDialog({ isOpen, onClose, onConfirm, mode }: SeriesEditDialogProps) {
  const t = useTranslation();

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      size="sm"
      title={
        mode === 'edit' ? t('appointments.series.editTitle') : t('appointments.series.deleteTitle')
      }
    >
      <div className="flex flex-col gap-6">
        <p className="text-pretty text-sm leading-relaxed text-[var(--text-secondary)]">
          {t('appointments.series.description')}
        </p>
        <div className="flex flex-col gap-3">
          <Button
            type="button"
            variant="secondary"
            fullWidth
            onClick={() => onConfirm(SeriesEditScope.THIS_OCCURRENCE)}
          >
            {t('appointments.series.thisOccurrence')}
          </Button>
          <Button
            type="button"
            variant="secondary"
            fullWidth
            onClick={() => onConfirm(SeriesEditScope.ENTIRE_SERIES)}
          >
            {t('appointments.series.entireSeries')}
          </Button>
        </div>
        <Button type="button" variant="ghost" fullWidth onClick={onClose}>
          {t('common.cancel')}
        </Button>
      </div>
    </Modal>
  );
}
