/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { useMemo } from 'react';
import {
  Award,
  Bell,
  CalendarOff,
  ClipboardList,
  Car,
  CheckSquare,
  Clock,
  FolderOpen,
  GraduationCap,
  IdCard,
  Inbox,
  ListChecks,
  Medal,
  Phone,
  Settings,
  Star,
} from 'lucide-react';
import { useTranslation } from '@/hooks/useTranslation';
import { useUserInfo } from '@/hooks/useUserInfo';
import { useSelf } from '@/lib/store';
import { HubPage } from '@/components/hub';
import {
  getMyAreaHubGroupTitle,
  getAccessibleMyAreaSections,
  MY_AREA_HUB_GROUP_ORDER,
  type MyAreaSectionId,
} from '@/lib/my-area/myAreaSections';
import {
  useUnreadNotificationCount,
  useOpenTodoCount,
  useMyWorkflowPendingCount,
  useUnreadDocumentCount,
  useHasActiveOrganizationMembership,
  useSelfPendingOnboardingInvitations,
} from '@/lib/store';

const SECTION_ICONS: Record<MyAreaSectionId, ReactNode> = {
  'master-data': <IdCard />,
  absence: <CalendarOff />,
  'emergency-contact': <Phone />,
  documents: <FolderOpen />,
  certifications: <Award />,
  'training-courses': <GraduationCap />,
  'course-registrations': <ClipboardList />,
  'driving-licenses': <Car />,
  qualifications: <Medal />,
  rank: <Star />,
  settings: <Settings />,
  'time-entries': <Clock />,
  workflows: <Inbox />,
  notifications: <Bell />,
  todos: <CheckSquare />,
  onboarding: <ListChecks />,
};

export default function MyAreaHubPage() {
  const t = useTranslation();
  const { firstName, lastName } = useUserInfo();
  const { data: self } = useSelf();
  const displayName = [firstName, lastName].filter(Boolean).join(' ').trim() || t('myArea.title');

  const { data: unreadNotificationCount } = useUnreadNotificationCount();
  const { data: openTodoCount } = useOpenTodoCount();
  const { data: pendingWorkflowCount } = useMyWorkflowPendingCount();
  const { data: unreadDocumentCount } = useUnreadDocumentCount();
  const { data: pendingOnboardings = [] } = useSelfPendingOnboardingInvitations();
  const { hasOrganizationMembership } = useHasActiveOrganizationMembership();
  const accessibleSections = useMemo(
    () => getAccessibleMyAreaSections(hasOrganizationMembership),
    [hasOrganizationMembership],
  );
  const sectionBadges = useMemo<Partial<Record<MyAreaSectionId, number>>>(
    () => ({
      notifications: unreadNotificationCount?.count,
      todos: openTodoCount,
      workflows: pendingWorkflowCount,
      documents: unreadDocumentCount?.count,
      onboarding: pendingOnboardings.length > 0 ? pendingOnboardings.length : undefined,
    }),
    [
      unreadNotificationCount?.count,
      openTodoCount,
      pendingWorkflowCount,
      unreadDocumentCount?.count,
      pendingOnboardings.length,
    ],
  );

  const navTiles = useMemo(
    () =>
      accessibleSections.map((section) => ({
        id: section.id,
        href: section.path,
        title: t(section.labelKey),
        icon: SECTION_ICONS[section.id],
        groupId: section.hubGroupId,
        badge: sectionBadges[section.id as MyAreaSectionId],
      })),
    [accessibleSections, sectionBadges, t],
  );

  const hubGroups = useMemo(
    () =>
      MY_AREA_HUB_GROUP_ORDER.map((id) => ({
        id,
        title: getMyAreaHubGroupTitle(id, t),
      })),
    [t],
  );

  return (
    <HubPage
      header={{
        eyebrow: t('myArea.hub.eyebrow'),
        title: displayName,
        subtitle:
          self?.personalNumber != null
            ? `${t('myArea.fields.personalNumber')}: ${self.personalNumber}`
            : t('myArea.subtitle'),
        density: 'compact',
      }}
      tiles={navTiles}
      groups={hubGroups}
      fallbackGroupTitle={t('common.hub.ungrouped')}
    />
  );
}
