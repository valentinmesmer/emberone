/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { EventCertificationGrantModal } from '@/components/event/EventCertificationGrantModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationEventGrantDto } from '@/lib/api/eventCertificationGrants';
import { allowsEventGrantEvidence } from '@/lib/certifications/evidenceTypes';
import {
  getEventCertificationGrantDetailPath,
  getSectionForEventType,
} from '@/lib/department/sections';
import {
  useDeleteEventCertificationGrant,
  useEventCertificationGrants,
  useOrgCertificationCatalog,
  useSelf,
} from '@/lib/store';
import type { EventType } from '@/types/event';

export interface EventCertificationGrantSectionProps {
  organizationId: number;
  eventType: EventType;
  eventId: number;
  canRead: boolean;
  canWrite: boolean;
}

export function EventCertificationGrantSection({
  organizationId,
  eventType,
  eventId,
  canRead,
  canWrite,
}: EventCertificationGrantSectionProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: self } = useSelf();
  const userId = self?.id ?? 0;
  const departmentSection = getSectionForEventType(eventType);

  const { data: grants = [], isLoading } = useEventCertificationGrants(
    organizationId,
    eventType,
    eventId,
    { enabled: canRead },
  );
  const { data: catalog } = useOrgCertificationCatalog(
    organizationId,
    userId,
    canWrite && userId > 0,
  );
  const deleteGrant = useDeleteEventCertificationGrant(organizationId, eventType, eventId);

  const availableDefinitions = useMemo(() => {
    const grantedIds = new Set(grants.map((grant) => grant.tenantCertDefinitionId));
    return (catalog?.definitions ?? []).filter(
      (def) =>
        def.active &&
        allowsEventGrantEvidence(def.evidenceTypes, eventType) &&
        !grantedIds.has(def.id),
    );
  }, [catalog?.definitions, eventType, grants]);

  const canAdd = canWrite && availableDefinitions.length > 0;

  const [createOpen, setCreateOpen] = useState(false);
  const [editGrantId, setEditGrantId] = useState<number | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CertificationEventGrantDto | null>(null);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteGrant.mutateAsync(deleteTarget.id);
      showSuccess(t('events.certificationGrant.removeSuccess'));
      setDeleteTarget(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('events.certificationGrant.removeError'));
    }
  };

  if (!canRead) {
    return null;
  }

  return (
    <>
      <Card className="p-3 sm:p-4">
        <div className="mb-3">
          <SectionHeader
            className="flex-wrap"
            title={t('events.certificationGrant.sectionTitle')}
            action={
              canAdd ? (
                <Button
                  type="button"
                  size="xs"
                  variant="primary"
                  fullWidth="responsive"
                  onClick={() => setCreateOpen(true)}
                >
                  {t('events.certificationGrant.add')}
                </Button>
              ) : null
            }
          />
        </div>

        {isLoading ? (
          <LoadingState variant="embedded" size="sm" />
        ) : grants.length === 0 ? (
          <EmptyState>{t('events.certificationGrant.empty')}</EmptyState>
        ) : (
          <ListContainer variant="inset" label={t('events.certificationGrant.sectionTitle')}>
            {grants.map((grant) => (
              <ListRow
                key={grant.id}
                accent={false}
                compact
                href={getEventCertificationGrantDetailPath(
                  organizationId,
                  departmentSection,
                  eventId,
                  grant.id,
                )}
                title={grant.certDefinitionName ?? `#${grant.tenantCertDefinitionId}`}
                badges={
                  <Badge tone="neutral" pill>
                    {grant.recipientMode === 'ALL_ATTENDEES'
                      ? t('events.certificationGrant.allAttendees')
                      : t('events.certificationGrant.specificAttendees')}
                  </Badge>
                }
                actions={
                  canWrite ? (
                    <>
                      <IconButton
                        variant="action"
                        icon={<Pencil className="h-4 w-4" aria-hidden />}
                        aria-label={t('common.edit')}
                        title={t('common.edit')}
                        onClick={() => setEditGrantId(grant.id)}
                        disabled={deleteGrant.isPending}
                      />
                      <IconButton
                        variant="action"
                        icon={<Trash2 className="h-4 w-4" aria-hidden />}
                        aria-label={t('events.certificationGrant.remove')}
                        title={t('events.certificationGrant.remove')}
                        onClick={() => setDeleteTarget(grant)}
                        disabled={deleteGrant.isPending}
                      />
                    </>
                  ) : undefined
                }
              />
            ))}
          </ListContainer>
        )}
      </Card>

      {createOpen ? (
        <EventCertificationGrantModal
          isOpen
          onClose={() => setCreateOpen(false)}
          organizationId={organizationId}
          eventType={eventType}
          eventId={eventId}
          eventDefinitions={availableDefinitions}
          grantId={null}
        />
      ) : null}

      {editGrantId != null ? (
        <EventCertificationGrantModal
          isOpen
          onClose={() => setEditGrantId(null)}
          organizationId={organizationId}
          eventType={eventType}
          eventId={eventId}
          eventDefinitions={[]}
          grantId={editGrantId}
        />
      ) : null}

      <ConfirmDialog
        isOpen={deleteTarget != null}
        onClose={() => !deleteGrant.isPending && setDeleteTarget(null)}
        onConfirm={() => void handleDelete()}
        title={t('events.certificationGrant.removeTitle')}
        message={t('events.certificationGrant.removeConfirm', {
          name:
            deleteTarget?.certDefinitionName ?? `#${deleteTarget?.tenantCertDefinitionId ?? ''}`,
        })}
        confirmText={t('events.certificationGrant.remove')}
        cancelText={t('common.cancel')}
        isLoading={deleteGrant.isPending}
      />
    </>
  );
}
