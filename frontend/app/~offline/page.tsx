/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import Image from 'next/image';
import Link from 'next/link';
import { t } from '@/lib/i18n/translations';

/**
 * Precached navigation fallback when the network is unavailable.
 * Server-rendered + public (no auth chrome) so the SW can serve it offline.
 */
export default function OfflinePage() {
  return (
    <main className="flex min-h-[100dvh] flex-col items-center justify-center gap-4 px-6 text-center">
      <Image
        src="/icon-192.png"
        alt=""
        width={72}
        height={72}
        className="rounded-2xl"
        unoptimized
      />
      <h1 className="text-2xl font-extrabold tracking-tight text-[var(--text-primary)]">
        {t('pwa.offline.title')}
      </h1>
      <p className="max-w-sm text-sm text-[var(--text-secondary)]">
        {t('pwa.offline.description')}
      </p>
      <Link
        href="/"
        className="inline-flex h-11 items-center justify-center rounded-xl bg-[var(--fire-red)] px-6 text-sm font-semibold text-white"
      >
        {t('common.pullToRefresh.retry')}
      </Link>
    </main>
  );
}
