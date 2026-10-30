/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import { ADMIN_HUB_PATH } from '@/lib/navigation/adminWorkspace';

const ORG_OVERVIEW = /^\/organizations\/(\d+)$/;

/**
 * Workspace hub landing routes: overview pages that list tiles/areas.
 * The global shell hides the workspace subsection strip until the user opens a concrete area.
 */
export function isWorkspaceHubLandingPath(pathname: string): boolean {
  const normalized =
    pathname.endsWith('/') && pathname.length > 1 ? pathname.slice(0, -1) : pathname;
  if (normalized === ADMIN_HUB_PATH) return true;
  if (normalized === MY_AREA_BASE_PATH) return true;
  return ORG_OVERVIEW.test(normalized);
}
