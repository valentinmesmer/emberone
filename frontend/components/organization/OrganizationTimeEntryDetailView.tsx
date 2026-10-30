/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { useOrganizationTimeEntry, useUsersForOrganization } from '@/lib/store';
import { formatDateTime, formatDurationBetween } from '@/lib/utils/date';
import type { UserTimeEntryDto } from '@/types/timeEntry';
import type { UserDto } from '@/types/user';

export interface OrganizationTimeEntryDetailViewProps {
  organizationId: number;
  organizationName: string;
  entryId: number;
}

function resolveUserName(
  userById: Map<number, UserDto>,
  entry: UserTimeEntryDto,
  t: (key: string, params?: Record<string, string | number>) => string,
) {
  const u = userById.get(entry.userId);
  if (!u) {
    return t('userManagement.timeEntries.userUnknown', { id: String(entry.userId) });
  }
  return `${u.firstName} ${u.lastName}`.trim();
}

export function OrganizationTimeEntryDetailView({
  organizationId,
  organizationName,
  entryId,
}: OrganizationTimeEntryDetailViewProps) {
  const t = useTranslation();
  const { data: entry, isLoading: loadingEntry } = useOrganizationTimeEntry(
    organizationId,
    entryId,
  );
  const { data: users = [], isLoading: loadingUsers } = useUsersForOrganization(organizationId);

  const userById = useMemo(() => new Map(users.map((u) => [u.id, u] as const)), [users]);

  const loading = loadingEntry || loadingUsers;

  const belongsToOrg = entry?.organizationId === organizationId;
  const isOpen = Boolean(entry && !entry.endTime);

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!entry || !belongsToOrg) {
    return <NotFoundState message={t('myArea.timeEntries.error.notFound')} />;
  }

  const userName = resolveUserName(userById, entry, t);
  const groupName = entry.organizationGroupName?.trim() || t('myArea.timeEntries.noGroup');
  const durationLabel = entry.endTime
    ? formatDurationBetween(entry.startTime, entry.endTime)
    : t('common.notAvailable');

  return (
    <div className="w-full">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          title={userName}
          subtitle={organizationName}
          badges={
            isOpen ? (
              <Badge tone="accent" pill>
                {t('myArea.timeEntries.running')}
              </Badge>
            ) : undefined
          }
        />

        <div className="space-y-4">
          <FieldDisplay label={t('userManagement.userLabel')} value={userName} />
          <FieldDisplay
            label={t('myArea.timeEntries.fields.organization')}
            value={entry.organizationName}
          />
          <FieldDisplay label={t('myArea.timeEntries.fields.group')} value={groupName} />
          <FieldDisplay
            label={t('myArea.timeEntries.fields.startTime')}
            value={formatDateTime(entry.startTime)}
          />
          <FieldDisplay
            label={t('myArea.timeEntries.fields.endTime')}
            value={entry.endTime ? formatDateTime(entry.endTime) : t('common.notAvailable')}
          />
          <FieldDisplay label={t('myArea.timeEntries.fields.duration')} value={durationLabel} />
          <FieldDisplay
            label={t('myArea.timeEntries.fields.description')}
            value={
              entry.description && entry.description.trim() !== ''
                ? entry.description
                : t('common.notAvailable')
            }
          />
        </div>
      </Card>
    </div>
  );
}
