/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { ManagementActivationBanner } from '@/components/management/ManagementActivationBanner';
import { MobileNavigationLayout } from '@/components/layout/MobileNavigationLayout';

export default function OrganizationIdLayout({ children }: { children: React.ReactNode }) {
  const params = useParams();
  const organizationId = Number.parseInt(String(params?.id ?? ''), 10);

  return (
    <MobileNavigationLayout
      leadingContent={
        Number.isFinite(organizationId) ? (
          <ManagementActivationBanner organizationId={organizationId} />
        ) : null
      }
    >
      {children}
    </MobileNavigationLayout>
  );
}
