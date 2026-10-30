/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { Card } from '@/components/ui/card';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { getAbsenceReasonLabelKey } from '@/lib/my-area/absenceReasonOptions';
import { useOrganizationAbsence, useOrganizationById, useUsersForOrganization } from '@/lib/store';
import { formatDateLong } from '@/lib/utils';
import type { UserAbsenceDto } from '@/types/absence';
import type { UserDto } from '@/types/user';

export interface OrgUserAbsenceDetailViewProps {
  organizationId: number;
  userId: number;
  absenceId: number;
}

function resolveUserName(
  userById: Map<number, UserDto>,
  absence: UserAbsenceDto,
  t: (key: string, params?: Record<string, string | number>) => string,
) {
  const u = userById.get(absence.userId);
  if (!u) {
    return t('userManagement.absences.userUnknown', { id: String(absence.userId) });
  }
  return `${u.firstName} ${u.lastName}`.trim();
}

export function OrgUserAbsenceDetailView({
  organizationId,
  userId,
  absenceId,
}: OrgUserAbsenceDetailViewProps) {
  const t = useTranslation();
  const { data: absence, isLoading: loadingAbsence } = useOrganizationAbsence(
    organizationId,
    absenceId,
  );
  const { data: users = [], isLoading: loadingUsers } = useUsersForOrganization(organizationId);
  const { data: organization, isLoading: loadingOrganization } =
    useOrganizationById(organizationId);

  const userById = useMemo(() => new Map(users.map((u) => [u.id, u] as const)), [users]);

  const loading = loadingAbsence || loadingUsers || loadingOrganization;

  const belongsToUser = absence?.userId === userId;
  const belongsToOrg = Boolean(
    absence?.organizationIds?.length && absence.organizationIds.includes(organizationId),
  );

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!absence || !belongsToUser || !belongsToOrg || !organization) {
    return <NotFoundState message={t('myArea.absence.error.notFound')} />;
  }

  const organizationName = organization.name;
  const userName = resolveUserName(userById, absence, t);
  const organizationsLabel = (absence.organizationIds ?? [])
    .map((id) =>
      id === organizationId ? organizationName : `${t('myArea.absence.orgUnknown')} (${id})`,
    )
    .join(', ');

  return (
    <div className="w-full">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader className="mb-2" title={userName} subtitle={organizationName} />

        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FieldDisplay
              label={t('myArea.absence.fields.startDate')}
              value={formatDateLong(absence.startDate)}
            />
            <FieldDisplay
              label={t('myArea.absence.fields.endDate')}
              value={formatDateLong(absence.endDate)}
            />
          </div>
          <FieldDisplay
            label={t('myArea.absence.fields.reason')}
            value={t(getAbsenceReasonLabelKey(absence.reason))}
          />
          <FieldDisplay
            label={t('myArea.absence.fields.organizations')}
            value={organizationsLabel}
          />
          <FieldDisplay
            label={t('myArea.absence.fields.comment')}
            value={
              absence.comment && absence.comment.trim() !== ''
                ? absence.comment
                : t('common.notAvailable')
            }
          />
        </div>
      </Card>
    </div>
  );
}
