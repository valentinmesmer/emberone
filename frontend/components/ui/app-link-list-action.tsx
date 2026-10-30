/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ExternalLink } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { IconButton } from '@/components/ui/icon-button';
import { useTranslation } from '@/hooks/useTranslation';
import { appLinkActionLabel } from '@/lib/utils/appLinkLabels';
import type { AppLinkFields } from '@/types/appLink';

interface AppLinkListActionProps {
  href: string;
  link?: Pick<AppLinkFields, 'linkType'>;
  /** Overrides the label derived from `link.linkType`. */
  ariaLabel?: string;
  onNavigate?: () => void;
}

export function AppLinkListAction({ href, link, ariaLabel, onNavigate }: AppLinkListActionProps) {
  const t = useTranslation();
  const router = useRouter();
  const label = ariaLabel ?? appLinkActionLabel(link ?? {}, t);

  return (
    <IconButton
      variant="action"
      icon={<ExternalLink className="h-4 w-4" aria-hidden />}
      aria-label={label}
      title={label}
      onClick={() => {
        onNavigate?.();
        router.push(href);
      }}
    />
  );
}

export function AppLinkListIndicator({ ariaLabel }: { ariaLabel: string }) {
  return (
    <span
      className="inline-flex h-9 w-9 shrink-0 items-center justify-center text-[var(--text-secondary)]"
      title={ariaLabel}
    >
      <ExternalLink className="h-4 w-4" aria-hidden />
    </span>
  );
}
