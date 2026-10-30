/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { SourceOffer } from '@/components/legal/SourceOffer';
import { Brand } from '@/components/layout/Brand';
import { useTranslation } from '@/hooks/useTranslation';

/**
 * Public AGPL Corresponding Source offer. Unauthenticated on purpose so every
 * remote user can reach the source of this running version (AGPL-3.0 §13).
 */
export default function SourcePage() {
  const t = useTranslation();

  return (
    <main className="mx-auto flex min-h-[100dvh] w-full max-w-lg flex-col px-4 py-6 sm:px-6 sm:py-10">
      <div className="flex flex-1 flex-col gap-5 sm:justify-center sm:gap-6 sm:py-8">
        <Brand variant="compact" showSubtitle={false} />
        <div>
          <h1 className="text-xl font-extrabold tracking-tight text-[var(--text-primary)] sm:text-2xl">
            {t('legal.source.title')}
          </h1>
          <p className="mt-1 text-sm text-[var(--text-secondary)]">{t('legal.source.subtitle')}</p>
        </div>
        <SourceOffer />
      </div>
      <p className="shrink-0 pt-8 text-center text-[11px] leading-snug text-[var(--text-secondary)] sm:pt-6">
        {t('legal.source.copyright')}
      </p>
    </main>
  );
}
