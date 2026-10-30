/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useTranslation } from '@/hooks/useTranslation';
import {
  getMyAreaOnboardingDetailPath,
  getMyAreaOnboardingPath,
  MY_AREA_ONBOARDING_PATH,
} from '@/lib/my-area/myAreaSections';
import {
  dismissOnboardingReminderForSession,
  wasOnboardingReminderDismissedThisSession,
} from '@/lib/onboarding/reminderSession';
import { useSelfPendingOnboardingInvitations } from '@/lib/store';

const HIDDEN_PREFIXES = ['/auth', '/setup', '/~offline'] as const;

function isHiddenPath(pathname: string): boolean {
  if (pathname === MY_AREA_ONBOARDING_PATH || pathname.startsWith(`${MY_AREA_ONBOARDING_PATH}/`)) {
    return true;
  }
  return HIDDEN_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`));
}

/**
 * Reminds the user once per app session when they still have open onboardings.
 */
export function OnboardingReminderPrompt() {
  const t = useTranslation();
  const pathname = usePathname();
  const router = useRouter();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const { data: invitations = [], isSuccess } = useSelfPendingOnboardingInvitations({
    enabled: hasEmberOneUserId,
  });
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!hasEmberOneUserId || !isSuccess) return;
    if (invitations.length === 0) return;
    if (
      pathname === MY_AREA_ONBOARDING_PATH ||
      pathname.startsWith(`${MY_AREA_ONBOARDING_PATH}/`)
    ) {
      dismissOnboardingReminderForSession();
      setOpen(false);
      return;
    }
    if (wasOnboardingReminderDismissedThisSession()) return;
    if (isHiddenPath(pathname)) return;
    setOpen(true);
  }, [hasEmberOneUserId, isSuccess, invitations.length, pathname]);

  const dismiss = () => {
    dismissOnboardingReminderForSession();
    setOpen(false);
  };

  const openOnboarding = () => {
    dismissOnboardingReminderForSession();
    setOpen(false);
    router.push(
      invitations.length === 1
        ? getMyAreaOnboardingDetailPath(invitations[0].id)
        : getMyAreaOnboardingPath(),
    );
  };

  if (!open || isHiddenPath(pathname) || invitations.length === 0) {
    return null;
  }

  const count = invitations.length;
  const message =
    count === 1
      ? t('onboarding.reminder.messageSingle', {
          organization: invitations[0].targetOrganizationName,
        })
      : t('onboarding.reminder.messageMultiple', { count });

  return (
    <Modal
      isOpen
      onClose={dismiss}
      title={t('onboarding.reminder.title')}
      size="sm"
      footer={
        <ModalFooter
          onCancel={dismiss}
          onSave={openOnboarding}
          cancelText={t('onboarding.reminder.later')}
          saveText={t('onboarding.reminder.open')}
        />
      }
    >
      <div className="py-2">
        <p className="text-[var(--text-primary)]">{message}</p>
        {count > 1 ? (
          <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-[var(--text-secondary)]">
            {invitations.map((invitation) => (
              <li key={invitation.id}>{invitation.targetOrganizationName}</li>
            ))}
          </ul>
        ) : null}
      </div>
    </Modal>
  );
}
