/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { ListControls, ListLoadMore, ListNoResults } from '@/components/ui/list-filter';
import { LoadingState } from '@/components/ui/loading';
import { PageHeader } from '@/components/ui/page-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { organizationGroupMemberListDefinition } from '@/lib/list/organizationGroupMemberList';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useCreateUserOrganizationGroupMembership,
  useOrganizationById,
  useOrganizationGroupById,
  useOrganizationGroupMemberList,
  useUpdateUserOrganizationGroupMembership,
  useUrlSyncedOrganizationGroupMemberListState,
  useUsersForOrganization,
} from '@/lib/store';
import {
  filterVisibleOrganizationGroups,
  getGroupDisplayName,
  isActiveGroupMembership,
  translateBackendError,
  validateGroupMembershipAssignment,
} from '@/lib/utils';
import { getOrganizationWallClockNow } from '@/lib/utils/date';
import { getOrganizationGroupMembershipDetailPathFromGroup } from '@/lib/utils/organization';
import type { OrganizationGroupMemberDto } from '@/types/organizationGroup';
import { PermissionTypeEnum } from '@/types/permission';
import type {
  CreateUserOrganizationGroupMembershipRequest,
  UpdateUserOrganizationGroupMembershipRequest,
  UserDto,
  UserOrganizationGroupMembershipDto,
} from '@/types/user';
import {
  type OrganizationGroupMemberEntry,
  OrganizationGroupMembersManager,
} from './OrganizationGroupMembersManager';

interface OrganizationGroupMembersPanelProps {
  organizationId: number;
  groupId: number;
}

function toGroupMemberEntries(
  apiMembers: OrganizationGroupMemberDto[],
  groupId: number,
  users?: UserDto[],
): OrganizationGroupMemberEntry[] {
  return apiMembers.map((member) => {
    const name = `${member.firstName} ${member.lastName}`.trim();
    if (users) {
      const user = users.find((item) => item.id === member.id);
      const membership = user?.organizationGroupMemberships?.find(
        (item) => item.organizationGroupId === groupId && isActiveGroupMembership(item),
      );
      if (membership) {
        return {
          membershipId: membership.id,
          userId: member.id,
          name,
          membershipType: membership.membershipType,
        };
      }
    }
    return {
      membershipId: null,
      userId: member.id,
      name,
      membershipType: null,
    };
  });
}

