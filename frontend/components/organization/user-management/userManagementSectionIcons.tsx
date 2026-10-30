/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import {
  Award,
  CalendarOff,
  Car,
  ClipboardList,
  Clock,
  FolderOpen,
  GraduationCap,
  IdCard,
  Inbox,
  ListChecks,
  Medal,
  Phone,
  Star,
  Users,
} from 'lucide-react';
import type { ReactNode } from 'react';
import type { UserManagementSectionId } from '@/lib/navigation/userManagementSections';

const iconClassName = 'h-4 w-4';

export const USER_MANAGEMENT_SECTION_ICONS: Record<UserManagementSectionId, ReactNode> = {
  'master-data': <IdCard className={iconClassName} aria-hidden />,
  memberships: <Users className={iconClassName} aria-hidden />,
  'emergency-contacts': <Phone className={iconClassName} aria-hidden />,
  documents: <FolderOpen className={iconClassName} aria-hidden />,
  certifications: <Award className={iconClassName} aria-hidden />,
  qualifications: <Medal className={iconClassName} aria-hidden />,
  'training-courses': <GraduationCap className={iconClassName} aria-hidden />,
  'course-registrations': <ClipboardList className={iconClassName} aria-hidden />,
  'driving-licenses': <Car className={iconClassName} aria-hidden />,
  rank: <Star className={iconClassName} aria-hidden />,
  onboarding: <ListChecks className={iconClassName} aria-hidden />,
  workflows: <Inbox className={iconClassName} aria-hidden />,
  absences: <CalendarOff className={iconClassName} aria-hidden />,
  'time-entries': <Clock className={iconClassName} aria-hidden />,
};
