/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { BadgeTone } from '@/components/ui/badge';
import { getOrganizationWallClockNow } from '@/lib/utils/date';
import type {
  CourseAudienceEntryDto,
  CourseAudienceEntryRequest,
  CourseAudienceKind,
  CourseOfferingDto,
  CourseOfferingStatus,
  CoursePrerequisiteKind,
  CourseRegistrationStatus,
  CourseSessionType,
} from '@/types/courseOffering';

export function getOrganizationCourseOfferingsPath(organizationId: number): string {
  return `/organizations/${organizationId}/course-offerings`;
}

export function getOrganizationCourseOfferingDetailPath(
  organizationId: number,
  offeringId: number,
): string {
  return `${getOrganizationCourseOfferingsPath(organizationId)}/${offeringId}`;
}

export function getOrganizationCourseSessionDetailPath(
  organizationId: number,
  offeringId: number,
  sessionId: number,
): string {
  return `${getOrganizationCourseOfferingDetailPath(organizationId, offeringId)}/sessions/${sessionId}`;
}

export function getOrganizationCourseAudienceDetailPath(
  organizationId: number,
  offeringId: number,
  audienceId: number,
): string {
  return `${getOrganizationCourseOfferingDetailPath(organizationId, offeringId)}/audiences/${audienceId}`;
}

export function getOrganizationCourseRegistrationsPath(
  organizationId: number,
  offeringId: number,
): string {
  return `${getOrganizationCourseOfferingDetailPath(organizationId, offeringId)}/registrations`;
}

export function getOrganizationCourseRegistrationDetailPath(
  organizationId: number,
  offeringId: number,
  registrationId: number,
): string {
  return `${getOrganizationCourseRegistrationsPath(organizationId, offeringId)}/${registrationId}`;
}

export function getOrganizationCourseCompletionPath(
  organizationId: number,
  offeringId: number,
): string {
  return `${getOrganizationCourseOfferingDetailPath(organizationId, offeringId)}/completion`;
}

export function getOrganizationCourseCompletionParticipantPath(
  organizationId: number,
  offeringId: number,
  registrationId: number,
): string {
  return `${getOrganizationCourseCompletionPath(organizationId, offeringId)}/${registrationId}`;
}

export type CoursePrerequisiteKindSlug = 'training-courses' | 'certifications' | 'driving-licenses';

const PREREQUISITE_KIND_BY_SLUG: Record<CoursePrerequisiteKindSlug, CoursePrerequisiteKind> = {
  'training-courses': 'TRAINING_COURSE',
  certifications: 'CERTIFICATION',
  'driving-licenses': 'DRIVING_LICENSE',
};

const PREREQUISITE_SLUG_BY_KIND: Record<CoursePrerequisiteKind, CoursePrerequisiteKindSlug> = {
  TRAINING_COURSE: 'training-courses',
  CERTIFICATION: 'certifications',
  DRIVING_LICENSE: 'driving-licenses',
};

export function coursePrerequisiteKindSlug(
  kind: CoursePrerequisiteKind,
): CoursePrerequisiteKindSlug {
  return PREREQUISITE_SLUG_BY_KIND[kind];
}

export function coursePrerequisiteKindFromSlug(slug: string): CoursePrerequisiteKind | null {
  if (slug in PREREQUISITE_KIND_BY_SLUG) {
    return PREREQUISITE_KIND_BY_SLUG[slug as CoursePrerequisiteKindSlug];
  }
  return null;
}

export function getOrganizationCoursePrerequisiteDetailPath(
  organizationId: number,
  offeringId: number,
  kind: CoursePrerequisiteKind,
): string {
  return `${getOrganizationCourseOfferingDetailPath(organizationId, offeringId)}/prerequisites/${coursePrerequisiteKindSlug(kind)}`;
}

export function isOrganizationCourseOfferingsPathActive(
  organizationId: number,
  pathname: string,
): boolean {
  const base = getOrganizationCourseOfferingsPath(organizationId);
  return pathname === base || pathname.startsWith(`${base}/`);
}

