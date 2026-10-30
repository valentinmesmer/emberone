/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { DoorOpen, ShieldCheck, ShieldOff } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { ActivateManagementDialog } from '@/components/management/ActivateManagementDialog';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import { useEndManagementActivation } from '@/lib/store';
import type { ManagementActivationDto, ManagementTargetOrganizationDto } from '@/types/management';

interface ManagementTargetActionsProps {
  target: ManagementTargetOrganizationDto;
  /** The open activation of the current user for this target, if any. */
  activation: ManagementActivationDto | undefined;
}

/** Verwalten / Öffnen / Beenden for one target organization. Owns its dialogs. */
export function ManagementTargetActions({ target, activation }: ManagementTargetActionsProps) {
  const t = useTranslation();
  const router = useRouter();
  const endActivation = useEndManagementActivation();
  const [activateOpen, setActivateOpen] = useState(false);
  const [endOpen, setEndOpen] = useState(false);

  const openLabel = t('management.row.open');
  const activateLabel = t('management.row.activate');
  const endLabel = t('management.banner.end');

  const open = () => router.push(`/organizations/${target.organizationId}`);

  const confirmEnd = async () => {
    if (!activation) return;
    await endActivation.mutateAsync(activation.id);
    setEndOpen(false);
  };

  const controls = (
    <ActionsSection title={t('management.actions.title')}>
      {activation ? (
        <ListRow
          leading={<ActionRowIcon icon={DoorOpen} />}
          title={openLabel}
          subtitle={t('management.actions.openHint')}
          onClick={open}
        />
      ) : null}
      {activation ? (
        <ListRow
          leading={<ActionRowIcon icon={ShieldOff} />}
          title={endLabel}
          subtitle={t('management.actions.endHint')}
          onClick={() => setEndOpen(true)}
        />
      ) : (
        <ListRow
          leading={<ActionRowIcon icon={ShieldCheck} />}
          title={activateLabel}
          subtitle={t('management.actions.activateHint')}
          onClick={() => setActivateOpen(true)}
        />
      )}
    </ActionsSection>
  );

  return (
    <>
      {controls}
      {activateOpen ? (
        <ActivateManagementDialog
          key={target.organizationId}
          target={target}
          onClose={() => setActivateOpen(false)}
        />
      ) : null}
      <ConfirmDialog
        isOpen={endOpen}
        onClose={() => setEndOpen(false)}
        onConfirm={() => void confirmEnd()}
        message={t('management.banner.endConfirm')}
        variant="warning"
        isLoading={endActivation.isPending}
      />
    </>
  );
}
