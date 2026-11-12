/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense, useMemo } from 'react';
import { AdminMobileLayout } from '@/components/admin';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, NoticeCard } from '@/components/ui/message-cards';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import type { RetentionCategoryPeriodDto } from '@/lib/api/admin';
import { useQueryErrorToast, useTenantRetentionProfile } from '@/lib/store';
import {
  formatRetentionPeriod,
  isRetentionCategory,
  retentionCategoryStart,
  retentionCategoryTitle,
  retentionTableLabel,
} from '@/lib/retention/labels';

function RetentionCategoryPageInner() {
  const params = useParams();
  const idParam = params?.id as string | undefined;
  const categoryParam = params?.category as string | undefined;
  const tenantId = idParam ? Number.parseInt(idParam, 10) : NaN;
  const idValid = Number.isFinite(tenantId) && tenantId > 0;
  const categoryValid = !!categoryParam && isRetentionCategory(categoryParam);
  const category = categoryValid ? categoryParam : null;

  const {
    canAccessAdministration,
    isGlobalAdmin,
    tenantId: authTenantId,
    isLoading: authLoading,
  } = useAuth();
  const t = useTranslation();

  const tenantOutOfScope =
    !isGlobalAdmin && idValid && (authTenantId == null || tenantId !== authTenantId);

  const profileQuery = useTenantRetentionProfile(
    tenantId,
    canAccessAdministration && !authLoading && idValid && !tenantOutOfScope,
  );
  useQueryErrorToast(profileQuery.error, { messageKey: 'admin.errors.retentionLoadFailed' });
  const profile = profileQuery.data ?? null;
  const loading = profileQuery.isLoading;

  const period: RetentionCategoryPeriodDto | null = useMemo(() => {
    if (!profile || !category) return null;
    return profile.periods.find((row) => row.category === category) ?? null;
  }, [profile, category]);

  if (authLoading) {
    return (
      <div className="min-h-screen bg-[var(--page-bg)] pt-20">
        <LoadingState variant="embedded" />
      </div>
    );
  }

  if (!canAccessAdministration) {
    return (
      <div className="min-h-screen bg-[var(--page-bg)] px-4 pt-24 pb-12">
        <div className="mx-auto max-w-lg">
          <NoticeCard>{t('admin.accessDenied')}</NoticeCard>
        </div>
      </div>
    );
  }

  if (!idValid) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.errors.tenantLoadFailed')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  if (tenantOutOfScope) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.errors.tenantScopeDenied')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  if (!categoryValid || !category) {
    return (
      <AdminMobileLayout>
        <NoticeCard>{t('admin.retention.categoryNotFound')}</NoticeCard>
      </AdminMobileLayout>
    );
  }

  return (
    <AdminMobileLayout>
      {loading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : !profile || !period ? (
        <NoticeCard>{t('admin.retention.categoryNotFound')}</NoticeCard>
      ) : (
        <div className="w-full space-y-4">
          <Card className="p-3 sm:p-4">
            <h1 className="text-base font-semibold text-[var(--text-primary)] sm:text-lg">
              {retentionCategoryTitle(t, category)}
            </h1>
            <div className="mt-3 grid gap-3 sm:grid-cols-2">
              <FieldDisplay
                label={t('admin.retention.startLabel')}
                value={retentionCategoryStart(t, category)}
              />
              <FieldDisplay
                label={t('admin.retention.periodLabel')}
                value={formatRetentionPeriod(t, period.amount, period.unit)}
              />
            </div>
          </Card>

          {(period.tables ?? []).length === 0 ? (
            <EmptyStateCard>{t('admin.retention.entitiesEmpty')}</EmptyStateCard>
          ) : (
            <ListContainer label={t('admin.retention.entitiesLabel')}>
              {(period.tables ?? []).map((table) => (
                <ListRow
                  key={table}
                  title={retentionTableLabel(t, table)}
                  accent={false}
                  chevron={false}
                />
              ))}
            </ListContainer>
          )}
        </div>
      )}
    </AdminMobileLayout>
  );
}

export default function TenantRetentionCategoryPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <RetentionCategoryPageInner />
    </Suspense>
  );
}
