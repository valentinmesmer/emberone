/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { CalendarDateTime, parseZonedDateTime, type ZonedDateTime } from '@internationalized/date';
import { ChevronDown, ChevronLeft, ChevronRight } from 'lucide-react';
import React from 'react';
import {
  Modal as AriaModal,
  ModalOverlay as AriaModalOverlay,
  Button,
  Calendar,
  CalendarCell,
  CalendarGrid,
  CalendarGridBody,
  CalendarGridHeader,
  CalendarHeaderCell,
  DateField,
  DateInput,
  type DateValue,
  Dialog,
  DialogTrigger,
  FieldError,
  Group,
  Heading,
  Label,
  TimeField,
  type ValidationResult,
} from 'react-aria-components';
import { DateFieldSegment } from '@/components/ui/date-field-segment';
import { HoverHint } from '@/components/ui/hover-hint';
import { useFieldHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

const MOBILE_VIEWPORT_QUERY = '(max-width: 767px)';

const subscribeToMobileViewport = (callback: () => void) => {
  const mediaQuery = window.matchMedia(MOBILE_VIEWPORT_QUERY);
  mediaQuery.addEventListener('change', callback);
  return () => mediaQuery.removeEventListener('change', callback);
};

const getMobileViewportSnapshot = () => window.matchMedia(MOBILE_VIEWPORT_QUERY).matches;
const getServerMobileViewportSnapshot = () => false;

/** Soft keyboards stay open while a date segment is focused; dismiss before showing the calendar. */
const dismissSoftKeyboard = () => {
  const active = document.activeElement;
  if (active instanceof HTMLElement) active.blur();
};

export interface DateTimePickerProps
  extends Omit<
    React.InputHTMLAttributes<HTMLInputElement>,
    'type' | 'value' | 'onChange' | 'defaultValue'
  > {
  label?: string;
  error?: string;
  hint?: string;
  value?: string;
  defaultValue?: DateValue | null;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  errorMessage?: string | ((validation: ValidationResult) => string);
  granularity?: 'minute' | 'second' | 'hour';
  hourCycle?: 12 | 24;
  hideTimeZone?: boolean;
  shouldForceLeadingZeros?: boolean;
  hideTime?: boolean;
  /** When hideTime is true, use midnight for start dates and end-of-day for end dates. */
  allDayTime?: 'start' | 'end';
  /** Tighter layout for dense forms (e.g. side-by-side start/end on mobile). */
  compact?: boolean;
  /** Allow clearing the value to an empty string (save still blocked by form validation). */
  allowEmpty?: boolean;
  /** Inclusive maximum (`yyyy-MM-ddTHH:mm:ss`). Days after this cannot be selected. */
  max?: string;
}

const stringToDateTime = (
  value: string | undefined,
  defaultValue?: DateValue | null,
): CalendarDateTime | ZonedDateTime | null => {
  if (defaultValue !== undefined) return defaultValue as CalendarDateTime | ZonedDateTime | null;
  if (!value) return null;
  try {
    if (value.includes('[') && value.includes(']')) return parseZonedDateTime(value);
    if (value.match(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/)) {
      const [datePart, timePart] = value.split('T');
      const [year, month, day] = datePart.split('-').map(Number);
      const [hours, minutes, seconds = 0] = timePart.split(':').map(Number);
      return new CalendarDateTime(year, month, day, hours, minutes, seconds);
    }
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) {
      return new CalendarDateTime(
        date.getFullYear(),
        date.getMonth() + 1,
        date.getDate(),
        date.getHours(),
        date.getMinutes(),
        date.getSeconds(),
      );
    }
  } catch {}
  return null;
};

const dateTimeToString = (dateTime: DateValue | null): string => {
  if (!dateTime) return '';
  const y = String(dateTime.year).padStart(4, '0');
  const m = String(dateTime.month).padStart(2, '0');
  const d = String(dateTime.day).padStart(2, '0');

  // Check if dateTime has time properties (CalendarDateTime or ZonedDateTime)
  if ('hour' in dateTime && 'minute' in dateTime) {
    const h = String(dateTime.hour).padStart(2, '0');
    const min = String(dateTime.minute).padStart(2, '0');
    const s = 'second' in dateTime ? String(dateTime.second).padStart(2, '0') : '00';
    return `${y}-${m}-${d}T${h}:${min}:${s}`;
  }

  return `${y}-${m}-${d}T00:00:00`;
};

