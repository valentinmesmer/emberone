/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { createElement, useMemo, type ReactNode } from 'react';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import {
  Award,
  Bell,
  IdCard,
  CalendarOff,
  ClipboardList,
  Phone,
  Clock,
  Inbox,
  FolderOpen,
  CheckSquare,
  Settings,
  GraduationCap,
  ListChecks,
  Medal,
  Car,
  Star,
} from 'lucide-react';
import type { MenuItem, WorkspaceMiddleItemGroup } from '@/components/navigation/NavigationMenu';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useAuth } from '@/hooks/useAuth';
import { useHasActiveOrganizationMembership } from '@/lib/store';
import { useWorkspaceOrganizations } from '@/hooks/useWorkspaceOrganizations';
import {
  getMyAreaHubGroupTitle,
  getAccessibleMyAreaSections,
  MY_AREA_BASE_PATH,
  MY_AREA_HUB_GROUP_ORDER,
  type MyAreaSectionId,
} from '@/lib/my-area/myAreaSections';
import {
  ORG_HUB_GROUP_ORDER,
  buildFlatOrgNavItems,
  getOrganizationHubGroupTitle,
} from '@/lib/navigation/organizationNavItems';
import { ADMIN_MODULE_ICONS } from '@/lib/navigation/adminModuleIcons';
import { getAdminWorkspaceModules, resolveAdminNavSection } from '@/lib/navigation/adminWorkspace';
import { isWorkspaceHubLandingPath } from '@/lib/navigation/workspaceHubLanding';
import { PermissionTypeEnum } from '@/types/permission';
import {
  useUnreadNotificationCount,
  useOpenTodoCount,
  useMyWorkflowPendingCount,
  useUnreadDocumentCount,
  useAppointmentUnreadCount,
  useAppointmentUnreadCountsByOrganization,
  useSelfPendingOnboardingInvitations,
} from '@/lib/store';

const ORG_PATH = /^\/organizations\/(\d+)/;

const MY_AREA_WORKSPACE_ICONS: Record<MyAreaSectionId, ReactNode> = {
  'master-data': <IdCard className="h-4 w-4" />,
  absence: <CalendarOff className="h-4 w-4" />,
  'emergency-contact': <Phone className="h-4 w-4" />,
  documents: <FolderOpen className="h-4 w-4" />,
  certifications: <Award className="h-4 w-4" />,
  'training-courses': <GraduationCap className="h-4 w-4" />,
  'course-registrations': <ClipboardList className="h-4 w-4" />,
  'driving-licenses': <Car className="h-4 w-4" />,
  qualifications: <Medal className="h-4 w-4" />,
  rank: <Star className="h-4 w-4" />,
  settings: <Settings className="h-4 w-4" />,
  'time-entries': <Clock className="h-4 w-4" />,
  workflows: <Inbox className="h-4 w-4" />,
  notifications: <Bell className="h-4 w-4" />,
  todos: <CheckSquare className="h-4 w-4" />,
  onboarding: <ListChecks className="h-4 w-4" />,
};