/** Registrations of all offerings of the organization (`…/course-registrations`). */
export function getOrganizationCourseRegistrationListPath(organizationId: number): string {
  return `/organizations/${organizationId}/course-registrations`;
}

export function isOrganizationCourseRegistrationListPathActive(
  organizationId: number,
  pathname: string,
): boolean {
  const base = getOrganizationCourseRegistrationListPath(organizationId);
  return pathname === base || pathname.startsWith(`${base}/`);
}

export function courseOfferingStatusLabelKey(status: CourseOfferingStatus): string {
  return `courseOfferings.status.${status}`;
}

export function courseSessionTypeLabelKey(type: CourseSessionType): string {
  return `courseOfferings.sessions.types.${type}`;
}

export function courseAudienceKindLabelKey(kind: CourseAudienceKind): string {
  return `courseOfferings.audience.kinds.${kind}`;
}

const RESTRICTION_KINDS: CourseAudienceKind[] = ['ORG', 'ORG_SUBTREE', 'ORG_TYPE'];

export function isCourseAudienceRestrictionKind(kind: CourseAudienceKind): boolean {
  return RESTRICTION_KINDS.includes(kind);
}

/** Private offering: exactly one {@code OWN_ORG} entry. */
export function offeringIsPrivate(
  audiences: readonly Pick<CourseAudienceEntryDto, 'kind' | 'refId' | 'quota'>[],
): boolean {
  if (audiences.length !== 1) {
    return false;
  }
  const only = audiences[0];
  return only.kind === 'OWN_ORG' && only.refId == null && only.quota == null;
}

export function courseAudienceEntryToRequest(
  entry: Pick<CourseAudienceEntryDto, 'kind' | 'refId' | 'quota'>,
): CourseAudienceEntryRequest {
  return { kind: entry.kind, refId: entry.refId, quota: entry.quota };
}

/** Requests for the given groups; quotas only add up for the full set, so they are dropped on demand. */
export function courseAudienceEntriesToRequests(
  entries: readonly CourseAudienceEntryDto[],
  keepQuotas: boolean,
): CourseAudienceEntryRequest[] {
  return entries.map((entry) => ({
    ...courseAudienceEntryToRequest(entry),
    quota: keepQuotas ? entry.quota : null,
  }));
}

export function offeringPublicRestrictions(
  audiences: readonly CourseAudienceEntryDto[],
): CourseAudienceEntryDto[] {
  if (offeringIsPrivate(audiences)) {
    return [];
  }
  return audiences.filter((entry) => isCourseAudienceRestrictionKind(entry.kind));
}

export function canEditCourseOffering(status: CourseOfferingStatus): boolean {
  return status === 'TEMPLATE' || status === 'DRAFT' || status === 'PUBLISHED';
}

export function canDeleteCourseOffering(status: CourseOfferingStatus): boolean {
  return status === 'TEMPLATE' || status === 'DRAFT';
}

export function canPublishCourseOffering(status: CourseOfferingStatus): boolean {
  return status === 'DRAFT';
}

/**
 * Why a draft cannot be published yet - mirrors backend {@code validatePublishable}.
 * {@code null} means the offering is ready to publish.
 */
export type CourseOfferingPublishBlockReason =
  | 'registrationPeriod'
  | 'sessions'
  | 'registrationBeforeFirstSession'
  | 'examSession'
  | 'quotasIncomplete'
  | 'quotasSum'
  | 'ownOrgQuota';

