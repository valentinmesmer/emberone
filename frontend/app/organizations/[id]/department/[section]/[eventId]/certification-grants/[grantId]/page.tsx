/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { EventCertificationGrantDetailView } from '@/components/event/EventCertificationGrantDetailView';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import {
  type DepartmentEventSection,
  isDepartmentEventSection,
  isDepartmentSection,
} from '@/lib/department/sections';

function EventCertificationGrantDetailPageContent() {
  const t = useTranslation();
  const params = useParams();

  const orgId = params?.id ? parseInt(params.id as string, 10) : null;
  const sectionParam = params?.section as string | undefined;
  const eventId = params?.eventId ? parseInt(params.eventId as string, 10) : null;
  const grantId = params?.grantId ? parseInt(params.grantId as string, 10) : null;

  if (
    !orgId ||
    Number.isNaN(orgId) ||
    !sectionParam ||
    !isDepartmentSection(sectionParam) ||
    !isDepartmentEventSection(sectionParam) ||
    eventId == null ||
    Number.isNaN(eventId) ||
    grantId == null ||
    Number.isNaN(grantId)
  ) {
    return <NotFoundState message={t('organization.errors.notFound')} />;
  }

  return (
    <EventCertificationGrantDetailView
      organizationId={orgId}
      departmentSection={sectionParam as DepartmentEventSection}
      eventId={eventId}
      grantId={grantId}
    />
  );
}

export default function EventCertificationGrantDetailPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <EventCertificationGrantDetailPageContent />
    </Suspense>
  );
}
