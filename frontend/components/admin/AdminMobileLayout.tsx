/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { MobileNavigationLayout } from '@/components/layout/MobileNavigationLayout';

interface AdminMobileLayoutProps {
  children: React.ReactNode;
}

/**
 * Shared admin shell: sidebar + workspace strip use {@link useWorkspaceNavMenu} from the layout.
 */
export function AdminMobileLayout({ children }: AdminMobileLayoutProps) {
  return <MobileNavigationLayout>{children}</MobileNavigationLayout>;
}
