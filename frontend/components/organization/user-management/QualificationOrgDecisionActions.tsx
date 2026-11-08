/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Check, X } from 'lucide-react';
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
  className?: string;
}

export function QualificationOrgDecisionActions({
  canGrant,
  canRevoke,
  isGrantPending = false,
  isRevokePending = false,
  onGrant,
  onRevoke,
  className,
}: QualificationOrgDecisionActionsProps) {
  const t = useTranslation();

  if (!canGrant && !canRevoke) {
    return null;
  }

  const pending = isGrantPending || isRevokePending;

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
