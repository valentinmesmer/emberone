/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { useEffect } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import {
  getDepartmentSectionPath,
  getFirstAccessibleSection,
  getSectionForEventType,
} from '@/lib/department/sections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationById } from '@/lib/store';
import { canAccessDepartment, getOrganizationWorkspaceEntryPath } from '@/lib/utils/organization';
import { EventType } from '@/types/event';

export default function DepartmentIndexPage() {
  const t = useTranslation();
  const params = useParams();
  const router = useRouter();
  const searchParams = useSearchParams();
  const { hasPermission, isLoading: permissionsLoading } = usePermissions();

  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const { isLoading } = useOrganizationById(orgId);

  useEffect(() => {
    if (!orgId || isLoading || permissionsLoading) return;

    if (!canAccessDepartment(orgId, hasPermission)) {
      router.replace(getOrganizationWorkspaceEntryPath(orgId));
      return;
    }

    const eventTypeParam = searchParams.get('eventType');
    if (eventTypeParam && Object.values(EventType).includes(eventTypeParam as EventType)) {
      const section = getSectionForEventType(eventTypeParam as EventType);
      router.replace(getDepartmentSectionPath(orgId, section));
      return;
    }

    const section = getFirstAccessibleSection(orgId, hasPermission);
    if (section) {
      router.replace(getDepartmentSectionPath(orgId, section));
    }
  }, [orgId, isLoading, permissionsLoading, searchParams, hasPermission, router]);

  if (!orgId || isLoading || permissionsLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!canAccessDepartment(orgId, hasPermission)) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  const eventTypeParam = searchParams.get('eventType');
  if (eventTypeParam && Object.values(EventType).includes(eventTypeParam as EventType)) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  const firstSection = getFirstAccessibleSection(orgId, hasPermission);
  if (firstSection) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  return <EmptyStateCard>{t('organization.departmentView.workspaceEmpty')}</EmptyStateCard>;
}
