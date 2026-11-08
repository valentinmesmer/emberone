/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Sparkles } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { pickExtractionFiles } from '@/lib/ai/extractionFields';
import { useExtractProofFromDocument, useProofExtractionAvailable } from '@/lib/store';
import type { AiExtractionTarget, AiProofType, ExtractionHintDto } from '@/types/ai';

export interface AiExtractionAssistProps {
  proofType: AiProofType;
  /** Admin forms: read the proof on behalf of this member (uses the organization endpoint). */
  target?: AiExtractionTarget;
  /** Files already chosen in the form; every readable one is sent together. */
  files: File[];
  /** Catalog entry chosen in the form (required for certifications). */
  selectionId?: number | null;
  /** Needs a selection before it can run (certification). */
  requiresSelection?: boolean;
  disabled?: boolean;
  /** Maps a form field name of the suggestion to its translated label. */
  fieldLabels: Record<string, string>;
  /** Applies the suggested values to the form; returns the field names it actually filled. */
  onApply: (fields: Record<string, unknown>) => string[];
}

interface Notice {
  applied: string[];
  hints: ExtractionHintDto[];
}

/**
 * "Fill from document" for proof forms: sends all chosen documents to the backend, pre-fills the form
 * and marks the result as an AI suggestion to be checked. The user submits the form themselves.
 * Renders nothing unless the backend offers the function for this proof type and entry.
 */
export function AiExtractionAssist({
  proofType,
  target,
  files,
  selectionId,
  requiresSelection = false,
  disabled = false,
  fieldLabels,
  onApply,
}: AiExtractionAssistProps) {
  const t = useTranslation();
  const available = useProofExtractionAvailable(proofType);
  const extract = useExtractProofFromDocument(proofType, target);
  const [notice, setNotice] = useState<Notice | null>(null);

  if (!available) {
    return null;
  }

  const readable = pickExtractionFiles(files);
  const missingSelection = requiresSelection && (selectionId == null || selectionId <= 0);
  let disabledReason: string | undefined;
  if (files.length === 0) {
    disabledReason = t('ai.extract.needFile');
  } else if (readable.length === 0) {
    disabledReason = t('ai.extract.imageOnly');
  } else if (missingSelection) {
    disabledReason = t('ai.extract.needSelection');
  }

  const run = async () => {
    if (readable.length === 0 || disabledReason) return;
    const result = await extract.mutateAsync({ files: readable, selectionId });
    setNotice({ applied: onApply(result.fields), hints: result.hints });
  };

  const labelFor = (field: string | null) => (field ? (fieldLabels[field] ?? field) : '');

  return (
    <div className="space-y-2">
      <Button
        type="button"
        variant="secondary"
        size="md"
        fullWidth
        icon={<Sparkles className="h-4 w-4" />}
        isLoading={extract.isPending}
        disabled={disabled || disabledReason != null}
        title={disabledReason}
        onClick={() => void run().catch(() => undefined)}
      >
        {extract.isPending ? t('ai.extract.running') : t('ai.extract.button')}
      </Button>
      {notice ? (
        <NoticeCard tone="warning" className="!p-3 sm:!p-3 md:!p-3">
          <p className="text-sm font-semibold">{t('ai.extract.suggestionTitle')}</p>
          <p className="mt-1 text-sm text-[var(--text-secondary)]">
            {notice.applied.length > 0
              ? t('ai.extract.suggestionFilled', {
                  fields: notice.applied.map((field) => labelFor(field)).join(', '),
                })
              : t('ai.extract.suggestionEmpty')}
          </p>
          {notice.hints.length > 0 ? (
            <ul className="mt-1 list-disc space-y-0.5 pl-5 text-sm text-[var(--text-secondary)]">
              {notice.hints
                .filter((hint) => hint.code !== 'NOTHING_RECOGNIZED')
                .map((hint) => (
                  <li key={`${hint.code}:${hint.field ?? ''}`}>
                    {t(`ai.extract.hints.${hint.code}`, { field: labelFor(hint.field) })}
                  </li>
                ))}
            </ul>
          ) : null}
        </NoticeCard>
      ) : null}
    </div>
  );
}
