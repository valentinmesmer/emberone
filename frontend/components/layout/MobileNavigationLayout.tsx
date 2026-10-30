/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Suspense, useLayoutEffect, useRef } from 'react';
import { SidebarBrand } from '@/components/layout/SidebarBrand';
import {
  type MenuItem,
  NavigationMenu,
  type NavigationMenuProps,
} from '@/components/navigation/NavigationMenu';
import { BackButton } from '@/components/ui/back-button';
import { ListSiblingNav } from '@/components/ui/list-sibling-nav';
import { useWorkspaceNavMenu } from '@/hooks/useWorkspaceNavMenu';
import { BackHrefOverrideProvider } from '@/lib/navigation/backHrefOverride';
import { cn } from '@/lib/utils/cn';

interface MobileNavigationLayoutProps {
  children: React.ReactNode;
  navigationMenuProps?: Pick<
    NavigationMenuProps,
    'middleItems' | 'middleItemGroups' | 'selectedId'
  >;
  /** Overrides the centered `max-w-4xl` content wrapper (e.g. the wider dashboard). */
  contentClassName?: string;
  /** Rendered above the back control, e.g. a status banner. */
  leadingContent?: React.ReactNode;
}

function MobileNavigationShell({
  children,
  contentClassName,
  leadingContent,
  workspaceStripItems,
  workspaceMenuMiddleItems,
  workspaceMenuMiddleGroups,
  selectedId,
  myAreaBadge,
  orgBadge,
  orgBadges,
}: MobileNavigationLayoutProps & {
  workspaceStripItems: MenuItem[];
  workspaceMenuMiddleItems: MenuItem[];
  workspaceMenuMiddleGroups?: NavigationMenuProps['middleItemGroups'];
  selectedId?: string;
  myAreaBadge?: number;
  orgBadge?: number;
  orgBadges?: Record<number, number>;
}) {
  const hasWorkspace = workspaceStripItems.length > 0;
  const workspaceStripScrollRef = useRef<HTMLDivElement>(null);

  useLayoutEffect(() => {
    if (!selectedId || !workspaceStripScrollRef.current) return;
    const container = workspaceStripScrollRef.current;
    const tab = container.querySelector<HTMLElement>(
      `[data-workspace-strip-tab="${CSS.escape(selectedId)}"]`,
    );
    if (!tab) return;

    const isFirstTab = workspaceStripItems.length > 0 && workspaceStripItems[0]?.id === selectedId;

    const centeredLeft = tab.offsetLeft - (container.clientWidth - tab.offsetWidth) / 2;
    const maxScrollLeft = Math.max(0, container.scrollWidth - container.clientWidth);

    // scrollIntoView() can also move the body and hide the page header below the sticky strip.
    container.scrollTo({
      left: isFirstTab ? 0 : Math.min(Math.max(0, centeredLeft), maxScrollLeft),
      behavior: 'smooth',
    });
  }, [selectedId, workspaceStripItems]);

  return (
    <div className="min-h-screen bg-[var(--page-bg)] flex flex-col xl:flex-row">
      <div className="hidden xl:flex xl:fixed xl:bottom-0 xl:left-0 xl:top-0 xl:z-20 xl:w-[280px] 2xl:w-[300px] xl:flex-col xl:overflow-hidden xl:border-r xl:border-[var(--border-color)] bg-[var(--card-bg)]">
        <div className="flex min-h-0 flex-1 flex-col overflow-hidden px-4 pb-[max(0.75rem,env(safe-area-inset-bottom,0px))] 2xl:px-6">
          <SidebarBrand />
          <div className="flex min-h-0 flex-1 flex-col overflow-hidden pt-3">
            <NavigationMenu
              middleItems={workspaceMenuMiddleItems}
              middleItemGroups={workspaceMenuMiddleGroups}
              selectedId={selectedId}
              myAreaBadge={myAreaBadge}
              orgBadge={orgBadge}
              orgBadges={orgBadges}
            />
          </div>
        </div>
      </div>

      <div className="flex min-h-screen w-full min-w-0 flex-1 flex-col xl:ml-[280px] 2xl:ml-[300px] px-4 sm:px-6 xl:px-8 2xl:px-12 pb-[var(--app-mobile-bottom-safe)] xl:pb-12">
        {hasWorkspace && (
          <div className="xl:hidden sticky top-[var(--app-header-offset)] z-20 -mx-4 mt-[var(--app-header-offset)] border-b border-[var(--border-color)] bg-[var(--page-bg)] px-4 sm:-mx-6 sm:px-6">
            <div
              ref={workspaceStripScrollRef}
              className="-mb-px flex items-center gap-2 overflow-x-auto py-2"
              style={{ scrollbarWidth: 'none' }}
              role="tablist"
            >
              {workspaceStripItems.map((item) => {
                const active = selectedId === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    data-workspace-strip-tab={item.id}
                    role="tab"
                    aria-selected={active}
                    disabled={item.disabled}
                    onClick={item.onClick}
                    className={cn(
                      'flex shrink-0 items-center gap-1.5 rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors',
                      'touch-manipulation [&>svg]:h-4 [&>svg]:w-4',
                      item.disabled && 'cursor-not-allowed opacity-50',
                      active
                        ? 'border-[var(--fire-red)] bg-[var(--fire-red)] text-white'
                        : 'border-[var(--border-color)] bg-[var(--card-bg)] text-[var(--text-secondary)] hover:text-[var(--text-primary)] active:bg-[var(--hover-bg)]',
                    )}
                  >
                    {item.icon && <span className="flex shrink-0 items-center">{item.icon}</span>}
                    <span className="whitespace-nowrap">{item.label}</span>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        <BackHrefOverrideProvider>
          <div
            data-pull-to-refresh-content
            className={cn(
              'w-full space-y-1.5 sm:space-y-3 xl:space-y-6 xl:pt-8',
              hasWorkspace ? 'max-xl:pt-3' : 'max-xl:pt-[var(--app-content-top)]',
              contentClassName ?? 'max-w-4xl mx-auto',
            )}
          >
            {leadingContent}
            <div className="flex items-center justify-between gap-2">
              <BackButton />
              <ListSiblingNav />
            </div>
            {children}
          </div>
        </BackHrefOverrideProvider>
      </div>
    </div>
  );
}

function MobileNavigationLayoutResolved({
  children,
  navigationMenuProps,
  contentClassName,
  leadingContent,
}: MobileNavigationLayoutProps) {
  const derived = useWorkspaceNavMenu();
  const middleItems = navigationMenuProps?.middleItems ?? derived.middleItems;
  const middleItemGroups = navigationMenuProps?.middleItemGroups ?? derived.middleItemGroups;
  const selectedId = navigationMenuProps?.selectedId ?? derived.selectedId;
  const workspaceStripItems =
    middleItemGroups?.some((g) => g.items.length > 0) === true
      ? middleItemGroups.flatMap((g) => g.items)
      : middleItems;

  return (
    <MobileNavigationShell
      contentClassName={contentClassName}
      leadingContent={leadingContent}
      workspaceStripItems={workspaceStripItems}
      workspaceMenuMiddleItems={middleItems}
      workspaceMenuMiddleGroups={middleItemGroups}
      selectedId={selectedId}
      myAreaBadge={derived.myAreaBadge}
      orgBadge={derived.orgBadge}
      orgBadges={derived.orgBadges}
    >
      {children}
    </MobileNavigationShell>
  );
}

/**
 * Unified responsive app shell.
 * - Desktop (`xl+`): persistent left sidebar with branding, notifications, and full nav.
 *   No top header - chrome lives in the sidebar.
 * - Mobile / tablet (`< xl`): global header + bottom tab bar; workspace sub-sections
 *   appear as a sticky, horizontally scrollable segment strip.
 *
 * `Suspense` wraps workspace resolution (`useSearchParams` on admin routes).
 */
export function MobileNavigationLayout(props: MobileNavigationLayoutProps) {
  return (
    <Suspense
      fallback={
        <MobileNavigationShell
          {...props}
          workspaceStripItems={[]}
          workspaceMenuMiddleItems={[]}
          workspaceMenuMiddleGroups={undefined}
          selectedId={undefined}
        />
      }
    >
      <MobileNavigationLayoutResolved {...props} />
    </Suspense>
  );
}
