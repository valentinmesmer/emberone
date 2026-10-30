/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';

export const MY_AREA_SETTINGS_BASE_PATH = `${MY_AREA_BASE_PATH}/settings`;

export const MY_AREA_SETTINGS_SECTIONS = [
  {
    id: 'notifications',
    path: `${MY_AREA_SETTINGS_BASE_PATH}/notifications`,
    labelKey: 'myArea.settings.menu.notifications',
    descriptionKey: 'myArea.settings.menu.notificationsDescription',
  },
  {
    id: 'password',
    /** Managed by the IdP; no in-app route (see useIdpPasswordSettingsUrl). */
    path: null,
    labelKey: 'myArea.settings.menu.password',
    descriptionKey: 'myArea.settings.menu.passwordDescription',
  },
] as const;

export type MyAreaSettingsSectionId = (typeof MY_AREA_SETTINGS_SECTIONS)[number]['id'];
