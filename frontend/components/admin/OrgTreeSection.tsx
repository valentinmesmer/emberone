/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import Link from 'next/link';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import { summarizeLeaderIssuesFromTree, totalLeaderIssues } from '@/lib/admin/orgLeaderIssues';
import {
  type AdminOrganizationTreeNodeDto,
  adminApi,
  type RootOrganizationSummaryDto,
  type SetupOrganizationRequest,
} from '@/lib/api/admin';
import { type OrganizationTypeDto, organizationTypesApi } from '@/lib/api/organizationTypes';
import { AdminOrgTreeView } from './AdminOrgTreeView';
import { CreateRootOrganizationModal } from './CreateRootOrganizationModal';

export interface OrgTreeSectionProps {
  tenantId: number;
  rootOrganizations: RootOrganizationSummaryDto[];
  isGlobalAdmin: boolean;
  standardsConfiguredForOrgCreation: boolean;
  onRootOrgCreated: (data: SetupOrganizationRequest) => Promise<void>;
  creatingRootOrg: boolean;
  onTreeLoaded?: (roots: AdminOrganizationTreeNodeDto[]) => void;
  /** When false, leader issues are only indicated on tree rows (e.g. onboarding card above). */
  showLeaderIssueBanner?: boolean;
}

export function OrgTreeSection({
  tenantId,
  rootOrganizations,
  isGlobalAdmin,
  standardsConfiguredForOrgCreation,
  onRootOrgCreated,
  creatingRootOrg,
  onTreeLoaded,
  showLeaderIssueBanner = true,
}: OrgTreeSectionProps) {
  const t = useTranslation();
  const [rootOrgOpen, setRootOrgOpen] = useState(false);
  const [treeRoots, setTreeRoots] = useState<AdminOrganizationTreeNodeDto[] | null>(null);
  const [treeLoading, setTreeLoading] = useState(false);
  const [treeFailed, setTreeFailed] = useState(false);
  const [organizationTypes, setOrganizationTypes] = useState<OrganizationTypeDto[]>([]);

  const hasRoots = rootOrganizations.length > 0;
  const canCreateRoot = isGlobalAdmin && standardsConfiguredForOrgCreation;

  const loadTree = useCallback(async () => {
    setTreeLoading(true);
    setTreeFailed(false);
    try {
      const res = await adminApi.getAdminOrganizationTree(tenantId);
      setTreeRoots(res.roots);
      onTreeLoaded?.(res.roots);
    } catch {
      setTreeFailed(true);
      setTreeRoots(null);
    } finally {
      setTreeLoading(false);
    }
  }, [tenantId, onTreeLoaded]);

  useEffect(() => {
    if (!hasRoots) {
      return;
    }
    const id = window.setTimeout(() => {
      void loadTree();
    }, 0);
    return () => window.clearTimeout(id);
  }, [hasRoots, loadTree]);

  useEffect(() => {
    void organizationTypesApi
      .list(tenantId)
      .then(setOrganizationTypes)
      .catch(() => setOrganizationTypes([]));
  }, [tenantId]);

  const organizationTypeNames = useMemo(
    () => new Map(organizationTypes.map((type) => [type.id, type.name])),
    [organizationTypes],
  );

  const handleRootOrgCreated = async (data: SetupOrganizationRequest) => {
    await onRootOrgCreated(data);
    setRootOrgOpen(false);
    void loadTree();
  };

  const leaderSummary = useMemo(
    () => (treeRoots ? summarizeLeaderIssuesFromTree(treeRoots) : null),
    [treeRoots],
  );
  const leaderIssueCount = leaderSummary ? totalLeaderIssues(leaderSummary) : 0;

  return (
    <>
      <Card className="p-3 sm:p-4">
        <SectionHeader
          className="mb-3"
          title={t('admin.orgTree.sectionTitle')}
          subtitle={t('admin.orgTree.orgStructureTreeIntro')}
          action={
            isGlobalAdmin ? (
              <Button
                type="button"
                size="xs"
                variant="primary"
                onClick={() => setRootOrgOpen(true)}
                fullWidth="responsive"
                disabled={!standardsConfiguredForOrgCreation}
                title={
                  !standardsConfiguredForOrgCreation
                    ? t('admin.standards.orgCreationBlockedHint')
                    : undefined
                }
              >
                {t('admin.rootOrg.create')}
              </Button>
            ) : null
          }
        />

        {!hasRoots ? (
          <div className="flex flex-col gap-2">
            <EmptyState>{t('admin.rootOrg.none')}</EmptyState>
            {!isGlobalAdmin ? (
              <p className="text-xs text-[var(--text-secondary)]">
                {t('admin.rootOrgGlobalOnlyHint')}
              </p>
            ) : null}
          </div>
        ) : treeLoading ? (
          <LoadingState variant="embedded" size="sm" />
        ) : treeFailed ? (
          <EmptyState>{t('admin.errors.orgTreeLoadFailed')}</EmptyState>
        ) : (
          <>
            {showLeaderIssueBanner && leaderIssueCount > 0 && leaderSummary?.firstOrgId != null ? (
              <Link
                href={`/admin/tenants/${tenantId}/organizations/${leaderSummary.firstOrgId}`}
                className="mb-3 block rounded-lg border border-[var(--toast-warning-border)] bg-[var(--toast-warning-bg)] p-3 text-xs text-[var(--toast-warning-text)] transition-colors hover:bg-[var(--toast-warning-bg)]/80 sm:text-sm"
              >
                {leaderSummary.directCount > 0
                  ? t('admin.tenantSetup.leadersTreeHint', { count: leaderSummary.directCount })
                  : t('admin.tenantSetup.leadersMissingSubtree')}
              </Link>
            ) : null}
            <AdminOrgTreeView
              tenantId={tenantId}
              nodes={treeRoots ?? []}
              organizationTypeNames={organizationTypeNames}
            />
          </>
        )}
      </Card>

      <CreateRootOrganizationModal
        isOpen={rootOrgOpen && canCreateRoot}
        onClose={() => setRootOrgOpen(false)}
        onCreate={handleRootOrgCreated}
        isLoading={creatingRootOrg}
        organizationTypes={organizationTypes}
      />
    </>
  );
}
