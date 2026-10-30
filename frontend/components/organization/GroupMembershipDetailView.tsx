/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useLayoutEffect, useMemo, useState } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useOrganizationGroupById,
  useUpdateUserOrganizationGroupMembership,
  useUserMembershipOverview,
  useUsersForOrganization,
} from '@/lib/store';
import {
  getGroupDisplayName,
  isActiveGroupMembership,
  isLeadershipOrganizationGroup,
  translateBackendError,
} from '@/lib/utils';
import { getOrganizationWallClockNow } from '@/lib/utils/date';
import { sortMembershipsByRecency } from '@/lib/utils/membershipPartition';
import type { OrganizationGroupMembershipDetailReferrer } from '@/lib/utils/organization';
import { getOrganizationGroupMembershipBackPath } from '@/lib/utils/organization';
import type { OrganizationGroupType } from '@/types/organizationGroup';
import { PermissionTypeEnum } from '@/types/permission';
import type {
  UpdateUserOrganizationGroupMembershipRequest,
  UserOrganizationGroupMembershipDto,
} from '@/types/user';
import { GroupMembershipDetailContent } from './GroupMembershipDetailContent';

function findGroupMembershipInUsers(
  users: { organizationGroupMemberships?: UserOrganizationGroupMembershipDto[] }[],
  membershipId: number,
): UserOrganizationGroupMembershipDto | null {
  for (const user of users) {
    const membership = user.organizationGroupMemberships?.find((item) => item.id === membershipId);
    if (membership) return membership;
  }
  return null;
}

function findGroupMembershipsForUser(
  users: { organizationGroupMemberships?: UserOrganizationGroupMembershipDto[] }[],
  membershipId: number,
): UserOrganizationGroupMembershipDto[] {
  for (const user of users) {
    const memberships = user.organizationGroupMemberships ?? [];
    if (memberships.some((item) => item.id === membershipId)) {
      return memberships;
    }
  }
  return [];
}

function findGroupMembershipFromOverview(
  overview: ReturnType<typeof useUserMembershipOverview>['data'],
  membershipId: number,
): {
  activeMembership: UserOrganizationGroupMembershipDto | null;
  groupName: string;
  groupArchived: boolean;
  canWrite: boolean;
  groupType: OrganizationGroupType;
  history: UserOrganizationGroupMembershipDto[];
} | null {
  if (!overview) return null;
  for (const groupSection of overview.groups) {
    const found = groupSection.memberships.find((m) => m.id === membershipId);
    if (found) {
      const activeMembership =
        groupSection.memberships.find((m) => isActiveGroupMembership(m)) ?? null;
      return {
        activeMembership,
        groupName: getGroupDisplayName(groupSection.group),
        groupArchived: groupSection.group.archived ?? false,
        canWrite: overview.canWrite,
        groupType: groupSection.group.type,
        history: sortMembershipsByRecency(
          groupSection.memberships.filter((m) => m !== activeMembership),
        ),
      };
    }
  }
  return null;
}

interface GroupMembershipDetailViewProps {
  organizationId: number;
  membershipId: number;
  referrer?: OrganizationGroupMembershipDetailReferrer;
}

