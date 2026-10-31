/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useParams } from 'next/navigation';
import { UserDocumentDetailView } from '@/components/my-area/documents/UserDocumentDetailView';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';

export default function UserDocumentDetailPage() {
  const t = useTranslation();
  const params = useParams();
  const documentId = params?.documentId ? parseInt(params.documentId as string, 10) : null;

  if (!documentId || Number.isNaN(documentId)) {
    return <NotFoundState message={t('myArea.documents.error.notFound')} />;
  }

  return <UserDocumentDetailView documentId={documentId} />;
}
