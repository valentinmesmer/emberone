/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { useOrganizationGroupById } from '@/lib/store';
import { OrganizationGroupMembersPanel } from './OrganizationGroupMembersPanel';

interface OrganizationGroupDetailPanelProps {
  organizationId: number;
  groupId: number;
}

export function OrganizationGroupDetailPanel({
  organizationId,
  groupId,
}: OrganizationGroupDetailPanelProps) {
  const t = useTranslation();
  const { data: group, isLoading } = useOrganizationGroupById(groupId, organizationId);

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!group) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  return <OrganizationGroupMembersPanel organizationId={organizationId} groupId={groupId} />;
}
