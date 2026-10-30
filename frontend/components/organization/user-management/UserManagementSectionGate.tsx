/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import {
  USER_MANAGEMENT_SECTIONS,
  type UserManagementSectionId,
} from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';

interface UserManagementSectionGateProps {
  organizationId: number;
  sectionId: UserManagementSectionId;
  children: (access: { canWrite: boolean }) => React.ReactNode;
}

export function UserManagementSectionGate({
  organizationId,
  sectionId,
  children,
}: UserManagementSectionGateProps) {
  const t = useTranslation();
  const { hasPermission, isLoading } = usePermissions();
  const section = USER_MANAGEMENT_SECTIONS.find((item) => item.id === sectionId);

  if (!section) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!hasPermission(organizationId, section.readPermission)) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  const canWrite = hasPermission(organizationId, section.writePermission);
  return <>{children({ canWrite })}</>;
}
