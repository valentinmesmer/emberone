/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Eye, FileText } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { EmptyState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export interface OnboardingProofItem {
  key: string;
  fileName: string;
  subtitle?: string;
  onView?: () => void;
}

interface OnboardingProofListProps {
  items: OnboardingProofItem[];
  viewingKey: string | null;
  showWhenEmpty?: boolean;
}

export function OnboardingProofList({
  items,
  viewingKey,
  showWhenEmpty = false,
}: OnboardingProofListProps) {
  const t = useTranslation();
  if (items.length === 0 && !showWhenEmpty) return null;

  const label = t('myArea.onboarding.detail.documentsTitle');

  return (
    <section>
      <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
        {label}
      </h2>
      {items.length === 0 ? (
        <EmptyState>{t('myArea.onboarding.detail.noDocuments')}</EmptyState>
      ) : (
        <ListContainer label={label} rowSeparator="inset">
          {items.map((item) => (
            <ListRow
              key={item.key}
              compact
              keepActionsOnMobile
              leading={
                <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--surface-muted)] text-[var(--text-secondary)]">
                  <FileText className="h-4 w-4" aria-hidden />
                </div>
              }
              title={item.fileName}
              subtitle={item.subtitle}
              actions={
                item.onView ? (
                  <Button
                    type="button"
                    variant="ghost"
                    size="xs"
                    className="h-7 w-7 min-w-7 shrink-0 p-0"
                    isLoading={viewingKey === item.key}
                    onClick={item.onView}
                    aria-label={t('myArea.workflowDetail.viewAttachment')}
                    title={t('myArea.workflowDetail.viewAttachment')}
                  >
                    <Eye className="h-3.5 w-3.5" aria-hidden />
                  </Button>
                ) : undefined
              }
            />
          ))}
        </ListContainer>
      )}
    </section>
  );
}
