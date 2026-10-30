/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { Building2, type LucideIcon, ShieldCheck, User } from 'lucide-react';
import type { AdminModuleIcon } from './adminWorkspace';

/** Single source for the icon of an administration module (hub tiles, sidebar menu, page headers). */
export const ADMIN_MODULE_ICONS: Record<AdminModuleIcon, LucideIcon> = {
  tenants: Building2,
  users: User,
  management: ShieldCheck,
  organization: Building2,
};
