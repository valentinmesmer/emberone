/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { SectionEditAction, SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useUpdatePersonalData, useUsersForOrganization } from '@/lib/store';
import { formatDateLong } from '@/lib/utils';
import { Salutation } from '@/types/enums/salutation';
import type { UpdatePersonalDataRequest } from '@/types/user';
import { EditUserModal } from '../EditUserModal';

interface UserMasterDataSectionProps {
  organizationId: number;
  userId: number;
  canWrite: boolean;
}

export function UserMasterDataSection({
  organizationId,
  userId,
  canWrite,
}: UserMasterDataSectionProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: users = [] } = useUsersForOrganization(organizationId);
  const updateUser = useUpdatePersonalData();

  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const user = users.find((item) => item.id === userId);
  if (!user) {
    return null;
  }

  const getSalutationLabel = (salutation?: Salutation) => {
    if (!salutation) return t('common.notAvailable');
    return salutation === Salutation.MR
      ? t('setup.user.salutationMr')
      : t('setup.user.salutationMrs');
  };

  const handleUpdateUser = async (data: UpdatePersonalDataRequest) => {
    try {
      setIsSubmitting(true);
      await updateUser.mutateAsync({ organizationId, userId, data });
      showSuccess(t('userManagement.success.userUpdated'));
      setIsEditOpen(false);
    } catch (err) {
      showError(err instanceof Error ? err.message : t('userManagement.errors.updateFailed'));
      throw err;
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <Card className="p-3 sm:p-4">
        <SectionHeader
          className="mb-2"
          title={t('userManagement.sections.masterData')}
          action={
            canWrite ? (
              <SectionEditAction
                label={t('userManagement.editUser')}
                onClick={() => setIsEditOpen(true)}
              />
            ) : undefined
          }
        />

        <div className="space-y-4 text-sm">
          <div className="space-y-1.5">
            <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
              {user.salutation ? (
                <Badge tone="accent" className="shrink-0">
                  {getSalutationLabel(user.salutation)}
                </Badge>
              ) : null}
              <span className="text-sm text-[var(--text-primary)]">
                {t('userManagement.personalId')}: {user.personalNumber}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-3 border-t border-[var(--border-color)] pt-4 text-sm lg:grid-cols-2">
            <div>
              <span className="mr-2 font-semibold text-[var(--text-secondary)]">
                {t('userManagement.userIdentifier')}:
              </span>
              <span className="font-mono text-[var(--text-primary)]">
                {user.username?.trim() ? user.username : t('common.notAvailable')}
              </span>
            </div>
            <div>
              <span className="mr-2 font-semibold text-[var(--text-secondary)]">
                {t('setup.user.firstName')}:
              </span>
              <span className="text-[var(--text-primary)]">{user.firstName}</span>
            </div>
            <div>
              <span className="mr-2 font-semibold text-[var(--text-secondary)]">
                {t('setup.user.lastName')}:
              </span>
              <span className="text-[var(--text-primary)]">{user.lastName}</span>
            </div>
            {user.dateOfBirth ? (
              <div>
                <span className="mr-2 font-semibold text-[var(--text-secondary)]">
                  {t('setup.user.dateOfBirth')}:
                </span>
                <span className="text-[var(--text-primary)]">
                  {formatDateLong(user.dateOfBirth)}
                </span>
              </div>
            ) : null}
          </div>
        </div>
      </Card>

      {canWrite ? (
        <EditUserModal
          isOpen={isEditOpen}
          onClose={() => setIsEditOpen(false)}
          onUpdate={handleUpdateUser}
          user={user}
          isLoading={isSubmitting}
        />
      ) : null}
    </>
  );
}
