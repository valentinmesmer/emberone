/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { ListContainer } from '@/components/ui/list';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { SectionActions } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import { useUrlSyncedListState } from '@/lib/list';
import { userDocumentSelfListDefinition } from '@/lib/list/userDocumentList';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import {
  useMyBeginnableWorkflowTypes,
  useQueryErrorToast,
  useUserDocumentList,
  useUserDocumentTags,
} from '@/lib/store';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';
import { UserDocumentCard } from './UserDocumentCard';
import { UserDocumentMarkAllReadButton } from './UserDocumentMarkAllReadButton';
import { UserDocumentSubmissionRequestModal } from './UserDocumentSubmissionRequestModal';

export function UserDocumentListView() {
  const t = useTranslation();
  const listState = useUrlSyncedListState(userDocumentSelfListDefinition);
  const {
    data: documents = [],
    pages,
    isPending,
    error,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useUserDocumentList({ listQuery: listState.listQuery });
  const { data: tagSuggestions = [] } = useUserDocumentTags();
  const { data: beginnableTypes = [] } = useMyBeginnableWorkflowTypes();
  const canSubmitDocument = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.USER_DOCUMENT_SUBMISSION,
      ),
    [beginnableTypes],
  );
  const [isSubmitOpen, setIsSubmitOpen] = useState(false);

  useQueryErrorToast(error, { messageKey: 'myArea.error.loadFailed' });

  const subtitle = useMemo(() => {
    const count = pages?.[0]?.totalElements ?? documents.length;
    if (count > 0) {
      return t('myArea.documents.count', { count });
    }
    return t('myArea.documents.subtitle');
  }, [documents.length, pages, t]);

  const emptyMessage = canSubmitDocument
    ? t('myArea.documents.emptySubmitAvailable')
    : t('myArea.documents.empty');

  return (
    <>
      <PageHeader
        title={t('myArea.documents.title')}
        description={subtitle}
        size="compact"
        action={
          <SectionActions layout="inline">
            <UserDocumentMarkAllReadButton variant="outline" size="xs" fullWidth="responsive" />
            {canSubmitDocument ? (
              <Button
                type="button"
                onClick={() => setIsSubmitOpen(true)}
                size="xs"
                fullWidth="responsive"
              >
                <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
                {t('myArea.documents.submitRequest')}
              </Button>
            ) : null}
          </SectionActions>
        }
      />

      <PaginatedListView
        definition={userDocumentSelfListDefinition}
        state={listState}
        totalItems={documents.length}
        isPending={isPending}
        emptyState={paginatedListEmptyCard(emptyMessage)}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer label={t('myArea.documents.title')} className="mt-1 sm:mt-0">
          {documents.map((doc) => (
            <UserDocumentCard
              key={doc.id}
              doc={doc}
              href={`${MY_AREA_BASE_PATH}/documents/${doc.id}`}
              canWrite={false}
              isUnread={!doc.viewedAt}
            />
          ))}
        </ListContainer>
      </PaginatedListView>

      {canSubmitDocument ? (
        <UserDocumentSubmissionRequestModal
          isOpen={isSubmitOpen}
          onClose={() => setIsSubmitOpen(false)}
          tagSuggestions={tagSuggestions}
        />
      ) : null}
    </>
  );
}
