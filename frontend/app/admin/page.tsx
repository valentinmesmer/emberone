/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { createElement, Suspense, useMemo } from 'react';
import { AdminMobileLayout } from '@/components/admin';
import { HubPage } from '@/components/hub';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import { ADMIN_MODULE_ICONS } from '@/lib/navigation/adminModuleIcons';
import { getAdminWorkspaceModules } from '@/lib/navigation/adminWorkspace';

function AdminHubInner() {
  const {
    canAccessAdministration,
    isGlobalAdmin,
    isLoading: authLoading,
    appRole,
    tenantId: authTenantId,
  } = useAuth();
  const t = useTranslation();

  const navTiles = useMemo(
    () =>
      getAdminWorkspaceModules({
        appRole,
        tenantId: authTenantId,
      }).map((module) => ({
        id: module.id,
        href: module.href,
        icon: createElement(ADMIN_MODULE_ICONS[module.icon]),
        title: t(module.labelKey),
      })),
    [appRole, authTenantId, t],
  );

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

  return (
    <AdminMobileLayout>
      <HubPage
        header={{
          eyebrow: t('admin.hub.eyebrow'),
          title: t('admin.title'),
          subtitle: isGlobalAdmin ? t('admin.subtitleGlobal') : t('admin.subtitleTenant'),
          density: 'compact',
        }}
        tiles={navTiles}
      />
    </AdminMobileLayout>
  );
}

export default function AdminHubPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <AdminHubInner />
    </Suspense>
  );
}
