/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ArrowRightLeft, CalendarX2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { type ReactNode, useMemo, useState } from 'react';
import { HubPage } from '@/components/hub';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState, NoticeCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import {
  getOrganizationUserSectionPath,
  getOrganizationUserWorkflowDetailPath,
  getUserManagementHubGroupTitle,
  getVisibleUserManagementSections,
  USER_MANAGEMENT_HUB_GROUP_ORDER,
} from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useOpenOrganizationMembershipEndWorkflow,
  useUserMembershipOverview,
  useUsersForOrganization,
} from '@/lib/store';
import { translateBackendError } from '@/lib/utils';
import { partitionOrganizationMemberships } from '@/lib/utils/membershipPartition';
import { resolveWorkflowErrorMessage } from '@/lib/utils/workflowErrors';
import { PermissionTypeEnum } from '@/types/permission';
import type { EndUserOrganizationMembershipRequest } from '@/types/user';
import { EndOrganizationMembershipDialog } from './EndOrganizationMembershipDialog';
import { OrganizationUserMainOrganizationChangeModal } from './OrganizationUserMainOrganizationChangeModal';
import { USER_MANAGEMENT_SECTION_ICONS } from './userManagementSectionIcons';

interface UserManagementHubViewProps {
  organizationId: number;
  userId: number;
}

export function UserManagementHubView({ organizationId, userId }: UserManagementHubViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const { hasPermission, isLoading: permLoading } = usePermissions();
  const { data: users = [], isLoading: usersLoading } = useUsersForOrganization(organizationId);
  const { data: overview, isLoading: overviewLoading } = useUserMembershipOverview(
    organizationId,
    userId,
  );
  const openMembershipEndWorkflow = useOpenOrganizationMembershipEndWorkflow(
    organizationId,
    userId,
  );

  const [endMembershipOpen, setEndMembershipOpen] = useState(false);
  const [mainChangeOpen, setMainChangeOpen] = useState(false);

  const user = users.find((item) => item.id === userId);

  const activeOrgMembership = overview
    ? (partitionOrganizationMemberships(overview.organizationMemberships).active[0] ?? null)
    : null;

  const canUserWrite = hasPermission(organizationId, PermissionTypeEnum.USER_WRITE);
  const canRequestMembershipEnd =
    canUserWrite && overview?.canRequestOrganizationMembershipEnd === true;
  const canRequestMainChange =
    canUserWrite && overview?.canRequestOrganizationUserMainOrganizationChange === true;

  const visibleSections = useMemo(
    () => getVisibleUserManagementSections(organizationId, hasPermission),
    [organizationId, hasPermission],
  );

  const hubGroups = useMemo(
    () =>
      USER_MANAGEMENT_HUB_GROUP_ORDER.map((id) => ({
        id,
        title: getUserManagementHubGroupTitle(id, t),
      })),
    [t],
  );

  const actionTiles = useMemo(() => {
    const tiles: Array<{
      id: string;
      title: string;
      icon: ReactNode;
      onClick: () => void;
      groupId?: string;
    }> = [];
    if (canRequestMembershipEnd) {
      tiles.push({
        id: 'membership-end',
        title: t('userManagement.organizationMembership.requestEnd'),
        icon: <CalendarX2 className="h-4 w-4" aria-hidden />,
        onClick: () => setEndMembershipOpen(true),
      });
    }
    if (canRequestMainChange) {
      tiles.push({
        id: 'main-organization-change',
        title: t('userManagement.mainOrganizationChange.action'),
        icon: <ArrowRightLeft className="h-4 w-4" aria-hidden />,
        onClick: () => setMainChangeOpen(true),
      });
    }
    return tiles;
  }, [canRequestMembershipEnd, canRequestMainChange, t]);

  const tiles = useMemo(() => {
    const sectionTiles = visibleSections.map((section) => ({
      id: section.id,
      href: getOrganizationUserSectionPath(organizationId, userId, section.id),
      title: t(section.labelKey),
      icon: USER_MANAGEMENT_SECTION_ICONS[section.id],
      groupId: section.hubGroupId,
    }));
    const withGroup = actionTiles.map((tile) => ({
      ...tile,
      groupId: 'actions',
    }));
    return [...sectionTiles, ...withGroup];
  }, [visibleSections, organizationId, userId, t, actionTiles]);

  if (permLoading || usersLoading || overviewLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!user) {
    return <NotFoundState message={t('userManagement.errors.notFound')} />;
  }

  if (visibleSections.length === 0) {
    return <NoticeCard>{t('organizationManagement.errors.noPermission')}</NoticeCard>;
  }

  const displayName = `${user.firstName} ${user.lastName}`.trim();
  const orgName = overview?.organization.name ?? t('common.notAvailable');

  const handleEndMembership = async (data: EndUserOrganizationMembershipRequest) => {
    if (!activeOrgMembership) return;
    try {
      const workflow = await openMembershipEndWorkflow.mutateAsync(data);
      showSuccess(t('userManagement.organizationMembership.success.endWorkflowOpened'));
      setEndMembershipOpen(false);
      router.push(getOrganizationUserWorkflowDetailPath(organizationId, userId, workflow.id));
    } catch (err) {
      const workflowMessage =
        err instanceof ApiRequestError
          ? resolveWorkflowErrorMessage(
              err,
              t,
              t('userManagement.organizationMembership.errors.endFailed'),
            )
          : null;
      showError(
        workflowMessage ??
          translateBackendError(
            err instanceof Error
              ? err.message
              : t('userManagement.organizationMembership.errors.endFailed'),
            t,
          ),
      );
    }
  };

  const hasActionTiles = actionTiles.length > 0;

  return (
    <>
      <HubPage
        header={{
          eyebrow: t('organization.hub.navigation.manageUsers'),
          title: displayName,
          subtitle: `${t('userManagement.personalId')}: ${user.personalNumber}`,
          density: 'compact',
        }}
        tiles={tiles}
        groups={
          hasActionTiles
            ? [...hubGroups, { id: 'actions', title: t('admin.userDetail.actionsSection') }]
            : hubGroups
        }
        fallbackGroupTitle={t('common.hub.ungrouped')}
      />
      {canRequestMembershipEnd && activeOrgMembership ? (
        <EndOrganizationMembershipDialog
          isOpen={endMembershipOpen}
          onClose={() => setEndMembershipOpen(false)}
          onEnd={handleEndMembership}
          membership={activeOrgMembership}
          organizationName={orgName}
          isLoading={openMembershipEndWorkflow.isPending}
        />
      ) : null}
      {canRequestMainChange && overview ? (
        <OrganizationUserMainOrganizationChangeModal
          isOpen={mainChangeOpen}
          onClose={() => setMainChangeOpen(false)}
          organizationId={organizationId}
          userId={userId}
          onWorkflowOpened={(workflowId) => {
            router.push(getOrganizationUserWorkflowDetailPath(organizationId, userId, workflowId));
          }}
        />
      ) : null}
    </>
  );
}
