/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { DatePicker } from '@/components/ui/date-picker';
import { DateTimePicker } from '@/components/ui/date-time-picker';
import { HoverHint } from '@/components/ui/hover-hint';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { SwitchField } from '@/components/ui/switch';
import { Textarea } from '@/components/ui/textarea';
import { useActionHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';
import { isEndStrictlyBeforeStart, shiftEndWithStartDateChange } from '@/lib/utils/date';
import { getGroupDisplayName } from '@/lib/utils/membership';
import {
  anchorWeekday,
  ensureAnchorWeekday,
  ICAL_WEEKDAYS,
  MONTHLY_POSITIONS,
  seriesEndFromDate,
  toggleWeekdaySelection,
} from '@/lib/utils/recurrence';
import type { ResponseDeadlineUnit } from '@/lib/utils/responseDeadline';
import {
  AppointmentResponseMode,
  type AppointmentType,
  AudienceScope,
  type RecurrenceFormState,
} from '@/types/appointment';
import type { OrganizationGroupDto } from '@/types/organizationGroup';
import {
  type AppointmentFormValidityPolicy,
  isAppointmentEndInFuture,
  isAppointmentStartValidForEdit,
  isAudienceRestricted,
  toggleAudienceGroupId,
} from './appointmentFormUtils';

export interface AppointmentFormState {
  name: string;
  type: AppointmentType;
  start: string;
  end: string;
  /** Single API `location` string (varchar). */
  location: string;
  description: string;
  responseMode: AppointmentResponseMode;
  responseDeadlineBeforeAmount: number;
  responseDeadlineBeforeUnit: ResponseDeadlineUnit;
  audienceScope: AudienceScope;
  audienceGroupIds: number[];
  recurrence: RecurrenceFormState;
}

interface AppointmentFormFieldsProps {
  form: AppointmentFormState;
  onChange: (form: AppointmentFormState) => void;
  organizationGroups: OrganizationGroupDto[];
  showRecurrence?: boolean;
  /** When set, start must stay in the future (create) or must not move a future occurrence into the past (edit). */
  futureStartPolicy?: AppointmentFormValidityPolicy;
}

function SelectablePill({
  label,
  selected,
  locked,
  onChange,
}: {
  label: string;
  selected: boolean;
  locked?: boolean;
  onChange: (selected: boolean) => void;
}) {
  const hint = useActionHoverTitle({ disabled: locked, isLoading: false });
  return (
    <HoverHint hint={hint} capturePointer={!!locked} shape="rounded-lg">
      <button
        type="button"
        aria-pressed={selected}
        aria-disabled={locked}
        disabled={locked}
        title={undefined}
        onClick={() => onChange(!selected)}
        className={cn(
          'rounded-lg border px-3 py-1.5 text-sm font-medium transition-colors',
          selected
            ? 'border-[var(--fire-red)] bg-[var(--fire-red)] text-white shadow-sm'
            : 'border-[var(--border-color)] bg-[var(--card-bg)] text-[var(--text-primary)] hover:border-[var(--fire-red)]/50',
          locked &&
            'cursor-default ring-2 ring-[var(--fire-red)]/40 ring-offset-1 ring-offset-[var(--card-bg)]',
        )}
      >
        {label}
      </button>
    </HoverHint>
  );
}

function syncRecurrenceAnchors(
  recurrence: RecurrenceFormState,
  seriesStart: string,
  frequency?: RecurrenceFormState['frequency'],
  previousSeriesStart?: string,
): RecurrenceFormState {
  const next = { ...recurrence, ...(frequency != null ? { frequency } : {}) };
  const prevAnchor =
    previousSeriesStart && previousSeriesStart.length > 0
      ? anchorWeekday(previousSeriesStart)
      : null;
  const newAnchor = anchorWeekday(seriesStart);

  if (next.frequency === 'WEEKLY') {
    if (prevAnchor != null && prevAnchor !== newAnchor) {
      next.weekDays = ensureAnchorWeekday(
        next.weekDays.filter((d) => d !== prevAnchor),
        seriesStart,
      );
    } else {
      next.weekDays = ensureAnchorWeekday(next.weekDays, seriesStart);
    }
  }
  if (next.frequency === 'MONTHLY' && next.monthlyType === 'NTH_WEEKDAY') {
    if (prevAnchor != null && prevAnchor !== newAnchor) {
      next.monthlyWeekdays = ensureAnchorWeekday(
        next.monthlyWeekdays.filter((d) => d !== prevAnchor),
        seriesStart,
      );
    } else {
      next.monthlyWeekdays = ensureAnchorWeekday(next.monthlyWeekdays, seriesStart);
    }
  }
  return next;
}

function toggleMonthlyPosition(positions: number[], position: number): number[] {
  return positions.includes(position)
    ? positions.filter((p) => p !== position)
    : [...positions, position].sort((a, b) => a - b);
}

function FormSubsection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="rounded-lg border border-[var(--border-color)]">
      <h3 className="border-b border-[var(--border-color)] px-3 py-2.5 text-sm font-medium text-[var(--text-primary)]">
        {title}
      </h3>
      <div className="space-y-3 px-3 pb-3 pt-2">{children}</div>
    </div>
  );
}

