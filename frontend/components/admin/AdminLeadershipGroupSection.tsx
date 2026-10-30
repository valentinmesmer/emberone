/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import {
  type OrganizationGroupMemberEntry,
  OrganizationGroupMembersManager,
} from '@/components/organization/OrganizationGroupMembersManager';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { LoadingState } from '@/components/ui/loading';
import { SectionActions, SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { CreateAdminLeadershipGroupMembershipRequest } from '@/lib/api/admin';
import { compareText } from '@/lib/i18n/locale';
import {
  useAdminLeadershipGroupMemberships,
  useAdminOrganizationUsers,
  useCreateAdminLeadershipGroupMembership,
  useUpdateAdminLeadershipGroupMembership,
} from '@/lib/store';
import {
  hasActiveLeadershipLeader,
  isActiveGroupMembership,
  isActiveMemberOfOrganizationGroup,
  translateBackendError,
  validateGroupMembershipAssignment,
} from '@/lib/utils';
import { getAdminLeadershipGroupMembershipDetailPath } from '@/lib/utils/adminPaths';
import { getOrganizationWallClockNow } from '@/lib/utils/date';
import type { OrganizationGroupDto } from '@/types/organizationGroup';
import type {
  CreateUserOrganizationGroupMembershipRequest,
  UpdateUserOrganizationGroupMembershipRequest,
  UserOrganizationGroupMembershipDto,
} from '@/types/user';

interface AdminLeadershipGroupSectionProps {
  tenantId: number;
  organizationId: number;
  onCreateUser: () => void;
  onChanged: () => void;
}

export function AdminLeadershipGroupSection({
  tenantId,
  organizationId,
  onCreateUser,
  onChanged,
}: AdminLeadershipGroupSectionProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();

  const { data, isLoading, isError, error } = useAdminLeadershipGroupMemberships(
    tenantId,
    organizationId,
  );
  const { data: orgUsers = [], isLoading: orgUsersLoading } = useAdminOrganizationUsers(
    tenantId,
    organizationId,
  );
  const createMembership = useCreateAdminLeadershipGroupMembership(tenantId, organizationId);
  const updateMembership = useUpdateAdminLeadershipGroupMembership(tenantId, organizationId);

  const leadershipGroupId = data?.leadershipGroupId ?? null;
  const memberships = useMemo(() => data?.memberships ?? [], [data?.memberships]);

  const [isAddOpen, setIsAddOpen] = useState(false);
  const [updatingMembership, setUpdatingMembership] =
    useState<UserOrganizationGroupMembershipDto | null>(null);

  const usersById = useMemo(() => new Map(orgUsers.map((user) => [user.id, user])), [orgUsers]);

  const members = useMemo((): OrganizationGroupMemberEntry[] => {
    return memberships
      .filter((membership) => isActiveGroupMembership(membership))
      .map((membership) => {
        const user = usersById.get(membership.userId);
        const name = user ? `${user.firstName} ${user.lastName}`.trim() : t('common.notAvailable');
        return {
          membershipId: membership.id,
          userId: membership.userId,
          name,
          membershipType: membership.membershipType,
          metaExtra: user?.username ? (
            <span className="text-xs text-[var(--text-secondary)]">({user.username})</span>
          ) : null,
        };
      })
      .sort((a, b) => compareText(a.name, b.name));
  }, [memberships, usersById, t]);

  const leadershipGroupStub = useMemo((): OrganizationGroupDto[] => {
    if (leadershipGroupId == null) return [];
    return [
      {
        id: leadershipGroupId,
        name: t('admin.orgTree.leadershipGroupLabel'),
        type: 'LEADERSHIP',
        organizationId,
        archived: false,
      },
    ];
  }, [leadershipGroupId, organizationId, t]);

  const hasAssignableUsers = useMemo(() => {
    if (leadershipGroupId == null) return false;
    return orgUsers.some(
      (user) =>
        !isActiveMemberOfOrganizationGroup(user.organizationGroupMemberships, leadershipGroupId),
    );
  }, [leadershipGroupId, orgUsers]);

  const busy = createMembership.isPending || updateMembership.isPending;
  const canCreateLeader = !isLoading && !hasActiveLeadershipLeader(memberships);
  const canManage = leadershipGroupId != null;
  const groupName = t('admin.orgTree.leadershipGroupLabel');

  const sectionLoading = isLoading || orgUsersLoading;

  useEffect(() => {
    if (!isError) return;
    showError(error instanceof Error ? error.message : t('admin.errors.orgDetailLoadFailed'));
  }, [isError, error, showError, t]);

  const findMembership = (membershipId: number): UserOrganizationGroupMembershipDto | null =>
    memberships.find((item) => item.id === membershipId) ?? null;

  const handleAssign = async (assignData: CreateUserOrganizationGroupMembershipRequest) => {
    const user = orgUsers.find((item) => item.id === assignData.userId);
    if (!user || leadershipGroupId == null) {
      showError(t('userManagement.errors.notFound'));
      throw new Error('USER_NOT_FOUND');
    }
    const isValid = validateGroupMembershipAssignment(
      user,
      orgUsers,
      leadershipGroupStub,
      leadershipGroupId,
      assignData.startDate,
      assignData.endDate,
      assignData.membershipType,
      undefined,
      t,
      showError,
      { allowLeadershipGroups: true },
    );
    if (!isValid) throw new Error('VALIDATION_ERROR');

    try {
      const payload: CreateAdminLeadershipGroupMembershipRequest = {
        userId: assignData.userId,
        startDate: assignData.startDate,
        endDate: assignData.endDate,
        membershipType: assignData.membershipType,
      };
      await createMembership.mutateAsync(payload);
      showSuccess(t('userManagement.groupMembership.success.assigned'));
      setIsAddOpen(false);
      onChanged();
    } catch (err) {
      if (err instanceof Error && err.message === 'VALIDATION_ERROR') {
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
    }
  };

  const handleUpdate = async (updateData: UpdateUserOrganizationGroupMembershipRequest) => {
    if (!updatingMembership || leadershipGroupId == null) return;
    const user = orgUsers.find((item) => item.id === updatingMembership.userId);
    if (!user) {
      showError(t('userManagement.errors.notFound'));
      throw new Error('USER_NOT_FOUND');
    }
    const isValid = validateGroupMembershipAssignment(
      user,
      orgUsers,
      leadershipGroupStub,
      leadershipGroupId,
      updateData.startDate,
      updateData.endDate,
      updateData.membershipType,
      updatingMembership.id,
      t,
      showError,
      { allowLeadershipGroups: true },
    );
    if (!isValid) throw new Error('VALIDATION_ERROR');

    try {
      await updateMembership.mutateAsync({
        membershipId: updatingMembership.id,
        data: updateData,
      });
      showSuccess(t('userManagement.groupMembership.success.updated'));
      setUpdatingMembership(null);
      onChanged();
    } catch (err) {
      if (err instanceof Error && err.message === 'VALIDATION_ERROR') {
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
    }
  };

  const handleEnd = async (membership: UserOrganizationGroupMembershipDto) => {
    try {
      await updateMembership.mutateAsync({
        membershipId: membership.id,
        data: {
          startDate: membership.startDate,
          endDate: getOrganizationWallClockNow(),
          membershipType: membership.membershipType,
        },
      });
      showSuccess(t('userManagement.groupMembership.success.ended'));
      onChanged();
    } catch (err) {
      showError(
        translateBackendError(
          err instanceof Error ? err.message : t('userManagement.groupMembership.errors.endFailed'),
          t,
        ),
      );
      throw err;
    }
  };

  const addMemberButton =
    canManage && hasAssignableUsers ? (
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

  const createLeaderButton = canCreateLeader ? (
    <Button
      type="button"
      variant="secondary"
      size="xs"
      fullWidth="responsive"
      onClick={onCreateUser}
    >
      {t('admin.leader.create')}
    </Button>
  ) : null;

  const headerAction =
    canManage && (addMemberButton || createLeaderButton) ? (
      <SectionActions layout="inline">
        {createLeaderButton}
        {addMemberButton}
      </SectionActions>
    ) : undefined;

  return (
    <Card className="p-3 sm:p-4">
      <SectionHeader
        className="mb-3"
        title={groupName}
        subtitle={t('admin.orgTree.detailLeaderIntro')}
        action={headerAction}
      />

      {leadershipGroupId == null && sectionLoading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : leadershipGroupId != null ? (
        <OrganizationGroupMembersManager
          organizationGroupId={leadershipGroupId}
          groupName={groupName}
          members={members}
          users={orgUsers}
          canManage={canManage}
          isLoading={sectionLoading}
          isBusy={busy}
          isAddOpen={isAddOpen}
          onAddOpenChange={setIsAddOpen}
          updatingMembership={updatingMembership}
          onUpdatingMembershipChange={setUpdatingMembership}
          getMembershipHref={(membershipId) =>
            getAdminLeadershipGroupMembershipDetailPath(tenantId, organizationId, membershipId)
          }
          findMembership={findMembership}
          onAssign={handleAssign}
          onUpdate={handleUpdate}
          onEnd={handleEnd}
        />
      ) : (
        <LoadingState variant="embedded" size="sm" />
      )}
    </Card>
  );
}
