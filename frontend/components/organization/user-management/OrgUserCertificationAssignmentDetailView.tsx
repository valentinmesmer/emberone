/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Trash2, UserRound } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { CERTIFICATION_STATUS_BADGE_TONE } from '@/lib/certifications/statusPresentation';
import {
  getOrganizationUserCertificationDetailPath,
  getOrganizationUserSectionPath,
} from '@/lib/navigation/userManagementSections';
import {
  useDeleteCertificationAssignment,
  useOrganizationById,
  useUserCertificationAssignments,
  useUserCertificationStatus,
} from '@/lib/store';
import { formatDateLong } from '@/lib/utils';

export interface OrgUserCertificationAssignmentDetailViewProps {
  organizationId: number;
  userId: number;
  assignmentId: number;
  canWrite: boolean;
}

export function OrgUserCertificationAssignmentDetailView({
  organizationId,
  userId,
  assignmentId,
  canWrite,
}: OrgUserCertificationAssignmentDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const { data: assignments = [], isLoading: assignmentsLoading } = useUserCertificationAssignments(
    organizationId,
    userId,
  );
  const { data: status } = useUserCertificationStatus(organizationId, userId);
  const { data: organization } = useOrganizationById(organizationId);
  const deleteAssignment = useDeleteCertificationAssignment(organizationId, userId);

  const [confirmOpen, setConfirmOpen] = useState(false);

  const assignment = assignments.find((a) => a.id === assignmentId);
  const title = assignment?.certDefinitionName ?? `#${assignmentId}`;

  const linkedStatusItem =
    assignment?.tenantCertDefinitionId && status
      ? (status.items.find((i) => i.tenantCertDefinitionId === assignment.tenantCertDefinitionId) ??
        null)
      : null;

  const handleRemove = async () => {
    if (!assignment) return;
    try {
      await deleteAssignment.mutateAsync(assignment.id);
      showSuccess(t('certifications.assign.removed'));
      router.push(getOrganizationUserSectionPath(organizationId, userId, 'certifications'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('certifications.assign.error'));
    }
  };

  if (assignmentsLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!assignment) {
    return <NotFoundState message={t('certifications.assign.detail.notFound')} />;
  }

  return (
    <div className="w-full space-y-4">
      <Card density="compact">
        <DetailCardHeader
          className="mb-0"
          leading={
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--surface-muted)] text-[var(--text-secondary)]">
              <UserRound className="h-4 w-4" aria-hidden />
            </div>
          }
          title={title}
          actions={
            canWrite ? (
              <DetailCardAction
                label={t('certifications.removeIndividual')}
                icon={<Trash2 className="h-4 w-4" aria-hidden />}
                onClick={() => setConfirmOpen(true)}
              />
            ) : undefined
          }
        />

        <div className="mt-3 space-y-3 border-t border-[var(--border-color)]/30 pt-3">
          <FieldDisplay
            label={t('certifications.assign.detail.responsibleOrg')}
            value={organization?.name ?? t('common.notAvailable')}
          />
          {assignment.startDate ? (
            <FieldDisplay
              label={t('certifications.assign.detail.startDate')}
              value={formatDateLong(assignment.startDate)}
            />
          ) : null}
          {assignment.endDate ? (
            <FieldDisplay
              label={t('certifications.assign.detail.endDate')}
              value={formatDateLong(assignment.endDate)}
            />
          ) : null}
          {assignment.source ? (
            <FieldDisplay
              label={t('certifications.assign.detail.source')}
              value={assignment.source}
            />
          ) : null}
        </div>
      </Card>

      {assignment.tenantCertDefinitionId ? (
        <section>
          <ListContainer label={t('certifications.statusTitle')}>
            <ListRow
              href={getOrganizationUserCertificationDetailPath(
                organizationId,
                userId,
                assignment.tenantCertDefinitionId,
              )}
              title={assignment.certDefinitionName ?? title}
              badges={
                linkedStatusItem ? (
                  <Badge tone={CERTIFICATION_STATUS_BADGE_TONE[linkedStatusItem.status]}>
                    {t(`certifications.status.${linkedStatusItem.status}`)}
                  </Badge>
                ) : undefined
              }
            />
          </ListContainer>
        </section>
      ) : null}

      <ConfirmDialog
        isOpen={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={() => void handleRemove()}
        message={t('certifications.removeIndividualConfirm')}
        confirmText={t('certifications.assign.remove')}
        cancelText={t('common.cancel')}
        isLoading={deleteAssignment.isPending}
      />
    </div>
  );
}
