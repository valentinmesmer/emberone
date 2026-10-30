/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams, useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationById } from '@/lib/store';
import { canAccessDepartment, getOrganizationWorkspaceEntryPath } from '@/lib/utils/organization';

interface DepartmentLayoutProps {
  children: React.ReactNode;
}

export function DepartmentLayout({ children }: DepartmentLayoutProps) {
  const params = useParams();
  const router = useRouter();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const { data: organization, isLoading } = useOrganizationById(orgId);
  const { hasPermission, isLoading: permissionsLoading } = usePermissions();

  const departmentAccessDenied =
    orgId != null &&
    !Number.isNaN(orgId) &&
    !permissionsLoading &&
    !isLoading &&
    organization != null &&
    !canAccessDepartment(orgId, hasPermission);

  useEffect(() => {
    if (!departmentAccessDenied || orgId == null) return;
    router.replace(getOrganizationWorkspaceEntryPath(orgId));
  }, [departmentAccessDenied, orgId, router]);

  if (isLoading || permissionsLoading) {
    return <LoadingState variant="embedded" />;
  }

  if (departmentAccessDenied) {
    return <LoadingState variant="embedded" />;
  }

  return <>{children}</>;
}
