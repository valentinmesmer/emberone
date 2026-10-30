/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

  return (
    <div className="space-y-4">
      {WORKFLOW_OPEN_SCOPE_ORDER.map((scope) => {
        const scopeItems = byScope.get(scope) ?? [];
        if (scopeItems.length === 0) {
          return null;
        }
        return (
          <section key={scope} className="space-y-2">
            <h3 className="text-sm font-medium text-[var(--text-primary)]">
              {t(workflowOpenScopeTranslationKey(scope))}
            </h3>
            <ListContainer label={listLabel} variant={listVariant}>
              {scopeItems.map((item) => (
                <Fragment key={getItemKey(item)}>{renderRow(item)}</Fragment>
              ))}
            </ListContainer>
          </section>
        );
      })}
    </div>
  );
}
