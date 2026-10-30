/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams, usePathname } from 'next/navigation';
import { useMemo } from 'react';
import { HubOrganizationSwitch, HubPage } from '@/components/hub';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import {
  buildFlatOrgNavItems,
  getOrganizationHubGroupTitle,
  ORG_HUB_GROUP_ORDER,
} from '@/lib/navigation/organizationNavItems';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationMembership } from '@/hooks/useWorkspaceOrganizations';
import { useAppointmentUnreadCount, useOrganizationById } from '@/lib/store';

export default function OrganizationHubPage() {
  const t = useTranslation();
  const params = useParams();
  const pathname = usePathname();
  const parsedId = params?.id ? Number.parseInt(params.id as string, 10) : NaN;
  const orgId = Number.isFinite(parsedId) ? parsedId : null;

  const { hasPermission, isLoading: permissionsLoading } = usePermissions();
  const { data: organization, isLoading: orgLoading } = useOrganizationById(orgId);
  // Unread state is personal: a managed organization must not call the membership-gated endpoint.
  const { isMember } = useOrganizationMembership(orgId);
  const { data: unreadAppointments } = useAppointmentUnreadCount(isMember ? orgId : null);

  const hubGroups = useMemo(
    () =>
      ORG_HUB_GROUP_ORDER.map((id) => ({
        id,
        title: getOrganizationHubGroupTitle(id, t),
      })),
    [t],
  );

  const unreadAppointmentCount = unreadAppointments?.count;
  const appointmentsTileId = orgId != null ? `org-${orgId}-appointments` : null;

  const navTiles = useMemo(() => {
    if (orgId == null || !organization) return [];
    return buildFlatOrgNavItems(
      { id: orgId, name: organization.name },
      pathname,
      t,
      hasPermission,
      'hub',
    ).map((tile) => ({
      id: tile.id,
      href: tile.href,
      icon: tile.icon,
      title: tile.label,
      groupId: tile.hubGroupId,
      badge:
        tile.id === appointmentsTileId &&
        unreadAppointmentCount != null &&
        unreadAppointmentCount > 0
          ? unreadAppointmentCount
          : undefined,
    }));
  }, [orgId, organization, pathname, t, hasPermission, appointmentsTileId, unreadAppointmentCount]);

  if (permissionsLoading || orgLoading) {
    return <LoadingState variant="embedded" />;
  }

  if (!organization) {
    return <NotFoundState message={t('organization.errors.notFound')} />;
  }

  return (
    <HubPage
      header={{
        eyebrow: t('organization.hub.eyebrow'),
        title: organization.name,
        subtitle: organization.organizationCode
          ? `${t('organization.hub.organizationCode')}: ${organization.organizationCode}`
          : undefined,
        action: <HubOrganizationSwitch currentOrganizationId={organization.id} />,
        density: 'compact',
        organizationMenuAnchor: true,
      }}
      tiles={navTiles}
      groups={hubGroups}
      fallbackGroupTitle={t('common.hub.ungrouped')}
      emptyContent={
        <EmptyStateCard>
          <p className="mb-2 font-medium text-[var(--text-primary)]">
            {t('organization.hub.noAccessTitle')}
          </p>
          <p>{t('organization.hub.noAccess')}</p>
        </EmptyStateCard>
      }
    />
  );
}