export function AppointmentFormFields({
  form,
  onChange,
  organizationGroups,
  showRecurrence = true,
  futureStartPolicy,
}: AppointmentFormFieldsProps) {
  const t = useTranslation();

  const update = (partial: Partial<AppointmentFormState>) => onChange({ ...form, ...partial });

  const audienceRestricted = isAudienceRestricted(form.audienceScope);
  const showResponseDeadline = form.responseMode !== AppointmentResponseMode.NONE;

  const startEndOrderError =
    form.start && form.end && isEndStrictlyBeforeStart(form.start, form.end)
      ? t('common.validation.endNotBeforeStart')
      : undefined;

  const scheduleInPastError =
    futureStartPolicy && form.end
      ? futureStartPolicy.mode === 'create'
        ? isAppointmentEndInFuture(form.end)
          ? undefined
          : t('appointments.validation.endMustBeFuture')
        : !form.start || isAppointmentStartValidForEdit(form.start, futureStartPolicy.originalStart)
          ? undefined
          : t('appointments.validation.startMustRemainFuture')
      : undefined;

  return (
    <div className="space-y-4">
      <Input
        label={t('appointments.fields.name')}
        value={form.name}
        onChange={(e) => update({ name: e.target.value })}
        required
        className="py-2.5 sm:py-3"
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 [&>*]:min-w-0">
        <DateTimePicker
          label={t('appointments.fields.start')}
          name="start"
          value={form.start}
          onChange={(e) => {
            const start = e.target.value;
            const prevStart = form.start;
            update({
              start,
              ...(form.end ? { end: shiftEndWithStartDateChange(prevStart, start, form.end) } : {}),
              ...(form.recurrence.enabled
                ? {
                    recurrence: syncRecurrenceAnchors(form.recurrence, start, undefined, prevStart),
                  }
                : {}),
            });
          }}
          required
          allowEmpty
          error={scheduleInPastError}
        />
        <DateTimePicker
          label={t('appointments.fields.end')}
          name="end"
          value={form.end}
          onChange={(e) => update({ end: e.target.value })}
          required
          allowEmpty
          error={startEndOrderError ?? scheduleInPastError}
        />
      </div>

      <Input
        label={t('appointments.fields.location')}
        name="location"
        value={form.location}
        onChange={(e) => update({ location: e.target.value })}
        placeholder={t('appointments.fields.locationPlaceholder')}
        className="py-2.5 sm:py-3"
      />
      <Textarea
        label={t('appointments.fields.description')}
        value={form.description}
        onChange={(e) => update({ description: e.target.value })}
        rows={2}
        className="min-h-[4.5rem] resize-y"
      />

      <Select
        label={t('appointments.fields.responseMode')}
        value={form.responseMode}
        onChange={(e) => {
          const responseMode = e.target.value as AppointmentResponseMode;
          update({
            responseMode,
            responseDeadlineBeforeAmount:
              responseMode === AppointmentResponseMode.NONE ? 0 : form.responseDeadlineBeforeAmount,
          });
        }}
        required
        options={[
          { value: AppointmentResponseMode.NONE, label: t('appointments.responseModes.NONE') },
          { value: AppointmentResponseMode.YES_NO, label: t('appointments.responseModes.YES_NO') },
          {
            value: AppointmentResponseMode.YES_NO_UNCERTAIN,
            label: t('appointments.responseModes.YES_NO_UNCERTAIN'),
          },
          {
            value: AppointmentResponseMode.YES_NO_WITH_COMMENT,
            label: t('appointments.responseModes.YES_NO_WITH_COMMENT'),
          },
          {
            value: AppointmentResponseMode.YES_NO_UNCERTAIN_WITH_COMMENT,
            label: t('appointments.responseModes.YES_NO_UNCERTAIN_WITH_COMMENT'),
          },
        ]}
      />

      <FormSubsection title={t('appointments.form.sectionAudience')}>
        <SwitchField
          label={t('appointments.fields.audienceRestricted')}
          checked={audienceRestricted}
          onCheckedChange={(checked) =>
            update({
              audienceScope: checked ? AudienceScope.SELECTED_GROUPS : AudienceScope.ALL,
              audienceGroupIds: checked ? form.audienceGroupIds : [],
            })
          }
        />
        {audienceRestricted &&
          (organizationGroups.length === 0 ? (
            <p className="text-sm text-[var(--text-secondary)]">
              {t('appointments.audience.noGroups')}
            </p>
          ) : (
            <fieldset
              className="flex flex-wrap gap-2 pt-1"
              aria-label={t('appointments.fields.audienceGroups')}
            >
              {organizationGroups.map((group) => (
                <SelectablePill
                  key={group.id}
                  label={getGroupDisplayName(group)}
                  selected={form.audienceGroupIds.includes(group.id)}
                  onChange={() =>
                    update({
                      audienceGroupIds: toggleAudienceGroupId(form.audienceGroupIds, group.id),
                    })
                  }
                />
              ))}
            </fieldset>
          ))}
      </FormSubsection>

      {showResponseDeadline && (
        <FormSubsection title={t('appointments.form.sectionResponseDeadline')}>
          <p className="text-sm text-[var(--text-secondary)]">
            {t('appointments.fields.responseDeadlineHint')}
          </p>
          <p className="text-sm font-medium text-[var(--text-primary)]">
            {t('appointments.fields.responseDeadlineBefore')}
          </p>
          <div className="grid grid-cols-2 gap-2">
            <Input
              label={t('appointments.fields.responseDeadlineAmount')}
              type="number"
              min={0}
              value={String(form.responseDeadlineBeforeAmount)}
              onChange={(e) =>
                update({
                  responseDeadlineBeforeAmount: Math.max(0, parseInt(e.target.value, 10) || 0),
                })
              }
              required
              className="py-2.5"
            />
            <Select
              label={t('appointments.fields.responseDeadlineUnit')}
              value={form.responseDeadlineBeforeUnit}
              onChange={(e) =>
                update({
                  responseDeadlineBeforeUnit: e.target.value as ResponseDeadlineUnit,
                })
              }
              required
              disabled={form.responseDeadlineBeforeAmount === 0}
              options={[
                {
                  value: 'MINUTES',
                  label: t('appointments.fields.responseDeadlineUnitMinutes'),
                },
                {
                  value: 'HOURS',
                  label: t('appointments.fields.responseDeadlineUnitHours'),
                },
                {
                  value: 'DAYS',
                  label: t('appointments.fields.responseDeadlineUnitDays'),
                },
              ]}
            />
          </div>
        </FormSubsection>
      )}

      {showRecurrence && (
        <FormSubsection title={t('appointments.form.sectionSeries')}>
          <SwitchField
            label={t('appointments.recurrence.enable')}
            checked={form.recurrence.enabled}
            onCheckedChange={(checked) =>
              update({
                recurrence: checked
                  ? syncRecurrenceAnchors(
                      {
                        ...form.recurrence,
                        enabled: true,
                        interval: Math.max(form.recurrence.interval, 1),
                        weekDays: [anchorWeekday(form.start)],
                        monthlyWeekdays: [anchorWeekday(form.start)],
                      },
                      form.start,
                    )
                  : { ...form.recurrence, enabled: false },
              })
            }
          />
          {form.recurrence.enabled && (
            <div className="space-y-3">
              <div className="grid grid-cols-2 gap-2">
                <Select
                  label={t('appointments.recurrence.frequency')}
                  value={form.recurrence.frequency}
                  onChange={(e) =>
                    update({
                      recurrence: syncRecurrenceAnchors(
                        form.recurrence,
                        form.start,
                        e.target.value as RecurrenceFormState['frequency'],
                      ),
                    })
                  }
                  required
                  options={[
                    { value: 'DAILY', label: t('appointments.recurrence.daily') },
                    { value: 'WEEKLY', label: t('appointments.recurrence.weekly') },
                    { value: 'MONTHLY', label: t('appointments.recurrence.monthly') },
                  ]}
                />
                <Input
                  label={t('appointments.recurrence.interval')}
                  type="number"
                  min={1}
                  value={form.recurrence.interval < 1 ? '' : String(form.recurrence.interval)}
                  onChange={(e) => {
                    const raw = e.target.value;
                    if (raw === '') {
                      update({
                        recurrence: { ...form.recurrence, interval: 0 },
                      });
                      return;
                    }
                    const n = parseInt(raw, 10);
                    update({
                      recurrence: {
                        ...form.recurrence,
                        interval: Number.isFinite(n) && n >= 1 ? n : 0,
                      },
                    });
                  }}
                  onBlur={() => {
                    if (form.recurrence.interval < 1) {
                      update({
                        recurrence: { ...form.recurrence, interval: 1 },
                      });
                    }
                  }}
                  required
                  className="py-2.5"
                />
              </div>
              {form.recurrence.frequency === 'WEEKLY' && (
                <div className="space-y-2">
                  <p className="text-sm font-medium text-[var(--text-primary)]">
                    {t('appointments.recurrence.weekdays')}
                  </p>
                  <div className="flex flex-wrap gap-2">
                    {ICAL_WEEKDAYS.map((day) => {
                      const isAnchor = day === anchorWeekday(form.start);
                      return (
                        <SelectablePill
                          key={day}
                          label={t(`appointments.recurrence.weekday.${day}`)}
                          selected={form.recurrence.weekDays.includes(day)}
                          locked={isAnchor}
                          onChange={() =>
                            update({
                              recurrence: {
                                ...form.recurrence,
                                weekDays: toggleWeekdaySelection(
                                  form.recurrence.weekDays,
                                  day,
                                  form.start,
                                ),
                              },
                            })
                          }
                        />
                      );
                    })}
                  </div>
                </div>
              )}

              {form.recurrence.frequency === 'MONTHLY' && (
                <div className="space-y-3">
                  <Select
                    label={t('appointments.recurrence.monthlyType')}
                    value={form.recurrence.monthlyType}
                    onChange={(e) =>
                      update({
                        recurrence: syncRecurrenceAnchors(
                          {
                            ...form.recurrence,
                            monthlyType: e.target.value as RecurrenceFormState['monthlyType'],
                          },
                          form.start,
                        ),
                      })
                    }
                    required
                    options={[
                      {
                        value: 'DAY_OF_MONTH',
                        label: t('appointments.recurrence.monthlyDayOfMonth'),
                      },
                      {
                        value: 'NTH_WEEKDAY',
                        label: t('appointments.recurrence.monthlyNthWeekday'),
                      },
                    ]}
                  />
                  {form.recurrence.monthlyType === 'NTH_WEEKDAY' && (
                    <div className="space-y-3">
                      <div className="space-y-2">
                        <p className="text-sm font-medium text-[var(--text-primary)]">
                          {t('appointments.recurrence.monthlyWeekday')}
                        </p>
                        <div className="flex flex-wrap gap-2">
                          {ICAL_WEEKDAYS.map((day) => {
                            const isAnchor = day === anchorWeekday(form.start);
                            return (
                              <SelectablePill
                                key={day}
                                label={t(`appointments.recurrence.weekday.${day}`)}
                                selected={form.recurrence.monthlyWeekdays.includes(day)}
                                locked={isAnchor}
                                onChange={() =>
                                  update({
                                    recurrence: {
                                      ...form.recurrence,
                                      monthlyWeekdays: toggleWeekdaySelection(
                                        form.recurrence.monthlyWeekdays,
                                        day,
                                        form.start,
                                      ),
                                    },
                                  })
                                }
                              />
                            );
                          })}
                        </div>
                      </div>
                      <div className="space-y-2">
                        <p className="text-sm font-medium text-[var(--text-primary)]">
                          {t('appointments.recurrence.monthlyPositions')}
                        </p>
                        <div className="flex flex-wrap gap-2">
                          {MONTHLY_POSITIONS.map((position) => (
                            <SelectablePill
                              key={position}
                              label={t(`appointments.recurrence.position.${position}`)}
                              selected={form.recurrence.monthlyPositions.includes(position)}
                              onChange={() =>
                                update({
                                  recurrence: {
                                    ...form.recurrence,
                                    monthlyPositions: toggleMonthlyPosition(
                                      form.recurrence.monthlyPositions,
                                      position,
                                    ),
                                  },
                                })
                              }
                            />
                          ))}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}

              <SwitchField
                label={t('appointments.recurrence.ends')}
                checked={form.recurrence.seriesEnd != null}
                onCheckedChange={(checked) =>
                  update({
                    recurrence: {
                      ...form.recurrence,
                      seriesEnd: checked ? seriesEndFromDate(form.end.slice(0, 10)) : null,
                    },
                  })
                }
              />
              {form.recurrence.seriesEnd != null && (
                <DatePicker
                  label={t('appointments.recurrence.endsAt')}
                  value={form.recurrence.seriesEnd.slice(0, 10)}
                  onChange={(e) =>
                    update({
                      recurrence: {
                        ...form.recurrence,
                        seriesEnd: e.target.value
                          ? seriesEndFromDate(e.target.value)
                          : form.recurrence.seriesEnd,
                      },
                    })
                  }
                  required
                />
              )}
            </div>
          )}
        </FormSubsection>
      )}
    </div>
  );
}
