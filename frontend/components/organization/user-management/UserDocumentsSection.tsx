/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { UserDocumentCard } from '@/components/my-area/documents/UserDocumentCard';
import { UserDocumentFormModal } from '@/components/my-area/documents/UserDocumentFormModal';
import { ListContainer } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { userDocumentOrgListDefinition } from '@/lib/list/userDocumentList';
import { getOrganizationUserDocumentDetailPath } from '@/lib/navigation/userManagementSections';
import {
  useOrganizationUserDocumentList,
  useOrganizationUserDocumentListState,
  useOrganizationUserDocumentTags,
  useUpdateOrganizationUserDocument,
  useUploadOrganizationUserDocument,
} from '@/lib/store';
import type { UserDocumentDto } from '@/types/userDocument';

interface UserDocumentsSectionProps {
  organizationId: number;
  userId: number;
  canWrite: boolean;
  isAddOpen: boolean;
  onAddClose: () => void;
}

export function UserDocumentsSection({
  organizationId,
  userId,
  canWrite,
  isAddOpen,
  onAddClose,
}: UserDocumentsSectionProps) {
  const t = useTranslation();
  const { showSuccess } = useToast();
  const listState = useOrganizationUserDocumentListState();
  const {
    data: documents = [],
    isPending,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationUserDocumentList({
    organizationId,
    userId,
    listQuery: listState.listQuery,
  });
  const { data: availableTags = [] } = useOrganizationUserDocumentTags(organizationId, userId);
  const uploadDocument = useUploadOrganizationUserDocument(organizationId, userId);
  const updateDocument = useUpdateOrganizationUserDocument(organizationId, userId);

  const [editTarget, setEditTarget] = useState<UserDocumentDto | null>(null);

  return (
    <div className="space-y-3">
      <PaginatedListView
        definition={userDocumentOrgListDefinition}
        state={listState}
        organizationUserDocumentFilterOptions={{
          scope: 'organizationUserDocuments',
          organizationId,
          userId,
        }}
        totalItems={documents.length}
        isPending={isPending}
        emptyState={paginatedListEmptyCard(t('myArea.documents.empty'))}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer label={t('userManagement.sections.documents')} className="mt-1 sm:mt-0">
          {documents.map((document) => (
            <UserDocumentCard
              key={document.id}
              doc={document}
              href={getOrganizationUserDocumentDetailPath(organizationId, userId, document.id)}
              canWrite={canWrite}
              navigationContext={{ scope: 'org', organizationId, userId }}
              onEdit={() => setEditTarget(document)}
            />
          ))}
        </ListContainer>
      </PaginatedListView>

      {canWrite ? (
        <>
          <UserDocumentFormModal
            isOpen={isAddOpen}
            mode="upload"
            tagSuggestions={availableTags}
            isLoading={uploadDocument.isPending}
            onClose={onAddClose}
            onSubmit={async (params) => {
              await uploadDocument.mutateAsync(params);
              showSuccess(t('myArea.documents.uploadSuccess'));
              onAddClose();
            }}
          />

          <UserDocumentFormModal
            isOpen={editTarget !== null}
            mode="edit"
            initialDocument={editTarget ?? undefined}
            tagSuggestions={availableTags}
            isLoading={updateDocument.isPending}
            onClose={() => setEditTarget(null)}
            onSubmit={async (params) => {
              if (!editTarget) return;
              await updateDocument.mutateAsync({
                documentId: editTarget.id,
                data: {
                  title: params.title,
                  description: params.description,
                  tags: params.tags,
                },
              });
              showSuccess(t('myArea.documents.updateSuccess'));
              setEditTarget(null);
            }}
          />
        </>
      ) : null}
    </div>
  );
}
