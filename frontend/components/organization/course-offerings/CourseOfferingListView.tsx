/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { AddCourseOfferingModal } from '@/components/organization/course-offerings/AddCourseOfferingModal';
import { CourseOfferingTemplatesModal } from '@/components/organization/course-offerings/CourseOfferingTemplatesModal';
import { CreateCourseOfferingModal } from '@/components/organization/course-offerings/CreateCourseOfferingModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { SectionActions } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { orgCourseOfferingListDefinition } from '@/lib/list/courseOfferingList';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useCreateCourseOffering,
  useOrganizationById,
  useOrganizationCourseOfferings,
  useOrgCourseOfferingListState,
} from '@/lib/store';
import { getOrganizationCourseOfferingDetailPath } from '@/lib/utils';
import { courseOfferingStatusLabelKey } from '@/lib/utils/courseOffering';
import { formatDateShort } from '@/lib/utils/date';
import type { CourseOfferingDto } from '@/types/courseOffering';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
}

function offeringStatusTone(
  status: CourseOfferingDto['status'],
): 'neutral' | 'accent' | 'success' | 'warning' | 'danger' {
  switch (status) {
    case 'PUBLISHED':
      return 'success';
    case 'DRAFT':
      return 'accent';
    case 'COMPLETED':
      return 'neutral';
    case 'CANCELLED':
      return 'danger';
    default:
      return 'neutral';
  }
}

export function CourseOfferingListView({ organizationId }: Props) {
  const t = useTranslation();
  const router = useRouter();
  const { showSuccess } = useToast();
  const { hasPermission } = usePermissions();
  const listState = useOrgCourseOfferingListState();

  const canRead = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_READ);
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_WRITE);

  const { data: organization, isLoading: orgLoading } = useOrganizationById(
    canRead ? organizationId : null,
  );

  const { data, isPending, isError, hasNextPage, isFetchingNextPage, fetchNextPage } =
    useOrganizationCourseOfferings({
      organizationId: canRead ? organizationId : null,
      listQuery: listState.listQuery,
      enabled: canRead,
    });

  const offerings = data ?? [];

  const createDraft = useCreateCourseOffering(organizationId);

  const [dialog, setDialog] = useState<'add' | 'templates' | 'create' | null>(null);

  if (orgLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!canRead) {
    return <p className="text-[var(--text-secondary)]">{t('organization.manage.noPermission')}</p>;
  }

  if (!organization) {
    return <ErrorState message={t('organization.errors.notFound')} className="mt-4" />;
  }

  return (
    <>
      <PageHeader
        title={t('courseOfferings.title')}
        description={organization.name}
        size="compact"
        action={
          canWrite ? (
            <SectionActions layout="inline">
              <Button
                variant="secondary"
                size="xs"
                fullWidth="responsive"
                onClick={() => setDialog('templates')}
              >
                {t('courseOfferings.templates.title')}
              </Button>
              <Button
                variant="primary"
                size="xs"
                fullWidth="responsive"
                onClick={() => setDialog('add')}
              >
                {t('common.add')}
              </Button>
            </SectionActions>
          ) : null
        }
      />

      <PaginatedListView
        definition={orgCourseOfferingListDefinition}
        state={listState}
        organizationCourseOfferingFilterOptions={{
          scope: 'organizationCourseOfferings',
          organizationId,
        }}
        totalItems={offerings.length}
        isPending={isPending}
        error={isError ? t('courseOfferings.list.loadFailed') : undefined}
        emptyState={paginatedListEmptyCard(t('courseOfferings.list.empty'))}
        hasNextPage={hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer>
          {offerings.map((offering) => {
            const detailHref = getOrganizationCourseOfferingDetailPath(organizationId, offering.id);
            const subtitle = offering.content?.trim() || undefined;

            return (
              <ListRow
                key={offering.id}
                href={detailHref}
                title={offering.trainingCourseTypeName}
                subtitle={subtitle}
                meta={
                  offering.firstSessionStart ? (
                    <span className="text-xs text-[var(--text-secondary)]">
                      {t('courseOfferings.fields.courseStart')}:{' '}
                      {formatDateShort(offering.firstSessionStart)}
                    </span>
                  ) : undefined
                }
                trailing={
                  <Badge tone={offeringStatusTone(offering.status)}>
                    {t(courseOfferingStatusLabelKey(offering.status))}
                  </Badge>
                }
              />
            );
          })}
        </ListContainer>
      </PaginatedListView>

      {dialog === 'add' ? (
        <AddCourseOfferingModal
          organizationId={organizationId}
          onClose={() => setDialog(null)}
          onCreateNew={() => setDialog('create')}
          onCreated={(created) => {
            setDialog(null);
            router.push(getOrganizationCourseOfferingDetailPath(organizationId, created.id));
          }}
        />
      ) : null}

      {dialog === 'templates' ? (
        <CourseOfferingTemplatesModal
          organizationId={organizationId}
          canWrite={canWrite}
          onClose={() => setDialog(null)}
        />
      ) : null}

      {dialog === 'create' ? (
        <CreateCourseOfferingModal
          isOpen
          organizationId={organizationId}
          onClose={() => setDialog(null)}
          isLoading={createDraft.isPending}
          onSubmit={async (payload) => {
            const created = await createDraft.mutateAsync(payload);
            setDialog(null);
            showSuccess(t('courseOfferings.create.success'));
            router.push(getOrganizationCourseOfferingDetailPath(organizationId, created.id));
          }}
        />
      ) : null}
    </>
  );
}
