/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { OrgUserDocumentDetailView } from '@/components/organization/user-management/OrgUserDocumentDetailView';
import { UserManagementSectionGate } from '@/components/organization/user-management/UserManagementSectionGate';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function OrgUserDocumentDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const userId = params?.userId ? parseInt(params.userId as string, 10) : null;
  const documentId = params?.documentId ? parseInt(params.documentId as string, 10) : null;

  if (
    !orgId ||
    !userId ||
    !documentId ||
    Number.isNaN(orgId) ||
    Number.isNaN(userId) ||
    Number.isNaN(documentId)
  ) {
    return <NotFoundState message={t('myArea.documents.error.notFound')} />;
  }

  return (
    <UserManagementSectionGate organizationId={orgId} sectionId="documents">
      {({ canWrite }) => (
        <OrgUserDocumentDetailView
          organizationId={orgId}
          userId={userId}
          documentId={documentId}
          canWrite={canWrite}
        />
      )}
    </UserManagementSectionGate>
  );
}
