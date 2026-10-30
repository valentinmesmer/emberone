/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Check, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { IconButton } from '@/components/ui/icon-button';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

interface QualificationOrgDecisionActionsProps {
  canGrant: boolean;
  canRevoke: boolean;
  isGrantPending?: boolean;
  isRevokePending?: boolean;
  onGrant: () => void;
  onRevoke: () => void;
  variant?: 'icon' | 'button';
  className?: string;
}

export function QualificationOrgDecisionActions({
  canGrant,
  canRevoke,
  isGrantPending = false,
  isRevokePending = false,
  onGrant,
  onRevoke,
  variant = 'icon',
  className,
}: QualificationOrgDecisionActionsProps) {
  const t = useTranslation();

  if (!canGrant && !canRevoke) {
    return null;
  }

  const pending = isGrantPending || isRevokePending;
  const actionCount = (canGrant ? 1 : 0) + (canRevoke ? 1 : 0);

  if (variant === 'button') {
    return (
      <div
        className={cn(
          'grid w-full gap-2',
          actionCount > 1 ? 'grid-cols-2' : 'grid-cols-1',
          'sm:flex sm:w-auto sm:flex-row sm:flex-wrap sm:justify-end',
          className,
        )}
      >
        {canGrant ? (
          <Button
            type="button"
            size="xs"
            variant="secondary"
            fullWidth="responsive"
            icon={<Check className="h-3.5 w-3.5" aria-hidden />}
            onClick={onGrant}
            disabled={pending}
            isLoading={isGrantPending}
          >
            {t('qualifications.orgDecision.grant')}
          </Button>
        ) : null}
        {canRevoke ? (
          <Button
            type="button"
            size="xs"
            variant="outline"
            fullWidth="responsive"
            icon={<X className="h-3.5 w-3.5" aria-hidden />}
            onClick={onRevoke}
            disabled={pending}
            isLoading={isRevokePending}
          >
            {t('qualifications.orgDecision.revoke')}
          </Button>
        ) : null}
      </div>
    );
  }

  return (
    <div className={cn('flex shrink-0 items-center gap-1', className)}>
      {canGrant ? (
        <IconButton
          variant="action"
          icon={<Check className="h-4 w-4" aria-hidden />}
          aria-label={t('qualifications.orgDecision.grant')}
          title={t('qualifications.orgDecision.grant')}
          onClick={onGrant}
          disabled={pending}
        />
      ) : null}
      {canRevoke ? (
        <IconButton
          variant="action"
          icon={<X className="h-4 w-4" aria-hidden />}
          aria-label={t('qualifications.orgDecision.revoke')}
          title={t('qualifications.orgDecision.revoke')}
          onClick={onRevoke}
          disabled={pending}
        />
      ) : null}
    </div>
  );
}
