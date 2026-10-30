/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Bell, KeyRound } from 'lucide-react';
import { useMemo } from 'react';
import { HubPage } from '@/components/hub';
import { useIdpPasswordSettingsUrl } from '@/hooks/useIdpPasswordSettingsUrl';
import { useTranslation } from '@/hooks/useTranslation';
import {
  MY_AREA_SETTINGS_BASE_PATH,
  MY_AREA_SETTINGS_SECTIONS,
  type MyAreaSettingsSectionId,
} from '@/lib/my-area/settingsSections';

const SECTION_ICONS: Record<MyAreaSettingsSectionId, React.ReactNode> = {
  notifications: <Bell />,
  password: <KeyRound />,
};

export function SettingsHubView() {
  const t = useTranslation();
  const { url: authentikPasswordUrl, ready: authentikPasswordUrlReady } =
    useIdpPasswordSettingsUrl();

  const tiles = useMemo(
    () =>
      MY_AREA_SETTINGS_SECTIONS.map((section) => {
        const isPassword = section.id === 'password';
        const passwordUnavailable =
          isPassword && authentikPasswordUrlReady && !authentikPasswordUrl;
        const passwordPending = isPassword && !authentikPasswordUrlReady;

        return {
          id: section.id,
          href: (isPassword ? authentikPasswordUrl : section.path) ?? MY_AREA_SETTINGS_BASE_PATH,
          title: t(section.labelKey),
          hint: t(section.descriptionKey),
          icon: SECTION_ICONS[section.id],
          groupId: 'general',
          disabled: passwordUnavailable || passwordPending ? true : undefined,
        };
      }),
    [t, authentikPasswordUrl, authentikPasswordUrlReady],
  );

  return (
    <HubPage
      header={{
        eyebrow: t('myArea.settings.hub.eyebrow'),
        title: t('myArea.settings.hub.title'),
        subtitle: t('myArea.settings.hub.subtitle'),
        density: 'compact',
      }}
      tiles={tiles}
      groups={[{ id: 'general', title: t('myArea.settings.hub.groupTitle') }]}
    />
  );
}
