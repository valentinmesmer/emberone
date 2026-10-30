/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense, useCallback, useEffect, useState } from 'react';
import { AdminMobileLayout } from '@/components/admin';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import { adminApi, type RetentionCategory, type TenantRetentionProfileDto } from '@/lib/api/admin';
import { getAdminTenantRetentionCategoryPath } from '@/lib/navigation/adminWorkspace';
import {
  formatRetentionPeriod,
  retentionCategoryStart,
  retentionCategoryTitle,
  retentionProfileLabel,
} from '@/lib/retention/labels';

function RetentionPageInner() {
  const params = useParams();
  const idParam = params?.id as string | undefined;
  const tenantId = idParam ? Number.parseInt(idParam, 10) : NaN;
  const idValid = Number.isFinite(tenantId) && tenantId > 0;

  const {
    canAccessAdministration,
    isGlobalAdmin,
    tenantId: authTenantId,
    isLoading: authLoading,
  } = useAuth();
  const t = useTranslation();
  const { showError } = useToast();

  const [profile, setProfile] = useState<TenantRetentionProfileDto | null>(null);
  const [loading, setLoading] = useState(false);

  const tenantOutOfScope =
    !isGlobalAdmin && idValid && (authTenantId == null || tenantId !== authTenantId);

  const loadProfile = useCallback(async () => {
    if (!idValid) return;
    setLoading(true);
    try {
      const data = await adminApi.getTenantRetentionProfile(tenantId);
      setProfile(data);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.errors.retentionLoadFailed'));
      setProfile(null);
    } finally {
      setLoading(false);
    }
  }, [idValid, tenantId, showError, t]);

  useEffect(() => {
    if (!canAccessAdministration || authLoading || !idValid || tenantOutOfScope) {
      return;
    }
    const id = window.setTimeout(() => {
      void loadProfile();
    }, 0);
    return () => window.clearTimeout(id);
  }, [canAccessAdministration, authLoading, idValid, loadProfile, tenantOutOfScope]);

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

  return (
    <AdminMobileLayout>
      {loading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : !profile ? (
        <NoticeCard>{t('admin.errors.retentionLoadFailed')}</NoticeCard>
      ) : (
        <div className="w-full space-y-4">
          <Card className="p-3 sm:p-4">
            <h1 className="text-base font-semibold text-[var(--text-primary)] sm:text-lg">
              {t('admin.retention.title')}
            </h1>
            <p className="mt-0.5 text-xs text-[var(--text-secondary)] sm:text-sm">
              {t('admin.retention.pageSubtitle')}
            </p>
            <div className="mt-3 border-t border-[var(--border-color)] pt-3">
              <FieldDisplay
                label={t('admin.retention.profileLabel')}
                value={retentionProfileLabel(t, profile.profileKey)}
              />
              <p className="mt-1 text-xs text-[var(--text-secondary)]">
                {t('admin.retention.profileHint')}
              </p>
            </div>
          </Card>

          <ListContainer label={t('admin.retention.periodsLabel')}>
            {profile.periods.map((row) => {
              const category = row.category as RetentionCategory;
              return (
                <ListRow
                  key={category}
                  href={getAdminTenantRetentionCategoryPath(tenantId, category)}
                  title={retentionCategoryTitle(t, category)}
                  subtitle={retentionCategoryStart(t, category)}
                  trailing={
                    <span className="text-sm font-medium tabular-nums text-[var(--text-primary)]">
                      {formatRetentionPeriod(t, row.amount, row.unit)}
                    </span>
                  }
                  accent={false}
                />
              );
            })}
          </ListContainer>
        </div>
      )}
    </AdminMobileLayout>
  );
}

export default function TenantRetentionPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <RetentionPageInner />
    </Suspense>
  );
}
