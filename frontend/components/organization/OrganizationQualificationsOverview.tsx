/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ChevronDown, ChevronRight, UserPlus } from 'lucide-react';
import { useMemo, useState } from 'react';
import { QualificationOverviewAppointModal } from '@/components/organization/QualificationOverviewAppointModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { ListControls, ListNoResults } from '@/components/ui/list-filter';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, EmptyStateCard, NoticeCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { QualificationOverviewGroupDto } from '@/lib/api/qualifications';
import { organizationQualificationOverviewListDefinition } from '@/lib/list/organizationQualificationOverviewList';
import { getOrganizationUserQualificationDetailPath } from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useGrantQualificationOrgDecisionForOrganization,
  useOrganizationQualificationStatusOverview,
  useUrlSyncedOrganizationQualificationOverviewListState,
} from '@/lib/store';
import { PermissionTypeEnum } from '@/types/permission';

function memberCountLabel(
  count: number,
  t: (key: string, params?: Record<string, string | number>) => string,
) {
  return count === 1
    ? t('qualifications.overview.memberCountOne')
    : t('qualifications.overview.memberCount', { count });
}

function defaultExpandedIds(groups: QualificationOverviewGroupDto[]) {
  return new Set(
    groups
      .filter((group) => group.members.length > 0)
      .map((group) => group.tenantQualificationDefinitionId),
  );
}

function QualificationGroupSection({
  organizationId,
  group,
  expanded,
  canWrite,
  onToggle,
  onAppoint,
}: {
  organizationId: number;
  group: QualificationOverviewGroupDto;
  expanded: boolean;
  canWrite: boolean;
  onToggle: () => void;
  onAppoint: () => void;
}) {
  const t = useTranslation();
  const needsAppointment = group.requiresOrgDecision && group.members.length === 0;
  const showAppoint = canWrite && group.requiresOrgDecision;
  const countLabel = memberCountLabel(group.members.length, t);

  return (
    <section
      aria-label={group.name}
      className="overflow-hidden rounded-md border border-[var(--border-color)] bg-[var(--surface-muted)]/20"
    >
      <div className="flex items-stretch">
        <button
          type="button"
          className="flex min-w-0 flex-1 items-center gap-2 px-3 py-2.5 text-left transition-colors hover:bg-[var(--hover-bg)]/50"
          onClick={onToggle}
          aria-expanded={expanded}
          aria-label={
            expanded
              ? t('qualifications.overview.collapseGroup', { name: group.name })
              : t('qualifications.overview.expandGroup', { name: group.name })
          }
        >
          {expanded ? (
            <ChevronDown className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
          ) : (
            <ChevronRight className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
          )}
          <div className="min-w-0 flex-1">
            <div className="truncate text-sm font-medium text-[var(--text-primary)]">
              {group.name}
            </div>
            <div className="truncate text-xs text-[var(--text-secondary)]">{countLabel}</div>
          </div>
          <Badge tone={needsAppointment ? 'danger' : 'neutral'} pill>
            {group.members.length}
          </Badge>
        </button>
        {showAppoint ? (
          <div className="flex shrink-0 items-center border-l border-[var(--border-color)] px-2">
            <Button
              type="button"
              variant="primary"
              size="xs"
              fullWidth="responsive"
              icon={<UserPlus className="h-3.5 w-3.5" aria-hidden />}
              onClick={onAppoint}
            >
              {t('qualifications.orgDecision.grant')}
            </Button>
          </div>
        ) : null}
      </div>

      {expanded ? (
        group.members.length === 0 ? (
          <div className="border-t border-[var(--border-color)] bg-[var(--card-bg)] px-3 py-3">
            <EmptyState>
              {group.requiresOrgDecision
                ? t('qualifications.overview.noMembers')
                : t('qualifications.overview.noMembersViaPrerequisites')}
            </EmptyState>
          </div>
        ) : (
          <ListContainer
            label={group.name}
            variant="flush"
            rowSeparator="inset"
            className="border-t border-[var(--border-color)] bg-[var(--card-bg)]"
          >
            {group.members.map((member) => (
              <ListRow
                key={`${group.tenantQualificationDefinitionId}-${member.userId}`}
                href={getOrganizationUserQualificationDetailPath(
                  organizationId,
                  member.userId,
                  group.tenantQualificationDefinitionId,
                )}
                compact
                accent={false}
                title={member.userDisplayName}
              />
            ))}
          </ListContainer>
        )
      ) : null}
    </section>
  );
}

