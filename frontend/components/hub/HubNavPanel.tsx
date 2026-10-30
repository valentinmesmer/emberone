/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { type ReactNode, useMemo } from 'react';
import { cn } from '@/lib/utils/cn';
import { HubNavTile } from './HubNavTile';

export type HubNavPanelTile = {
  id: string;
  icon: ReactNode;
  title: string;
  hint?: string;
  disabled?: boolean;
  /** Must match a `groups` entry `id` when groups are used. */
  groupId?: string;
  /** Positive integer shown as a count badge (e.g. unread notifications, open todos). */
  badge?: number;
} & ({ href: string } | { onClick: () => void });

export type HubNavPanelGroup = {
  id: string;
  /** Already translated label */
  title: string;
};

/** Mobile: single column; sm+: two columns - tight gutters. */
const gridClass = 'grid grid-cols-1 gap-1.5 sm:grid-cols-2 sm:gap-2 md:gap-2';

type HubNavPanelProps = {
  tiles: HubNavPanelTile[];
  /** Optional subsection headings (e.g. operations vs. administration). */
  groups?: HubNavPanelGroup[];
  /** Shown above tiles that have no `groupId` or an unknown `groupId` when `groups` is set. */
  fallbackGroupTitle?: string;
  /** Compact card chrome for hub pages (passed through to each tile). */
  cardDensity?: 'default' | 'dense';
  /** Single-column list with compact rows (e.g. settings sub-areas). */
  layout?: 'grid' | 'list';
  className?: string;
};

function TileGrid({
  tiles,
  cardDensity,
}: {
  tiles: HubNavPanelTile[];
  cardDensity: 'default' | 'dense';
}) {
  return (
    <ul className={cn(gridClass, 'list-none p-0')}>
      {tiles.map((tile) => (
        <li key={tile.id} className="flex min-h-0 min-w-0">
          <HubNavTile
            {...('href' in tile ? { href: tile.href } : { onClick: tile.onClick })}
            icon={tile.icon}
            title={tile.title}
            hint={tile.hint}
            disabled={tile.disabled}
            badge={tile.badge}
            cardDensity={cardDensity}
            className="min-h-0 w-full min-w-0 flex-1"
          />
        </li>
      ))}
    </ul>
  );
}

function ListLayout({ tiles }: { tiles: HubNavPanelTile[] }) {
  return (
    <ul className="list-none divide-y divide-[var(--border-color)] overflow-hidden rounded-xl border border-[var(--border-color)] p-0">
      {tiles.map((tile) => (
        <li key={tile.id} className="min-w-0">
          <HubNavTile
            {...('href' in tile ? { href: tile.href } : { onClick: tile.onClick })}
            icon={tile.icon}
            title={tile.title}
            hint={tile.hint}
            disabled={tile.disabled}
            badge={tile.badge}
            variant="compact"
            className="min-h-0 w-full"
          />
        </li>
      ))}
    </ul>
  );
}

/** Hub navigation: grouped or flat two-column tile grid. */
export function HubNavPanel({
  tiles,
  groups,
  fallbackGroupTitle,
  cardDensity = 'default',
  layout = 'grid',
  className,
}: HubNavPanelProps) {
  const groupIdSet = useMemo(() => new Set(groups?.map((g) => g.id) ?? []), [groups]);

  const ungrouped = useMemo(() => {
    if (!groups?.length) return [];
    return tiles.filter((t) => !t.groupId || !groupIdSet.has(t.groupId));
  }, [tiles, groups, groupIdSet]);

  const renderTiles = (sectionTiles: HubNavPanelTile[]) =>
    layout === 'list' ? (
      <ListLayout tiles={sectionTiles} />
    ) : (
      <TileGrid tiles={sectionTiles} cardDensity={cardDensity} />
    );

  return (
    <div className={cn(layout === 'list' ? 'space-y-2' : 'space-y-3 sm:space-y-4', className)}>
      {groups?.length ? (
        <div className={cn(layout === 'list' ? 'space-y-2' : 'space-y-3 sm:space-y-4')}>
          {groups.map((group) => {
            const inGroup = tiles.filter((t) => t.groupId === group.id);
            if (!inGroup.length) return null;
            return (
              <section key={group.id} aria-labelledby={`hub-group-${group.id}`}>
                <h3
                  id={`hub-group-${group.id}`}
                  className="mb-1.5 text-[10px] font-semibold uppercase tracking-[0.06em] text-[var(--text-secondary)] sm:mb-2 sm:text-[11px]"
                >
                  {group.title}
                </h3>
                {renderTiles(inGroup)}
              </section>
            );
          })}
          {ungrouped.length > 0 ? (
            fallbackGroupTitle ? (
              <section aria-labelledby="hub-group-fallback">
                <h3
                  id="hub-group-fallback"
                  className="mb-1.5 text-[10px] font-semibold uppercase tracking-[0.06em] text-[var(--text-secondary)] sm:mb-2 sm:text-[11px]"
                >
                  {fallbackGroupTitle}
                </h3>
                {renderTiles(ungrouped)}
              </section>
            ) : (
              renderTiles(ungrouped)
            )
          ) : null}
        </div>
      ) : (
        renderTiles(tiles)
      )}
    </div>
  );
}
