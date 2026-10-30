/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { standardsApi } from '@/lib/api/standards';
import {
  type TenantTrainingCourseTypeDto,
  tenantTrainingCourseTypesApi,
} from '@/lib/api/tenantTrainingCourseTypes';

interface Props {
  state: StandardContentState;
  embedded?: boolean;
}

const SAVE_DEBOUNCE_MS = 400;

function setsEqual(a: Set<string>, b: Set<string>): boolean {
  if (a.size !== b.size) return false;
  for (const key of a) {
    if (!b.has(key)) return false;
  }
  return true;
}

export function StandardTrainingCourseTypeSelectionManagement({ state, embedded = false }: Props) {
  const t = useTranslation();
  const { showError } = useToast();
  const { tenantId, standardId, items, editable } = state;

  const [loading, setLoading] = useState(true);
  const [catalog, setCatalog] = useState<TenantTrainingCourseTypeDto[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const selectedRef = useRef(selected);
  const lastSavedRef = useRef<Set<string>>(new Set());
  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    selectedRef.current = selected;
  }, [selected]);

  const selectedFromStandard = useMemo(
    () =>
      new Set(
        items.filter((item) => item.domainKey === 'training-courses').map((item) => item.configKey),
      ),
    [items],
  );

  const loadCatalog = useCallback(async () => {
    setLoading(true);
    try {
      const types = await tenantTrainingCourseTypesApi.list(tenantId);
      setCatalog(types.filter((type) => !type.archived));
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.standards.trainingCourses.loadCatalogFailed'),
      );
      setCatalog([]);
    } finally {
      setLoading(false);
    }
  }, [tenantId, showError, t]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadCatalog();
    });
  }, [loadCatalog]);

  useEffect(() => {
    if (setsEqual(selectedFromStandard, lastSavedRef.current)) return;
    const next = new Set(selectedFromStandard);
    setSelected(next);
    lastSavedRef.current = next;
  }, [selectedFromStandard]);

  const flushSave = useCallback(async () => {
    const toSave = selectedRef.current;
    if (setsEqual(toSave, lastSavedRef.current)) return;

    try {
      await standardsApi.updateTrainingCourseTypeSelections(tenantId, standardId, {
        definitionConfigKeys: [...toSave],
      });
      lastSavedRef.current = new Set(toSave);
    } catch (e) {
      setSelected(new Set(lastSavedRef.current));
      showError(e instanceof Error ? e.message : t('admin.standards.errors.saveFailed'));
    }
  }, [tenantId, standardId, showError, t]);

  const scheduleSave = useCallback(() => {
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    saveTimeoutRef.current = setTimeout(() => {
      saveTimeoutRef.current = null;
      void flushSave();
    }, SAVE_DEBOUNCE_MS);
  }, [flushSave]);

  useEffect(() => {
    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
        void flushSave();
      }
    };
  }, [flushSave]);

  const toggle = (configKey: string) => {
    if (!editable) return;

    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(configKey)) next.delete(configKey);
      else next.add(configKey);
      return next;
    });
    scheduleSave();
  };

  const content = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : catalog.length === 0 ? (
    <EmptyStateCard>{t('admin.standards.trainingCourses.emptyCatalog')}</EmptyStateCard>
  ) : (
    <StandardCatalogCheckboxList
      listLabel={t('admin.standards.trainingCourses.title')}
      idPrefix="training-course-selection"
      items={catalog.map((type) => ({ id: type.configKey, label: type.name }))}
      selected={selected}
      onToggle={toggle}
      disabled={!editable}
    />
  );

  return (
    <OrganizationManageSection
      embedded={embedded}
      title={t('admin.standards.trainingCourses.title')}
      subtitle={t('admin.standards.sections.trainingCoursesHint')}
    >
      {content}
    </OrganizationManageSection>
  );
}
