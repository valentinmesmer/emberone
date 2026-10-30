/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Mail, MapPin, Pencil, Phone, Plus, Trash2 } from 'lucide-react';
import { forwardRef, useImperativeHandle, useState } from 'react';
import { readOnboardingMinimumCount } from '@/components/admin/standards/onboardingStepUtils';
import { EmergencyContactFormModal } from '@/components/my-area/emergency-contact/EmergencyContactFormModal';
import { Button } from '@/components/ui/button';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow, ListRowMeta, ListRowMetaItem } from '@/components/ui/list';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { getEmergencyContactRelationshipLabelKey } from '@/lib/my-area/emergencyContactOptions';
import { readEmergencyContacts } from '@/lib/onboarding/stepPayloads';
import { formatAddress } from '@/lib/utils/address';
import type { CreateEmergencyContactRequest, EmergencyContactDto } from '@/types/emergencyContact';
import type { OnboardingEmergencyContactDraft } from '@/types/onboardingInvitation';
import type { OnboardingStepHandle, OnboardingStepProps } from './onboardingStepHandle';
import { payloadsEqual } from './onboardingStepHandle';

interface OnboardingEmergencyContactsStepProps extends OnboardingStepProps {
  config: Record<string, unknown>;
}

type ContactRow = OnboardingEmergencyContactDraft & { key: string };

const toEmergencyContactsPayload = (rows: ContactRow[]) => ({
  contacts: rows.map((contact) => ({
    relationship: contact.relationship,
    firstName: contact.firstName,
    lastName: contact.lastName,
    street: contact.street,
    city: contact.city,
    postalCode: contact.postalCode,
    country: contact.country,
    phone: contact.phone,
    email: contact.email,
  })),
});

export const OnboardingEmergencyContactsStep = forwardRef<
  OnboardingStepHandle,
  OnboardingEmergencyContactsStepProps
>(function OnboardingEmergencyContactsStep({ payload, config, editable, isSaving }, ref) {
  const t = useTranslation();
  const minimumCount = readOnboardingMinimumCount(config);
  const [contacts, setContacts] = useState<ContactRow[]>(() =>
    readEmergencyContacts(payload).map((contact) => ({ ...contact, key: crypto.randomUUID() })),
  );
  const [formOpen, setFormOpen] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);

  const toPayload = toEmergencyContactsPayload;

  const savedSnapshot = useState(() =>
    toEmergencyContactsPayload(
      readEmergencyContacts(payload).map((contact) => ({ ...contact, key: '' })),
    ),
  )[0];

  useImperativeHandle(
    ref,
    () => ({
      getPayload: () => toPayload(contacts),
      hasChanges: () => !payloadsEqual(savedSnapshot, toPayload(contacts)),
    }),
    [contacts, savedSnapshot],
  );

  const editingContact: EmergencyContactDto | undefined =
    editingIndex == null
      ? undefined
      : {
          id: editingIndex + 1,
          userId: 0,
          priority: editingIndex,
          ...contacts[editingIndex],
        };

  return (
    <div className="space-y-3">
      {editable ? (
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.emergencyContacts.hint', { count: minimumCount })}
        </p>
      ) : null}
      {contacts.length === 0 ? (
        <EmptyStateCard>{t('myArea.onboarding.wizard.emergencyContacts.empty')}</EmptyStateCard>
      ) : (
        <ListContainer label={t('myArea.emergencyContact.title')}>
          {contacts.map((contact, index) => (
            <ListRow
              key={contact.key}
              title={`${contact.firstName} ${contact.lastName}`}
              subtitle={t(getEmergencyContactRelationshipLabelKey(contact.relationship))}
              meta={
                editable ? undefined : (
                  <ListRowMeta>
                    <ListRowMetaItem
                      icon={
                        <MapPin
                          className="mt-0.5 h-3 w-3 shrink-0 text-[var(--fire-red)]"
                          aria-hidden
                        />
                      }
                    >
                      {formatAddress(
                        contact.street,
                        contact.city,
                        contact.postalCode,
                        contact.country,
                      )}
                    </ListRowMetaItem>
                    <ListRowMetaItem
                      icon={
                        <Phone
                          className="mt-0.5 h-3 w-3 shrink-0 text-[var(--fire-red)]"
                          aria-hidden
                        />
                      }
                    >
                      {contact.phone}
                    </ListRowMetaItem>
                    <ListRowMetaItem
                      icon={
                        <Mail
                          className="mt-0.5 h-3 w-3 shrink-0 text-[var(--fire-red)]"
                          aria-hidden
                        />
                      }
                    >
                      {contact.email}
                    </ListRowMetaItem>
                  </ListRowMeta>
                )
              }
              actions={
                editable ? (
                  <div className="flex items-center gap-1">
                    <IconButton
                      icon={<Pencil className="h-4 w-4" />}
                      aria-label={t('common.edit')}
                      disabled={isSaving}
                      onClick={() => {
                        setEditingIndex(index);
                        setFormOpen(true);
                      }}
                    />
                    <IconButton
                      icon={<Trash2 className="h-4 w-4" />}
                      aria-label={t('common.delete')}
                      disabled={isSaving}
                      onClick={() =>
                        setContacts(contacts.filter((_, itemIndex) => itemIndex !== index))
                      }
                    />
                  </div>
                ) : undefined
              }
            />
          ))}
        </ListContainer>
      )}
      {editable ? (
        <Button
          variant="secondary"
          icon={<Plus className="h-4 w-4" />}
          disabled={isSaving}
          onClick={() => {
            setEditingIndex(null);
            setFormOpen(true);
          }}
        >
          {t('myArea.emergencyContact.addTitle')}
        </Button>
      ) : null}
      <EmergencyContactFormModal
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        contact={editingContact}
        onSave={async (data: CreateEmergencyContactRequest) => {
          const draft: OnboardingEmergencyContactDraft = {
            relationship: data.relationship,
            firstName: data.firstName,
            lastName: data.lastName,
            street: data.street,
            city: data.city,
            postalCode: data.postalCode,
            country: data.country,
            phone: data.phone,
            email: data.email,
          };
          const next =
            editingIndex == null
              ? [...contacts, { ...draft, key: crypto.randomUUID() }]
              : contacts.map((item, index) =>
                  index === editingIndex ? { ...draft, key: item.key } : item,
                );
          setContacts(next);
        }}
      />
    </div>
  );
});
