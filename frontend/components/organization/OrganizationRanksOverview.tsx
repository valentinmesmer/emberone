/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ChevronDown, ChevronRight } from 'lucide-react';
import { type ReactNode, useMemo, useState } from 'react';
import { OrganizationSetRankModal } from '@/components/organization/OrganizationSetRankModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { ListControls, ListNoResults } from '@/components/ui/list-filter';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, EmptyStateCard, NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { organizationRankMemberListDefinition } from '@/lib/list/organizationRankMemberList';
import { getOrganizationUserSectionPath } from '@/lib/navigation/userManagementSections';
import {
  buildRankOverviewGroups,
  defaultExpandedGroupIds,
  type FunctionOverviewGroup,
  mapFunctionOverviewGroups,
  type RankOverviewGroup,
} from '@/lib/organization/organizationRanksOverviewUtils';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useOrganizationAvailableRanks,
  useOrganizationFunctionsOverview,
  useOrganizationRankMembers,
  useUrlSyncedOrganizationRankMemberListState,
} from '@/lib/store';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
}

function SectionLabel({ children }: { children: ReactNode }) {
  return (
    <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
      {children}
    </h2>
  );
}

function memberCountLabel(
  count: number,
  t: (key: string, params?: Record<string, string | number>) => string,
) {
  return count === 1
    ? t('qualifications.overview.memberCountOne')
    : t('qualifications.overview.memberCount', { count });
}

function memberDisplayName(member: { firstName: string; lastName: string }) {
  return `${member.lastName}, ${member.firstName}`;
}

function OverviewGroupSection({
  groupTitle,
  groupSubtitle,
  memberCount,
  expanded,
  onToggle,
  emptyMessage,
  children,
}: {
  groupTitle: string;
  groupSubtitle?: string;
  memberCount: number;
  expanded: boolean;
  onToggle: () => void;
  emptyMessage?: string;
  children: ReactNode;
}) {
  const t = useTranslation();

  return (
    <section
      aria-label={groupTitle}
      className="overflow-hidden rounded-md border border-[var(--border-color)] bg-[var(--surface-muted)]/20"
    >
      <button
        type="button"
        className="flex w-full min-w-0 items-center gap-2 px-3 py-2.5 text-left transition-colors hover:bg-[var(--hover-bg)]/50"
        onClick={onToggle}
        aria-expanded={expanded}
        aria-label={
          expanded
            ? t('qualifications.overview.collapseGroup', { name: groupTitle })
            : t('qualifications.overview.expandGroup', { name: groupTitle })
        }
      >
        {expanded ? (
          <ChevronDown className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
        ) : (
          <ChevronRight className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
        )}
        <div className="min-w-0 flex-1">
          <div className="truncate text-sm font-medium text-[var(--text-primary)]">
            {groupTitle}
          </div>
          <div className="truncate text-xs text-[var(--text-secondary)]">
            {groupSubtitle
              ? `${groupSubtitle} · ${memberCountLabel(memberCount, t)}`
              : memberCountLabel(memberCount, t)}
          </div>
        </div>
        <Badge tone="neutral" pill>
          {memberCount}
        </Badge>
      </button>

      {expanded ? (
        memberCount === 0 && emptyMessage ? (
          <div className="border-t border-[var(--border-color)] bg-[var(--card-bg)] px-3 py-3">
            <EmptyState>{emptyMessage}</EmptyState>
          </div>
        ) : (
          children
        )
      ) : null}
    </section>
  );
}

function RankGroupSection({
  organizationId,
  group,
  expanded,
  canWrite,
  onToggle,
  onSetRank,
}: {
  organizationId: number;
  group: RankOverviewGroup;
  expanded: boolean;
  canWrite: boolean;
  onToggle: () => void;
  onSetRank: (userId: number) => void;
}) {
  const t = useTranslation();

  return (
    <OverviewGroupSection
      groupTitle={group.title}
      memberCount={group.members.length}
      expanded={expanded}
      onToggle={onToggle}
      emptyMessage={t('ranks.overview.noMembersInGroup')}
    >
      {group.members.length > 0 ? (
        <ListContainer
          label={group.title}
          variant="flush"
          rowSeparator="inset"
          className="border-t border-[var(--border-color)] bg-[var(--card-bg)]"
        >
          {group.members.map((member) => {
            const canSetRank = canWrite && member.mainOrganization;
            return (
              <ListRow
                key={member.userId}
                title={memberDisplayName(member)}
                compact
                accent={false}
                href={getOrganizationUserSectionPath(organizationId, member.userId, 'rank')}
                actions={
                  canSetRank ? (
                    <Button size="xs" onClick={() => onSetRank(member.userId)}>
                      {member.currentRank
                        ? t('ranks.overview.changeRank')
                        : t('ranks.overview.setRank')}
                    </Button>
                  ) : undefined
                }
              />
            );
          })}
        </ListContainer>
      ) : null}
    </OverviewGroupSection>
  );
}

