/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ClipboardList } from 'lucide-react';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import { useEffect, useMemo, useRef, useState } from 'react';
import { CourseOfferingRegisterDialog } from '@/components/my-area/courseOfferings/CourseOfferingRegisterDialog';
import {
  formatOfferingCoursePeriod,
  type OfferingStatusBadge,
  offeringRegistrationStateBadge,
  type Translate,
} from '@/components/my-area/courseOfferings/selfCourseOfferingDisplay';
import { Badge } from '@/components/ui/badge';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { ListLoadMore } from '@/components/ui/list-filter';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import {
  selfCourseOfferingListDefinition,
  selfCourseOfferingListQuery,
} from '@/lib/list/courseOfferingList';
import { getMyAreaCourseOfferingDetailPath } from '@/lib/my-area/myAreaSections';
import { useSelfCourseOfferings, useUrlSyncedSelfCourseOfferingListState } from '@/lib/store';
import type { SelfCourseOfferingDto } from '@/types/courseOffering';

export function SelfCourseOfferingList() {
  const t = useTranslation();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const listState = useUrlSyncedSelfCourseOfferingListState();

  const migratedLegacyTab = useRef(false);
  useEffect(() => {
    if (migratedLegacyTab.current || searchParams.get('tab') !== 'registrations') {
      return;
    }
    migratedLegacyTab.current = true;
    const params = new URLSearchParams(searchParams.toString());
    params.delete('tab');
    const qs = params.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  }, [pathname, router, searchParams]);

  const listQuery = useMemo(
    () =>
      selfCourseOfferingListQuery({
        debouncedSearch: listState.debouncedSearch,
        filters: listState.filters,
        sort: listState.sort,
      }),
    [listState.debouncedSearch, listState.filters, listState.sort],
  );

  const offeringsQuery = useSelfCourseOfferings({ listQuery });
  const [registerTarget, setRegisterTarget] = useState<SelfCourseOfferingDto | null>(null);

  const offerings = offeringsQuery.data ?? [];
  const emptyState = paginatedListEmptyCard(t('myArea.courseRegistrations.offerings.empty'));

  return (
    <>
      <PaginatedListView
        definition={selfCourseOfferingListDefinition}
        state={listState}
        totalItems={offerings.length}
        isPending={offeringsQuery.isPending}
        emptyState={emptyState}
      >
        <ListContainer label={t('myArea.courseRegistrations.offerings.listLabel')}>
          {offerings.map((offering) => {
            const myRegistration = offering.myRegistration;
            const canRegister =
              offering.registrationOpen && offering.prerequisitesMet && myRegistration == null;
            return (
              <ListRow
                key={offering.id}
                href={getMyAreaCourseOfferingDetailPath(offering.id)}
                keepActionsOnMobile
                title={offering.trainingCourseTypeName}
                subtitle={formatOfferingCoursePeriod(offering, t)}
                badges={offeringListStatusBadges(offering, t).map((badge) => (
                  <Badge key={badge.label} tone={badge.tone} pill>
                    {badge.label}
                  </Badge>
                ))}
                actions={
                  canRegister ? (
                    <IconButton
                      variant="action"
                      size="sm"
                      icon={<ClipboardList className="h-4 w-4" aria-hidden />}
                      aria-label={t('myArea.courseRegistrations.register.action')}
                      title={t('myArea.courseRegistrations.register.action')}
                      onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        setRegisterTarget(offering);
                      }}
                    />
                  ) : undefined
                }
              />
            );
          })}
        </ListContainer>
        <ListLoadMore
          hasNextPage={!!offeringsQuery.hasNextPage}
          isFetchingNextPage={offeringsQuery.isFetchingNextPage}
          onLoadMore={() => void offeringsQuery.fetchNextPage()}
        />
      </PaginatedListView>

      {registerTarget ? (
        <CourseOfferingRegisterDialog
          offering={registerTarget}
          onClose={() => setRegisterTarget(null)}
        />
      ) : null}
    </>
  );
}

/** Held and prerequisite markers first, then the registration state. */
function offeringListStatusBadges(
  offering: SelfCourseOfferingDto,
  t: Translate,
): OfferingStatusBadge[] {
  const badges: OfferingStatusBadge[] = [];
  if (offering.alreadyHeld) {
    badges.push({
      tone: 'accent',
      label: t('myArea.courseRegistrations.offerings.alreadyHeld'),
    });
  }
  if (!offering.prerequisitesMet && !offering.myRegistration) {
    badges.push({
      tone: 'warning',
      label: t('myArea.courseRegistrations.offerings.prerequisitesMissing'),
    });
  }
  badges.push(offeringRegistrationStateBadge(offering, t));
  return badges;
}