const fieldGroupClass = (compact: boolean, isInvalid: boolean, disabled?: boolean) =>
  cn(
    'flex w-full flex-nowrap items-center bg-[var(--card-bg)] text-[var(--text-primary)]',
    'border border-[var(--border-color)] transition-colors duration-200',
    'focus-within:border-[var(--fire-red)] focus-within:outline-none focus-within:ring-2 focus-within:ring-[var(--focus-ring)]',
    compact ? 'h-[34px] rounded-lg px-1.5' : 'rounded-xl px-4 py-3',
    disabled && 'opacity-50 cursor-not-allowed',
    isInvalid && 'border-red-500 focus-within:ring-red-500/25',
  );

const dateInputRowClass = (compact: boolean) =>
  cn(
    'flex min-h-0 min-w-0 flex-nowrap items-center gap-0 whitespace-nowrap',
    compact ? 'text-xs' : 'text-sm',
  );

export function DateTimePicker({
  label,
  error,
  hint,
  value = '',
  defaultValue,
  onChange,
  required,
  disabled,
  errorMessage,
  granularity = 'minute',
  hourCycle = 24,
  shouldForceLeadingZeros = true,
  hideTime = false,
  allDayTime = 'start',
  compact = false,
  allowEmpty = false,
  max,
  title,
  id,
  name,
  ..._rest
}: DateTimePickerProps) {
  void _rest;
  const t = useTranslation();
  const isMobileViewport = React.useSyncExternalStore(
    subscribeToMobileViewport,
    getMobileViewportSnapshot,
    getServerMobileViewportSnapshot,
  );
  const inputId = id || name;
  const fieldHoverTitle = useFieldHoverTitle(disabled, title);
  const dateTimeValue = stringToDateTime(value, defaultValue);
  const maxDateTime = stringToDateTime(max);
  const isInvalid = !!error;
  const fieldRequired = required && !allowEmpty;

  const openCalendarBtnLabel = label
    ? `${t('common.a11y.openCalendar')}: ${label}`
    : t('common.a11y.openCalendar');
  const calendarOpenHoverHint = fieldHoverTitle;
  const dateFieldAria = (base?: string) =>
    base ? `${base}, ${t('common.a11y.date')}` : t('common.a11y.date');
  const timeFieldAria = (base?: string) =>
    base ? `${base}, ${t('common.a11y.time')}` : t('common.a11y.time');

  // Split date and time - extract date and time from CalendarDateTime
  const dateValue =
    dateTimeValue && 'year' in dateTimeValue && 'month' in dateTimeValue && 'day' in dateTimeValue
      ? (dateTimeValue as CalendarDateTime)
      : null;

  // For TimeField, we need to pass the full CalendarDateTime, not just the time part
  const timeValue = dateValue;

  const handleDateChange = (date: DateValue | null) => {
    if (!date || !('year' in date && 'month' in date && 'day' in date)) {
      if (allowEmpty && onChange) {
        onChange({
          target: { value: '', name: name ?? '' },
        } as React.ChangeEvent<HTMLInputElement>);
      }
      return;
    }

    const newDateTime = hideTime
      ? allDayTime === 'end'
        ? new CalendarDateTime(date.year, date.month, date.day, 23, 59, 59)
        : new CalendarDateTime(date.year, date.month, date.day, 0, 0, 0)
      : timeValue
        ? new CalendarDateTime(
            date.year,
            date.month,
            date.day,
            timeValue.hour,
            timeValue.minute,
            timeValue.second || 0,
          )
        : new CalendarDateTime(date.year, date.month, date.day, 0, 0, 0);

    if (onChange) {
      onChange({
        target: { value: dateTimeToString(newDateTime), name: name ?? '' },
      } as React.ChangeEvent<HTMLInputElement>);
    }
  };

  const handleTimeChange = (time: DateValue | null) => {
    if (!time || !('hour' in time && 'minute' in time)) {
      if (allowEmpty && onChange) {
        onChange({
          target: { value: '', name: name ?? '' },
        } as React.ChangeEvent<HTMLInputElement>);
        return;
      }
      // If no time is set but we have a date, set time to 00:00:00
      if (dateValue) {
        const newDateTime = new CalendarDateTime(
          dateValue.year,
          dateValue.month,
          dateValue.day,
          0,
          0,
          0,
        );
        if (onChange) {
          onChange({
            target: { value: dateTimeToString(newDateTime), name: name ?? '' },
          } as React.ChangeEvent<HTMLInputElement>);
        }
      }
      return;
    }
    if (!dateValue) return;

    const newDateTime = new CalendarDateTime(
      dateValue.year,
      dateValue.month,
      dateValue.day,
      time.hour,
      time.minute,
      'second' in time ? time.second || 0 : 0,
    );

    if (onChange) {
      onChange({
        target: { value: dateTimeToString(newDateTime), name: name ?? '' },
      } as React.ChangeEvent<HTMLInputElement>);
    }
  };

  const calendarTrigger = (
    <DialogTrigger
      onOpenChange={(isOpen) => {
        if (isOpen) dismissSoftKeyboard();
      }}
    >
      <HoverHint
        hint={calendarOpenHoverHint}
        capturePointer={!!disabled}
        className="inline-flex shrink-0"
        shape="rounded-lg"
      >
        <Button
          slot="trigger"
          aria-label={openCalendarBtnLabel}
          className={cn(
            'text-[var(--text-secondary)] hover:text-[var(--fire-red)] transition-colors outline-none focus:ring-2 focus:ring-[var(--focus-ring)] rounded',
            compact ? 'px-1' : '-mr-1 px-1',
          )}
          isDisabled={disabled}
        >
          <ChevronDown className={compact ? 'h-3.5 w-3.5' : 'h-4 w-4'} aria-hidden />
        </Button>
      </HoverHint>
      <AriaModalOverlay
        isDismissable
        className="fixed inset-0 z-[110] flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-[max(1rem,env(safe-area-inset-top))] backdrop-blur-sm sm:items-center sm:pt-4"
      >
        <AriaModal className="my-auto w-[min(340px,calc(100vw-2rem))] rounded-2xl border border-[var(--border-color)] bg-[var(--card-bg)] p-4 shadow-2xl">
          <Dialog className="outline-none">
            <Calendar
              value={dateValue}
              onChange={handleDateChange}
              maxValue={maxDateTime ?? undefined}
            >
              <header className="flex items-center justify-between mb-4">
                <Button
                  slot="previous"
                  aria-label={t('common.a11y.previousMonth')}
                  className="p-2 rounded-lg text-[var(--text-primary)] hover:bg-[var(--hover-bg)] outline-none transition-colors"
                >
                  <ChevronLeft className="h-5 w-5" aria-hidden />
                </Button>
                <Heading className="font-semibold text-base text-[var(--text-primary)]" />
                <Button
                  slot="next"
                  aria-label={t('common.a11y.nextMonth')}
                  className="p-2 rounded-lg text-[var(--text-primary)] hover:bg-[var(--hover-bg)] outline-none transition-colors"
                >
                  <ChevronRight className="h-5 w-5" aria-hidden />
                </Button>
              </header>
              <CalendarGrid className="w-full border-separate border-spacing-1">
                <CalendarGridHeader>
                  {(day) => (
                    <CalendarHeaderCell className="text-center text-xs font-semibold text-[var(--text-secondary)] py-2">
                      {day}
                    </CalendarHeaderCell>
                  )}
                </CalendarGridHeader>
                <CalendarGridBody>
                  {(date) => (
                    <CalendarCell
                      date={date}
                      className={({ isSelected, isOutsideMonth, isToday, isDisabled }) =>
                        cn(
                          'm-0 h-8 w-8 rounded-lg p-0 text-sm outline-none transition-colors sm:h-9 sm:w-9',
                          'flex items-center justify-center font-medium',
                          'hover:bg-[var(--hover-bg)]',
                          isDisabled && 'opacity-30 cursor-not-allowed',
                          isOutsideMonth && 'text-[var(--text-secondary)]/40',
                          !isOutsideMonth &&
                            !isSelected &&
                            !isToday &&
                            'text-[var(--text-primary)]',
                          isToday &&
                            !isSelected &&
                            'font-bold border-2 border-[var(--fire-red)] text-[var(--fire-red)]',
                          isSelected && 'bg-[var(--fire-red)] text-white font-bold shadow-md',
                        )
                      }
                    >
                      {date.day}
                    </CalendarCell>
                  )}
                </CalendarGridBody>
              </CalendarGrid>
            </Calendar>

            {/* On mobile, keep time editing in the main field so the soft keyboard cannot cover the calendar. */}
            {!hideTime && !isMobileViewport && (
              <div className="mt-4 pt-4 border-t border-[var(--border-color)]">
                <TimeField
                  value={timeValue}
                  onChange={handleTimeChange}
                  granularity={granularity}
                  hourCycle={hourCycle}
                  shouldForceLeadingZeros={shouldForceLeadingZeros}
                >
                  <Label className="mb-2 block text-sm font-semibold text-[var(--text-primary)]">
                    {t('common.a11y.time')}
                  </Label>
                  <Group className="flex flex-nowrap items-center rounded-lg border-2 border-[var(--border-color)] bg-[var(--card-bg)] px-3 py-2 focus-within:border-[var(--fire-red)] focus-within:ring-2 focus-within:ring-[var(--fire-red)]/20">
                    <DateInput className="flex min-w-0 flex-1 flex-nowrap items-center justify-center gap-0 text-sm whitespace-nowrap">
                      {(segment) => <DateFieldSegment segment={segment} />}
                    </DateInput>
                  </Group>
                </TimeField>
              </div>
            )}
          </Dialog>
        </AriaModal>
      </AriaModalOverlay>
    </DialogTrigger>
  );

  return (
    <div className={cn('w-full', compact ? 'space-y-1' : 'space-y-2')}>
      {label && (
        <Label
          className={cn(
            'block font-semibold text-[var(--text-primary)]',
            compact ? 'text-[10px] leading-tight' : 'text-sm',
          )}
        >
          {label}
          {fieldRequired && (
            <span className={cn('text-[var(--fire-red)]', compact ? 'ml-0.5' : 'ml-1')}>*</span>
          )}
        </Label>
      )}
      {hint && <p className="text-xs text-[var(--text-secondary)] mt-1">{hint}</p>}

      <HoverHint
        hint={fieldHoverTitle}
        capturePointer={!!disabled}
        className="relative block min-w-0 w-full"
        shape="rounded-xl"
      >
        <div className="relative min-w-0">
          <Group className={fieldGroupClass(compact, isInvalid, disabled)}>
            <div
              className={cn(
                'flex min-w-0 flex-1 flex-nowrap items-center overflow-x-auto',
                compact ? 'gap-1.5' : 'gap-3',
              )}
            >
              <DateField
                value={dateValue}
                onChange={handleDateChange}
                isRequired={fieldRequired}
                isDisabled={disabled}
                isInvalid={isInvalid}
                granularity="day"
                hourCycle={hourCycle}
                shouldForceLeadingZeros={shouldForceLeadingZeros}
                maxValue={maxDateTime ?? undefined}
                className="shrink-0"
                aria-label={dateFieldAria(label || undefined)}
              >
                <DateInput className={dateInputRowClass(compact)}>
                  {(segment) => <DateFieldSegment segment={segment} />}
                </DateInput>
              </DateField>

              {!hideTime && (
                <>
                  <div
                    className={cn(
                      'shrink-0 text-[var(--text-secondary)]',
                      compact ? 'text-xs' : 'text-sm',
                    )}
                    aria-hidden
                  >
                    |
                  </div>

                  <TimeField
                    value={timeValue}
                    onChange={handleTimeChange}
                    isRequired={fieldRequired}
                    isDisabled={disabled}
                    isInvalid={isInvalid}
                    granularity={granularity}
                    hourCycle={hourCycle}
                    shouldForceLeadingZeros={shouldForceLeadingZeros}
                    className="shrink-0"
                    aria-label={timeFieldAria(label || undefined)}
                  >
                    <DateInput className={dateInputRowClass(compact)}>
                      {(segment) => <DateFieldSegment segment={segment} />}
                    </DateInput>
                  </TimeField>
                </>
              )}
            </div>

            {calendarTrigger}
          </Group>
        </div>
      </HoverHint>

      {error && (
        <FieldError className="text-sm text-red-600 dark:text-red-400 font-medium">
          {error}
        </FieldError>
      )}
      {errorMessage && !error && (
        <FieldError className="text-sm text-red-600 dark:text-red-400 font-medium">
          {typeof errorMessage === 'function'
            ? errorMessage({
                isInvalid,
                validationErrors: [],
                validationDetails: { valid: !isInvalid } as ValidityState,
              })
            : errorMessage}
        </FieldError>
      )}

      <input type="hidden" name={name} value={value ?? ''} id={inputId} />
    </div>
  );
}