function FunctionGroupSection({
  organizationId,
  group,
  expanded,
  onToggle,
}: {
  organizationId: number;
  group: FunctionOverviewGroup;
  expanded: boolean;
  onToggle: () => void;
}) {
  const t = useTranslation();

  return (
    <OverviewGroupSection
      groupTitle={group.title}
      groupSubtitle={group.subtitle}
      memberCount={group.members.length}
      expanded={expanded}
      onToggle={onToggle}
      emptyMessage={t('ranks.overview.noMembersInFunction')}
    >
      {group.members.length > 0 ? (
        <ListContainer
          label={group.title}
          variant="flush"
          rowSeparator="inset"
          className="border-t border-[var(--border-color)] bg-[var(--card-bg)]"
        >
          {group.members.map((member) => (
            <ListRow
              key={`${group.id}-${member.userId}`}
              title={memberDisplayName(member)}
              compact
              accent={false}
              href={getOrganizationUserSectionPath(organizationId, member.userId, 'rank')}
              badges={<Badge tone="neutral">{member.functionAbbreviation}</Badge>}
            />
          ))}
        </ListContainer>
      ) : null}
    </OverviewGroupSection>
  );
}

export function OrganizationRanksOverview({ organizationId }: Props) {
  const t = useTranslation();
  const { hasPermission } = usePermissions();
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.RANK_WRITE);
  const listState = useUrlSyncedOrganizationRankMemberListState();
  const {
    data: members,
    isLoading,
    error,
  } = useOrganizationRankMembers({
    organizationId,
    listQuery: listState.listQuery,
    fetchAll: true,
  });
  const {
    data: functionsOverview,
    isLoading: functionsLoading,
    error: functionsError,
  } = useOrganizationFunctionsOverview(organizationId, listState.debouncedSearch);
  const { data: catalogRanks } = useOrganizationAvailableRanks(organizationId, canWrite);
  const [setRankUserId, setSetRankUserId] = useState<number | null>(null);
  const [expandedIds, setExpandedIds] = useState<Set<string> | null>(null);

  const rankGroups = useMemo(
    () =>
      buildRankOverviewGroups(members ?? [], catalogRanks, canWrite, t('ranks.overview.noRank')),
    [members, catalogRanks, canWrite, t],
  );

  const functionGroups = useMemo(
    () => mapFunctionOverviewGroups(functionsOverview),
    [functionsOverview],
  );

  const defaultExpanded = useMemo(
    () =>
      new Set([...defaultExpandedGroupIds(functionGroups), ...defaultExpandedGroupIds(rankGroups)]),
    [functionGroups, rankGroups],
  );

  const resolvedExpandedIds = expandedIds ?? defaultExpanded;

  const selectedMember = useMemo(
    () => members?.find((member) => member.userId === setRankUserId) ?? null,
    [members, setRankUserId],
  );

  const toggleExpanded = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev ?? defaultExpanded);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  if (error) {
    return <NoticeCard>{t('ranks.overview.loadFailed')}</NoticeCard>;
  }
  if (functionsError) {
    return <NoticeCard>{t('ranks.overview.loadFailed')}</NoticeCard>;
  }

  const showEmptyMembers =
    !isLoading &&
    !functionsLoading &&
    members.length === 0 &&
    functionGroups.length === 0 &&
    !listState.hasActiveFilters;
  const showNoSearchResults =
    !isLoading &&
    !functionsLoading &&
    listState.hasActiveFilters &&
    functionGroups.length === 0 &&
    rankGroups.every((group) => group.members.length === 0);

  return (
    <div className="space-y-3">
      <ListControls definition={organizationRankMemberListDefinition} state={listState} />
      {isLoading || functionsLoading ? <LoadingState variant="embedded" size="sm" /> : null}
      {showEmptyMembers ? (
        <EmptyStateCard>{t('ranks.overview.emptyMembers')}</EmptyStateCard>
      ) : null}
      {showNoSearchResults ? <ListNoResults onReset={listState.reset} /> : null}
      {!isLoading && !functionsLoading && !showEmptyMembers && !showNoSearchResults ? (
        <div className="space-y-5">
          <section>
            <SectionLabel>{t('ranks.overview.functionsSection')}</SectionLabel>
            {functionGroups.length === 0 ? (
              listState.hasActiveFilters ? null : (
                <EmptyStateCard>{t('ranks.overview.emptyFunctions')}</EmptyStateCard>
              )
            ) : (
              <div className="space-y-2">
                {functionGroups.map((group) => (
                  <FunctionGroupSection
                    key={group.id}
                    organizationId={organizationId}
                    group={group}
                    expanded={resolvedExpandedIds.has(group.id)}
                    onToggle={() => toggleExpanded(group.id)}
                  />
                ))}
              </div>
            )}
          </section>

          <section>
            <SectionLabel>{t('ranks.overview.ranksSection')}</SectionLabel>
            <div className="space-y-2">
              {rankGroups.map((group) => (
                <RankGroupSection
                  key={group.id}
                  organizationId={organizationId}
                  group={group}
                  expanded={resolvedExpandedIds.has(group.id)}
                  canWrite={canWrite}
                  onToggle={() => toggleExpanded(group.id)}
                  onSetRank={setSetRankUserId}
                />
              ))}
            </div>
          </section>
        </div>
      ) : null}

      {selectedMember ? (
        <OrganizationSetRankModal
          organizationId={organizationId}
          member={selectedMember}
          isOpen
          onClose={() => setSetRankUserId(null)}
        />
      ) : null}
    </div>
  );
}
