/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { List, ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import {
  DEPARTMENT_SECTION,
  getAppointmentDetailPath,
  getDepartmentSectionPath,
} from '@/lib/department/sections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useMyAppointmentResponses,
  useMyOrganizations,
  useUIStore,
  useUpcomingAppointments,
} from '@/lib/store';
import { getAppointmentResponseTone } from '@/lib/utils/appointmentResponse';
import { formatDateShort, formatTime } from '@/lib/utils/date';
import { AppointmentResponseMode } from '@/types/appointment';
import { PermissionTypeEnum } from '@/types/permission';
import {
  HOME_DASHBOARD_PREVIEW_LIMIT,
  HOME_DASHBOARD_ROW_CLASSNAME,
  HomeDashboardListPanel,
} from './HomeDashboardListPanel';

export function HomeAppointmentsCard() {
  const t = useTranslation();
  const { hasPermission } = usePermissions();
  const { selectedOrganizationId } = useUIStore();
  const { data: organizations = [] } = useMyOrganizations();

  const {
    data: upcomingAppointmentsPage,
    isPending,
    isFetching,
  } = useUpcomingAppointments(HOME_DASHBOARD_PREVIEW_LIMIT);

  const appointments = useMemo(
    () => upcomingAppointmentsPage?.content ?? [],
    [upcomingAppointmentsPage?.content],
  );

  const appointmentIds = useMemo(() => appointments.map((a) => a.id), [appointments]);
  const { data: myResponses = [] } = useMyAppointmentResponses(appointmentIds);
  const myResponseByAppointmentId = useMemo(
    () => new Map(myResponses.map((r) => [r.appointmentId, r])),
    [myResponses],
  );

  const isLoading = isPending && isFetching;
  /** Badge = unread total across orgs; list = next upcoming preview. */
  const count = isLoading ? null : (upcomingAppointmentsPage?.unreadCount ?? 0);

  const listPath = useMemo(() => {
    const orgId =
      selectedOrganizationId ??
      organizations.find((o) => o.organization)?.organization?.id ??
      appointments[0]?.organizationId;
    if (orgId == null) return null;
    if (!hasPermission(orgId, PermissionTypeEnum.APPOINTMENT_READ)) return null;
    return getDepartmentSectionPath(orgId, DEPARTMENT_SECTION.APPOINTMENTS);
  }, [selectedOrganizationId, organizations, appointments, hasPermission]);

  return (
    <HomeDashboardListPanel
      title={t('home.dashboard.appointments.title')}
      count={count}
      href={listPath}
      isLoading={isLoading}
      loadingLabel={t('common.loading')}
      emptyLabel={t('home.dashboard.appointments.empty')}
      isEmpty={appointments.length === 0}
      listLabel={t('home.dashboard.appointments.title')}
    >
      <List
        variant="flush"
        rowSeparator="inset"
        label={t('home.dashboard.appointments.title')}
        className="flex-1"
      >
        {appointments.map((item) => {
          const canOpenAppointment = hasPermission(
            item.organizationId,
            PermissionTypeEnum.APPOINTMENT_READ,
          );
          const tone = getAppointmentResponseTone(item, myResponseByAppointmentId.get(item.id));
          return (
            <ListRow
              key={item.id}
              href={
                canOpenAppointment
                  ? getAppointmentDetailPath(item.organizationId, item.id)
                  : undefined
              }
              title={item.name}
              subtitle={`${formatDateShort(item.start)}, ${formatTime(item.start)}`}
              tone={tone}
              accent={item.responseMode !== AppointmentResponseMode.NONE}
              compact
              className={HOME_DASHBOARD_ROW_CLASSNAME}
            />
          );
        })}
      </List>
    </HomeDashboardListPanel>
  );
}
