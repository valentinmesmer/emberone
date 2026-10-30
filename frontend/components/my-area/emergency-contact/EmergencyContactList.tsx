/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import {
  useCreateEmergencyContact,
  useDeleteEmergencyContact,
  useEmergencyContacts,
  useReorderEmergencyContacts,
  useUpdateEmergencyContact,
} from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import { EmergencyContactCard } from './EmergencyContactCard';
import { EmergencyContactFormModal } from './EmergencyContactFormModal';
import { useEmergencyContactReorder } from './useEmergencyContactReorder';

export function EmergencyContactList() {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: contacts = [], isLoading, error } = useEmergencyContacts();
  const createContact = useCreateEmergencyContact();
  const updateContact = useUpdateEmergencyContact();
  const deleteContact = useDeleteEmergencyContact();
  const reorderContacts = useReorderEmergencyContacts();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingContactId, setEditingContactId] = useState<number | null>(null);
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);

  const openAddForm = () => {
    setEditingContactId(null);
    setIsFormOpen(true);
  };

  const actionsBusy =
    reorderContacts.isPending ||
    deleteContact.isPending ||
    updateContact.isPending ||
    createContact.isPending;

  const reorder = useEmergencyContactReorder({
    contacts,
    disabled: reorderContacts.isPending,
    onReorder: async (orderedIds) => {
      try {
        await reorderContacts.mutateAsync(orderedIds);
        showSuccess(t('myArea.emergencyContact.success.reordered'));
      } catch (err) {
        showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
      }
    },
  });

  const subtitle =
    contacts.length > 1
      ? `${t('myArea.emergencyContact.count', { count: contacts.length })} · ${t('myArea.emergencyContact.dragHint')}`
      : contacts.length === 1
        ? t('myArea.emergencyContact.count', { count: contacts.length })
        : t('myArea.emergencyContact.subtitle');

  const addLabel = t('common.add');

  if (error) {
    return (
      <>
        <PageHeader
          title={t('myArea.emergencyContact.title')}
          description={subtitle}
          size="compact"
        />
        <ErrorState message={t('myArea.error.loadFailed')} className="mt-1 sm:mt-0" />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title={t('myArea.emergencyContact.title')}
        description={subtitle}
        size="compact"
        action={
          <Button type="button" onClick={openAddForm} size="xs" fullWidth="responsive">
            <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
            {addLabel}
          </Button>
        }
      />

      <div className="mt-1 w-full sm:mt-0">
        {isLoading ? (
          <LoadingState variant="embedded" size="sm" />
        ) : contacts.length === 0 ? (
          <EmptyStateCard>{t('myArea.emergencyContact.empty')}</EmptyStateCard>
        ) : (
          <ListContainer
            label={t('myArea.emergencyContact.title')}
            className={cn(reorder.isDragging && 'select-none')}
          >
            {contacts.map((contact) => (
              <EmergencyContactCard
                key={contact.id}
                contact={contact}
                href={`${MY_AREA_BASE_PATH}/emergency-contact/${contact.id}`}
                isReordering={reorderContacts.isPending}
                isDragging={reorder.draggedId === contact.id}
                isDragOver={reorder.dragOverId === contact.id}
                reorderable={contacts.length > 1}
                onReorderPointerDown={(e) => reorder.onHandlePointerDown(contact.id, e)}
                onEdit={() => {
                  setIsFormOpen(false);
                  setEditingContactId(contact.id);
                }}
                onDelete={() => setDeleteTargetId(contact.id)}
                actionsDisabled={actionsBusy}
              />
            ))}
          </ListContainer>
        )}
      </div>

      <EmergencyContactFormModal
        isOpen={isFormOpen || editingContactId !== null}
        onClose={() => {
          setIsFormOpen(false);
          setEditingContactId(null);
        }}
        contact={
          editingContactId !== null ? contacts.find((c) => c.id === editingContactId) : undefined
        }
        onSave={async (data) => {
          if (editingContactId !== null) {
            await updateContact.mutateAsync({ contactId: editingContactId, data });
          } else {
            await createContact.mutateAsync(data);
          }
        }}
      />

      <ConfirmDialog
        isOpen={deleteTargetId !== null}
        onClose={() => setDeleteTargetId(null)}
        onConfirm={async () => {
          if (deleteTargetId === null) return;
          try {
            await deleteContact.mutateAsync(deleteTargetId);
            showSuccess(t('myArea.emergencyContact.success.deleted'));
            setDeleteTargetId(null);
          } catch (err) {
            showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
          }
        }}
        title={t('myArea.emergencyContact.deleteConfirmTitle')}
        message={t('myArea.emergencyContact.deleteConfirm')}
        confirmText={t('common.delete')}
        isLoading={deleteContact.isPending}
      />
    </>
  );
}
