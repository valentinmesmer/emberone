/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useParams } from 'next/navigation';
import { Suspense } from 'react';
import { AdminMobileLayout } from '@/components/admin';
import { AdminOrganizationTypeManagement } from '@/components/admin/AdminOrganizationTypeManagement';
import { AdminTenantCertificationManagement } from '@/components/admin/AdminTenantCertificationManagement';
import { AdminTenantDrivingLicenseManagement } from '@/components/admin/AdminTenantDrivingLicenseManagement';
import { AdminTenantFunctionManagement } from '@/components/admin/AdminTenantFunctionManagement';
import { AdminTenantOnboardingDocumentManagement } from '@/components/admin/AdminTenantOnboardingDocumentManagement';
import { AdminTenantQualificationManagement } from '@/components/admin/AdminTenantQualificationManagement';
import { AdminTenantRankManagement } from '@/components/admin/AdminTenantRankManagement';
import { AdminTenantTrainingCourseManagement } from '@/components/admin/AdminTenantTrainingCourseManagement';
import { AdminTenantWorkflowDefinitionManagement } from '@/components/admin/AdminTenantWorkflowDefinitionManagement';
import { Card } from '@/components/ui/card';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';

function TenantStandardsPageInner() {
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

  const tenantOutOfScope =
    !isGlobalAdmin && idValid && (authTenantId == null || tenantId !== authTenantId);

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
      <div className="w-full space-y-4">
        <Card className="p-3 sm:p-4">
          <h1 className="text-base font-semibold text-[var(--text-primary)] sm:text-lg">
            {t('admin.tenantStandards.title')}
          </h1>
          <p className="mt-0.5 text-xs text-[var(--text-secondary)] sm:text-sm">
            {t('admin.tenantStandards.pageSubtitle')}
          </p>
        </Card>

        <Card className="p-3 sm:p-4">
          <AdminOrganizationTypeManagement tenantId={tenantId} embedded />
        </Card>

        <Card className="p-3 sm:p-4">
          <AdminTenantOnboardingDocumentManagement tenantId={tenantId} embedded />
        </Card>

        <Card className="p-3 sm:p-4">
          <AdminTenantTrainingCourseManagement tenantId={tenantId} embedded />
        </Card>

        <Card className="p-3 sm:p-4">
          <AdminTenantDrivingLicenseManagement tenantId={tenantId} embedded />
        </Card>

        <Card className="p-3 sm:p-4">
          <AdminTenantCertificationManagement tenantId={tenantId} embedded />
        </Card>

        <Card className="p-3 sm:p-4">
          <AdminTenantQualificationManagement tenantId={tenantId} embedded />
        </Card>

        <Card className="p-3 sm:p-4">
          <AdminTenantRankManagement tenantId={tenantId} embedded />
        </Card>

        <Card className="p-3 sm:p-4">
          <AdminTenantFunctionManagement tenantId={tenantId} />
        </Card>

        <Card className="p-3 sm:p-4">
          <SectionHeader
            className="mb-3"
            title={t('admin.tenantStandards.workflowsSectionTitle')}
            subtitle={t('admin.tenantStandards.workflowsSectionHint')}
          />
          <AdminTenantWorkflowDefinitionManagement tenantId={tenantId} embedded />
        </Card>
      </div>
    </AdminMobileLayout>
  );
}

export default function TenantStandardsPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <TenantStandardsPageInner />
    </Suspense>
  );
}