export function OrganizationQualificationsOverview({ organizationId }: { organizationId: number }) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.QUALIFICATION_WRITE);
  const listState = useUrlSyncedOrganizationQualificationOverviewListState();
  const { data, isLoading, error } = useOrganizationQualificationStatusOverview(
    organizationId,
    listState.debouncedSearch,
  );
  const grant = useGrantQualificationOrgDecisionForOrganization(organizationId);
  const groups = useMemo(() => data?.groups ?? [], [data?.groups]);
  const [appointTarget, setAppointTarget] = useState<QualificationOverviewGroupDto | null>(null);
  const [expandedIds, setExpandedIds] = useState<Set<number> | null>(null);
  const resolvedExpandedIds = expandedIds ?? defaultExpandedIds(groups);

  const toggleExpanded = (id: number) => {
    setExpandedIds((prev) => {
      const next = new Set(prev ?? defaultExpandedIds(groups));
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleAppoint = async (params: { userId: number; startDate: string; endDate?: string }) => {
    if (!appointTarget) return;
    try {
      await grant.mutateAsync({
        userId: params.userId,
        qualificationConfigKey: appointTarget.configKey,
        startDate: params.startDate,
        endDate: params.endDate,
      });
      showSuccess(t('qualifications.orgDecision.granted'));
      setExpandedIds((prev) => {
        const next = new Set(prev ?? defaultExpandedIds(groups));
        next.add(appointTarget.tenantQualificationDefinitionId);
        return next;
      });
      setAppointTarget(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('qualifications.orgDecision.grantFailed'));
      throw e;
    }
  };

  if (error) {
    return (
      <NoticeCard>
        {error instanceof Error ? error.message : t('myArea.error.loadFailed')}
      </NoticeCard>
    );
  }

  const showEmpty = !isLoading && groups.length === 0 && !listState.hasActiveFilters;
  const showNoResults = !isLoading && listState.hasActiveFilters && groups.length === 0;

  return (
    <div className="space-y-3">
      <ListControls
        definition={organizationQualificationOverviewListDefinition}
        state={listState}
      />
      {isLoading ? <LoadingState variant="embedded" size="sm" /> : null}
      {showEmpty ? <EmptyStateCard>{t('qualifications.overview.empty')}</EmptyStateCard> : null}
      {showNoResults ? <ListNoResults onReset={listState.reset} /> : null}
      {!isLoading && !showEmpty && !showNoResults ? (
        <div className="space-y-2">
          {groups.map((group) => (
            <QualificationGroupSection
              key={group.tenantQualificationDefinitionId}
              organizationId={organizationId}
              group={group}
              expanded={resolvedExpandedIds.has(group.tenantQualificationDefinitionId)}
              canWrite={canWrite}
              onToggle={() => toggleExpanded(group.tenantQualificationDefinitionId)}
              onAppoint={() => setAppointTarget(group)}
            />
          ))}
        </div>
      ) : null}

      <QualificationOverviewAppointModal
        isOpen={appointTarget != null}
        onClose={() => setAppointTarget(null)}
        qualificationName={appointTarget?.name ?? ''}
        appointableMembers={appointTarget?.appointableMembers ?? []}
        isLoading={grant.isPending}
        onConfirm={handleAppoint}
      />
    </div>
  );
}
