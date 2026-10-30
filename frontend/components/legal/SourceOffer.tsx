/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ArrowLeft, ExternalLink } from 'lucide-react';
import Link from 'next/link';
import type { ReactNode } from 'react';
import { useTranslation } from '@/hooks/useTranslation';
import { getBuildInfo } from '@/lib/buildInfo';
import { cn } from '@/lib/utils/cn';

type SourceOfferProps = {
  className?: string;
};

function SourceActionLink({ href, children }: { href: string; children: ReactNode }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className={cn(
        'inline-flex min-h-11 w-full items-center justify-between gap-3 rounded-xl border border-[var(--border-color)]',
        'bg-[var(--surface-soft)] px-4 py-3 text-sm font-semibold text-[var(--text-primary)]',
        'transition-colors duration-200 hover:bg-[var(--hover-bg)] active:bg-[var(--hover-bg)]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
      )}
    >
      <span className="min-w-0 text-left leading-snug">{children}</span>
      <ExternalLink className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
    </a>
  );
}

/** AGPL-3.0 §13 offer: version + Corresponding Source of this running build. */
export function SourceOffer({ className }: SourceOfferProps) {
  const t = useTranslation();
  const info = getBuildInfo();
  const versionLabel = info.version
    ? t('legal.source.version', { version: info.version })
    : t('legal.source.unreleased');

  return (
    <div className={cn('flex flex-col gap-4', className)}>
      <div className="rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)] p-4">
        <p className="text-sm font-semibold text-[var(--text-primary)]">{versionLabel}</p>
        <p className="mt-2 text-sm leading-relaxed text-[var(--text-secondary)]">
          {t('legal.source.agplNotice')}
        </p>
      </div>
      <div className="flex flex-col gap-2">
        <SourceActionLink href={info.sourceUrl}>{t('legal.source.codeLink')}</SourceActionLink>
        {info.releaseUrl ? (
          <SourceActionLink href={info.releaseUrl}>
            {t('legal.source.releaseLink')}
          </SourceActionLink>
        ) : null}
        <SourceActionLink href={info.licenseUrl}>{t('legal.source.licenseLink')}</SourceActionLink>
        <Link
          href="/"
          className={cn(
            'inline-flex min-h-11 w-full items-center gap-3 rounded-xl border border-[var(--border-color)]',
            'bg-[var(--surface-soft)] px-4 py-3 text-sm font-semibold text-[var(--text-primary)]',
            'transition-colors duration-200 hover:bg-[var(--hover-bg)] active:bg-[var(--hover-bg)]',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
          )}
        >
          <ArrowLeft className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
          <span>{t('legal.source.backToApp')}</span>
        </Link>
      </div>
    </div>
  );
}
