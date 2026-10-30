/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useEffect, type ReactNode } from 'react';
import { ArrowRight } from 'lucide-react';
import { useTranslation } from '@/hooks/useTranslation';
import { useHomeSearchItems } from '@/hooks/useHomeSearchItems';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import {
  useMyOrganizations,
  useSelfServiceHoursTotal,
  useSelfOperationAlarmAttendanceStats,
  useUIStore,
} from '@/lib/store';
import { getOrganizationWorkspaceEntryPath } from '@/lib/utils';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  GreetingSection,
  HomeQuickSearch,
  HomeQualificationsStatCard,
  HomeTodosCard,
  HomeAppointmentsCard,
} from '@/components/home';
import { MobileNavigationLayout } from '@/components/layout/MobileNavigationLayout';
import { ORG_SWITCHER_THRESHOLD } from '@/components/navigation/OrganizationNavSwitcher';
import { Card } from '@/components/ui/card';
import { DropdownButton } from '@/components/ui/button-dropdown';
import { DEPARTMENT_SECTION, getDepartmentSectionPath } from '@/lib/department/sections';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';

function SectionHeading({ children }: { children: ReactNode }) {
  return (
    <div className="mb-1.5 sm:mb-2">
      <h2 className="text-sm font-semibold tracking-tight text-[var(--text-primary)]">
        {children}
      </h2>
    </div>
  );
}

function DashboardStatCard({
  title,
  value,
  subtitle,
  detailRoute,
}: {
  title: string;
  value: string;
  subtitle: string;
  detailRoute: string | null;
}) {
  const content = (
    <div className="flex min-h-[4.75rem] flex-col justify-between gap-2 sm:min-h-[5rem]">
      <p className="text-[10px] font-semibold uppercase tracking-wide text-[var(--text-secondary)] sm:text-[11px]">
        {title}
      </p>
      <p className="text-lg font-bold tabular-nums tracking-tight leading-none text-[var(--text-primary)] sm:text-xl">
        {value}
      </p>
      <p className="text-[11px] leading-snug text-[var(--text-secondary)] sm:text-xs">{subtitle}</p>
    </div>
  );

  return (
    <Card className="!p-0">
      {detailRoute ? (
        <Link
          href={detailRoute}
          className="block h-full w-full p-2.5 text-left outline-none ring-offset-2 ring-offset-[var(--page-bg)] transition-colors hover:bg-[var(--hover-bg)]/40 focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] sm:p-3"
        >
          {content}
        </Link>
      ) : (
        <div className="h-full w-full p-2.5 text-left sm:p-3">{content}</div>
      )}
    </Card>
  );
}

