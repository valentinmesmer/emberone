/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { Card } from '@/components/ui/card';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { getEmergencyContactRelationshipLabelKey } from '@/lib/my-area/emergencyContactOptions';
import { useOrganizationEmergencyContacts, useUsersForOrganization } from '@/lib/store';
import { formatAddress } from '@/lib/utils/address';
import type { EmergencyContactDto } from '@/types/emergencyContact';
import type { UserDto } from '@/types/user';

export interface OrganizationEmergencyContactDetailViewProps {
  organizationId: number;
  organizationName: string;
  contactId: number;
}

function resolveUserName(
  userById: Map<number, UserDto>,
  contact: EmergencyContactDto,
  t: (key: string, params?: Record<string, string | number>) => string,
) {
  const u = userById.get(contact.userId);
  if (!u) {
    return t('userManagement.emergencyContacts.userUnknown', { id: String(contact.userId) });
  }
  return `${u.firstName} ${u.lastName}`.trim();
}

export function OrganizationEmergencyContactDetailView({
  organizationId,
  organizationName,
  contactId,
}: OrganizationEmergencyContactDetailViewProps) {
  const t = useTranslation();
  const { data: contacts = [], isLoading: loadingContacts } =
    useOrganizationEmergencyContacts(organizationId);
  const { data: users = [], isLoading: loadingUsers } = useUsersForOrganization(organizationId);

  const userById = useMemo(() => new Map(users.map((u) => [u.id, u] as const)), [users]);

  const contact = contacts.find((c) => c.id === contactId);
  const loading = loadingContacts || loadingUsers;

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!contact) {
    return <NotFoundState message={t('myArea.emergencyContact.error.notFound')} />;
  }

  const userName = resolveUserName(userById, contact, t);
  const contactName = `${contact.firstName} ${contact.lastName}`.trim();
  const relationshipLabel = t(getEmergencyContactRelationshipLabelKey(contact.relationship));
  const address = formatAddress(contact.street, contact.city, contact.postalCode, contact.country);

  return (
    <div className="w-full">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader className="mb-2" title={userName} subtitle={organizationName} />

        <div className="space-y-4">
          <FieldDisplay label={t('userManagement.userLabel')} value={userName} />
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
            label={t('myArea.emergencyContact.fields.relationship')}
            value={relationshipLabel}
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
          <FieldDisplay
            label={t('userManagement.emergencyContacts.contactLabel')}
            value={contactName}
          />
        </div>
      </Card>
    </div>
  );
}
