/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { useMyManagementActivations, useMyOrganizations } from '@/lib/store';
import {
  buildWorkspaceOrganizations,
  type WorkspaceOrganization,
} from '@/lib/management/workspaceOrganizations';

/**
 * Memberships and open management activations as one organization list. Each entry names its
 * `source`, so callers never guess which endpoint or rights apply.
 */
export function useWorkspaceOrganizations(): {
  organizations: WorkspaceOrganization[];
  isPending: boolean;
  isFetching: boolean;
} {
  const memberships = useMyOrganizations();
  const activations = useMyManagementActivations();

  const organizations = useMemo(
    () => buildWorkspaceOrganizations(memberships.data ?? [], activations.data ?? [], Date.now()),
    [memberships.data, activations.data],
  );

  return {
    organizations,
    isPending: memberships.isPending || activations.isPending,
    isFetching: memberships.isFetching || activations.isFetching,
  };
}

/**
 * Whether the user is an active member of the organization. Personal appointment state (read
 * marks, own responses) belongs to memberships only; nothing personal is requested while the
 * memberships load.
 */
export function useOrganizationMembership(organizationId: number | null | undefined): {
  isMember: boolean;
  isPending: boolean;
} {
  const memberships = useMyOrganizations();
  const isMember = (memberships.data ?? []).some(
    (membership) => membership.organization?.id === organizationId,
  );
  return { isMember, isPending: memberships.isPending };
}
