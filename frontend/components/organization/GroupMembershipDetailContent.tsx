/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { LogOut, Pencil } from 'lucide-react';
import { useState } from 'react';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import { formatDateTime, getMembershipTypeLabel } from '@/lib/utils';
import { formatMembershipPeriod } from '@/lib/utils/membershipPartition';
import type {
  UpdateUserOrganizationGroupMembershipRequest,
  UserOrganizationGroupMembershipDto,
} from '@/types/user';
import { UpdateMembershipModal } from './UpdateMembershipModal';

interface GroupMembershipDetailContentProps {
  groupName: string;
  groupArchived?: boolean;
  activeMembership: UserOrganizationGroupMembershipDto | null;
  history: UserOrganizationGroupMembershipDto[];
  canEditActive: boolean;
  canManageHistory: boolean;
  readOnlyHint?: string;
  busy: boolean;
  onUpdate: (
    membership: UserOrganizationGroupMembershipDto,
    data: UpdateUserOrganizationGroupMembershipRequest,
  ) => Promise<void>;
  onEnd: (membership: UserOrganizationGroupMembershipDto) => Promise<void>;
}

export function GroupMembershipDetailContent({
  groupName,
  groupArchived = false,
  activeMembership,
  history,
  canEditActive,
  canManageHistory,
  readOnlyHint,
  busy,
  onUpdate,
  onEnd,
}: GroupMembershipDetailContentProps) {
  const t = useTranslation();
  const [editTarget, setEditTarget] = useState<UserOrganizationGroupMembershipDto | null>(null);
  const [isEndConfirmOpen, setIsEndConfirmOpen] = useState(false);

  const handleUpdate = async (data: UpdateUserOrganizationGroupMembershipRequest) => {
    if (!editTarget) return;
    await onUpdate(editTarget, data);
    setEditTarget(null);
  };

  const handleEnd = async () => {
    if (!activeMembership) return;
    await onEnd(activeMembership);
    setIsEndConfirmOpen(false);
  };

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          eyebrow={
            <span className="font-semibold uppercase tracking-wide">
              {t('userManagement.groupMembership.detailTitle')}
            </span>
          }
          title={groupName}
          badges={
            <>
              {activeMembership ? (
                <Badge tone="accent">
                  {getMembershipTypeLabel(activeMembership.membershipType, t)}
                </Badge>
              ) : null}
              {groupArchived ? (
                <Badge tone="neutral">{t('userManagement.groupMembership.archivedBadge')}</Badge>
              ) : null}
            </>
          }
          subtitle={
            <>
              {!activeMembership ? (
                <p>{t('userManagement.groupMembership.noActiveMembership')}</p>
              ) : null}
              {readOnlyHint ? <p>{readOnlyHint}</p> : null}
            </>
          }
          actions={
            canEditActive && activeMembership ? (
              <DetailCardAction
                label={t('common.edit')}
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={() => setEditTarget(activeMembership)}
              />
            ) : undefined
          }
        />

        {activeMembership ? (
          <div className="grid grid-cols-1 gap-3 border-t border-[var(--border-color)] pt-4 text-sm lg:grid-cols-2">
            <FieldDisplay
              label={t('userManagement.groupMembership.startDate')}
              value={formatDateTime(activeMembership.startDate)}
            />
            <FieldDisplay
              label={t('userManagement.groupMembership.endDate')}
              value={
                activeMembership.endDate ? formatDateTime(activeMembership.endDate) : undefined
              }
              fallback={t('userManagement.memberships.ongoing')}
            />
          </div>
        ) : null}
      </Card>

      {canEditActive && activeMembership ? (
        <ActionsSection>
          <ListRow
            leading={<ActionRowIcon icon={LogOut} />}
            title={t('userManagement.groupMembership.end')}
            subtitle={t('userManagement.groupMembership.endActionHint')}
            onClick={() => setIsEndConfirmOpen(true)}
          />
        </ActionsSection>
      ) : null}

      {history.length > 0 ? (
        <section>
          <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('userManagement.memberships.historyTitle')}
          </h2>
          <ListContainer label={t('userManagement.memberships.historyTitle')} rowSeparator="inset">
            {history.map((m) => (
              <ListRow
                key={m.id}
                accent={false}
                title={getMembershipTypeLabel(m.membershipType, t)}
                badges={
                  <Badge tone="neutral">{t('userManagement.memberships.historicalTag')}</Badge>
                }
                meta={
                  <span className="text-xs text-[var(--text-secondary)]">
                    {formatMembershipPeriod(m.startDate, m.endDate)}
                  </span>
                }
                actions={
                  canManageHistory ? (
                    <IconButton
                      variant="action"
                      icon={<Pencil className="h-4 w-4" aria-hidden />}
                      onClick={() => setEditTarget(m)}
                      aria-label={t('common.edit')}
                      title={t('common.edit')}
                    />
                  ) : undefined
                }
              />
            ))}
          </ListContainer>
        </section>
      ) : null}

      {editTarget ? (
        <UpdateMembershipModal
          isOpen={!!editTarget}
          onClose={() => setEditTarget(null)}
          onUpdate={handleUpdate}
          membership={editTarget}
          isLoading={busy}
        />
      ) : null}

      <ConfirmDialog
        isOpen={isEndConfirmOpen}
        onClose={() => setIsEndConfirmOpen(false)}
        onConfirm={handleEnd}
        title={t('userManagement.groupMembership.confirmEndTitle')}
        message={t('userManagement.groupMembership.confirmEndMessage', { groupName })}
        confirmText={t('userManagement.groupMembership.end')}
        isLoading={busy}
      />
    </div>
  );
}
