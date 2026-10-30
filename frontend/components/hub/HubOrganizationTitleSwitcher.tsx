/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useRouter } from 'next/navigation';
import { useMemo } from 'react';
import { DropdownButton } from '@/components/ui/button-dropdown';
import { useTranslation } from '@/hooks/useTranslation';
import { useWorkspaceOrganizations } from '@/hooks/useWorkspaceOrganizations';
import { formatManagementUntil } from '@/lib/management/workspaceOrganizations';
import { useUIStore } from '@/lib/store';
import { getOrganizationWorkspaceEntryPath } from '@/lib/utils/organization';

/** Anchors the `DropdownButton` menu width to the org hub header card. */
const ORG_HUB_HEADER_ANCHOR_SELECTOR = '[data-org-hub-header]';

/** Shared org-switch state so the title text and the chevron control stay in sync. */
function useOrganizationSwitch() {
  const t = useTranslation();
  const router = useRouter();
  const setSelectedOrganizationId = useUIStore((s) => s.setSelectedOrganizationId);

  const { organizations, isPending, isFetching } = useWorkspaceOrganizations();
  const isLoading = isPending && isFetching;

  const options = useMemo(
    () =>
      organizations.map((org) => {
        const suffix = org.activation
          ? ` (${t('organization.management.switcherBadge', {
              time: formatManagementUntil(org.activation.validUntil),
            })})`
          : org.main
            ? ` (${t('myArea.fields.mainOrganization')})`
            : '';
        return { value: org.id.toString(), label: `${org.name}${suffix}` };
      }),
    [organizations, t],
  );

  const selectOrganization = (orgId: string) => {
    const target = organizations.find((org) => org.id.toString() === orgId);
    if (!target) return;
    setSelectedOrganizationId(target.id);
    router.push(getOrganizationWorkspaceEntryPath(target.id));
  };

  return { options, isLoading, canSwitch: organizations.length > 1, selectOrganization };
}

type HubOrganizationSwitchProps = {
  currentOrganizationId: number;
};

/** Chevron control to switch the active organization; renders nothing with a single org. */
export function HubOrganizationSwitch({ currentOrganizationId }: HubOrganizationSwitchProps) {
  const { options, isLoading, canSwitch, selectOrganization } = useOrganizationSwitch();

  if (!canSwitch) {
    return null;
  }

  const optionListClassName =
    options.length > 10
      ? 'max-h-[min(55vh,20rem)]'
      : options.length > 5
        ? 'max-h-[min(40vh,14rem)]'
        : undefined;

  return (
    <DropdownButton
      variant="ghost"
      chevronOnly
      menuAnchorSelector={ORG_HUB_HEADER_ANCHOR_SELECTOR}
      isLoading={isLoading}
      options={options}
      selectedValue={currentOrganizationId.toString()}
      onOptionSelect={selectOrganization}
      {...(optionListClassName ? { optionListClassName } : {})}
    />
  );
}
