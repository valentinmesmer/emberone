/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { getEmergencyContactRelationshipLabelKey } from '@/lib/my-area/emergencyContactOptions';
import { useOrganizationEmergencyContacts, useUsersForOrganization } from '@/lib/store';
import { getOrganizationEmergencyContactDetailPath } from '@/lib/utils';
import type { EmergencyContactDto } from '@/types/emergencyContact';
import type { UserDto } from '@/types/user';

interface OrganizationEmergencyContactsManagementProps {
  organizationId: number;
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

export function OrganizationEmergencyContactsManagement({
  organizationId,
}: OrganizationEmergencyContactsManagementProps) {
  const t = useTranslation();
  const { data: contacts = [], isLoading: loadingContacts } =
    useOrganizationEmergencyContacts(organizationId);
  const { data: users = [], isLoading: loadingUsers } = useUsersForOrganization(organizationId);

  const userById = useMemo(() => new Map(users.map((u) => [u.id, u] as const)), [users]);

  const isLoading = loadingContacts || loadingUsers;

  return (
    <>
      {isLoading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : contacts.length === 0 ? (
        <EmptyStateCard>{t('userManagement.emergencyContacts.empty')}</EmptyStateCard>
      ) : (
        <ListContainer label={t('userManagement.emergencyContacts.title')}>
          {contacts.map((contact) => {
            const contactName = `${contact.firstName} ${contact.lastName}`.trim();
            return (
              <ListRow
                key={contact.id}
                title={resolveUserName(userById, contact, t)}
                href={getOrganizationEmergencyContactDetailPath(organizationId, contact.id)}
                subtitle={
                  <>
                    <span className="font-medium text-[var(--text-primary)]">{contactName}</span>
                    <span aria-hidden> · </span>
                    <span>{t(getEmergencyContactRelationshipLabelKey(contact.relationship))}</span>
                    <span aria-hidden> · </span>
                    <span>
                      {t('myArea.emergencyContact.priority', { priority: contact.priority })}
                    </span>
                  </>
                }
              />
            );
          })}
        </ListContainer>
      )}
    </>
  );
}
