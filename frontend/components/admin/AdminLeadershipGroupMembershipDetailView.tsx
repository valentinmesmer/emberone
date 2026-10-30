/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useLayoutEffect, useMemo } from 'react';
import { AdminLeadershipOnboardingSection } from '@/components/admin/AdminLeadershipOnboardingSection';
import { GroupMembershipDetailContent } from '@/components/organization/GroupMembershipDetailContent';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import {
  useAdminLeadershipGroupMemberships,
  useUpdateAdminLeadershipGroupMembership,
} from '@/lib/store';
import { isActiveGroupMembership, translateBackendError } from '@/lib/utils';
import { getAdminOrganizationDetailPath } from '@/lib/utils/adminPaths';
import { getOrganizationWallClockNow } from '@/lib/utils/date';
import { sortMembershipsByRecency } from '@/lib/utils/membershipPartition';
import type {
  UpdateUserOrganizationGroupMembershipRequest,
  UserOrganizationGroupMembershipDto,
} from '@/types/user';

interface AdminLeadershipGroupMembershipDetailViewProps {
  tenantId: number;
  organizationId: number;
  membershipId: number;
}

export function AdminLeadershipGroupMembershipDetailView({
  tenantId,
  organizationId,
  membershipId,
}: AdminLeadershipGroupMembershipDetailViewProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const setBackHref = useSetBackHref();

  const backHref = useMemo(
    () => getAdminOrganizationDetailPath(tenantId, organizationId),
    [tenantId, organizationId],
  );

  useLayoutEffect(() => {
    setBackHref(backHref);
    return () => setBackHref(null);
  }, [backHref, setBackHref]);

  const { data, isLoading, isError } = useAdminLeadershipGroupMemberships(tenantId, organizationId);
  const updateMembership = useUpdateAdminLeadershipGroupMembership(tenantId, organizationId);

  const viewed = useMemo(
    () => data?.memberships.find((item) => item.id === membershipId) ?? null,
    [data?.memberships, membershipId],
  );

  const membershipsForUser = useMemo(() => {
    if (!viewed || !data) return [];
    return data.memberships.filter((item) => item.userId === viewed.userId);
  }, [data, viewed]);

  const activeMembership = useMemo(
    () => membershipsForUser.find((item) => isActiveGroupMembership(item)) ?? null,
    [membershipsForUser],
  );

  const history = useMemo(
    () => sortMembershipsByRecency(membershipsForUser.filter((item) => item !== activeMembership)),
    [membershipsForUser, activeMembership],
  );

  const busy = updateMembership.isPending;
  const groupName = t('admin.orgTree.leadershipGroupLabel');

  const handleUpdate = async (
    membership: UserOrganizationGroupMembershipDto,
    updateData: UpdateUserOrganizationGroupMembershipRequest,
  ) => {
    try {
      await updateMembership.mutateAsync({
        membershipId: membership.id,
        data: updateData,
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
    } catch (err) {
      showError(
        translateBackendError(
          err instanceof Error ? err.message : t('userManagement.groupMembership.errors.endFailed'),
          t,
        ),
      );
    }
  };

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (isError || !viewed) {
    return <NotFoundState message={t('userManagement.errors.notFound')} />;
  }

  return (
    <div className="w-full space-y-4">
      <GroupMembershipDetailContent
        groupName={groupName}
        activeMembership={activeMembership}
        history={history}
        canEditActive={!!activeMembership}
        canManageHistory
        busy={busy}
        onUpdate={handleUpdate}
        onEnd={handleEnd}
      />

      <AdminLeadershipOnboardingSection
        tenantId={tenantId}
        organizationId={organizationId}
        membershipId={membershipId}
        userId={viewed.userId}
        canStart={!!activeMembership}
      />
    </div>
  );
}