export default function Home() {
  const t = useTranslation();
  const router = useRouter();
  const hasEmberOneUserId = useHasEmberOneUserId();

  const {
    data: organizations = [],
    isPending: isPendingMyOrgs,
    isFetching: isFetchingMyOrgs,
  } = useMyOrganizations();
  /** Disabled query (no employee id) stays pending without fetch - do not treat as loading. */
  const isLoadingMyOrgs = isPendingMyOrgs && isFetchingMyOrgs;
  const { selectedOrganizationId, setSelectedOrganizationId } = useUIStore();

  useEffect(() => {
    if (organizations.length > 0 && !selectedOrganizationId) {
      const mainOrg = organizations.find((org) => org.main);
      if (mainOrg?.organization) {
        setSelectedOrganizationId(mainOrg.organization.id);
      } else if (organizations[0].organization) {
        setSelectedOrganizationId(organizations[0].organization.id);
      }
    }
  }, [organizations, selectedOrganizationId, setSelectedOrganizationId]);

  const handleOrganizationSelect = (orgId: string) => {
    const orgIdNum = parseInt(orgId, 10);
    setSelectedOrganizationId(orgIdNum);
    const selectedOrg = organizations.find(
      (org) => org.organization && org.organization.id.toString() === orgId,
    );
    if (selectedOrg?.organization) {
      router.push(getOrganizationWorkspaceEntryPath(selectedOrg.organization.id));
    }
  };

  const organizationOptions = useMemo(
    () =>
      organizations.flatMap((org) => {
        if (!org.organization) return [];
        return [
          {
            value: org.organization.id.toString(),
            label: `${org.organization.name}${org.main ? ` · ${t('home.navigation.organizations.mainBadge')}` : ''}`,
          },
        ];
      }),
    [organizations, t],
  );
  const showOrganizationDropdown = organizationOptions.length >= ORG_SWITCHER_THRESHOLD;

  const handleNavigateToMainOrganization = () => {
    const mainOrg = organizations.find((org) => org.main);
    const targetOrg = mainOrg || organizations[0];

    if (targetOrg?.organization?.id) {
      router.push(getOrganizationWorkspaceEntryPath(targetOrg.organization.id));
    }
  };

  const handleNavigateToProfile = () => {
    router.push(MY_AREA_BASE_PATH);
  };

  const {
    data: serviceHoursTotal,
    isPending: isPendingAttendanceHours,
    isFetching: isFetchingAttendanceHours,
  } = useSelfServiceHoursTotal(new Date().getFullYear());

  const {
    data: alarmAttendanceStats,
    isPending: isPendingAlarmStats,
    isFetching: isFetchingAlarmStats,
  } = useSelfOperationAlarmAttendanceStats();

  /** TanStack Query v5: disabled queries stay `isPending` without fetching - only treat as loading while actually fetching. */
  const isLoadingAttendanceHours = isPendingAttendanceHours && isFetchingAttendanceHours;
  const isLoadingAlarmStats = isPendingAlarmStats && isFetchingAlarmStats;

  const alarmPresenceLabel = useMemo(() => {
    if (isLoadingAlarmStats) {
      return '…';
    }
    const total = alarmAttendanceStats?.totalAlarms ?? 0;
    const current = alarmAttendanceStats?.attendedAlarms ?? 0;
    const percent = total > 0 ? Math.round((current / total) * 100) : 0;
    return t('home.dashboard.stats.missions.value', { current, total, percent });
  }, [isLoadingAlarmStats, alarmAttendanceStats, t]);

  const serviceLeadershipHoursLabel = useMemo(() => {
    if (isLoadingAttendanceHours) {
      return '…';
    }
    const hours = Math.round(serviceHoursTotal?.totalHours ?? 0);
    return t('home.dashboard.stats.leadershipHours.value', { hours });
  }, [isLoadingAttendanceHours, serviceHoursTotal, t]);

  const alarmStatWidget = useMemo(
    () => ({
      title: t('home.dashboard.stats.missions.title'),
      value: alarmPresenceLabel,
      subtitle: t('home.dashboard.stats.missions.subtitle'),
      detailRoute: selectedOrganizationId
        ? getDepartmentSectionPath(selectedOrganizationId, DEPARTMENT_SECTION.OPERATION)
        : null,
    }),
    [t, alarmPresenceLabel, selectedOrganizationId],
  );

  const serviceHoursStatWidget = useMemo(
    () => ({
      title: t('home.dashboard.stats.leadershipHours.title'),
      value: serviceLeadershipHoursLabel,
      subtitle: t('home.dashboard.stats.leadershipHours.subtitle'),
      detailRoute: selectedOrganizationId
        ? getDepartmentSectionPath(selectedOrganizationId, DEPARTMENT_SECTION.SERVICE)
        : null,
    }),
    [t, serviceLeadershipHoursLabel, selectedOrganizationId],
  );

  const { items: homeSearchItems, isLoading: isHomeSearchLoading } = useHomeSearchItems();

  return (
    <MobileNavigationLayout contentClassName="w-full xl:max-w-6xl 2xl:max-w-[90rem] mx-auto">
      <div className="w-full">
        <GreetingSection />

        {homeSearchItems.length > 0 || isHomeSearchLoading ? (
          <HomeQuickSearch items={homeSearchItems} isLoading={isHomeSearchLoading} />
        ) : null}

        <section className="space-y-1.5 sm:space-y-2">
          <SectionHeading>{t('home.dashboard.quickActionsTitle')}</SectionHeading>
          <div className="grid grid-cols-1 items-stretch gap-3 xl:grid-cols-2 xl:gap-4 2xl:gap-5">
            <Card className="flex h-full !p-4 sm:!p-5 lg:!p-6">
              <div className="flex min-h-0 w-full flex-1 flex-col gap-3 sm:gap-3.5">
                <div className="min-w-0">
                  <h3 className="text-base font-semibold leading-snug text-[var(--text-primary)] sm:text-lg">
                    {t('home.navigation.personalData.title')}
                  </h3>

                  <p className="mt-1 text-[11px] leading-snug text-[var(--text-secondary)]/80 sm:text-xs">
                    {hasEmberOneUserId
                      ? t('home.dashboard.personalDataHint')
                      : t('home.dashboard.personalDataNoEmployeeHint')}
                  </p>
                </div>
                <div className="mt-auto w-full shrink-0">
                  <DropdownButton
                    variant="primary"
                    className="w-full text-sm"
                    onMainClick={handleNavigateToProfile}
                    disabled={!hasEmberOneUserId}
                    title={!hasEmberOneUserId ? t('myArea.error.noEmployeeId') : undefined}
                    icon={<ArrowRight className="h-4 w-4" />}
                  >
                    {t('home.navigation.personalData.button')}
                  </DropdownButton>
                </div>
              </div>
            </Card>

            <Card className="flex h-full !p-4 sm:!p-5 lg:!p-6">
              <div className="flex min-h-0 w-full flex-1 flex-col gap-3 sm:gap-3.5">
                <div className="min-w-0">
                  <h3 className="text-base font-semibold leading-snug text-[var(--text-primary)] sm:text-lg">
                    {t('home.navigation.organizations.title')}
                  </h3>
                  <p className="mt-1 text-[11px] leading-snug text-[var(--text-secondary)]/80 sm:text-xs">
                    {!hasEmberOneUserId
                      ? t('home.dashboard.organizationsNoEmployeeHint')
                      : isLoadingMyOrgs
                        ? t('common.loading')
                        : organizations.length === 0
                          ? t('home.dashboard.organizationsNoneHint')
                          : t('home.dashboard.organizationHint', {
                              count: organizations.length,
                            })}
                  </p>
                </div>
                <div className="mt-auto w-full shrink-0">
                  <DropdownButton
                    variant="primary"
                    className="w-full text-sm"
                    onMainClick={handleNavigateToMainOrganization}
                    disabled={!hasEmberOneUserId || isLoadingMyOrgs || organizations.length === 0}
                    title={
                      !hasEmberOneUserId
                        ? t('myArea.error.noEmployeeId')
                        : organizations.length === 0 && !isLoadingMyOrgs
                          ? t('home.dashboard.organizationsNoneHint')
                          : undefined
                    }
                    options={showOrganizationDropdown ? organizationOptions : undefined}
                    selectedValue={
                      showOrganizationDropdown
                        ? (selectedOrganizationId?.toString() ?? '')
                        : undefined
                    }
                    onOptionSelect={showOrganizationDropdown ? handleOrganizationSelect : undefined}
                    icon={<ArrowRight className="h-4 w-4" />}
                  >
                    {t('home.navigation.organizations.button')}
                  </DropdownButton>
                </div>
              </div>
            </Card>
          </div>
        </section>

        <section className="mt-3 space-y-1.5 sm:mt-4 sm:space-y-2">
          <SectionHeading>{t('home.dashboard.stats.title')}</SectionHeading>

          <div className="grid grid-cols-1 items-stretch gap-3 xl:grid-cols-3 xl:gap-4 2xl:gap-5">
            <DashboardStatCard {...alarmStatWidget} />
            <HomeQualificationsStatCard />
            <DashboardStatCard {...serviceHoursStatWidget} />
          </div>
        </section>

        <section className="mt-3 space-y-1.5 sm:mt-4 sm:space-y-2">
          <SectionHeading>{t('home.dashboard.tasksAndAppointmentsTitle')}</SectionHeading>

          <div className="grid grid-cols-1 items-stretch gap-3 xl:grid-cols-2 xl:gap-4 2xl:gap-5">
            <HomeTodosCard />
            <HomeAppointmentsCard />
          </div>
        </section>
      </div>
    </MobileNavigationLayout>
  );
}
