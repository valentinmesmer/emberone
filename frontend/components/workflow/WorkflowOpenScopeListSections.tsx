/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Fragment, type ReactNode } from 'react';
import { ListContainer } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import {
  groupByWorkflowOpenScope,
  WORKFLOW_OPEN_SCOPE_ORDER,
  workflowOpenScopeTranslationKey,
} from '@/lib/utils/workflowLabels';

interface WorkflowOpenScopeListSectionsProps<T> {
  items: T[];
  getWorkflowTypeId: (item: T) => number;
  getItemKey: (item: T) => string;
  renderRow: (item: T) => ReactNode;
  listLabel: string;
  listVariant?: 'card' | 'inset' | 'flush';
}

export function WorkflowOpenScopeListSections<T>({
  items,
  getWorkflowTypeId,
  getItemKey,
  renderRow,
  listLabel,
  listVariant = 'inset',
}: WorkflowOpenScopeListSectionsProps<T>) {
  const t = useTranslation();
  const byScope = groupByWorkflowOpenScope(items, getWorkflowTypeId);

  // One <ul> for all scopes, so the sibling pager on the detail pages spans every workflow.
  return (
    <ListContainer label={listLabel} variant={listVariant}>
      {WORKFLOW_OPEN_SCOPE_ORDER.map((scope) => {
        const scopeItems = byScope.get(scope) ?? [];
        if (scopeItems.length === 0) {
          return null;
        }
        return (
          <Fragment key={scope}>
            <li className="bg-[var(--hover-bg)] px-3 py-1.5 text-xs font-medium text-[var(--text-secondary)]">
              {t(workflowOpenScopeTranslationKey(scope))}
            </li>
            {scopeItems.map((item) => (
              <Fragment key={getItemKey(item)}>{renderRow(item)}</Fragment>
            ))}
          </Fragment>
        );
      })}
    </ListContainer>
  );
}
