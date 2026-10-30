/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ListContainer, ListRow } from '@/components/ui/list';
import { ListControls, ListLoadMore, ListNoResults } from '@/components/ui/list-filter';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, ErrorState } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import { eventAttendanceListDefinition } from '@/lib/list/eventAttendanceList';
import { useEventAttendanceList, useEventAttendanceListState } from '@/lib/store';
import type { EventType } from '@/types/event';

export interface EventAttendanceSectionProps {
  organizationId: number;
  eventType: EventType;
  eventId: number;
  canWriteAttendance: boolean;
  onManage: () => void;
}

export function EventAttendanceSection({
  organizationId,
  eventType,
  eventId,
  canWriteAttendance,
  onManage,
}: EventAttendanceSectionProps) {
  const t = useTranslation();
  const listState = useEventAttendanceListState();
  const {
    data: rows,
    pages,
    isPending,
    isError,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useEventAttendanceList(organizationId, eventType, eventId, listState.listQuery);
  const totalCount = pages?.[0]?.totalElements ?? rows.length;

  return (
    <Card className="p-3 sm:p-4">
      <div className="mb-3">
        <SectionHeader
          className="flex-wrap"
          title={t('events.attendance.title')}
          subtitle={t('events.attendance.countLabel', { count: totalCount })}
          action={
            canWriteAttendance ? (
              <Button
                type="button"
                size="xs"
                variant="primary"
                fullWidth="responsive"
                onClick={onManage}
              >
                {t('events.attendance.edit')}
              </Button>
            ) : null
          }
        />
      </div>

      <ListControls definition={eventAttendanceListDefinition} state={listState} />

      {isPending ? (
        <LoadingState variant="embedded" size="sm" />
      ) : isError ? (
        <ErrorState message={t('events.attendance.loadFailed')} />
      ) : rows.length === 0 ? (
        listState.hasActiveFilters ? (
          <ListNoResults onReset={listState.reset} />
        ) : (
          <EmptyStateCard>{t('events.attendance.emptySection')}</EmptyStateCard>
        )
      ) : (
        <>
          <ListContainer variant="inset">
            {rows.map((row) => (
              <ListRow
                key={row.id}
                accent={false}
                chevron={false}
                compact
                title={
                  <>
                    <span className="font-semibold">{row.lastName}</span>
                    {', '}
                    <span className="font-normal">{row.firstName}</span>
                  </>
                }
              />
            ))}
          </ListContainer>
          <ListLoadMore
            hasNextPage={!!hasNextPage}
            isFetchingNextPage={isFetchingNextPage}
            onLoadMore={() => void fetchNextPage()}
          />
        </>
      )}
    </Card>
  );
}
