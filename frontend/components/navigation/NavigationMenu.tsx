/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Building2, Home, LogOut, Moon, Shield, Sun, User } from 'lucide-react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useTheme } from 'next-themes';
import {
  type ReactNode,
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { SourceFooter } from '@/components/legal/SourceFooter';
import {
  MainOrgIndicator,
  ORG_SWITCHER_THRESHOLD,
  OrganizationNavSwitcher,
} from '@/components/navigation/OrganizationNavSwitcher';
import { useAuth } from '@/hooks/useAuth';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useMounted } from '@/hooks/useMounted';
import { useTranslation } from '@/hooks/useTranslation';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import { isWorkspaceHubLandingPath } from '@/lib/navigation/workspaceHubLanding';
import { useMyOrganizations, useUIStore } from '@/lib/store';
import { getOrganizationOverviewPath, getOrganizationWorkspaceEntryPath } from '@/lib/utils';
import { cn } from '@/lib/utils/cn';

/** Match `scroll-pt-9` / top fade (`h-9`) on the nav scroller. */
const NAV_SCROLL_PAD_TOP_PX = 36;
/** Match `scroll-pb-14` / bottom fade (`h-14`) on the nav scroller. */
const NAV_SCROLL_PAD_BOTTOM_PX = 56;

function alignNavScrollerToAnchor(root: HTMLElement, scrollToBottom: boolean) {
  if (scrollToBottom) {
    root.scrollTop = Math.max(0, root.scrollHeight - root.clientHeight);
    return;
  }

  const anchor = root.querySelector<HTMLElement>('[data-nav-scroll-anchor]');
  if (!anchor) return;

  const rootRect = root.getBoundingClientRect();
  const anchorRect = anchor.getBoundingClientRect();
  const visibleTop = rootRect.top + NAV_SCROLL_PAD_TOP_PX;
  const visibleBottom = rootRect.bottom - NAV_SCROLL_PAD_BOTTOM_PX;

  if (anchorRect.top >= visibleTop && anchorRect.bottom <= visibleBottom) {
    return;
  }

  const anchorTopInScroller = anchorRect.top - rootRect.top + root.scrollTop;
  const maxScrollTop = Math.max(0, root.scrollHeight - root.clientHeight);
  let nextScrollTop = root.scrollTop;

  if (anchorRect.top < visibleTop) {
    nextScrollTop = anchorTopInScroller - NAV_SCROLL_PAD_TOP_PX;
  } else if (anchorRect.bottom > visibleBottom) {
    nextScrollTop =
      anchorTopInScroller + anchorRect.height - root.clientHeight + NAV_SCROLL_PAD_BOTTOM_PX;
  }

  root.scrollTop = Math.min(maxScrollTop, Math.max(0, nextScrollTop));
}

const navBtn =
  'w-full flex items-center gap-2.5 rounded-md border text-sm font-medium transition-colors duration-150 sm:gap-3';
/** 44px min touch target; slightly tighter horizontal padding on narrow screens */
const navPrimaryRowMin = 'min-h-11 px-2.5 py-2 sm:px-3';
const navIdle =
  'text-[var(--text-primary)] border-transparent hover:bg-[var(--hover-bg)] active:bg-[var(--hover-bg)]';
/** Footer actions: same row weight as main nav, full-width label. */
const footerActionRow = cn(navBtn, navPrimaryRowMin, navIdle, 'text-left');
/** Main sidebar: subtle highlight in brand color */
const navActiveMain =
  'border-transparent bg-[color-mix(in_srgb,var(--fire-red)_12%,transparent)] font-medium text-[var(--fire-red)] dark:bg-[color-mix(in_srgb,var(--fire-red-light)_18%,transparent)] dark:text-[var(--fire-red-light)]';
/** Workspace sub-nav: full primary fill */
const navActiveWorkspace =
  'border-transparent bg-[var(--fire-red)] font-medium text-white shadow-sm dark:bg-[var(--fire-red)] dark:text-white';

