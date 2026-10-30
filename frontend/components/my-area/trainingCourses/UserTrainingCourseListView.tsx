/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Upload } from 'lucide-react';
import { useMemo, useState } from 'react';
import { TrainingCourseRecordSubmitModal } from '@/components/my-area/trainingCourses/TrainingCourseRecordSubmitModal';
import { TrainingCourseSubmissionListRow } from '@/components/my-area/trainingCourses/TrainingCourseSubmissionListRow';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ListContainer } from '@/components/ui/list';
import { ListLoadMore, ListNoResults } from '@/components/ui/list-filter';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import {
  getMyAreaTrainingCourseRecordDetailPath,
  getMyAreaWorkflowDetailPath,
} from '@/lib/my-area/myAreaSections';
import {
  useMyAvailableTrainingCourseTypes,
  useMyBeginnableWorkflowTypes,
  useMyTrainingCourseRecordList,
  useMyWorkflowOpenedPage,
  useUrlSyncedTrainingCourseRecordListState,
  useWorkflowOpenConcurrencyKeys,
} from '@/lib/store';
import {
  filterSubmittableTrainingCourseTypes,
  getOpenTrainingCourseWorkflows,
} from '@/lib/trainingCourses/submittableTrainingCourses';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';

export function UserTrainingCourseListView() {
  const t = useTranslation();
  const listState = useUrlSyncedTrainingCourseRecordListState();
  const {
    data: records = [],
    pages,
    isPending: recordsLoading,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useMyTrainingCourseRecordList({ listQuery: listState.listQuery });
  const { data: openedWorkflows = [] } = useMyWorkflowOpenedPage();
  const { data: beginnableTypes = [] } = useMyBeginnableWorkflowTypes();
  const { data: availableTypes = [], isLoading: catalogLoading } =
    useMyAvailableTrainingCourseTypes();

  const [submitOpen, setSubmitOpen] = useState(false);

  const openTrainingCourseWorkflows = useMemo(
    () => getOpenTrainingCourseWorkflows(openedWorkflows),
    [openedWorkflows],
  );
  const trainingCourseWorkflowBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD,
      ),
    [beginnableTypes],
  );
  const { data: trainingOpenKeys = [] } = useWorkflowOpenConcurrencyKeys(
    WORKFLOW_TYPE_IDS.TRAINING_COURSE_RECORD,
    trainingCourseWorkflowBeginnable,
  );
  const submittableTypes = useMemo(
    () => filterSubmittableTrainingCourseTypes(availableTypes, trainingOpenKeys),
    [availableTypes, trainingOpenKeys],
  );

  const resolveTypeName = (typeId: number) =>
    availableTypes.find((type) => type.id === typeId)?.name ?? `#${typeId}`;

  const canOpenSubmitWorkflow = trainingCourseWorkflowBeginnable && submittableTypes.length > 0;

  const subtitle = useMemo(() => {
    const count = pages?.[0]?.totalElements ?? records.length;
    if (count > 0) {
      return t('trainingCourse.count', { count });
    }
    return t('trainingCourse.subtitle');
  }, [pages, records.length, t]);

  const isLoading = recordsLoading || catalogLoading;
  const hasOpenWorkflows = openTrainingCourseWorkflows.length > 0;

  return (
    <>
      <PageHeader
        title={t('trainingCourse.title')}
        description={subtitle}
        size="compact"
        action={
          canOpenSubmitWorkflow ? (
            <Button
              type="button"
              onClick={() => setSubmitOpen(true)}
              size="xs"
              fullWidth="responsive"
            >
              <Upload className="mr-1 h-3.5 w-3.5" aria-hidden />
              {t('trainingCourse.submit.action')}
            </Button>
          ) : undefined
        }
      />

      <div className="mt-1 w-full sm:mt-0">
        {isLoading ? (
          <LoadingState variant="embedded" size="sm" />
        ) : records.length === 0 && !hasOpenWorkflows ? (
          listState.hasActiveFilters ? (
            <ListNoResults onReset={listState.reset} />
          ) : (
            <EmptyStateCard>{t('trainingCourse.empty')}</EmptyStateCard>
          )
        ) : (
          <ListContainer label={t('trainingCourse.title')}>
            {openTrainingCourseWorkflows.map(({ workflow, typeIds, payload }) => (
              <TrainingCourseSubmissionListRow
                key={workflow.id}
                record={{
                  id: workflow.id,
                  userId: workflow.openedByUserId,
                  name:
                    payload.name?.trim() ||
                    (typeIds.length === 1
                      ? resolveTypeName(typeIds[0])
                      : t('trainingCourse.record.detailTitle')),
                  startDate: payload.startDate,
                  endDate: payload.endDate,
                  location: payload.location,
                  hours: payload.hours ?? null,
                  userDocumentIds: [],
                  grantedTypes: typeIds.map((typeId) => ({
                    tenantTrainingCourseTypeId: typeId,
                    courseTypeName: resolveTypeName(typeId),
                    typeArchived: false,
                  })),
                  notes: payload.notes ?? null,
                  appLinkType: null,
                  appLinkId: workflow.id,
                  navigationPath: getMyAreaWorkflowDetailPath(workflow.id),
                }}
                href={getMyAreaWorkflowDetailPath(workflow.id)}
                tone="warning"
                badges={<Badge tone="warning">{t('certifications.status.IN_REVIEW')}</Badge>}
              />
            ))}
            {records.map((record) => (
              <TrainingCourseSubmissionListRow
                key={record.id}
                record={record}
                href={getMyAreaTrainingCourseRecordDetailPath(record.id)}
                badges={
                  record.grantedTypes.some((type) => type.typeArchived) ? (
                    <Badge tone="neutral">{t('trainingCourse.archivedTag')}</Badge>
                  ) : undefined
                }
              />
            ))}
          </ListContainer>
        )}
        <ListLoadMore
          hasNextPage={hasNextPage}
          isFetchingNextPage={isFetchingNextPage}
          onLoadMore={() => void fetchNextPage()}
        />
      </div>

      <TrainingCourseRecordSubmitModal
        isOpen={submitOpen}
        onClose={() => setSubmitOpen(false)}
        submittableTypes={submittableTypes}
      />
    </>
  );
}