export function OrganizationGroupMembersPanel({
  organizationId,
  groupId,
}: OrganizationGroupMembersPanelProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();

  const canWrite = hasPermission(organizationId, PermissionTypeEnum.USER_WRITE);
  const canReadUsers = hasPermission(organizationId, PermissionTypeEnum.USER_READ);
  const canAddMembers = canWrite && canReadUsers;

  const listState = useUrlSyncedOrganizationGroupMemberListState();
  const {
    data: apiMembers = [],
    isPending: membersLoading,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationGroupMemberList(groupId, organizationId, listState.listQuery);

  const { data: users = [], isLoading: usersLoading } = useUsersForOrganization(
    canReadUsers ? organizationId : null,
  );
  const createGroupMembership = useCreateUserOrganizationGroupMembership(organizationId);
  const updateGroupMembership = useUpdateUserOrganizationGroupMembership(organizationId);

  const [isAddOpen, setIsAddOpen] = useState(false);
  const [updatingMembership, setUpdatingMembership] =
    useState<UserOrganizationGroupMembershipDto | null>(null);
  const [localBusy, setLocalBusy] = useState(false);

  const members = useMemo(
    () => toGroupMemberEntries(apiMembers, groupId, canReadUsers ? users : undefined),
    [apiMembers, canReadUsers, groupId, users],
  );

  const { data: organization } = useOrganizationById(organizationId);
  const { data: group } = useOrganizationGroupById(groupId, organizationId);
  const visibleGroups = useMemo(
    () => (group ? filterVisibleOrganizationGroups([group]) : []),
    [group],
  );

  const handleAssignToGroup = async (data: CreateUserOrganizationGroupMembershipRequest) => {
    try {
      setLocalBusy(true);
      const user = users.find((item) => item.id === data.userId);
      if (!user) {
        showError(t('userManagement.errors.notFound'));
        throw new Error('USER_NOT_FOUND');
      }
      const isValid = validateGroupMembershipAssignment(
        user,
        users,
        visibleGroups,
        data.organizationGroupId,
        data.startDate,
        data.endDate,
        data.membershipType,
        undefined,
        t,
        showError,
      );
      if (!isValid) throw new Error('VALIDATION_ERROR');
      await createGroupMembership.mutateAsync(data);
      showSuccess(t('userManagement.groupMembership.success.assigned'));
      setIsAddOpen(false);
    } catch (err) {
      if (
        err instanceof Error &&
        (err.message === 'VALIDATION_ERROR' || err.message === 'USER_NOT_FOUND')
      ) {
        throw err;
      }
      showError(
        translateBackendError(
          err instanceof Error
            ? err.message
            : t('userManagement.groupMembership.errors.assignFailed'),
          t,
        ),
      );
      throw err;
    } finally {
      setLocalBusy(false);
    }
  };

  const findMembership = (membershipId: number): UserOrganizationGroupMembershipDto | null => {
    for (const user of users) {
      const membership = user.organizationGroupMemberships?.find(
        (item) => item.id === membershipId,
      );
      if (membership) return membership;
    }
    return null;
  };

  const handleUpdateMembership = async (data: UpdateUserOrganizationGroupMembershipRequest) => {
    if (!updatingMembership) return;
    const user = users.find((item) => item.id === updatingMembership.userId);
    if (!user) {
      showError(t('userManagement.errors.notFound'));
      throw new Error('USER_NOT_FOUND');
    }
    try {
      setLocalBusy(true);
      const isValid = validateGroupMembershipAssignment(
        user,
        users,
        visibleGroups,
        updatingMembership.organizationGroupId,
        data.startDate,
        data.endDate,
        data.membershipType,
        updatingMembership.id,
        t,
        showError,
      );
      if (!isValid) throw new Error('VALIDATION_ERROR');
      await updateGroupMembership.mutateAsync({
        membershipId: updatingMembership.id,
        data,
      });
      showSuccess(t('userManagement.groupMembership.success.updated'));
      setUpdatingMembership(null);
    } catch (err) {
      if (
        err instanceof Error &&
        (err.message === 'VALIDATION_ERROR' || err.message === 'USER_NOT_FOUND')
      ) {
        throw err;
      }
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

  const handleEndMembership = async (membership: UserOrganizationGroupMembershipDto) => {
    try {
      setLocalBusy(true);
      await updateGroupMembership.mutateAsync({
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
      throw err;
    } finally {
      setLocalBusy(false);
    }
  };

  const loading = membersLoading || (canReadUsers && usersLoading);
  const membershipBusy =
    localBusy || createGroupMembership.isPending || updateGroupMembership.isPending;
  const isLeadershipGroup = group?.type === 'LEADERSHIP';
  // Leadership group of a department is only editable in the admin area.
  const canManageMembers = canAddMembers && !isLeadershipGroup;

  const addMemberButton = canManageMembers ? (
    <Button
      variant="primary"
      size="xs"
      onClick={() => setIsAddOpen(true)}
      fullWidth="responsive"
      icon={<Plus className="h-3.5 w-3.5" aria-hidden />}
    >
      {t('common.add')}
    </Button>
  ) : undefined;

  if (!group && !loading) {
    return null;
  }

  const groupName = group ? getGroupDisplayName(group) : '';

  return (
    <>
      {group ? (
        <PageHeader
          title={groupName}
          description={
            canWrite && isLeadershipGroup
              ? t('organizationManagement.organizationGroups.leadershipReadOnly')
              : organization?.name
          }
          size="compact"
          action={addMemberButton}
        />
      ) : null}

      <div className="mt-1 w-full space-y-3 sm:mt-0">
        <ListControls definition={organizationGroupMemberListDefinition} state={listState} />
        {loading ? <LoadingState variant="embedded" size="sm" /> : null}
        {!loading && members.length === 0 && listState.hasActiveFilters ? (
          <ListNoResults onReset={listState.reset} />
        ) : null}
        {!loading && !(members.length === 0 && listState.hasActiveFilters) && group ? (
          <OrganizationGroupMembersManager
            organizationGroupId={groupId}
            groupName={getGroupDisplayName(group)}
            members={members}
            users={users}
            canManage={canManageMembers}
            isBusy={membershipBusy}
            isAddOpen={isAddOpen}
            onAddOpenChange={setIsAddOpen}
            updatingMembership={updatingMembership}
            onUpdatingMembershipChange={setUpdatingMembership}
            getMembershipHref={(membershipId) =>
              getOrganizationGroupMembershipDetailPathFromGroup(
                organizationId,
                groupId,
                membershipId,
              )
            }
            findMembership={findMembership}
            onAssign={handleAssignToGroup}
            onUpdate={handleUpdateMembership}
            onEnd={handleEndMembership}
          />
        ) : null}
        {!loading ? (
          <ListLoadMore
            hasNextPage={hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            onLoadMore={() => void fetchNextPage()}
          />
        ) : null}
      </div>
    </>
  );
}
