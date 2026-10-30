/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect, useMemo } from 'react';
import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import { Card } from '@/components/ui/card';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  useAdminCourseOfferingTypeCatalog,
  useAdminCourseOfferingTypes,
  useReplaceAdminCourseOfferingTypes,
} from '@/lib/store';

interface AdminOrganizationCourseOfferingTypeSectionProps {
  tenantId: number;
  organizationId: number;
}

/** Course types the organization may offer. Hidden while the course offering module is off. */
export function AdminOrganizationCourseOfferingTypeSection({
  tenantId,
  organizationId,
}: AdminOrganizationCourseOfferingTypeSectionProps) {
  const t = useTranslation();
  const { showError } = useToast();
  const settings = useAdminCourseOfferingTypes(tenantId, organizationId);
  const enabled = settings.data?.enabled === true;
  const catalog = useAdminCourseOfferingTypeCatalog(tenantId, enabled);
  const replaceTypes = useReplaceAdminCourseOfferingTypes(tenantId, organizationId);

  useEffect(() => {
    if (!settings.isError && !catalog.isError) return;
    const error = settings.error ?? catalog.error;
    showError(error instanceof Error ? error.message : t('admin.courseOfferingTypes.loadError'));
  }, [settings.isError, settings.error, catalog.isError, catalog.error, showError, t]);

  const selected = useMemo(
    () => new Set(settings.data?.definitionConfigKeys ?? []),
    [settings.data?.definitionConfigKeys],
  );

  if (settings.isLoading) {
    return (
      <Card className="p-3 sm:p-4">
        <LoadingState />
      </Card>
    );
  }
  if (!enabled) return null;

  const toggle = async (configKey: string) => {
    const next = new Set(selected);
    if (!next.delete(configKey)) next.add(configKey);
    try {
      await replaceTypes.mutateAsync({ definitionConfigKeys: [...next] });
    } catch (err) {
      showError(err instanceof Error ? err.message : t('admin.courseOfferingTypes.saveFailed'));
    }
  };

  const types = catalog.data ?? [];

  return (
    <Card className="p-3 sm:p-4">
      <SectionHeader
        title={t('admin.courseOfferingTypes.title')}
        subtitle={t('admin.courseOfferingTypes.intro')}
      />
      <div className="mt-3">
        {catalog.isLoading ? (
          <LoadingState variant="embedded" size="sm" />
        ) : types.length === 0 ? (
          <EmptyStateCard>{t('admin.courseOfferingTypes.emptyCatalog')}</EmptyStateCard>
        ) : (
          <StandardCatalogCheckboxList
            listLabel={t('admin.courseOfferingTypes.title')}
            idPrefix="course-offering-type"
            items={types.map((type) => ({ id: type.configKey, label: type.name }))}
            selected={selected}
            onToggle={(configKey) => void toggle(configKey)}
            disabled={replaceTypes.isPending}
          />
        )}
      </div>
    </Card>
  );
}
