/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { getEmergencyContactRelationshipLabelKey } from '@/lib/my-area/emergencyContactOptions';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import {
  useDeleteEmergencyContact,
  useEmergencyContacts,
  useUpdateEmergencyContact,
} from '@/lib/store';
import { formatAddress } from '@/lib/utils/address';
import { EmergencyContactFormModal } from './EmergencyContactFormModal';

export interface MyAreaEmergencyContactDetailViewProps {
  contactId: number;
}

export function MyAreaEmergencyContactDetailView({
  contactId,
}: MyAreaEmergencyContactDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const { data: contacts = [], isLoading } = useEmergencyContacts();
  const updateContact = useUpdateEmergencyContact();
  const deleteContact = useDeleteEmergencyContact();

  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);

  const contact = contacts.find((c) => c.id === contactId);
  const loading = isLoading;

  const fullName = contact ? `${contact.firstName} ${contact.lastName}`.trim() : '';
  const relationshipLabel = contact
    ? t(getEmergencyContactRelationshipLabelKey(contact.relationship))
    : '';

  const address = contact
    ? formatAddress(contact.street, contact.city, contact.postalCode, contact.country)
    : '';

  const handleDelete = async () => {
    if (!contact) return;
    try {
      await deleteContact.mutateAsync(contact.id);
      showSuccess(t('myArea.emergencyContact.success.deleted'));
      router.push(`${MY_AREA_BASE_PATH}/emergency-contact`);
    } catch (err) {
      showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!contact) {
    return <NotFoundState message={t('myArea.emergencyContact.error.notFound')} />;
  }

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          title={fullName}
          subtitle={relationshipLabel}
          actions={
            <>
              <DetailCardAction
                label={t('myArea.emergencyContact.editTitle')}
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={() => setIsEditOpen(true)}
              />
              <DetailCardAction
                label={t('myArea.emergencyContact.delete')}
                icon={<Trash2 className="h-4 w-4" aria-hidden />}
                onClick={() => setIsDeleteOpen(true)}
              />
            </>
          }
        />

        <div className="space-y-4">
          <FieldDisplay
            label={t('myArea.emergencyContact.fields.priority')}
            value={String(contact.priority)}
          />
          <FieldDisplay
            label={t('myArea.emergencyContact.fields.firstName')}
            value={contact.firstName}
          />
          <FieldDisplay
            label={t('myArea.emergencyContact.fields.lastName')}
            value={contact.lastName}
          />
          <FieldDisplay
            label={t('myArea.emergencyContact.fields.address')}
            value={address || t('common.notAvailable')}
          />
          <FieldDisplay label={t('myArea.emergencyContact.fields.phone')} value={contact.phone} />
          <FieldDisplay
            label={t('myArea.emergencyContact.fields.email')}
            value={contact.email?.trim() ? contact.email : t('common.notAvailable')}
          />
        </div>
      </Card>

      <EmergencyContactFormModal
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        contact={contact}
        onSave={async (data) => {
          await updateContact.mutateAsync({ contactId: contact.id, data });
        }}
      />

      <ConfirmDialog
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        onConfirm={() => void handleDelete()}
        title={t('myArea.emergencyContact.deleteConfirmTitle')}
        message={t('myArea.emergencyContact.deleteConfirm')}
        confirmText={t('common.delete')}
        isLoading={deleteContact.isPending}
      />
    </div>
  );
}
