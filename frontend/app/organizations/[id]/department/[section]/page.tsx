/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { AppointmentListView } from '@/components/appointment';
import { EVENT_SECTION_VIEWS } from '@/components/event/eventSectionViews';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import {
  getReadPermissionForSection,
  isDepartmentEventSection,
  isDepartmentSection,
} from '@/lib/department/sections';
import { usePermissions } from '@/lib/permission/PermissionContext';

export default function DepartmentSectionPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission } = usePermissions();

  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const sectionParam = params?.section as string | undefined;

  if (!orgId || !sectionParam || !isDepartmentSection(sectionParam)) {
    return <NotFoundState message={t('organization.errors.notFound')} />;
  }

  const readPermission = getReadPermissionForSection(sectionParam);
  if (!readPermission || !hasPermission(orgId, readPermission)) {
    return <EmptyStateCard>{t('organization.departmentView.workspaceEmpty')}</EmptyStateCard>;
  }

  if (sectionParam === 'appointments') {
    return (
      <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
        <AppointmentListView organizationId={orgId} />
      </Suspense>
    );
  }

  if (!isDepartmentEventSection(sectionParam)) {
    return <NotFoundState message={t('organization.errors.notFound')} />;
  }

  const EventView = EVENT_SECTION_VIEWS[sectionParam];
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <EventView organizationId={orgId} />
    </Suspense>
  );
}
