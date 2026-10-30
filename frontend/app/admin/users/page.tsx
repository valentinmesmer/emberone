/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Suspense } from 'react';
import { AdminGlobalUsersPanel, AdminMobileLayout } from '@/components/admin';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { PageContentSection } from '@/components/ui/page-content-section';
import { PageHeader } from '@/components/ui/page-header';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';

export default function AdminUsersPage() {
  const { canAccessAdministration, isGlobalAdmin, isLoading: authLoading } = useAuth();
  const t = useTranslation();

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

  const description = isGlobalAdmin ? t('admin.subtitleGlobal') : t('admin.subtitleTenant');
  const listHint = t('admin.userManagementListHint');

  return (
    <AdminMobileLayout>
      <PageHeader
        title={t('admin.sections.userManagement')}
        description={`${description}. ${listHint}`}
        size="compact"
      />

      <PageContentSection>
        <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
          <AdminGlobalUsersPanel isGlobalAdmin={isGlobalAdmin} />
        </Suspense>
      </PageContentSection>
    </AdminMobileLayout>
  );
}
