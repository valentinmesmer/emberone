/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { ListLoadMore } from '@/components/ui/list-filter';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { createInitialListQueryState, toListQuery, useUrlSyncedListState } from '@/lib/list';
import { onboardingInvitationListDefinition } from '@/lib/list/onboardingInvitationList';
import {
  organizationMemberListDefinition,
  resolveOrganizationMemberListVisibility,
  toOrganizationMemberApiListQuery,
} from '@/lib/list/organizationMemberList';
import { getOrganizationUserPath } from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useCreateUserForOrganization,
  useOnboardingInvitationList,
  useOrganizationMemberList,
} from '@/lib/store';
import type { OnboardingInvitationStatus } from '@/types/onboardingInvitation';
import { PermissionTypeEnum } from '@/types/permission';
import type { CreateUserRequest } from '@/types/user';
import { CreateUserModal } from './CreateUserModal';

function invitationStatusTone(
  status: OnboardingInvitationStatus,
): 'accent' | 'warning' | 'neutral' {
  switch (status) {
    case 'OPEN':
      return 'accent';
    case 'SUBMITTED':
      return 'warning';
    default:
      return 'neutral';
  }
}

interface OrganizationUsersManagementProps {
  organizationId: number;
  organizationName: string;
}

export function OrganizationUsersManagement({
  organizationId,
  organizationName,
}: OrganizationUsersManagementProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.USER_WRITE);

  const memberListState = useUrlSyncedListState(organizationMemberListDefinition);
  const { showMembers, showInvitations, showSectionHeadings } =
    resolveOrganizationMemberListVisibility(memberListState.listQuery);

  const memberApiQuery = useMemo(
    () => toOrganizationMemberApiListQuery(memberListState.listQuery),
    [memberListState.listQuery],
  );
  const memberDebouncedSearch = memberListState.debouncedSearch;
  // Invitations share the member search.
  const invitationListQuery = useMemo(
    () =>
      toOrganizationMemberApiListQuery(
        toListQuery(onboardingInvitationListDefinition, {
          ...createInitialListQueryState(onboardingInvitationListDefinition),
          debouncedSearch: memberDebouncedSearch,
        }),
      ),
    [memberDebouncedSearch],
  );

  const {
    data: members = [],
    isPending: membersLoading,
    hasNextPage: membersHasNext,
    isFetchingNextPage: membersFetchingNext,
    fetchNextPage: fetchNextMembers,
  } = useOrganizationMemberList(showMembers ? organizationId : null, memberApiQuery);

  const {
    data: invitations = [],
    isPending: invitationsLoading,
    hasNextPage: invitationsHasNext,
    isFetchingNextPage: invitationsFetchingNext,
    fetchNextPage: fetchNextInvitations,
  } = useOnboardingInvitationList(showInvitations ? organizationId : null, invitationListQuery);

  const createUser = useCreateUserForOrganization();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleCreateUser = async (data: CreateUserRequest) => {
    try {
      setIsSubmitting(true);
      await createUser.mutateAsync({ organizationId, data });
      showSuccess(t('userManagement.success.userCreated'));
      setIsModalOpen(false);
    } catch (err) {
      const errorMessage =
        err instanceof Error ? err.message : t('userManagement.errors.createFailed');
      showError(errorMessage);
      throw err;
    } finally {
      setIsSubmitting(false);
    }
  };

  const visibleMemberCount = showMembers ? members.length : 0;
  const visibleInvitationCount = showInvitations ? invitations.length : 0;
  const totalItems = visibleMemberCount + visibleInvitationCount;
  const isPending = (showMembers && membersLoading) || (showInvitations && invitationsLoading);

  const emptyMessageKey =
    showInvitations && !showMembers ? 'userManagement.invitations.empty' : 'userManagement.noUsers';

  return (
    <>
      <PageHeader
        title={t('organization.hub.navigation.manageUsers')}
        description={organizationName}
        size="compact"
        action={
          canWrite ? (
            <Button
              variant="primary"
              size="xs"
              onClick={() => setIsModalOpen(true)}
              fullWidth="responsive"
            >
              {t('common.add')}
            </Button>
          ) : undefined
        }
      />

      <PaginatedListView
        definition={organizationMemberListDefinition}
        state={memberListState}
        totalItems={totalItems}
        isPending={isPending}
        emptyState={paginatedListEmptyCard(t(emptyMessageKey))}
        className="w-full space-y-4"
      >
        {showInvitations && invitations.length > 0 ? (
          <section>
            {showSectionHeadings ? (
              <h2 className="mb-1 text-sm font-medium text-muted-foreground">
                {t('userManagement.listFilter.invited')}
              </h2>
            ) : null}
            <ListContainer label={t('userManagement.listFilter.invited')}>
              {invitations.map((invitation) => (
                <ListRow
                  key={`invitation-${invitation.id}`}
                  title={`${invitation.firstName} ${invitation.lastName}`}
                  subtitle={`${t('userManagement.personalId')}: ${invitation.personalNumber}`}
                  badges={
                    <Badge tone={invitationStatusTone(invitation.status)} pill>
                      {t(`myArea.onboarding.status.${invitation.status}`)}
                    </Badge>
                  }
                  tone="warning"
                />
              ))}
            </ListContainer>
            <ListLoadMore
              hasNextPage={invitationsHasNext}
              isFetchingNextPage={invitationsFetchingNext}
              onLoadMore={() => fetchNextInvitations()}
            />
          </section>
        ) : null}

        {showMembers && members.length > 0 ? (
          <section>
            {showSectionHeadings ? (
              <h2 className="mb-1 text-sm font-medium text-muted-foreground">
                {t('userManagement.listFilter.active')}
              </h2>
            ) : null}
            <ListContainer label={t('userManagement.tabs.users')}>
              {members.map((user) => (
                <ListRow
                  key={`member-${user.id}`}
                  href={getOrganizationUserPath(organizationId, user.id)}
                  title={`${user.firstName} ${user.lastName}`}
                  subtitle={`${t('userManagement.personalId')}: ${user.personalNumber}`}
                />
              ))}
            </ListContainer>
            <ListLoadMore
              hasNextPage={membersHasNext}
              isFetchingNextPage={membersFetchingNext}
              onLoadMore={() => fetchNextMembers()}
            />
          </section>
        ) : null}
      </PaginatedListView>

      {canWrite ? (
        <CreateUserModal
          isOpen={isModalOpen}
          onClose={() => setIsModalOpen(false)}
          onCreate={handleCreateUser}
          isLoading={isSubmitting}
        />
      ) : null}
    </>
  );
}
