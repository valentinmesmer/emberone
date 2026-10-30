/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { SectionActions, SectionEditAction, SectionHeader } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import type { UserContactInformationDto } from '@/types/user';

interface NotificationEmailCardProps {
  contactInformation: UserContactInformationDto;
  onEdit?: () => void;
  onResendVerification?: () => void;
  onCancelPending?: () => void;
  isResending?: boolean;
  isCancelling?: boolean;
}

export function NotificationEmailCard({
  contactInformation,
  onEdit,
  onResendVerification,
  onCancelPending,
  isResending,
  isCancelling,
}: NotificationEmailCardProps) {
  const t = useTranslation();
  const pendingEmail = contactInformation.pendingEmail?.trim();

  return (
    <Card className="p-3 sm:p-4">
      <SectionHeader
        className="mb-4"
        title={t('myArea.sections.notificationEmail')}
        action={
          <SectionEditAction
            label={t('myArea.edit.notificationEmail')}
            onClick={() => onEdit?.()}
            disabled={!onEdit}
          />
        }
      />

      <div className="space-y-4 border-t border-[var(--border-color)] pt-4">
        <FieldDisplay
          label={t('myArea.fields.notificationEmail')}
          value={contactInformation.privateEmail}
          fallback="_"
        />

        {pendingEmail ? (
          <div className="space-y-3 rounded-md border border-[var(--toast-warning-border)] bg-[var(--toast-warning-bg)] p-3">
            <p className="text-sm text-[var(--text-primary)]">
              {t('myArea.notificationEmail.pendingMessage', { email: pendingEmail })}
            </p>
            <SectionActions>
              {onCancelPending ? (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  fullWidth="responsive"
                  onClick={onCancelPending}
                  disabled={isResending || isCancelling}
                >
                  {isCancelling
                    ? t('myArea.notificationEmail.cancelling')
                    : t('myArea.notificationEmail.cancelPending')}
                </Button>
              ) : null}
              {onResendVerification ? (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  fullWidth="responsive"
                  onClick={onResendVerification}
                  disabled={isResending || isCancelling}
                >
                  {isResending
                    ? t('myArea.notificationEmail.resending')
                    : t('myArea.notificationEmail.resend')}
                </Button>
              ) : null}
            </SectionActions>
          </div>
        ) : null}
      </div>
    </Card>
  );
}
