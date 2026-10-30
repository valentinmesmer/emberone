/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { UserManagementSectionView } from '@/components/organization/user-management/UserManagementSectionView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import {
  USER_MANAGEMENT_SECTIONS,
  type UserManagementSectionId,
} from '@/lib/navigation/userManagementSections';

function parseSectionId(value: string | undefined): UserManagementSectionId | null {
  if (!value) return null;
  return USER_MANAGEMENT_SECTIONS.some((section) => section.id === value)
    ? (value as UserManagementSectionId)
    : null;
}

export default function UserManagementSectionPage() {
  const t = useTranslation();
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const userId = params?.userId ? parseInt(params.userId as string, 10) : null;
  const sectionId = parseSectionId(params?.section as string | undefined);

  if (!orgId || !userId || Number.isNaN(orgId) || Number.isNaN(userId) || !sectionId) {
    return <NotFoundState message={t('userManagement.errors.notFound')} />;
  }

  return <UserManagementSectionView organizationId={orgId} userId={userId} sectionId={sectionId} />;
}
