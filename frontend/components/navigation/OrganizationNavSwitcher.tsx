/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Building2, Check, ChevronDown } from 'lucide-react';
import {
  type CSSProperties,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from 'react';
import { createPortal } from 'react-dom';
import { HoverHint } from '@/components/ui/hover-hint';
import { useTranslation } from '@/hooks/useTranslation';
import { resolvePreferredOrganization } from '@/lib/navigation/preferredOrganization';
import { cn } from '@/lib/utils/cn';

type OrganizationNavOrg = { id: number; name: string; main?: boolean };

interface OrganizationNavSwitcherProps {
  organizations: OrganizationNavOrg[];
  activeOrganizationId: number | null;
  /** Last-used org when not on an organization route (null → main/fallback). */
  preferredOrganizationId?: number | null;
  fallbackOrganization: OrganizationNavOrg;
  isOrgRouteActive: boolean;
  orgBadges?: Record<number, number>;
  /** Shown when no org route is active (aggregated pending count). */
  aggregatedBadge?: number;
  scrollAnchor?: boolean;
  onSelect: (orgId: number) => void;
}

const MENU_MAX_HEIGHT_PX = 320;
const VIEWPORT_MARGIN_PX = 12;
const MENU_MIN_WIDTH_PX = 260;
const MENU_MAX_WIDTH_PX = 360;

/** From this count upward, sidebar uses the flyout switcher instead of inline org rows. */
export const ORG_SWITCHER_THRESHOLD = 2;

const triggerShell =
  'flex w-full min-h-11 items-center rounded-md border text-sm font-medium overflow-hidden transition-colors duration-150';
const triggerMain =
  'flex min-w-0 flex-1 items-center gap-2 px-2.5 py-2 text-left sm:gap-2.5 sm:px-3';
const triggerChevron =
  'flex h-11 w-9 shrink-0 items-center justify-center border-l border-current/15 transition-colors';
const triggerIdle =
  'text-[var(--text-primary)] border-transparent hover:bg-[var(--hover-bg)] active:bg-[var(--hover-bg)]';
const triggerActive =
  'border-transparent bg-[color-mix(in_srgb,var(--fire-red)_12%,transparent)] font-medium text-[var(--fire-red)] dark:bg-[color-mix(in_srgb,var(--fire-red-light)_18%,transparent)] dark:text-[var(--fire-red-light)]';

function OrgCountBadge({ count, active }: { count: number; active: boolean }) {
  return (
    <span
      className={cn(
        'inline-flex h-[1.125rem] min-w-[1.125rem] shrink-0 items-center justify-center rounded-full px-1 text-[10px] font-semibold tabular-nums leading-none',
        active
          ? 'bg-[var(--fire-red)]/20 text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
          : 'bg-[var(--fire-red)]/12 text-[var(--fire-red)] dark:bg-[var(--fire-red)]/20 dark:text-[var(--fire-red-light)]',
      )}
    >
      {count > 99 ? '99+' : count}
    </span>
  );
}

/** Compact “Haupt” marker - full label only in tooltip for long sidebar names. */
export function MainOrgIndicator({ className }: { className?: string }) {
  const t = useTranslation();
  return (
    <HoverHint
      hint={t('myArea.fields.mainOrganization')}
      side="bottom"
      shape="rounded-lg"
      className={cn('inline-flex shrink-0', className)}
    >
      <span
        title={t('myArea.fields.mainOrganization')}
        className="rounded px-1 py-px text-[9px] font-semibold uppercase tracking-wide text-[var(--text-secondary)] bg-[var(--surface-soft)]"
      >
        {t('home.navigation.organizations.mainBadge')}
      </span>
    </HoverHint>
  );
}

/**
 * Workspace-style org switcher: one sidebar row, flyout list for all organizations.
 */
export function OrganizationNavSwitcher({
  organizations,
  activeOrganizationId,
  preferredOrganizationId = null,
  fallbackOrganization,
  isOrgRouteActive,
  orgBadges = {},
  aggregatedBadge,
  scrollAnchor = false,
  onSelect,
}: OrganizationNavSwitcherProps) {
  const t = useTranslation();
  const [open, setOpen] = useState(false);
  const [menuStyle, setMenuStyle] = useState<CSSProperties>();
  const rootRef = useRef<HTMLDivElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  const displayedOrg = resolvePreferredOrganization(organizations, {
    activeId: isOrgRouteActive ? activeOrganizationId : null,
    preferredId: preferredOrganizationId,
    fallback: fallbackOrganization,
  });
  const rowBadge =
    isOrgRouteActive && activeOrganizationId != null
      ? orgBadges[activeOrganizationId]
      : aggregatedBadge;

  const otherOrgCount = Math.max(0, organizations.length - 1);
  const switchSubtitle =
    otherOrgCount === 1
      ? t('home.navigation.organizations.switchSubtitleMoreOne')
      : t('home.navigation.organizations.switchSubtitleMore', { count: otherOrgCount });

  const updateMenuPosition = useCallback(() => {
    const trigger = rootRef.current;
    if (!trigger) return;

    const rect = trigger.getBoundingClientRect();
    const menuWidth = Math.min(
      MENU_MAX_WIDTH_PX,
      Math.max(MENU_MIN_WIDTH_PX, rect.width, 280),
      window.innerWidth - VIEWPORT_MARGIN_PX * 2,
    );
    const sidebarMode = window.matchMedia('(min-width: 1024px)').matches;
    const top = rect.bottom + 6;
    const maxHeight = Math.min(MENU_MAX_HEIGHT_PX, window.innerHeight - top - VIEWPORT_MARGIN_PX);

    let left = rect.left;
    if (sidebarMode) {
      left = rect.right + 8;
    }

    const maxLeft = window.innerWidth - menuWidth - VIEWPORT_MARGIN_PX;
    left = Math.max(VIEWPORT_MARGIN_PX, Math.min(left, maxLeft));

    setMenuStyle({
      position: 'fixed',
      top,
      left,
      width: menuWidth,
      maxHeight,
      zIndex: 200,
    });
  }, []);

  useLayoutEffect(() => {
    if (!open) return;
    updateMenuPosition();
    window.addEventListener('resize', updateMenuPosition);
    window.addEventListener('scroll', updateMenuPosition, true);
    return () => {
      window.removeEventListener('resize', updateMenuPosition);
      window.removeEventListener('scroll', updateMenuPosition, true);
    };
  }, [open, updateMenuPosition]);

  useEffect(() => {
    if (!open) return;

    const onPointerDown = (event: MouseEvent | TouchEvent) => {
      const target = event.target as Node | null;
      if (!target) return;
      if (rootRef.current?.contains(target) || menuRef.current?.contains(target)) {
        return;
      }
      setOpen(false);
    };

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', onPointerDown);
    document.addEventListener('touchstart', onPointerDown);
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('mousedown', onPointerDown);
      document.removeEventListener('touchstart', onPointerDown);
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open]);

  const handleSelect = (orgId: number) => {
    setOpen(false);
    onSelect(orgId);
  };

  const menu =
    open && menuStyle ? (
      <div
        ref={menuRef}
        role="menu"
        aria-label={t('home.navigation.organizations.switchMenu')}
        style={menuStyle}
        className="flex flex-col overflow-hidden rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)] shadow-[var(--interactive-shadow-strong)] animate-in fade-in slide-in-from-top-1 duration-150"
      >
        <div className="shrink-0 border-b border-[var(--border-color)] px-3 py-2.5">
          <p className="text-[10px] font-semibold uppercase tracking-wide text-[var(--text-secondary)]/80">
            {t('home.navigation.organizations.switchMenu')}
          </p>
          <p className="mt-0.5 text-xs text-[var(--text-secondary)]">
            {t('home.navigation.organizations.switchHint', { count: organizations.length })}
          </p>
        </div>
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-1 [scrollbar-width:thin]">
          {organizations.map((org) => {
            const isActive = activeOrganizationId === org.id && isOrgRouteActive;
            const badge = orgBadges[org.id];
            return (
              <button
                key={org.id}
                type="button"
                role="menuitem"
                onClick={() => handleSelect(org.id)}
                className={cn(
                  'group flex w-full min-h-10 items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-sm transition-colors',
                  'hover:bg-[var(--hover-bg)] focus-visible:bg-[var(--hover-bg)] focus-visible:outline-none',
                  isActive
                    ? 'bg-[var(--fire-red)]/8 font-medium text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                    : 'text-[var(--text-primary)]',
                )}
              >
                <Building2
                  className={cn(
                    'h-4 w-4 shrink-0 transition-colors',
                    isActive
                      ? 'text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                      : 'text-[var(--text-secondary)] group-hover:text-[var(--text-primary)]',
                  )}
                />
                <span className="flex min-w-0 flex-1 flex-wrap items-center gap-x-1.5 gap-y-1">
                  <span className="break-words leading-snug">{org.name}</span>
                  {org.main ? <MainOrgIndicator /> : null}
                </span>
                <span className="flex shrink-0 items-center gap-1.5">
                  {badge != null && badge > 0 ? (
                    <OrgCountBadge count={badge} active={isActive} />
                  ) : null}
                  {isActive ? (
                    <Check className="h-4 w-4 text-[var(--fire-red)] dark:text-[var(--fire-red-light)]" />
                  ) : null}
                </span>
              </button>
            );
          })}
        </div>
      </div>
    ) : null;

  return (
    <>
      <div
        ref={rootRef}
        data-nav-scroll-anchor={scrollAnchor ? true : undefined}
        className={cn(
          triggerShell,
          'scroll-mt-9 scroll-mb-14',
          isOrgRouteActive ? triggerActive : triggerIdle,
        )}
      >
        <button type="button" className={triggerMain} onClick={() => onSelect(displayedOrg.id)}>
          <Building2
            className={cn(
              'h-4 w-4 shrink-0',
              isOrgRouteActive
                ? 'text-[var(--fire-red)] dark:text-[var(--fire-red-light)]'
                : 'text-[var(--text-secondary)]',
            )}
          />

          <span className="flex min-w-0 flex-1 flex-col justify-center gap-0.5">
            <span className="line-clamp-2 leading-snug">{displayedOrg.name}</span>
            <span className="text-[10px] font-normal leading-none text-[var(--text-secondary)]">
              {switchSubtitle}
            </span>
          </span>

          {rowBadge != null && rowBadge > 0 ? (
            <OrgCountBadge count={rowBadge} active={isOrgRouteActive} />
          ) : null}
        </button>

        <button
          type="button"
          aria-label={t('home.navigation.organizations.switchMenu')}
          aria-expanded={open}
          aria-haspopup="menu"
          onClick={() => setOpen((value) => !value)}
          className={cn(
            triggerChevron,
            'text-[var(--text-secondary)] hover:text-[var(--text-primary)]',
            isOrgRouteActive &&
              'text-[var(--fire-red)] dark:text-[var(--fire-red-light)] hover:text-[var(--fire-red)] dark:hover:text-[var(--fire-red-light)]',
          )}
        >
          <ChevronDown
            className={cn('h-4 w-4 transition-transform duration-200', open && 'rotate-180')}
            aria-hidden
          />
        </button>
      </div>
      {typeof document !== 'undefined' && menu ? createPortal(menu, document.body) : null}
    </>
  );
}
