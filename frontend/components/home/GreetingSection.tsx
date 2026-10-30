/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import Link from 'next/link';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useTranslation } from '@/hooks/useTranslation';
import { useUserInfo } from '@/hooks/useUserInfo';
import { APP_LOCALE_TAG } from '@/lib/i18n/locale';

function getGreetingKey():
  | 'home.greetingMorning'
  | 'home.greetingAfternoon'
  | 'home.greetingEvening' {
  const hour = new Date().getHours();
  if (hour < 12) return 'home.greetingMorning';
  if (hour < 18) return 'home.greetingAfternoon';
  return 'home.greetingEvening';
}

const TODAY_FORMAT = new Intl.DateTimeFormat(APP_LOCALE_TAG, {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
});

export function GreetingSection() {
  const t = useTranslation();
  const { firstName, lastName } = useUserInfo();
  const greeting = t(getGreetingKey());

  return (
    <Link
      href="/my-area/master-data"
      className="mb-2 block rounded-lg border border-[var(--border-color)] bg-[var(--card-bg)] p-3 transition-opacity hover:opacity-80 sm:mb-2.5 sm:p-3.5"
    >
      <div className="flex items-center gap-2.5 sm:gap-3">
        <UserAvatar
          firstName={firstName ?? undefined}
          lastName={lastName ?? undefined}
          size="lg"
          showProfileImage
        />
        <div className="min-w-0">
          <p className="text-[10px] font-medium uppercase tracking-wide text-[var(--text-secondary)] sm:text-xs">
            {TODAY_FORMAT.format(new Date())}
          </p>
          <h1 className="mt-0.5 text-lg font-semibold leading-tight text-[var(--text-primary)] sm:text-xl">
            {greeting}
            {firstName ? `, ${firstName}` : ''}
          </h1>
          <p className="mt-0.5 text-xs text-[var(--text-secondary)]">{t('home.greeting')}</p>
        </div>
      </div>
    </Link>
  );
}