export function GroupMembershipDetailView({
  organizationId,
  membershipId,
  referrer,
}: GroupMembershipDetailViewProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const setBackHref = useSetBackHref();
  const { hasPermission, isLoading: permLoading } = usePermissions();

  const userId = referrer?.kind === 'user' ? referrer.userId : null;
  const groupId = referrer?.kind === 'group' ? referrer.groupId : null;
  const canReadUsers = hasPermission(organizationId, PermissionTypeEnum.USER_READ);
  const canWriteOrg = hasPermission(organizationId, PermissionTypeEnum.USER_WRITE);

  const { data: overview, isLoading: overviewLoading } = useUserMembershipOverview(
    organizationId,
    userId ?? undefined,
  );
  const { data: users = [], isLoading: usersLoading } = useUsersForOrganization(
    groupId && canReadUsers ? organizationId : null,
  );
  const { data: group, isLoading: groupLoading } = useOrganizationGroupById(
    groupId,
    organizationId,
  );

  const resolved = useMemo(() => {
    if (userId) {
      return overviewLoading ? null : findGroupMembershipFromOverview(overview, membershipId);
    }
    if (groupId && group) {
      const membership = findGroupMembershipInUsers(users, membershipId);
      if (!membership || membership.organizationGroupId !== groupId) return null;
      const allInGroup = findGroupMembershipsForUser(users, membershipId).filter(
        (m) => m.organizationGroupId === groupId,
      );
      const activeMembership = allInGroup.find((m) => isActiveGroupMembership(m)) ?? null;
      return {
        activeMembership,
        groupName: getGroupDisplayName(group),
        groupArchived: group.archived ?? false,
        canWrite: canWriteOrg,
        groupType: group.type,
        history: sortMembershipsByRecency(allInGroup.filter((m) => m !== activeMembership)),
      };
    }
    return null;
  }, [userId, groupId, overview, overviewLoading, membershipId, users, group, canWriteOrg]);

  const isLoading =
    !referrer ||
    (userId
      ? overviewLoading
      : groupId
        ? permLoading || !canReadUsers || usersLoading || groupLoading
        : true);

  const updateMembership = useUpdateUserOrganizationGroupMembership(organizationId);

  const [localBusy, setLocalBusy] = useState(false);

  const backHref = useMemo(
    () => getOrganizationGroupMembershipBackPath(organizationId, referrer),
    [organizationId, referrer],
  );

  useLayoutEffect(() => {
    setBackHref(backHref);
    return () => setBackHref(null);
  }, [backHref, setBackHref]);

  const busy = localBusy || updateMembership.isPending;

  const handleUpdate = async (
    membership: UserOrganizationGroupMembershipDto,
    data: UpdateUserOrganizationGroupMembershipRequest,
  ) => {
    try {
      setLocalBusy(true);
      await updateMembership.mutateAsync({
        membershipId: membership.id,
        data,
      });
      showSuccess(t('userManagement.groupMembership.success.updated'));
    } catch (err) {
      showError(
        translateBackendError(
          err instanceof Error
            ? err.message
            : t('userManagement.groupMembership.errors.updateFailed'),
          t,
        ),
      );
      throw err;
    } finally {
      setLocalBusy(false);
    }
  };

  const handleEnd = async (membership: UserOrganizationGroupMembershipDto) => {
    try {
      setLocalBusy(true);
      await updateMembership.mutateAsync({
        membershipId: membership.id,
        data: {
          startDate: membership.startDate,
          endDate: getOrganizationWallClockNow(),
          membershipType: membership.membershipType,
        },
      });
      showSuccess(t('userManagement.groupMembership.success.ended'));
    } catch (err) {
      showError(
        translateBackendError(
          err instanceof Error ? err.message : t('userManagement.groupMembership.errors.endFailed'),
          t,
        ),
      );
    } finally {
      setLocalBusy(false);
    }
  };

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!resolved) {
    return <NotFoundState message={t('userManagement.errors.notFound')} />;
  }

  const { activeMembership, groupName, groupArchived, canWrite, groupType, history } = resolved;
  const isActive = !!activeMembership;
  const isLeadership = isLeadershipOrganizationGroup({ type: groupType });
  const canEditActive = canWrite && isActive && !isLeadership;
  const canManageHistory = canWrite && !isLeadership;

  return (
    <GroupMembershipDetailContent
      groupName={groupName}
      groupArchived={groupArchived}
      activeMembership={activeMembership}
      history={history}
      canEditActive={canEditActive}
      canManageHistory={canManageHistory}
      readOnlyHint={
        canWrite && !canEditActive && isActive
          ? t('organizationManagement.organizationGroups.leadershipReadOnly')
          : undefined
      }
      busy={busy}
      onUpdate={handleUpdate}
      onEnd={handleEnd}
    />
  );
}
