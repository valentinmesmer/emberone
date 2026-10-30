/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { LogOut } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import { getOrganizationUserWorkflowDetailPath } from '@/lib/navigation/userManagementSections';
import {
  useAssignableOrganizationGroups,
  useCreateUserOrganizationGroupMembership,
  useOpenOrganizationMembershipEndWorkflow,
  useUpdateUserOrganizationMembership,
  useUserMembershipOverview,
  useUsersForOrganization,
} from '@/lib/store';
import {
  filterVisibleOrganizationGroups,
  translateBackendError,
  validateGroupMembershipAssignment,
} from '@/lib/utils';
import { partitionOrganizationMemberships } from '@/lib/utils/membershipPartition';
import { resolveWorkflowErrorMessage } from '@/lib/utils/workflowErrors';
import type {
  CreateUserOrganizationGroupMembershipRequest,
  EndUserOrganizationMembershipRequest,
  UpdateUserOrganizationMembershipRequest,
} from '@/types/user';
import { AssignUserToGroupModal } from '../AssignUserToGroupModal';
import { UpdateOrganizationMembershipModal } from '../UpdateOrganizationMembershipModal';
import { EndOrganizationMembershipDialog } from './EndOrganizationMembershipDialog';
import { OrganizationMembershipCard } from './OrganizationMembershipCard';

interface UserMembershipsSectionProps {
  organizationId: number;
  userId: number;
  canWrite: boolean;
}

export function UserMembershipsSection({
  organizationId,
  userId,
  canWrite,
}: UserMembershipsSectionProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const { data: overview, isLoading } = useUserMembershipOverview(organizationId, userId);
  const { data: users = [] } = useUsersForOrganization(organizationId);
  const { data: assignableGroups = [] } = useAssignableOrganizationGroups(
    canWrite ? organizationId : null,
  );
  const createGroupMembership = useCreateUserOrganizationGroupMembership(organizationId);
  const updateOrgMembership = useUpdateUserOrganizationMembership(organizationId, userId);
  const openMembershipEndWorkflow = useOpenOrganizationMembershipEndWorkflow(
    organizationId,
    userId,
  );

  const [isAssignGroupOpen, setIsAssignGroupOpen] = useState(false);
  const [isEditOrgOpen, setIsEditOrgOpen] = useState(false);
  const [isEndOrgOpen, setIsEndOrgOpen] = useState(false);
  const [localBusy, setLocalBusy] = useState(false);

  const user = users.find((item) => item.id === userId);

  const visibleOrganizationGroups = useMemo(() => {
    if (!overview?.groups) return [];
    return filterVisibleOrganizationGroups(overview.groups.map((group) => group.group));
  }, [overview]);

  const activeOrgMembership = overview
    ? (partitionOrganizationMemberships(overview.organizationMemberships).active[0] ?? null)
    : null;

  const membershipBusy =
    localBusy ||
    createGroupMembership.isPending ||
    updateOrgMembership.isPending ||
    openMembershipEndWorkflow.isPending;

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  const hasMemberships =
    !!overview &&
    (overview.organizationMemberships.length > 0 || (overview.groups?.length ?? 0) > 0);

  if (!overview || !hasMemberships) {
    return <EmptyStateCard>{t('userManagement.memberships.noMemberships')}</EmptyStateCard>;
  }

  const handleAssignToGroup = async (data: CreateUserOrganizationGroupMembershipRequest) => {
    if (!user) return;
    try {
      setLocalBusy(true);
      const isValid = validateGroupMembershipAssignment(
        user,
        users,
        visibleOrganizationGroups,
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
      setIsAssignGroupOpen(false);
    } catch (err) {
      if (err instanceof Error && err.message === 'VALIDATION_ERROR') throw err;
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

  const handleUpdateOrgMembership = async (data: UpdateUserOrganizationMembershipRequest) => {
    if (!activeOrgMembership) return;
    try {
      setLocalBusy(true);
      await updateOrgMembership.mutateAsync({
        membershipId: activeOrgMembership.id,
        data,
      });
      showSuccess(t('userManagement.organizationMembership.success.updated'));
      setIsEditOrgOpen(false);
    } catch (err) {
      showError(
        translateBackendError(
          err instanceof Error
            ? err.message
            : t('userManagement.organizationMembership.errors.updateFailed'),
          t,
        ),
      );
      throw err;
    } finally {
      setLocalBusy(false);
    }
  };

  const handleEndOrgMembership = async (data: EndUserOrganizationMembershipRequest) => {
    if (!activeOrgMembership) return;
    try {
      setLocalBusy(true);
      const workflow = await openMembershipEndWorkflow.mutateAsync(data);
      showSuccess(t('userManagement.organizationMembership.success.endWorkflowOpened'));
      setIsEndOrgOpen(false);
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
      throw err;
    } finally {
      setLocalBusy(false);
    }
  };

  const canManageGroups = canWrite && !!user;
  const orgName = overview.organization.name ?? t('common.notAvailable');
  const canEndOrganizationMembership =
    canWrite && overview.canRequestOrganizationMembershipEnd === true && !!activeOrgMembership;

  return (
    <div className="space-y-4">
      <OrganizationMembershipCard
        section={overview}
        userId={userId}
        canWrite={canWrite}
        showAddGroup={canManageGroups && assignableGroups.length > 0}
        onAddGroupMembership={() => setIsAssignGroupOpen(true)}
        onEditOrganizationMembership={() => setIsEditOrgOpen(true)}
      />

      {canEndOrganizationMembership ? (
        <ActionsSection>
          <ListRow
            leading={<ActionRowIcon icon={LogOut} />}
            title={t('userManagement.organizationMembership.requestEnd')}
            subtitle={t('userManagement.organizationMembership.confirmEndHint')}
            onClick={() => setIsEndOrgOpen(true)}
          />
        </ActionsSection>
      ) : null}

      {canManageGroups ? (
        <AssignUserToGroupModal
          isOpen={isAssignGroupOpen}
          onClose={() => setIsAssignGroupOpen(false)}
          onAssign={handleAssignToGroup}
          userId={userId}
          organizationGroups={assignableGroups}
          isLoading={membershipBusy}
        />
      ) : null}

      {canWrite && activeOrgMembership && !overview.inLeadershipGroup ? (
        <UpdateOrganizationMembershipModal
          isOpen={isEditOrgOpen}
          onClose={() => setIsEditOrgOpen(false)}
          onUpdate={handleUpdateOrgMembership}
          membership={activeOrgMembership}
          isLoading={membershipBusy}
        />
      ) : null}
      {canEndOrganizationMembership ? (
        <EndOrganizationMembershipDialog
          isOpen={isEndOrgOpen}
          onClose={() => setIsEndOrgOpen(false)}
          onEnd={handleEndOrgMembership}
          membership={activeOrgMembership}
          organizationName={orgName}
          isLoading={membershipBusy}
        />
      ) : null}
    </div>
  );
}