function NavSidebarSection({
  title,
  ariaLabel,
  children,
  isFirst = false,
  variant = 'default',
}: {
  title?: string;
  /** When `title` is omitted, labels the region for assistive tech (e.g. workspace with only sub-headings). */
  ariaLabel?: string;
  children: ReactNode;
  isFirst?: boolean;
  /** Workspace: stacked groups; height comes from content (outer panel scrolls). */
  variant?: 'default' | 'workspace';
}) {
  const headingId = useId();
  const isWorkspace = variant === 'workspace';

  return (
    <section
      className={cn(
        !isFirst && 'mt-3.5 border-t border-[var(--border-color)]/50 pt-3',
        isWorkspace && 'flex shrink-0 flex-col',
      )}
      aria-labelledby={title ? headingId : undefined}
      aria-label={title ? undefined : ariaLabel}
    >
      {title ? (
        <h3
          id={headingId}
          className="mb-2 px-0.5 text-[10px] font-semibold uppercase tracking-wide text-[var(--text-secondary)]/80"
        >
          {title}
        </h3>
      ) : null}
      {children}
    </section>
  );
}

export interface MenuItem {
  id: string;
  label: string;
  icon?: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  /** Positive integer shown as a count badge next to the label. */
  badge?: number;
}

/** Workspace sub-nav blocks (same pattern as {@link HubNavPanel} group headings). */
export type WorkspaceMiddleItemGroup = {
  id: string;
  title: string;
  items: MenuItem[];
};

export interface NavigationMenuProps {
  middleItems?: MenuItem[];
  /** When set, desktop/workspace sidebar renders grouped blocks; `middleItems` may be empty. */
  middleItemGroups?: WorkspaceMiddleItemGroup[];
  selectedId?: string;
  /** Called after any navigation (used by the mobile drawer to close itself). */
  onNavigate?: () => void;
  /**
   * When `false` (e.g. mobile drawer closed), skip scrolling the active item into view.
   * Defaults to `true` for the persistent desktop sidebar.
   */
  selectionScrollEnabled?: boolean;
  /** Aggregated count shown on the "Mein Bereich" main-nav entry. */
  myAreaBadge?: number;
  /** Aggregated count shown on the active org main-nav entry. */
  orgBadge?: number;
  /** Pending counts by organization, shown while the organization list is expanded. */
  orgBadges?: Record<number, number>;
}

function NavButton({
  selected,
  onClick,
  disabled,
  variant = 'main',
  className,
  scrollAnchor,
  children,
}: {
  selected?: boolean;
  onClick: () => void;
  disabled?: boolean;
  /** Main nav: subtle brand tint; workspace: solid primary fill */
  variant?: 'main' | 'workspace';
  className?: string;
  /** Marks the one row the nav scroll area should align to (current route). */
  scrollAnchor?: boolean;
  children: ReactNode;
}) {
  const activeClass = variant === 'workspace' ? navActiveWorkspace : navActiveMain;
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      data-nav-scroll-anchor={scrollAnchor ? true : undefined}
      className={cn(
        navBtn,
        navPrimaryRowMin,
        disabled && 'cursor-not-allowed opacity-50',
        selected ? activeClass : navIdle,
        variant === 'workspace' &&
          selected &&
          'hover:bg-[var(--fire-red-dark)] active:bg-[var(--fire-red-dark)]',
        /** Lets `scrollIntoView` clear the sidebar fade overlays (h-9 / h-14). */
        scrollAnchor && 'scroll-mt-9 scroll-mb-14',
        className,
      )}
      aria-pressed={selected}
    >
      {children}
    </button>
  );
}

type Org = { id: number; name: string; main?: boolean };
/** Sub-headings inside workspace: sentence case, calmer than stacked submenu labels + all-caps hub labels. */
const workspaceGroupHeadingClass =
  'mb-1.5 px-0.5 text-xs font-medium text-[var(--text-secondary)] sm:text-[13px]';

