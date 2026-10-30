/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import Link from 'next/link';
import { useLayoutEffect, useMemo, useRef, useState } from 'react';
import { QualificationOrgProgressBadge } from '@/components/my-area/qualifications/QualificationOrgProgressBadge';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useTranslation } from '@/hooks/useTranslation';
import { getMyAreaQualificationsPath } from '@/lib/my-area/myAreaSections';
import {
  type AggregatedQualificationItem,
  aggregateSelfQualificationStatuses,
  qualificationListBadgeLabel,
} from '@/lib/qualifications/aggregateQualifications';
import { useMyOrganizations, useSelfQualificationStatus } from '@/lib/store';

const BADGE_GAP_PX = 6;
/** Approximate width of the +N chip including padding. */
const OVERFLOW_CHIP_RESERVE_PX = 36;

const pillClassName = 'max-w-[4.75rem] truncate px-2 py-1 text-[11px] leading-none sm:text-xs';

function countFittingPills(
  availableWidth: number,
  widths: number[],
  reserveOverflowChip: boolean,
): number {
  if (availableWidth <= 0 || widths.length === 0) return 0;

  const reserve = reserveOverflowChip ? BADGE_GAP_PX + OVERFLOW_CHIP_RESERVE_PX : 0;
  let used = 0;
  let count = 0;

  for (let i = 0; i < widths.length; i += 1) {
    const next = used + (i > 0 ? BADGE_GAP_PX : 0) + widths[i];
    if (next + reserve > availableWidth) break;
    used = next;
    count = i + 1;
  }

  return count;
}

function useVisiblePillCount(itemCount: number) {
  const containerRef = useRef<HTMLDivElement>(null);
  const measureRef = useRef<HTMLUListElement>(null);
  const [visibleCount, setVisibleCount] = useState(itemCount);

  useLayoutEffect(() => {
    const container = containerRef.current;
    const measure = measureRef.current;
    if (!container || !measure || itemCount === 0) {
      setVisibleCount(itemCount);
      return;
    }

    const update = () => {
      const available = container.clientWidth;
      const children = Array.from(measure.children) as HTMLElement[];
      const widths = children.slice(0, itemCount).map((child) => child.offsetWidth);

      const fitAll = countFittingPills(available, widths, false);
      if (fitAll >= itemCount) {
        setVisibleCount(itemCount);
        return;
      }

      const fitWithOverflow = countFittingPills(available, widths, true);
      setVisibleCount(Math.max(1, fitWithOverflow));
    };

    update();

    const observer = new ResizeObserver(update);
    observer.observe(container);
    return () => observer.disconnect();
  }, [itemCount]);

  return { containerRef, measureRef, visibleCount };
}

function QualificationPills({ items }: { items: AggregatedQualificationItem[] }) {
  const t = useTranslation();
  const { containerRef, measureRef, visibleCount } = useVisiblePillCount(items.length);

  const overflowCount = Math.max(0, items.length - visibleCount);
  const visibleItems = items.slice(0, visibleCount);

  return (
    <div ref={containerRef} className="relative w-full min-w-0">
      <ul
        ref={measureRef}
        aria-hidden
        className="pointer-events-none absolute inset-x-0 top-0 flex w-full list-none flex-nowrap gap-1.5 overflow-hidden p-0 opacity-0"
      >
        {items.map((item) => (
          <li key={item.tenantQualificationDefinitionId} className="shrink-0">
            {item.orgDecisionProgressRatio != null ? (
              <QualificationOrgProgressBadge
                ratio={item.orgDecisionProgressRatio}
                pill
                className={pillClassName}
              >
                {item.abbreviation}
              </QualificationOrgProgressBadge>
            ) : (
              <Badge tone={item.badgeTone} pill className={pillClassName}>
                {item.abbreviation}
              </Badge>
            )}
          </li>
        ))}
      </ul>

      <ul className="flex w-full list-none flex-nowrap items-center gap-1.5 overflow-hidden p-0">
        {visibleItems.map((item) => {
          const statusLabel = qualificationListBadgeLabel(item, t);
          return (
            <li key={item.tenantQualificationDefinitionId} className="min-w-0 shrink-0">
              <span title={`${item.name} · ${statusLabel}`}>
                {item.orgDecisionProgressRatio != null ? (
                  <QualificationOrgProgressBadge
                    ratio={item.orgDecisionProgressRatio}
                    pill
                    className={pillClassName}
                  >
                    {item.abbreviation}
                  </QualificationOrgProgressBadge>
                ) : (
                  <Badge tone={item.badgeTone} pill className={pillClassName}>
                    {item.abbreviation}
                  </Badge>
                )}
              </span>
            </li>
          );
        })}
        {overflowCount > 0 ? (
          <li className="shrink-0">
            <Badge
              tone="neutral"
              pill
              className="px-2 py-1 text-[11px] leading-none tabular-nums sm:text-xs"
              aria-label={t('home.dashboard.stats.qualifications.moreAria', {
                count: overflowCount,
              })}
            >
              {t('home.dashboard.stats.qualifications.more', { count: overflowCount })}
            </Badge>
          </li>
        ) : null}
      </ul>
    </div>
  );
}

