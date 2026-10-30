/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useRouter } from 'next/navigation';
import { Brand } from '@/components/layout/Brand';
import { NotificationBellButton } from '@/components/notification/NotificationBellButton';

/** Desktop sidebar chrome: app branding + notifications (replaces the global header on `xl+`). */
export function SidebarBrand() {
  const router = useRouter();

  return (
    <div className="flex shrink-0 items-center justify-between gap-3 border-b border-[var(--border-color)] py-4 pl-0.5">
      <Brand variant="sidebar" showSubtitle onClick={() => router.push('/')} />
      <NotificationBellButton />
    </div>
  );
}
