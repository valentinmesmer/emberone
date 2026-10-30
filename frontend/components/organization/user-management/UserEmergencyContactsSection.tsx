/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { EmergencyContactCard } from '@/components/my-area/emergency-contact/EmergencyContactCard';
import { EmergencyContactFormModal } from '@/components/my-area/emergency-contact/EmergencyContactFormModal';
import { useEmergencyContactReorder } from '@/components/my-area/emergency-contact/useEmergencyContactReorder';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { getOrganizationUserEmergencyContactDetailPath } from '@/lib/navigation/userManagementSections';
import {
  useCreateOrganizationUserEmergencyContact,
  useDeleteOrganizationUserEmergencyContact,
  useOrganizationUserEmergencyContacts,
  useReorderOrganizationUserEmergencyContacts,
  useUpdateOrganizationUserEmergencyContact,
} from '@/lib/store';
import { cn } from '@/lib/utils/cn';

interface UserEmergencyContactsSectionProps {
  organizationId: number;
  userId: number;
  canWrite: boolean;
  isAddOpen: boolean;
  onAddClose: () => void;
}

export function UserEmergencyContactsSection({
  organizationId,
  userId,
  canWrite,
  isAddOpen,
  onAddClose,
}: UserEmergencyContactsSectionProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: contacts = [], isLoading } = useOrganizationUserEmergencyContacts(
    organizationId,
    userId,
  );
  const createContact = useCreateOrganizationUserEmergencyContact(organizationId, userId);
  const updateContact = useUpdateOrganizationUserEmergencyContact(organizationId, userId);
  const deleteContact = useDeleteOrganizationUserEmergencyContact(organizationId, userId);
  const reorderContacts = useReorderOrganizationUserEmergencyContacts(organizationId, userId);

  const [editingContactId, setEditingContactId] = useState<number | null>(null);
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);

  const actionsBusy =
    reorderContacts.isPending ||
    deleteContact.isPending ||
    updateContact.isPending ||
    createContact.isPending;

  const reorder = useEmergencyContactReorder({
    contacts,
    disabled: !canWrite || reorderContacts.isPending,
    onReorder: async (orderedIds) => {
      try {
        await reorderContacts.mutateAsync(orderedIds);
        showSuccess(t('myArea.emergencyContact.success.reordered'));
      } catch (err) {
        showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
      }
    },
  });

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  return (
    <>
      {contacts.length === 0 ? (
        <EmptyStateCard>{t('userManagement.emergencyContacts.empty')}</EmptyStateCard>
      ) : (
        <ListContainer
          label={t('userManagement.emergencyContacts.title')}
          className={cn(reorder.isDragging && 'select-none')}
        >
          {contacts.map((contact) => (
            <EmergencyContactCard
              key={contact.id}
              contact={contact}
              href={getOrganizationUserEmergencyContactDetailPath(
                organizationId,
                userId,
                contact.id,
              )}
              isReordering={canWrite && reorderContacts.isPending}
              isDragging={reorder.draggedId === contact.id}
              isDragOver={reorder.dragOverId === contact.id}
              reorderable={canWrite && contacts.length > 1}
              onReorderPointerDown={(e) => reorder.onHandlePointerDown(contact.id, e)}
              onEdit={
                canWrite
                  ? () => {
                      onAddClose();
                      setEditingContactId(contact.id);
                    }
                  : () => {}
              }
              onDelete={canWrite ? () => setDeleteTargetId(contact.id) : () => {}}
              actionsDisabled={!canWrite || actionsBusy}
            />
          ))}
        </ListContainer>
      )}

      {canWrite ? (
        <>
          <EmergencyContactFormModal
            isOpen={isAddOpen || editingContactId !== null}
            onClose={() => {
              onAddClose();
              setEditingContactId(null);
            }}
            contact={
              editingContactId !== null
                ? contacts.find((c) => c.id === editingContactId)
                : undefined
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
      ) : null}
    </>
  );
}
