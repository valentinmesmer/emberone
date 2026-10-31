/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { downloadBlob } from '@/lib/utils/downloadBlob';

export function useDownloadOnboardingAttachment() {
  const t = useTranslation();
  const { showError } = useToast();

  return async (run: () => Promise<{ blob: Blob; fileName: string }>) => {
    try {
      const { blob, fileName } = await run();
      downloadBlob(blob, fileName);
    } catch (error) {
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
    }
  };
}