export function NavigationMenu({
  middleItems = [],
  middleItemGroups,
  selectedId,
  onNavigate,
  selectionScrollEnabled = true,
  myAreaBadge,
  orgBadge,
  orgBadges = {},
}: NavigationMenuProps) {
  const t = useTranslation();
  const router = useRouter();
  const pathname = usePathname();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const { canAccessAdministration, logout } = useAuth();
  const { theme, setTheme, resolvedTheme } = useTheme();
  const themeMounted = useMounted();
  const { data: myOrganizations = [] } = useMyOrganizations();

  const organizations = useMemo<Org[]>(
    () =>
      myOrganizations.flatMap((entry) => {
        if (!entry.organization) return [];
        return [
          {
            id: entry.organization.id,
            name: entry.organization.name,
            main: entry.main,
          },
        ];
      }),
    [myOrganizations],
  );

  const mainOrg = useMemo(
    () => organizations.find((o) => o.main) ?? organizations[0],
    [organizations],
  );

  const dropdownOrgs = useMemo(
    () => [...organizations].sort((a, b) => Number(Boolean(b.main)) - Number(Boolean(a.main))),
    [organizations],
  );
  const orgCount = organizations.length;
  const hasMultipleOrganizations = orgCount > 1;
  const showOrganizationSwitcher = orgCount >= ORG_SWITCHER_THRESHOLD;
  const showFlatOrganizationList = orgCount >= 2 && orgCount < ORG_SWITCHER_THRESHOLD;

  const activeOrganizationId = useMemo(() => {
    const active = organizations.find((org) => {
      const p = getOrganizationOverviewPath(org.id);
      return pathname === p || pathname.startsWith(`${p}/`);
    });
    return active?.id ?? null;
  }, [organizations, pathname]);

  const preferredOrganizationId = useUIStore((s) => s.selectedOrganizationId);
  const setSelectedOrganizationId = useUIStore((s) => s.setSelectedOrganizationId);

  useEffect(() => {
    if (activeOrganizationId != null) {
      setSelectedOrganizationId(activeOrganizationId);
    }
  }, [activeOrganizationId, setSelectedOrganizationId]);

  const navigate = (href: string) => {
    router.push(href);
    onNavigate?.();
  };

  const navigateToOrg = (orgId: number) => {
    setSelectedOrganizationId(orgId);
    navigate(getOrganizationWorkspaceEntryPath(orgId));
  };

  const handleLogout = async () => {
    onNavigate?.();
    try {
      await logout();
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  const isHomeSelected = pathname === '/';
  const isMyAreaRoute =
    hasEmberOneUserId &&
    (pathname === MY_AREA_BASE_PATH || pathname.startsWith(`${MY_AREA_BASE_PATH}/`));
  const isAdministrationSelected = pathname.startsWith('/admin');

  const showPersonalData = hasEmberOneUserId;
  const groupedWorkspaceNav = middleItemGroups?.some((g) => g.items.length > 0) ?? false;
  const hasWorkspace = middleItems.length > 0 || groupedWorkspaceNav;
  const workspaceGroupCount = middleItemGroups?.filter((g) => g.items.length > 0).length ?? 0;
  const showWorkspaceSubgroupTitles = workspaceGroupCount > 1;
  const hideLogout = pathname === '/auth/callback';
  const showOrganizationsNav = mainOrg != null;

  const selectedWorkspaceItemExists = Boolean(
    selectedId &&
      hasWorkspace &&
      (groupedWorkspaceNav
        ? middleItemGroups?.some((g) => g.items.some((i) => i.id === selectedId))
        : middleItems.some((i) => i.id === selectedId)),
  );

  const isAnyOrgRoute = useMemo(() => {
    return organizations.some((org) => {
      const p = getOrganizationOverviewPath(org.id);
      return pathname === p || pathname.startsWith(`${p}/`);
    });
  }, [organizations, pathname]);

  const orgScrollTargetId = useMemo(() => {
    if (selectedWorkspaceItemExists || !mainOrg) return null;
    if (!isAnyOrgRoute || !isWorkspaceHubLandingPath(pathname)) return null;
    return activeOrganizationId ?? mainOrg.id;
  }, [selectedWorkspaceItemExists, mainOrg, isAnyOrgRoute, activeOrganizationId, pathname]);

  const scrollNavToAdminContext =
    isAdministrationSelected &&
    !selectedWorkspaceItemExists &&
    orgScrollTargetId === null &&
    !isMyAreaRoute &&
    !isHomeSelected;

  const workspaceMenuItemIds = useMemo(() => {
    if (!hasWorkspace) return '';
    if (groupedWorkspaceNav) {
      return middleItemGroups?.flatMap((g) => g.items.map((i) => i.id)).join('\0') ?? '';
    }
    return middleItems.map((i) => i.id).join('\0');
  }, [hasWorkspace, groupedWorkspaceNav, middleItemGroups, middleItems]);

  const cycleTheme = () => {
    if (theme === 'system') {
      setTheme(resolvedTheme === 'dark' ? 'light' : 'dark');
    } else if (theme === 'light') {
      setTheme('dark');
    } else {
      setTheme('light');
    }
  };

  const displayTheme = themeMounted ? (theme === 'system' ? resolvedTheme : theme) : undefined;
  const themeRowLabel =
    displayTheme === 'light'
      ? t('theme.switchToDark')
      : displayTheme === 'dark'
        ? t('theme.switchToLight')
        : t('theme.switchTheme');

  const navScrollRef = useRef<HTMLDivElement>(null);
  const navScrollContentRef = useRef<HTMLDivElement>(null);
  const [navScrollOverflow, setNavScrollOverflow] = useState({ top: false, bottom: false });

  const updateNavScrollOverflow = useCallback(() => {
    const el = navScrollRef.current;
    if (!el) return;
    const gap = 4;
    const { scrollTop, scrollHeight, clientHeight } = el;
    const top = scrollTop > gap;
    const bottom = scrollTop + clientHeight < scrollHeight - gap;
    setNavScrollOverflow((current) => {
      if (current.top === top && current.bottom === bottom) {
        return current;
      }
      return { top, bottom };
    });
  }, []);

  useLayoutEffect(() => {
    const el = navScrollRef.current;
    const inner = navScrollContentRef.current;
    if (!el) return;
    updateNavScrollOverflow();
    el.addEventListener('scroll', updateNavScrollOverflow, { passive: true });
    const ro = new ResizeObserver(() => updateNavScrollOverflow());
    ro.observe(el);
    if (inner) ro.observe(inner);
    return () => {
      el.removeEventListener('scroll', updateNavScrollOverflow);
      ro.disconnect();
    };
  }, [updateNavScrollOverflow]);

  useLayoutEffect(() => {
    if (!selectionScrollEnabled) return;
    const root = navScrollRef.current;
    if (!root) return;
    const expectsWorkspaceAnchor =
      hasWorkspace &&
      selectedId != null &&
      workspaceMenuItemIds.includes(selectedId) &&
      !scrollNavToAdminContext;
    if (expectsWorkspaceAnchor && !root.querySelector('[data-nav-scroll-anchor]')) {
      return;
    }
    alignNavScrollerToAnchor(root, scrollNavToAdminContext);
    updateNavScrollOverflow();
  }, [
    selectionScrollEnabled,
    scrollNavToAdminContext,
    updateNavScrollOverflow,
    selectedId,
    hasWorkspace,
    workspaceMenuItemIds,
  ]);

  const scrollAnchorWorkspace = selectedWorkspaceItemExists && selectedId != null;
  const scrollAnchorMyArea =
    !scrollAnchorWorkspace && isMyAreaRoute && isWorkspaceHubLandingPath(pathname);
  const scrollAnchorHome = !scrollAnchorWorkspace && !scrollAnchorMyArea && isHomeSelected;
  const scrollAnchorOrgId = !scrollAnchorWorkspace ? orgScrollTargetId : null;

  const scrollableNav = (
    <div ref={navScrollContentRef} className="flex shrink-0 flex-col pb-14">
      <div className="shrink-0">
        <NavSidebarSection isFirst title={t('home.navigation.sections.main')}>
          <nav className="flex flex-col gap-0.5">
            <NavButton
              selected={isHomeSelected}
              scrollAnchor={scrollAnchorHome}
              onClick={() => navigate('/')}
            >
              <Home
                className={cn(
                  'h-4 w-4 shrink-0',
                  isHomeSelected
                    ? 'text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                    : 'text-[var(--text-secondary)]',
                )}
              />
              <span className="flex-1 text-left">{t('home.navigation.home.title')}</span>
            </NavButton>

            {showPersonalData ? (
              <NavButton
                className="min-w-0"
                selected={isMyAreaRoute}
                scrollAnchor={scrollAnchorMyArea}
                onClick={() => navigate(MY_AREA_BASE_PATH)}
              >
                <User
                  className={cn(
                    'h-4 w-4 shrink-0',
                    isMyAreaRoute
                      ? 'text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                      : 'text-[var(--text-secondary)]',
                  )}
                />
                <span className="min-w-0 flex-1 truncate text-left">
                  {t('home.navigation.personalData.title')}
                </span>
                {myAreaBadge != null && myAreaBadge > 0 ? (
                  <span
                    className={cn(
                      'inline-flex h-[1.125rem] min-w-[1.125rem] shrink-0 items-center justify-center rounded-full px-1 text-[10px] font-semibold tabular-nums leading-none',
                      isMyAreaRoute
                        ? 'bg-[var(--fire-red)]/20 text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                        : 'bg-[var(--fire-red)]/12 text-[var(--fire-red)] dark:bg-[var(--fire-red)]/20 dark:text-[var(--fire-red-light)]',
                    )}
                  >
                    {myAreaBadge > 99 ? '99+' : myAreaBadge}
                  </span>
                ) : null}
              </NavButton>
            ) : null}

            {showOrganizationsNav && showOrganizationSwitcher && mainOrg ? (
              <OrganizationNavSwitcher
                organizations={dropdownOrgs}
                activeOrganizationId={activeOrganizationId}
                preferredOrganizationId={preferredOrganizationId}
                fallbackOrganization={mainOrg}
                isOrgRouteActive={isAnyOrgRoute}
                orgBadges={orgBadges}
                aggregatedBadge={orgBadge}
                scrollAnchor={scrollAnchorOrgId != null}
                onSelect={(orgId) => navigateToOrg(orgId)}
              />
            ) : null}

            {showOrganizationsNav && showFlatOrganizationList
              ? dropdownOrgs.map((org) => {
                  const orgPath = getOrganizationOverviewPath(org.id);
                  const isOrgRoute = pathname === orgPath || pathname.startsWith(`${orgPath}/`);
                  const badge = orgBadges[org.id];

                  return (
                    <NavButton
                      key={org.id}
                      className="min-w-0"
                      selected={isOrgRoute}
                      scrollAnchor={scrollAnchorOrgId === org.id}
                      onClick={() => navigateToOrg(org.id)}
                    >
                      <Building2
                        className={cn(
                          'h-4 w-4 shrink-0',
                          isOrgRoute
                            ? 'text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                            : 'text-[var(--text-secondary)]',
                        )}
                      />
                      <span className="min-w-0 flex-1 truncate text-left">{org.name}</span>
                      {org.main ? <MainOrgIndicator /> : null}
                      {badge != null && badge > 0 ? (
                        <span
                          className={cn(
                            'inline-flex h-[1.125rem] min-w-[1.125rem] shrink-0 items-center justify-center rounded-full px-1 text-[10px] font-semibold tabular-nums leading-none',
                            isOrgRoute
                              ? 'bg-[var(--fire-red)]/20 text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                              : 'bg-[var(--fire-red)]/12 text-[var(--fire-red)] dark:bg-[var(--fire-red)]/20 dark:text-[var(--fire-red-light)]',
                          )}
                        >
                          {badge > 99 ? '99+' : badge}
                        </span>
                      ) : null}
                    </NavButton>
                  );
                })
              : null}

            {showOrganizationsNav && mainOrg && !hasMultipleOrganizations ? (
              <NavButton
                className="min-w-0"
                selected={isAnyOrgRoute}
                scrollAnchor={scrollAnchorOrgId === mainOrg.id}
                onClick={() => navigateToOrg(mainOrg.id)}
              >
                <Building2
                  className={cn(
                    'h-4 w-4 shrink-0',
                    isAnyOrgRoute
                      ? 'text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                      : 'text-[var(--text-secondary)]',
                  )}
                />
                <span className="min-w-0 flex-1 line-clamp-2 text-left leading-snug">
                  {mainOrg.name}
                </span>
                {orgBadge != null && orgBadge > 0 ? (
                  <span
                    className={cn(
                      'inline-flex h-[1.125rem] min-w-[1.125rem] shrink-0 items-center justify-center rounded-full px-1 text-[10px] font-semibold tabular-nums leading-none',
                      isAnyOrgRoute
                        ? 'bg-[var(--fire-red)]/20 text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                        : 'bg-[var(--fire-red)]/12 text-[var(--fire-red)] dark:bg-[var(--fire-red)]/20 dark:text-[var(--fire-red-light)]',
                    )}
                  >
                    {orgBadge > 99 ? '99+' : orgBadge}
                  </span>
                ) : null}
              </NavButton>
            ) : null}
          </nav>
        </NavSidebarSection>
      </div>

      {hasWorkspace ? (
        <NavSidebarSection
          variant="workspace"
          title={groupedWorkspaceNav ? undefined : t('home.navigation.sections.workspace')}
          ariaLabel={groupedWorkspaceNav ? t('home.navigation.sections.workspace') : undefined}
        >
          {groupedWorkspaceNav ? (
            <div className="flex shrink-0 flex-col gap-2.5 sm:gap-3">
              {middleItemGroups?.map((group) =>
                group.items.length ? (
                  <section
                    key={group.id}
                    className="shrink-0"
                    aria-labelledby={
                      showWorkspaceSubgroupTitles ? `workspace-group-${group.id}` : undefined
                    }
                  >
                    {showWorkspaceSubgroupTitles ? (
                      <h4 id={`workspace-group-${group.id}`} className={workspaceGroupHeadingClass}>
                        {group.title}
                      </h4>
                    ) : null}
                    <nav className="flex flex-col gap-0.5">
                      {group.items.map((item) => {
                        const selected = selectedId === item.id;
                        return (
                          <NavButton
                            key={item.id}
                            variant="workspace"
                            selected={selected}
                            scrollAnchor={scrollAnchorWorkspace && selectedId === item.id}
                            disabled={item.disabled}
                            onClick={() => {
                              item.onClick?.();
                              onNavigate?.();
                            }}
                          >
                            {item.icon && (
                              <span
                                className={cn(
                                  'flex shrink-0 items-center [&>svg]:h-4 [&>svg]:w-4',
                                  selected ? 'text-white' : 'text-[var(--text-secondary)]',
                                )}
                              >
                                {item.icon}
                              </span>
                            )}
                            <span className="flex-1 text-left">{item.label}</span>
                            {item.badge != null && item.badge > 0 ? (
                              <span
                                className={cn(
                                  'inline-flex h-[1.125rem] min-w-[1.125rem] shrink-0 items-center justify-center rounded-full px-1 text-[10px] font-semibold tabular-nums leading-none',
                                  selected
                                    ? 'bg-white/20 text-white'
                                    : 'bg-[var(--fire-red)]/12 text-[var(--fire-red)] dark:bg-[var(--fire-red)]/20 dark:text-[var(--fire-red-light)]',
                                )}
                              >
                                {item.badge > 99 ? '99+' : item.badge}
                              </span>
                            ) : null}
                          </NavButton>
                        );
                      })}
                    </nav>
                  </section>
                ) : null,
              )}
            </div>
          ) : (
            <nav className="flex flex-col gap-0.5">
              {middleItems.map((item) => {
                const selected = selectedId === item.id;
                return (
                  <NavButton
                    key={item.id}
                    variant="workspace"
                    selected={selected}
                    scrollAnchor={scrollAnchorWorkspace && selectedId === item.id}
                    disabled={item.disabled}
                    onClick={() => {
                      item.onClick?.();
                      onNavigate?.();
                    }}
                  >
                    {item.icon && (
                      <span
                        className={cn(
                          'flex shrink-0 items-center [&>svg]:h-4 [&>svg]:w-4',
                          selected ? 'text-white' : 'text-[var(--text-secondary)]',
                        )}
                      >
                        {item.icon}
                      </span>
                    )}
                    <span className="flex-1 text-left">{item.label}</span>
                    {item.badge != null && item.badge > 0 ? (
                      <span
                        className={cn(
                          'shrink-0 rounded-full px-1 py-px text-[10px] font-semibold leading-4 tabular-nums',
                          selected ? 'bg-white/25 text-white' : 'bg-[var(--fire-red)] text-white',
                        )}
                      >
                        {item.badge > 99 ? '99+' : item.badge}
                      </span>
                    ) : null}
                  </NavButton>
                );
              })}
            </nav>
          )}
        </NavSidebarSection>
      ) : null}
    </div>
  );

  return (
    <div className="flex h-full min-h-0 w-full max-w-full flex-1 flex-col">
      <div className="relative flex min-h-0 min-w-0 flex-1 flex-col">
        <div
          ref={navScrollRef}
          className="flex min-h-0 flex-1 flex-col overflow-y-auto overscroll-contain scroll-pt-9 scroll-pb-14 [scrollbar-width:thin]"
        >
          {scrollableNav}
        </div>
        {navScrollOverflow.top ? (
          <div
            className="pointer-events-none absolute inset-x-0 top-0 z-[1] h-9 bg-gradient-to-b from-[var(--card-bg)] from-25% to-transparent"
            aria-hidden
          />
        ) : null}
        {navScrollOverflow.bottom ? (
          <div
            className="pointer-events-none absolute inset-x-0 bottom-0 z-[1] h-14 bg-gradient-to-t from-[var(--card-bg)] from-20% via-[var(--card-bg)]/75 to-transparent"
            aria-hidden
          />
        ) : null}
      </div>
      {/* `sticky` (a no-op for layout - it is already the last flex row) makes
          this the box iOS 26 Safari samples for the bottom bar when the menu is
          rendered as the full-screen mobile drawer: WebKit only considers
          fixed/sticky containers at a viewport edge, and refuses to retint from
          the viewport-sized sheet itself. */}
      <div className="sticky bottom-0 shrink-0 border-t border-[var(--border-color)]/50 bg-[var(--card-bg)] pt-3">
        <div className="flex flex-col gap-0.5 pb-[max(0.75rem,env(safe-area-inset-bottom,0px))]">
          {canAccessAdministration ? (
            <Link
              href="/admin"
              onClick={() => onNavigate?.()}
              className={cn(footerActionRow, isAdministrationSelected && navActiveMain)}
            >
              <Shield
                className={cn(
                  'h-4 w-4 shrink-0',
                  isAdministrationSelected
                    ? 'text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                    : 'text-[var(--text-secondary)]',
                )}
              />
              <span className="min-w-0 flex-1 text-left">
                {t('home.navigation.administration.title')}
              </span>
            </Link>
          ) : null}
          {themeMounted ? (
            <button type="button" className={footerActionRow} onClick={cycleTheme}>
              {!displayTheme || displayTheme === 'light' ? (
                <Sun className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" />
              ) : (
                <Moon className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" />
              )}
              <span className="min-w-0 flex-1 text-left">{themeRowLabel}</span>
            </button>
          ) : (
            <button
              type="button"
              className={cn(footerActionRow, 'cursor-wait opacity-60')}
              disabled
            >
              <Sun className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" />
              <span className="min-w-0 flex-1 text-left">{t('theme.switchTheme')}</span>
            </button>
          )}
          {!hideLogout ? (
            <button type="button" className={footerActionRow} onClick={handleLogout}>
              <LogOut className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" />
              <span className="min-w-0 flex-1 text-left">{t('common.logout')}</span>
            </button>
          ) : null}
          <SourceFooter className="px-2.5 pt-2 sm:px-3" onNavigate={onNavigate} />
        </div>
      </div>
    </div>
  );
}
