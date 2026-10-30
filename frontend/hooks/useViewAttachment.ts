/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useState } from 'react';
import { useTranslation } from '@/hooks/useTranslation';
import { useToast } from '@/components/ui/toast';

export function useViewAttachment() {
  const t = useTranslation();
  const { showError } = useToast();
  const [viewingKey, setViewingKey] = useState<string | null>(null);

  const view = useCallback(
    async (key: string, run: (targetWindow: Window) => Promise<void>) => {
      const targetWindow = window.open('about:blank', '_blank');
      if (!targetWindow) {
        showError(t('myArea.workflowDetail.attachmentViewPopupBlocked'));
        return;
      }

      setViewingKey(key);
      try {
        await run(targetWindow);
      } catch (error) {
        try {
          targetWindow.close();
        } catch {
          // ignore
        }
        const message = error instanceof Error ? error.message : '';
        showError(message || t('myArea.workflowDetail.attachmentViewFailed'));
      } finally {
        setViewingKey(null);
      }
    },
    [showError, t],
  );

  return { view, viewingKey };
}
