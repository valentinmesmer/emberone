/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ChevronRight } from 'lucide-react';
import Link from 'next/link';
import { Badge } from '@/components/ui/badge';
import { EmptyState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import type { AdminOrganizationTreeNodeDto } from '@/lib/api/admin';
import { cn } from '@/lib/utils/cn';

interface AdminOrgTreeViewProps {
  tenantId: number;
  nodes: AdminOrganizationTreeNodeDto[];
  /** Organization type id -> display name, from the tenant's own organization-type catalog. */
  organizationTypeNames: Map<number, string>;
}

function NodeLink({
  tenantId,
  node,
  depth,
  organizationTypeNames,
}: {
  tenantId: number;
  node: AdminOrganizationTreeNodeDto;
  depth: number;
  organizationTypeNames: Map<number, string>;
}) {
  const t = useTranslation();

  return (
    <Link
      href={`/admin/tenants/${tenantId}/organizations/${node.id}`}
      className={cn(
        'group flex min-w-0 items-center gap-3 px-4 py-4 transition-colors',
        'hover:bg-[var(--hover-bg)] active:bg-[var(--hover-bg)]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-[var(--focus-ring)]',
      )}
    >
      <div className="min-w-0 flex-1">
        <div
          className={cn(
            'break-words leading-snug text-[var(--text-primary)]',
            depth === 0 ? 'text-[15px] font-semibold' : 'text-sm font-medium',
          )}
        >
          {node.name}
        </div>
        <div className="mt-0.5 text-xs text-[var(--text-secondary)]">
          <span className="font-mono">{node.organizationCode}</span>
          <span className="mx-1 opacity-40">·</span>
          {organizationTypeNames.get(node.organizationTypeId) ?? ''}
        </div>
        {node.deactivatedAt ? (
          <div className="mt-0.5">
            <Badge tone="neutral">{t('admin.orgDeactivate.deactivatedBadge')}</Badge>
          </div>
        ) : null}
        {node.leaderMissing ? (
          <div className="mt-0.5">
            <Badge tone="warning">{t('admin.orgTree.leaderMissingTitle')}</Badge>
          </div>
        ) : node.leaderMissingInSubtree ? (
          <div className="mt-0.5">
            <Badge tone="warning">{t('admin.orgTree.leaderMissingInSubtreeTitle')}</Badge>
          </div>
        ) : null}
      </div>

      <ChevronRight
        className="h-4 w-4 shrink-0 text-[var(--text-secondary)] opacity-30 transition-opacity group-hover:opacity-70"
        aria-hidden
      />
    </Link>
  );
}

function TreeBranch({
  tenantId,
  node,
  depth,
  organizationTypeNames,
}: {
  tenantId: number;
  node: AdminOrganizationTreeNodeDto;
  depth: number;
  organizationTypeNames: Map<number, string>;
}) {
  const children = node.children ?? [];

  return (
    <li className="min-w-0">
      <NodeLink
        tenantId={tenantId}
        node={node}
        depth={depth}
        organizationTypeNames={organizationTypeNames}
      />
      {children.length > 0 ? (
        <ul className="ml-4 border-l border-[var(--border-color)]/50 divide-y divide-[var(--border-color)]/40">
          {children.map((child) => (
            <TreeBranch
              key={child.id}
              tenantId={tenantId}
              node={child}
              depth={depth + 1}
              organizationTypeNames={organizationTypeNames}
            />
          ))}
        </ul>
      ) : null}
    </li>
  );
}

export function AdminOrgTreeView({
  tenantId,
  nodes,
  organizationTypeNames,
}: AdminOrgTreeViewProps) {
  const t = useTranslation();

  if (nodes.length === 0) {
    return <EmptyState>{t('admin.orgTree.treeEmpty')}</EmptyState>;
  }

  return (
    <ul
      aria-label={t('admin.orgTree.orgStructureTreeCardTitle')}
      className="divide-y divide-[var(--border-color)]/60 overflow-hidden rounded-xl border border-[var(--border-color)]/70"
    >
      {nodes.map((node) => (
        <TreeBranch
          key={node.id}
          tenantId={tenantId}
          node={node}
          depth={0}
          organizationTypeNames={organizationTypeNames}
        />
      ))}
    </ul>
  );
}
