/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ArrowRight } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { useTranslation } from '@/hooks/useTranslation';
import { appLinkActionLabel } from '@/lib/utils/appLinkLabels';
import type { AppLinkFields } from '@/types/appLink';

interface AppLinkNavigateButtonProps {
  href: string;
  link?: Pick<AppLinkFields, 'linkType'>;
  onNavigate?: () => void;
}

export function AppLinkNavigateButton({ href, link, onNavigate }: AppLinkNavigateButtonProps) {
  const t = useTranslation();
  const router = useRouter();

  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      icon={<ArrowRight className="h-4 w-4" aria-hidden />}
      onClick={() => {
        onNavigate?.();
        router.push(href);
      }}
    >
      {appLinkActionLabel(link ?? {}, t)}
    </Button>
  );
}