export function useWorkspaceNavMenu(): {
  middleItems: MenuItem[];
  middleItemGroups?: WorkspaceMiddleItemGroup[];
  selectedId?: string;
  myAreaBadge?: number;
  orgBadge?: number;
  orgBadges?: Record<number, number>;
} {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const router = useRouter();
  const t = useTranslation();
  const { hasPermission } = usePermissions();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const { canAccessAdministration, appRole, tenantId: authTenantId } = useAuth();
  const { organizations: workspaceOrganizations, isPending: workspacePending } =
    useWorkspaceOrganizations();
  const { hasOrganizationMembership } = useHasActiveOrganizationMembership();
  const accessibleMyAreaSections = useMemo(
    () => getAccessibleMyAreaSections(hasOrganizationMembership),
    [hasOrganizationMembership],
  );

  const { data: unreadNotificationCount } = useUnreadNotificationCount({
    enabled: hasEmberOneUserId,
  });
  const { data: openTodoCount } = useOpenTodoCount({ enabled: hasEmberOneUserId });
  const { data: pendingWorkflowCount } = useMyWorkflowPendingCount({ enabled: hasEmberOneUserId });
  const { data: unreadDocumentCount } = useUnreadDocumentCount({ enabled: hasEmberOneUserId });
  const { data: pendingOnboardings = [] } = useSelfPendingOnboardingInvitations({
    enabled: hasEmberOneUserId,
  });

  const orgPathMatch = pathname.match(ORG_PATH);
  const activeOrgId = orgPathMatch ? Number.parseInt(orgPathMatch[1], 10) : null;
  // Per-org count for the Termine sub-nav item
  // Appointment endpoints are membership-gated; no call until the source is known.
  const activeOrgIsManaged =
    workspacePending ||
    workspaceOrganizations.some((org) => org.id === activeOrgId && org.source === 'management');
  const { data: unreadAppointmentData } = useAppointmentUnreadCount(
    Number.isFinite(activeOrgId ?? NaN) && !activeOrgIsManaged ? activeOrgId : null,
  );
  const unreadAppointmentCount = unreadAppointmentData?.count;
  const appointmentOrganizationIds = useMemo(
    () =>
      workspaceOrganizations
        .filter(
          (org) =>
            org.source === 'membership' &&
            hasPermission(org.id, PermissionTypeEnum.APPOINTMENT_READ),
        )
        .map((org) => org.id),
    [workspaceOrganizations, hasPermission],
  );
  const orgBadges = useAppointmentUnreadCountsByOrganization(appointmentOrganizationIds);
  const crossOrgUnreadCount = Object.values(orgBadges).reduce((total, count) => total + count, 0);
  const orgBadgeCount = Math.max(crossOrgUnreadCount, unreadAppointmentCount ?? 0);
  const orgBadge = orgBadgeCount > 0 ? orgBadgeCount : undefined;

  const myAreaBadgeTotal =
    (unreadNotificationCount?.count ?? 0) +
    (openTodoCount ?? 0) +
    (pendingWorkflowCount ?? 0) +
    (unreadDocumentCount?.count ?? 0) +
    pendingOnboardings.length;
  const myAreaBadge = myAreaBadgeTotal > 0 ? myAreaBadgeTotal : undefined;

  return useMemo(() => {
    const myAreaBadges: Partial<Record<string, number>> = {
      notifications: unreadNotificationCount?.count,
      todos: openTodoCount,
      workflows: pendingWorkflowCount,
      documents: unreadDocumentCount?.count,
      onboarding: pendingOnboardings.length > 0 ? pendingOnboardings.length : undefined,
    };

    if (canAccessAdministration && pathname.startsWith('/admin')) {
      if (isWorkspaceHubLandingPath(pathname)) {
        return { middleItems: [], selectedId: undefined, myAreaBadge, orgBadge, orgBadges };
      }
      return {
        middleItems: [],
        myAreaBadge,
        orgBadge,
        orgBadges,
        middleItemGroups: [
          {
            id: 'admin-workspace',
            title: t('admin.hub.navTitle'),
            items: getAdminWorkspaceModules({
              appRole,
              tenantId: authTenantId,
            }).map((module) => ({
              id: module.id,
              label: t(module.labelKey),
              icon: createElement(ADMIN_MODULE_ICONS[module.icon], { className: 'h-4 w-4' }),
              onClick: () => router.push(module.href),
            })),
          },
        ],
        selectedId: resolveAdminNavSection(pathname, searchParams),
      };
    }

    const orgMatch = pathname.match(ORG_PATH);
    if (orgMatch) {
      const orgId = Number.parseInt(orgMatch[1], 10);
      if (!Number.isNaN(orgId)) {
        if (isWorkspaceHubLandingPath(pathname)) {
          return { middleItems: [], selectedId: undefined, myAreaBadge, orgBadge, orgBadges };
        }

        const entry = workspaceOrganizations.find((e) => e.id === orgId);
        const org = { id: orgId, name: entry?.name ?? '' };
        const flatItems = buildFlatOrgNavItems(org, pathname, t, hasPermission, 'workspace');
        if (flatItems.length === 0) {
          return { middleItems: [], selectedId: undefined, myAreaBadge, orgBadge, orgBadges };
        }
        const appointmentsTileId = `org-${orgId}-appointments`;
        const middleItemGroups: WorkspaceMiddleItemGroup[] = ORG_HUB_GROUP_ORDER.map((groupId) => ({
          id: groupId,
          title: getOrganizationHubGroupTitle(groupId, t),
          items: flatItems
            .filter((i) => i.hubGroupId === groupId)
            .map((item) => ({
              id: item.id,
              label: item.label,
              icon: item.icon,
              onClick: () => router.push(item.href),
              badge:
                item.id === appointmentsTileId &&
                unreadAppointmentCount != null &&
                unreadAppointmentCount > 0
                  ? unreadAppointmentCount
                  : undefined,
            })),
        })).filter((g) => g.items.length > 0);

        const assignedIds = new Set(middleItemGroups.flatMap((g) => g.items.map((i) => i.id)));
        const orphanItems = flatItems.filter((i) => !assignedIds.has(i.id));
        if (orphanItems.length > 0) {
          middleItemGroups.push({
            id: 'ungrouped',
            title: t('common.hub.ungrouped'),
            items: orphanItems.map((item) => ({
              id: item.id,
              label: item.label,
              icon: item.icon,
              onClick: () => router.push(item.href),
            })),
          });
        }

        return {
          middleItems: [],
          middleItemGroups,
          selectedId: flatItems.find((i) => i.active)?.id,
          myAreaBadge,
          orgBadge,
          orgBadges,
        };
      }
    }

    if (
      hasEmberOneUserId &&
      (pathname === MY_AREA_BASE_PATH || pathname.startsWith(`${MY_AREA_BASE_PATH}/`))
    ) {
      if (isWorkspaceHubLandingPath(pathname)) {
        return { middleItems: [], selectedId: undefined, myAreaBadge, orgBadge, orgBadges };
      }

      const middleItemGroups: WorkspaceMiddleItemGroup[] = MY_AREA_HUB_GROUP_ORDER.map(
        (groupId) => ({
          id: groupId,
          title: getMyAreaHubGroupTitle(groupId, t),
          items: accessibleMyAreaSections
            .filter((section) => section.hubGroupId === groupId)
            .map((section) => ({
              id: section.id,
              label: t(section.labelKey),
              icon: MY_AREA_WORKSPACE_ICONS[section.id],
              onClick: () => router.push(section.path),
              badge: myAreaBadges[section.id],
            })),
        }),
      ).filter((g) => g.items.length > 0);
      const selected = accessibleMyAreaSections.find(
        (s) => pathname === s.path || pathname.startsWith(`${s.path}/`),
      );
      return {
        middleItems: [],
        middleItemGroups,
        selectedId: selected?.id,
        myAreaBadge,
        orgBadge,
        orgBadges,
      };
    }

    return { middleItems: [], selectedId: undefined, myAreaBadge, orgBadge, orgBadges };
  }, [
    pathname,
    searchParams,
    router,
    t,
    hasPermission,
    hasEmberOneUserId,
    canAccessAdministration,
    appRole,
    authTenantId,
    workspaceOrganizations,
    accessibleMyAreaSections,
    myAreaBadge,
    orgBadge,
    orgBadges,
    unreadNotificationCount?.count,
    openTodoCount,
    pendingWorkflowCount,
    unreadDocumentCount?.count,
    pendingOnboardings.length,
    unreadAppointmentCount,
  ]);
}