export function HomeQualificationsStatCard() {
  const t = useTranslation();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const {
    data: statuses = [],
    isPending: isPendingStatus,
    isFetching: isFetchingStatus,
  } = useSelfQualificationStatus(hasEmberOneUserId);
  const {
    data: memberships = [],
    isPending: isPendingOrgs,
    isFetching: isFetchingOrgs,
  } = useMyOrganizations();

  const isLoading = (isPendingStatus && isFetchingStatus) || (isPendingOrgs && isFetchingOrgs);

  const organizationNameById = useMemo(
    () =>
      new Map(
        memberships
          .map((membership) => membership.organization)
          .filter(
            (organization): organization is NonNullable<typeof organization> =>
              organization != null,
          )
          .map((organization) => [organization.id, organization.name] as const),
      ),
    [memberships],
  );

  const items = useMemo(
    () => aggregateSelfQualificationStatuses(statuses, organizationNameById),
    [organizationNameById, statuses],
  );

  // Remeasure when abbreviation set changes, not only when count changes.
  const itemsMeasureKey = useMemo(
    () =>
      items.map((item) => `${item.tenantQualificationDefinitionId}:${item.abbreviation}`).join('|'),
    [items],
  );

  const detailPath = getMyAreaQualificationsPath();

  const content = (
    <div className="flex min-h-[4.75rem] flex-col justify-between gap-2 sm:min-h-[5rem]">
      <p className="text-[10px] font-semibold uppercase tracking-wide text-[var(--text-secondary)] sm:text-[11px]">
        {t('home.dashboard.stats.qualifications.title')}
      </p>

      <div className="relative min-h-[1.5rem] w-full min-w-0">
        {!hasEmberOneUserId ? (
          <p className="text-xs leading-snug text-[var(--text-secondary)]">
            {t('home.dashboard.stats.qualifications.unavailable')}
          </p>
        ) : isLoading ? (
          <p className="text-lg font-bold leading-none text-[var(--text-secondary)] sm:text-xl">
            …
          </p>
        ) : items.length === 0 ? (
          <p className="text-xs leading-snug text-[var(--text-secondary)]">
            {t('home.dashboard.stats.qualifications.empty')}
          </p>
        ) : (
          <QualificationPills key={itemsMeasureKey} items={items} />
        )}
      </div>

      <p className="text-[11px] leading-snug text-[var(--text-secondary)] sm:text-xs">
        {t('home.dashboard.stats.qualifications.subtitle')}
      </p>
    </div>
  );

  return (
    <Card className="!p-0">
      {hasEmberOneUserId ? (
        <Link
          href={detailPath}
          className="block h-full w-full p-2.5 text-left outline-none ring-offset-2 ring-offset-[var(--page-bg)] transition-colors hover:bg-[var(--hover-bg)]/40 focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] sm:p-3"
        >
          {content}
        </Link>
      ) : (
        <div className="h-full w-full p-2.5 text-left sm:p-3">{content}</div>
      )}
    </Card>
  );
}
