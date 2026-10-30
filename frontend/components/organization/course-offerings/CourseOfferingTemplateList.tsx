/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Trash2 } from 'lucide-react';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import { orgCourseOfferingTemplateListDefinition } from '@/lib/list/courseOfferingList';
import {
  useOrganizationCourseOfferingTemplates,
  useOrgCourseOfferingTemplateListState,
} from '@/lib/store';
import type { CourseOfferingDto } from '@/types/courseOffering';

interface Props {
  organizationId: number;
  onSelect: (template: CourseOfferingDto) => void;
  /** Adds a delete action to every row. */
  onDelete?: (template: CourseOfferingDto) => void;
}

/** Searchable, paginated templates of an organization; the caller decides what a click does. */
export function CourseOfferingTemplateList({ organizationId, onSelect, onDelete }: Props) {
  const t = useTranslation();
  const listState = useOrgCourseOfferingTemplateListState();
  const { data, isPending, isError, hasNextPage, isFetchingNextPage, fetchNextPage } =
    useOrganizationCourseOfferingTemplates({
      organizationId,
      listQuery: listState.listQuery,
    });
  const templates = data ?? [];

  return (
    <PaginatedListView
      definition={orgCourseOfferingTemplateListDefinition}
      state={listState}
      totalItems={templates.length}
      isPending={isPending}
      error={isError ? t('courseOfferings.templates.loadFailed') : undefined}
      emptyState={paginatedListEmptyCard(t('courseOfferings.templates.empty'))}
      hasNextPage={hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      onLoadMore={() => void fetchNextPage()}
    >
      <ListContainer variant="inset">
        {templates.map((template) => (
          <ListRow
            key={template.id}
            title={template.templateName}
            subtitle={template.trainingCourseTypeName}
            onClick={() => onSelect(template)}
            actions={
              onDelete ? (
                <IconButton
                  variant="action"
                  aria-label={t('common.delete')}
                  icon={<Trash2 className="h-4 w-4" aria-hidden />}
                  onClick={() => onDelete(template)}
                />
              ) : undefined
            }
          />
        ))}
      </ListContainer>
    </PaginatedListView>
  );
}
