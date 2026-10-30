/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { QualificationOrgDecisionActions } from '@/components/organization/user-management/QualificationOrgDecisionActions';
import { QualificationOrgDecisionGrantModal } from '@/components/organization/user-management/QualificationOrgDecisionGrantModal';
import { QualificationOrgDecisionRevokeModal } from '@/components/organization/user-management/QualificationOrgDecisionRevokeModal';
import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type {
  QualificationExerciseStatus,
  QualificationStatusItemDto,
} from '@/lib/api/qualifications';
import { getOrganizationUserQualificationDetailPath } from '@/lib/navigation/userManagementSections';
import {
  groupOrgQualificationStatusItems,
  QUALIFICATION_LIST_SECTION_TITLE_KEY,
  type SelfQualificationListSectionId,
} from '@/lib/qualifications/aggregateQualifications';
import { findActiveGrantInterval } from '@/lib/qualifications/orgDecisionIntervals';
import {
  QUALIFICATION_STATUS_BADGE_TONE,
  QUALIFICATION_STATUS_ROW_TONE,
} from '@/lib/qualifications/statusPresentation';
import {
  useGrantQualificationOrgDecision,
  useUpdateQualificationOrgDecision,
  useUserQualificationStatus,
} from '@/lib/store/queries/qualifications';

function shouldShowStatusBadge(
  sectionId: SelfQualificationListSectionId,
  itemStatus: QualificationExerciseStatus,
): boolean {
  if (sectionId === 'valid' || sectionId === 'prerequisitesMissing') {
    return false;
  }
  return itemStatus === 'ORG_DECISION_REVOKED';
}

interface Props {
  organizationId: number;
  userId: number;
  canWrite: boolean;
}

type ModalTarget =
  | { type: 'grant'; item: QualificationStatusItemDto }
  | {
      type: 'revoke';
      item: QualificationStatusItemDto;
      activeGrant: NonNullable<ReturnType<typeof findActiveGrantInterval>>;
    }
  | null;

export function UserQualificationsSection({ organizationId, userId, canWrite }: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: status, isLoading } = useUserQualificationStatus(organizationId, userId);
  const grant = useGrantQualificationOrgDecision(organizationId, userId);
  const updateOrgDecision = useUpdateQualificationOrgDecision(organizationId, userId);
  const [modalTarget, setModalTarget] = useState<ModalTarget>(null);

  const handleGrant = async (
    configKey: string,
    params: { startDate: string; endDate?: string },
  ) => {
    try {
      await grant.mutateAsync({
        qualificationConfigKey: configKey,
        startDate: params.startDate,
        endDate: params.endDate,
      });
      showSuccess(t('qualifications.orgDecision.granted'));
      setModalTarget(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('qualifications.orgDecision.grantFailed'));
      throw e;
    }
  };

  const handleSetEndDate = async (
    configKey: string,
    params: { decisionId: number; startDate: string; endDate: string },
  ) => {
    try {
      await updateOrgDecision.mutateAsync({
        qualificationConfigKey: configKey,
        id: params.decisionId,
        startDate: params.startDate,
        endDate: params.endDate,
      });
      showSuccess(t('qualifications.orgDecision.endDateSaved'));
      setModalTarget(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('qualifications.orgDecision.endDateSaveFailed'));
      throw e;
    }
  };

  const statusLabel = (itemStatus: QualificationExerciseStatus) =>
    t(`qualifications.status.${itemStatus}`);

  const modalItem = modalTarget?.item;
  const grantModalOpen = modalTarget?.type === 'grant';
  const revokeModalOpen = modalTarget?.type === 'revoke';
  const activeGrantForModal = modalTarget?.type === 'revoke' ? modalTarget.activeGrant : undefined;

  const sections = useMemo(
    () => (status?.items ? groupOrgQualificationStatusItems(status.items) : []),
    [status],
  );

  if (isLoading) return <LoadingState variant="embedded" />;
  if (!status?.items.length) return <EmptyStateCard>{t('qualifications.empty')}</EmptyStateCard>;

  return (
    <>
      <div className="flex w-full min-w-0 flex-col gap-5">
        {sections.map((section) => {
          const sectionTitle = t(QUALIFICATION_LIST_SECTION_TITLE_KEY[section.id]);
          return (
            <section key={section.id} className="w-full min-w-0">
              <h2 className="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
                {sectionTitle}
              </h2>
              <ListContainer label={sectionTitle}>
                {section.items.map((item) => {
                  const activeGrant = findActiveGrantInterval(item.orgDecisions);
                  const showGrant =
                    canWrite && item.requiresOrgDecision && item.prerequisitesMet && !activeGrant;
                  const showRevoke = canWrite && !!activeGrant;
                  const showBadge = shouldShowStatusBadge(section.id, item.status);

                  return (
                    <ListRow
                      key={item.tenantQualificationDefinitionId}
                      href={getOrganizationUserQualificationDetailPath(
                        organizationId,
                        userId,
                        item.tenantQualificationDefinitionId,
                      )}
                      tone={QUALIFICATION_STATUS_ROW_TONE[item.status]}
                      compact
                      title={item.name}
                      badges={
                        showBadge ? (
                          <Badge tone={QUALIFICATION_STATUS_BADGE_TONE[item.status]}>
                            {statusLabel(item.status)}
                          </Badge>
                        ) : undefined
                      }
                      actions={
                        showGrant || showRevoke ? (
                          <QualificationOrgDecisionActions
                            canGrant={showGrant}
                            canRevoke={showRevoke}
                            isGrantPending={grant.isPending}
                            isRevokePending={updateOrgDecision.isPending}
                            onGrant={() => setModalTarget({ type: 'grant', item })}
                            onRevoke={() => {
                              if (!activeGrant) return;
                              setModalTarget({ type: 'revoke', item, activeGrant });
                            }}
                          />
                        ) : undefined
                      }
                    />
                  );
                })}
              </ListContainer>
            </section>
          );
        })}
      </div>

      {modalItem ? (
        <QualificationOrgDecisionGrantModal
          isOpen={grantModalOpen}
          onClose={() => setModalTarget(null)}
          qualificationName={modalItem.name}
          isLoading={grant.isPending}
          onConfirm={(params) => handleGrant(modalItem.configKey, params)}
        />
      ) : null}

      {modalItem && activeGrantForModal ? (
        <QualificationOrgDecisionRevokeModal
          isOpen={revokeModalOpen}
          onClose={() => setModalTarget(null)}
          qualificationName={modalItem.name}
          intervalStartDate={activeGrantForModal.startDate}
          currentEndDate={activeGrantForModal.endDate}
          isLoading={updateOrgDecision.isPending}
          onConfirm={(params) =>
            handleSetEndDate(modalItem.configKey, {
              decisionId: activeGrantForModal.id,
              startDate: activeGrantForModal.startDate,
              endDate: params.endDate,
            })
          }
        />
      ) : null}
    </>
  );
}
