/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { OrganizationEmergencyContactDetailView } from '@/components/organization/OrganizationEmergencyContactDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationById } from '@/lib/store';
import { PermissionTypeEnum } from '@/types/permission';

export default function OrganizationEmergencyContactDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const { hasPermission, isLoading: permLoading } = usePermissions();

  const orgId = params?.id as string;
  const contactIdParam = params?.contactId as string;
  const orgIdNum = orgId ? parseInt(orgId, 10) : null;
  const contactIdNum = contactIdParam ? parseInt(contactIdParam, 10) : null;

  const canAccess = Boolean(
    orgIdNum != null &&
      !Number.isNaN(orgIdNum) &&
      contactIdNum != null &&
      !Number.isNaN(contactIdNum) &&
      hasPermission(orgIdNum, PermissionTypeEnum.EMERGENCY_CONTACT_READ),
  );

  const { data: organization, isLoading: orgLoading } = useOrganizationById(
    orgIdNum && !permLoading && canAccess ? orgIdNum : null,
  );

  const loading = permLoading || orgLoading;

  if (!orgIdNum || !contactIdNum || Number.isNaN(orgIdNum) || Number.isNaN(contactIdNum)) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!permLoading && !canAccess) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  if (!organization) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <OrganizationEmergencyContactDetailView
      organizationId={organization.id}
      organizationName={organization.name}
      contactId={contactIdNum}
    />
  );
}
