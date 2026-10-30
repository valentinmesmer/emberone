/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { CalendarX2, Pencil } from 'lucide-react';
import { type ReactNode, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { getMembershipTypeLabel } from '@/lib/utils';
import type { GroupMembershipType } from '@/types/enums/groupMembershipType';
import type {
  CreateUserOrganizationGroupMembershipRequest,
  UpdateUserOrganizationGroupMembershipRequest,
  UserDto,
  UserOrganizationGroupMembershipDto,
} from '@/types/user';
import { AddMemberToGroupModal } from './AddMemberToGroupModal';
import { UpdateMembershipModal } from './UpdateMembershipModal';

export interface OrganizationGroupMemberEntry {
  membershipId: number | null;
  userId: number;
  name: string;
  membershipType: GroupMembershipType | null;
  metaExtra?: ReactNode;
}

export interface OrganizationGroupMembersManagerProps {
  organizationGroupId: number;
  groupName: string;
  members: OrganizationGroupMemberEntry[];
  users: UserDto[];
  canManage: boolean;
  isLoading?: boolean;
  isBusy?: boolean;
  isAddOpen: boolean;
  onAddOpenChange: (open: boolean) => void;
  updatingMembership: UserOrganizationGroupMembershipDto | null;
  onUpdatingMembershipChange: (membership: UserOrganizationGroupMembershipDto | null) => void;
  getMembershipHref?: (membershipId: number) => string | undefined;
  findMembership: (membershipId: number) => UserOrganizationGroupMembershipDto | null;
  onAssign: (data: CreateUserOrganizationGroupMembershipRequest) => Promise<void>;
  onUpdate: (data: UpdateUserOrganizationGroupMembershipRequest) => Promise<void>;
  onEnd: (membership: UserOrganizationGroupMembershipDto) => Promise<void>;
}

/**
 * Shared group membership list + add/edit modals used by org view and admin leadership.
 */
export function OrganizationGroupMembersManager({
  organizationGroupId,
  groupName,
  members,
  users,
  canManage,
  isLoading = false,
  isBusy = false,
  isAddOpen,
  onAddOpenChange,
  updatingMembership,
  onUpdatingMembershipChange,
  getMembershipHref,
  findMembership,
  onAssign,
  onUpdate,
  onEnd,
}: OrganizationGroupMembersManagerProps) {
  const t = useTranslation();
  const [endingMembership, setEndingMembership] =
    useState<UserOrganizationGroupMembershipDto | null>(null);

  const handleConfirmEnd = async () => {
    if (!endingMembership) return;
    await onEnd(endingMembership);
    setEndingMembership(null);
  };

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  return (
    <>
      {members.length === 0 ? (
        <EmptyState>{t('organizationManagement.organizationGroups.noMembers')}</EmptyState>
      ) : (
        <ListContainer label={t('organizationManagement.organizationGroups.members')}>
          {members.map((member) => (
            <ListRow
              key={`${organizationGroupId}-${member.userId}-${member.membershipId ?? 'user'}`}
              compact
              title={member.name}
              href={
                member.membershipId != null ? getMembershipHref?.(member.membershipId) : undefined
              }
              badges={
                member.membershipType ? (
                  <Badge tone="accent">{getMembershipTypeLabel(member.membershipType, t)}</Badge>
                ) : undefined
              }
              meta={
                member.metaExtra ? (
                  <div className="mt-0.5 text-xs text-[var(--text-secondary)]">
                    {member.metaExtra}
                  </div>
                ) : undefined
              }
              actions={
                canManage && member.membershipId != null ? (
                  <div className="flex items-center gap-1">
                    <IconButton
                      variant="action"
                      icon={<Pencil className="h-4 w-4" aria-hidden />}
                      onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        if (member.membershipId == null) return;
                        const membership = findMembership(member.membershipId);
                        if (membership) onUpdatingMembershipChange(membership);
                      }}
                      aria-label={t('common.edit')}
                      title={t('common.edit')}
                    />
                    <IconButton
                      variant="action"
                      icon={<CalendarX2 className="h-4 w-4" aria-hidden />}
                      onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        if (member.membershipId == null) return;
                        const membership = findMembership(member.membershipId);
                        if (membership) setEndingMembership(membership);
                      }}
                      aria-label={t('userManagement.groupMembership.end')}
                      title={t('userManagement.groupMembership.end')}
                    />
                  </div>
                ) : undefined
              }
            />
          ))}
        </ListContainer>
      )}

      {canManage ? (
        <AddMemberToGroupModal
          isOpen={isAddOpen}
          onClose={() => onAddOpenChange(false)}
          onAssign={onAssign}
          organizationGroupId={organizationGroupId}
          users={users}
          isLoading={isBusy}
        />
      ) : null}

      {canManage ? (
        <UpdateMembershipModal
          isOpen={updatingMembership !== null}
          onClose={() => onUpdatingMembershipChange(null)}
          onUpdate={onUpdate}
          membership={updatingMembership}
          isLoading={isBusy}
        />
      ) : null}

      <ConfirmDialog
        isOpen={endingMembership !== null}
        onClose={() => setEndingMembership(null)}
        onConfirm={handleConfirmEnd}
        title={t('userManagement.groupMembership.confirmEndTitle')}
        message={t('userManagement.groupMembership.confirmEndMessage', { groupName })}
        confirmText={t('userManagement.groupMembership.end')}
        isLoading={isBusy}
      />
    </>
  );
}
