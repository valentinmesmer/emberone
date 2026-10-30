/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useTranslation } from '@/hooks/useTranslation';
import { onManagementActivationExpired } from '@/lib/management/activationExpiry';
import { formatManagementUntil } from '@/lib/management/workspaceOrganizations';
import {
  managementKeys,
  useEndManagementActivation,
  useMyManagementActivations,
} from '@/lib/store';

const COUNTDOWN_TICK_MS = 15_000;

/**
 * Shown on a managed organization. Counts down to `validUntil`. The expiry event from the backend
 * brings the user back to the management list of the managing organization; if the event does not
 * arrive, the activation dropping out of the list does the same.
 */
export function ManagementActivationBanner({ organizationId }: { organizationId: number }) {
  const t = useTranslation();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { data: activations = [] } = useMyManagementActivations();
  const endActivation = useEndManagementActivation();
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [confirmOpen, setConfirmOpen] = useState(false);
  const lastActivationRef = useRef<{
    targetOrganizationId: number;
    managingOrganizationId: number;
  } | null>(null);

  const activation = activations.find(
    (entry) =>
      entry.targetOrganizationId === organizationId && Date.parse(entry.validUntil) > nowMs,
  );
  const validUntilMs = activation ? Date.parse(activation.validUntil) : null;
  const managingOrganizationId = activation?.managingOrganizationId ?? null;

  useEffect(() => {
    if (validUntilMs == null) return;
    const tick = setInterval(() => setNowMs(Date.now()), COUNTDOWN_TICK_MS);
    return () => clearInterval(tick);
  }, [validUntilMs]);

  useEffect(() => {
    if (managingOrganizationId == null) return;
    lastActivationRef.current = {
      targetOrganizationId: organizationId,
      managingOrganizationId,
    };
  }, [organizationId, managingOrganizationId]);

  const leaveToManagement = useCallback(
    (targetManagingOrganizationId: number) => {
      void queryClient.invalidateQueries({ queryKey: managementKeys.all });
      router.replace(`/organizations/${targetManagingOrganizationId}/management`);
    },
    [queryClient, router],
  );

  useEffect(
    () =>
      onManagementActivationExpired((expired) => {
        if (expired.targetOrganizationId === organizationId) {
          leaveToManagement(expired.managingOrganizationId);
        }
      }),
    [leaveToManagement, organizationId],
  );

  useEffect(() => {
    const last = lastActivationRef.current;
    if (managingOrganizationId != null || last?.targetOrganizationId !== organizationId) return;
    lastActivationRef.current = null;
    leaveToManagement(last.managingOrganizationId);
  }, [leaveToManagement, managingOrganizationId, organizationId]);

  if (!activation || managingOrganizationId == null) {
    return null;
  }

  const end = async () => {
    await endActivation.mutateAsync(activation.id);
    setConfirmOpen(false);
    router.replace(`/organizations/${managingOrganizationId}/management`);
  };

  return (
    <>
      <div
        role="status"
        className="flex items-center justify-between gap-3 rounded-lg border border-[var(--status-warning-accent)]/40 bg-[var(--status-warning-accent)]/10 px-3 py-2 text-sm sm:px-4"
      >
        <p className="flex min-w-0 flex-wrap items-baseline gap-x-2">
          <span className="font-semibold text-[var(--status-warning-accent)]">
            {t('management.banner.badge')}
          </span>
          <span className="text-[var(--text-secondary)]">
            {t('management.banner.until', { time: formatManagementUntil(activation.validUntil) })}
          </span>
        </p>
        <Button
          variant="outline"
          size="sm"
          className="shrink-0"
          onClick={() => setConfirmOpen(true)}
        >
          {t('management.banner.end')}
        </Button>
      </div>
      <ConfirmDialog
        isOpen={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={() => void end()}
        message={t('management.banner.endConfirm')}
        variant="warning"
        isLoading={endActivation.isPending}
      />
    </>
  );
}
