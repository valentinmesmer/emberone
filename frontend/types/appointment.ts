/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export enum AppointmentType {
  SERVICE = 'SERVICE',
}

export enum AudienceScope {
  ALL = 'ALL',
  SELECTED_GROUPS = 'SELECTED_GROUPS',
}

export enum AppointmentResponseMode {
  NONE = 'NONE',
  YES_NO = 'YES_NO',
  YES_NO_UNCERTAIN = 'YES_NO_UNCERTAIN',
  YES_NO_WITH_COMMENT = 'YES_NO_WITH_COMMENT',
  YES_NO_UNCERTAIN_WITH_COMMENT = 'YES_NO_UNCERTAIN_WITH_COMMENT',
}

export enum AppointmentResponseValue {
  YES = 'YES',
  NO = 'NO',
  UNCERTAIN = 'UNCERTAIN',
}

export enum SeriesEditScope {
  THIS_OCCURRENCE = 'THIS_OCCURRENCE',
  THIS_AND_FUTURE = 'THIS_AND_FUTURE',
  ENTIRE_SERIES = 'ENTIRE_SERIES',
}

export interface RecurrenceRequest {
  recurrenceRule: string;
  seriesEnd?: string | null;
  timezone?: string;
}

export interface AppointmentDto {
  id: number;
  organizationId: number;
  seriesId?: number;
  recurrenceRule?: string;
  seriesEnd?: string;
  recurrenceTimezone?: string;
  occurrenceStart: string;
  name: string;
  type: AppointmentType;
  start: string;
  end: string;
  location?: string;
  description?: string;
  responseMode: AppointmentResponseMode;
  responseDeadlineMinutesBefore: number;
  audienceScope: AudienceScope;
  audienceGroupIds?: number[];
  audienceGroups?: { id: number; name: string }[];
  isException: boolean;
  /** When true, hidden from lists and not editable; set automatically after end+4h or event promotion. */
  archived?: boolean;
  /** True when the current user has not yet opened / marked this appointment as read. */
  unread?: boolean;
}

export interface AppointmentResponseDto {
  userId: number;
  userFirstName?: string;
  userLastName?: string;
  value: AppointmentResponseValue;
  comment?: string;
  lastRespondedAt: string;
}

export interface MyAppointmentResponseDto {
  appointmentId: number;
  value: AppointmentResponseValue;
  comment?: string;
  lastRespondedAt: string;
}

export interface AppointmentParticipantEntryDto {
  userId: number;
  userFirstName?: string;
  userLastName?: string;
}

export interface AppointmentParticipationStatsDto {
  yesCount: number;
  noCount: number;
  uncertainCount: number;
  yesParticipants?: AppointmentParticipantEntryDto[];
  noParticipants?: AppointmentParticipantEntryDto[];
  uncertainParticipants?: AppointmentParticipantEntryDto[];
}

export interface CreateAppointmentRequest {
  organizationId: number;
  name: string;
  type: AppointmentType;
  start: string;
  end: string;
  location?: string;
  description?: string;
  responseMode: AppointmentResponseMode;
  responseDeadlineMinutesBefore?: number;
  audienceScope: AudienceScope;
  audienceGroupIds?: number[];
  recurrence?: RecurrenceRequest;
}

export interface UpdateAppointmentRequest {
  name?: string;
  type?: AppointmentType;
  start?: string;
  end?: string;
  location?: string;
  description?: string;
  responseMode?: AppointmentResponseMode;
  responseDeadlineMinutesBefore?: number;
  audienceScope?: AudienceScope;
  audienceGroupIds?: number[];
  recurrence?: RecurrenceRequest;
}

export interface SubmitAppointmentResponseRequest {
  value: AppointmentResponseValue;
  comment?: string;
}

export interface UpcomingAppointmentsDto {
  content: AppointmentDto[];
  totalCount: number;
  unreadCount: number;
}

export type RecurrenceFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY';

export type ICalWeekday = 'MO' | 'TU' | 'WE' | 'TH' | 'FR' | 'SA' | 'SU';

export type MonthlyRecurrenceType = 'DAY_OF_MONTH' | 'NTH_WEEKDAY';

export interface RecurrenceFormState {
  enabled: boolean;
  frequency: RecurrenceFrequency;
  interval: number;
  seriesEnd: string | null;
  weekDays: ICalWeekday[];
  monthlyType: MonthlyRecurrenceType;
  monthlyWeekdays: ICalWeekday[];
  monthlyPositions: number[];
}
