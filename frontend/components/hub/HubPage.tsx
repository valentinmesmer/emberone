/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { cn } from '@/lib/utils/cn';
import { HubHeader } from './HubHeader';
import { HubNavPanel, type HubNavPanelGroup, type HubNavPanelTile } from './HubNavPanel';
import { HubSection } from './HubSection';

type HubPageHeaderConfig = {
  eyebrow?: string;
  title: string;
  subtitle?: string;
  media?: ReactNode;
  action?: ReactNode;
  density?: 'default' | 'compact';
  /** When true, the header is marked for org-switch dropdown width anchoring (`data-org-hub-header`). */
  organizationMenuAnchor?: boolean;
};

export type HubPageProps = {
  header: HubPageHeaderConfig;
  /** Optional section heading above the tile grid; omit when group labels suffice. */
  areasTitle?: string;
  areasAction?: ReactNode;
  tiles: HubNavPanelTile[];
  groups?: HubNavPanelGroup[];
  fallbackGroupTitle?: string;
  /** Single-column compact list instead of the default tile grid. */
  navLayout?: 'grid' | 'list';
  /**
   * Rendered instead of the tile grid when `tiles.length === 0`
   * (e.g. no permissions / empty org hub).
   */
  emptyContent?: ReactNode;
  className?: string;
};

/**
 * Single layout shell for workspace hubs (my area, organization, administration):
 * identity header + optional section title + optional grouping and tile grid.
 */
export function HubPage({
  header,
  areasTitle,
  areasAction,
  tiles,
  groups,
  fallbackGroupTitle,
  navLayout,
  emptyContent,
  className,
}: HubPageProps) {
  const hasTiles = tiles.length > 0;

  return (
    <div className={cn('w-full min-w-0', className)}>
      <HubHeader
        eyebrow={header.eyebrow}
        title={header.title}
        subtitle={header.subtitle}
        media={header.media}
        action={header.action}
        density={header.density ?? 'compact'}
        organizationMenuAnchor={header.organizationMenuAnchor}
      />

      <HubSection title={areasTitle} action={areasAction} density="compact">
        {!hasTiles && emptyContent ? (
          emptyContent
        ) : hasTiles ? (
          <HubNavPanel
            tiles={tiles}
            groups={groups}
            fallbackGroupTitle={fallbackGroupTitle}
            cardDensity="dense"
            layout={navLayout}
          />
        ) : null}
      </HubSection>
    </div>
  );
}
