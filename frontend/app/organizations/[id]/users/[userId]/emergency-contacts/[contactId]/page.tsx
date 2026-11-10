/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useParams } from 'next/navigation';
import { OrganizationEmergencyContactDetailView } from '@/components/organization/OrganizationEmergencyContactDetailView';
import { UserManagementSectionGate } from '@/components/organization/user-management/UserManagementSectionGate';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { useOrganizationById } from '@/lib/store';

function OrgUserEmergencyContactDetailPageContent({
  orgId,
  userId,
  contactId,
}: {
  orgId: number;
  userId: number;
  contactId: number;
}) {
  const t = useTranslation();
  const { data: organization, isLoading } = useOrganizationById(orgId);

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!organization) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return (
    <OrganizationEmergencyContactDetailView
      organizationId={organization.id}
      organizationName={organization.name}
      contactId={contactId}
      userId={userId}
    />
  );
}

export default function OrgUserEmergencyContactDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const userId = params?.userId ? parseInt(params.userId as string, 10) : null;
  const contactId = params?.contactId ? parseInt(params.contactId as string, 10) : null;

  if (
    !orgId ||
    !userId ||
    !contactId ||
    Number.isNaN(orgId) ||
    Number.isNaN(userId) ||
    Number.isNaN(contactId)
  ) {
    return <NotFoundState message={t('myArea.emergencyContact.error.notFound')} />;
  }

  return (
    <UserManagementSectionGate organizationId={orgId} sectionId="emergency-contacts">
      {() => (
        <OrgUserEmergencyContactDetailPageContent
          orgId={orgId}
          userId={userId}
          contactId={contactId}
        />
      )}
    </UserManagementSectionGate>
  );
}