export function courseOfferingPublishBlockReason(
  offering: Pick<
    CourseOfferingDto,
    | 'registrationStart'
    | 'registrationEnd'
    | 'sessions'
    | 'hasExam'
    | 'audiences'
    | 'maxParticipants'
  >,
): CourseOfferingPublishBlockReason | null {
  const audiences = offering.audiences;
  const withQuota = audiences.filter((entry) => entry.quota != null);
  if (withQuota.length > 0) {
    if (withQuota.length !== audiences.length) {
      return 'quotasIncomplete';
    }
    if (audiences.some((entry) => entry.kind === 'OWN_ORG')) {
      return 'ownOrgQuota';
    }
    const sum = withQuota.reduce((total, entry) => total + (entry.quota ?? 0), 0);
    if (sum > offering.maxParticipants) {
      return 'quotasSum';
    }
  }

  if (offering.registrationStart == null || offering.registrationEnd == null) {
    return 'registrationPeriod';
  }
  if (offering.sessions.length === 0) {
    return 'sessions';
  }

  const firstSessionStart = offering.sessions.map((session) => session.start).sort()[0];
  if (firstSessionStart != null && offering.registrationEnd > firstSessionStart) {
    return 'registrationBeforeFirstSession';
  }

  if (offering.hasExam && !offering.sessions.some((session) => session.type === 'EXAM')) {
    return 'examSession';
  }

  return null;
}

export function courseOfferingPublishBlockReasonLabelKey(
  reason: CourseOfferingPublishBlockReason,
): string {
  return `courseOfferings.actions.publishBlocked.${reason}`;
}

export function canCancelCourseOffering(status: CourseOfferingStatus): boolean {
  return status === 'PUBLISHED';
}

/** Calendar day (`yyyy-MM-dd`) of an organization wall-clock datetime string. */
function organizationWallClockDate(value: string | null | undefined): string | null {
  if (!value) {
    return null;
  }
  const day = value.slice(0, 10);
  return /^\d{4}-\d{2}-\d{2}$/.test(day) ? day : null;
}

function organizationWallClockToday(): string {
  return getOrganizationWallClockNow().slice(0, 10);
}

/** First session calendar day reached (org wall-clock) - required for results and completion. */
export function hasCourseOfferingStarted(
  offering: Pick<CourseOfferingDto, 'firstSessionStart'>,
): boolean {
  const firstDay = organizationWallClockDate(offering.firstSessionStart);
  if (firstDay == null) {
    return false;
  }
  return organizationWallClockToday() >= firstDay;
}

export function canSetCourseOfferingResults(
  offering: Pick<CourseOfferingDto, 'status' | 'firstSessionStart'>,
): boolean {
  return offering.status === 'PUBLISHED' && hasCourseOfferingStarted(offering);
}

export type CourseOfferingCompleteBlockReason = 'notStarted' | 'undecided';

export function courseOfferingCompleteBlockReason(
  offering: Pick<CourseOfferingDto, 'status' | 'firstSessionStart'>,
  undecidedCount: number,
  completionLoaded: boolean,
): CourseOfferingCompleteBlockReason | null {
  if (offering.status !== 'PUBLISHED') {
    return null;
  }
  if (!hasCourseOfferingStarted(offering)) {
    return 'notStarted';
  }
  if (!completionLoaded || undecidedCount > 0) {
    return 'undecided';
  }
  return null;
}

export function courseOfferingCompleteBlockReasonLabelKey(
  reason: CourseOfferingCompleteBlockReason,
): string {
  switch (reason) {
    case 'notStarted':
      return 'courseOfferings.completion.completeBlockedNotStarted';
    case 'undecided':
      return 'courseOfferings.completion.completeBlockedUndecided';
  }
}

export function isCourseOfferingTemplate(status: CourseOfferingStatus): boolean {
  return status === 'TEMPLATE';
}

export function courseRegistrationStatusLabelKey(status: CourseRegistrationStatus): string {
  return `courseOfferings.registrations.status.${status}`;
}

export function courseRegistrationStatusBadgeTone(status: CourseRegistrationStatus): BadgeTone {
  switch (status) {
    case 'PARTICIPANT':
      return 'success';
    case 'PENDING':
      return 'accent';
    case 'WAITLIST':
      return 'warning';
    case 'REJECTED':
    case 'CANCELLED':
      return 'neutral';
  }
}
