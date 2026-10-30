/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import {
  CalendarDate,
  parseDate,
  parseZonedDateTime,
  type ZonedDateTime,
} from '@internationalized/date';
import { ChevronDown, ChevronLeft, ChevronRight } from 'lucide-react';
import React from 'react';
import {
  DatePicker as AriaDatePicker,
  type DatePickerProps as AriaDatePickerProps,
  Modal as AriaModal,
  ModalOverlay as AriaModalOverlay,
  Button,
  Calendar,
  CalendarCell,
  CalendarGrid,
  CalendarGridBody,
  CalendarGridHeader,
  CalendarHeaderCell,
  DateInput,
  type DateValue,
  Dialog,
  FieldError,
  Group,
  Heading,
  Label,
  Popover,
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

export interface DatePickerProps
  extends Omit<
    React.InputHTMLAttributes<HTMLInputElement>,
    'type' | 'value' | 'onChange' | 'defaultValue'
  > {
  label?: string;
  error?: string;
  value?: string;
  defaultValue?: DateValue | null;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  errorMessage?: string | ((validation: ValidationResult) => string);
  granularity?: AriaDatePickerProps<DateValue>['granularity'];
  hourCycle?: 12 | 24;
  hideTimeZone?: boolean;
  shouldForceLeadingZeros?: boolean;
  minValue?: DateValue | null;
  maxValue?: DateValue | null;
}

const stringToDate = (
  value: string | undefined,
  defaultValue?: DateValue | null,
): CalendarDate | ZonedDateTime | null => {
  if (defaultValue !== undefined) return defaultValue as CalendarDate | ZonedDateTime | null;
  if (!value) return null;
  try {
    if (value.match(/^\d{4}-\d{2}-\d{2}$/)) return parseDate(value);
    if (value.includes('[') && value.includes(']')) return parseZonedDateTime(value);
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) {
      return new CalendarDate(date.getFullYear(), date.getMonth() + 1, date.getDate());
    }
  } catch {}
  return null;
};

const dateToString = (date: DateValue | null): string => {
  if (!date) return '';
  return `${String(date.year).padStart(4, '0')}-${String(date.month).padStart(2, '0')}-${String(date.day).padStart(2, '0')}`;
};

export function DatePicker({
  label,
  error,
  className,
  value = '',
  defaultValue,
  onChange,
  required,
  disabled,
  title,
  errorMessage,
  granularity = 'day',
  hourCycle,
  hideTimeZone,
  shouldForceLeadingZeros = true,
  minValue,
  maxValue,
  id,
  name,
  'aria-label': ariaLabel,
  ..._rest
}: DatePickerProps) {
  void _rest;
  const t = useTranslation();
  const isMobileViewport = React.useSyncExternalStore(
    subscribeToMobileViewport,
    getMobileViewportSnapshot,
    getServerMobileViewportSnapshot,
  );
  const inputId = id || name;
  const dateValue = stringToDate(value, defaultValue);
  const isInvalid = !!error;
  const fieldHoverTitle = useFieldHoverTitle(disabled, title);
  const openCalendarLabel = label
    ? `${t('common.a11y.openCalendar')}: ${label}`
    : t('common.a11y.openCalendar');

  const handleChange = (date: DateValue | null) => {
    if (onChange) {
      onChange({
        target: { value: dateToString(date), name: name ?? '' },
      } as React.ChangeEvent<HTMLInputElement>);
    }
  };

  const calendarDialog = (
    <Dialog className="outline-none">
      <Calendar className="w-full">
        <header className="flex items-center justify-between mb-3">
          <Button
            slot="previous"
            aria-label={t('common.a11y.previousMonth')}
            className="p-1.5 rounded text-[var(--text-primary)] hover:bg-[var(--hover-bg)] outline-none"
          >
            <ChevronLeft className="h-4 w-4" aria-hidden />
          </Button>
          <Heading className="font-medium text-sm text-[var(--text-primary)]" />
          <Button
            slot="next"
            aria-label={t('common.a11y.nextMonth')}
            className="p-1.5 rounded text-[var(--text-primary)] hover:bg-[var(--hover-bg)] outline-none"
          >
            <ChevronRight className="h-4 w-4" aria-hidden />
          </Button>
        </header>
        <CalendarGrid className="w-full border-separate border-spacing-1">
          <CalendarGridHeader>
            {(day) => (
              <CalendarHeaderCell className="text-center text-xs text-[var(--text-secondary)] py-1.5">
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
                    'h-8 w-8 rounded text-sm outline-none m-0 p-0',
                    'flex items-center justify-center',
                    'hover:bg-[var(--hover-bg)]',
                    isDisabled && 'opacity-30 cursor-not-allowed',
                    isOutsideMonth && 'text-[var(--text-secondary)]/40',
                    !isOutsideMonth && !isSelected && !isToday && 'text-[var(--text-primary)]',
                    isToday && !isSelected && 'font-medium text-[var(--text-primary)]',
                    isSelected && 'bg-[var(--fire-red)] text-white font-medium',
                  )
                }
              >
                <span className="tabular-nums">{date.day}</span>
              </CalendarCell>
            )}
          </CalendarGridBody>
        </CalendarGrid>
      </Calendar>
    </Dialog>
  );

  return (
    <div className="w-full space-y-2">
      <AriaDatePicker
        // Prefer null over undefined so an empty controlled field stays controlled.
        value={dateValue}
        defaultValue={defaultValue ?? undefined}
        onChange={handleChange}
        isRequired={required}
        isDisabled={disabled}
        isInvalid={isInvalid}
        minValue={minValue ?? undefined}
        maxValue={maxValue ?? undefined}
        granularity={granularity}
        hourCycle={hourCycle}
        hideTimeZone={hideTimeZone}
        shouldForceLeadingZeros={shouldForceLeadingZeros}
        className={cn('flex flex-col gap-2', className)}
        name={name}
        aria-label={label ? undefined : (ariaLabel ?? t('common.a11y.date'))}
        onOpenChange={(isOpen) => {
          if (isOpen) dismissSoftKeyboard();
        }}
      >
        {label && (
          <Label className="block text-sm font-semibold text-[var(--text-primary)]">
            {label}
            {required && <span className="text-[var(--fire-red)] ml-1">*</span>}
          </Label>
        )}

        <Group
          className={cn(
            'flex w-full items-center rounded-xl border bg-[var(--card-bg)] px-4 py-3',
            'text-[var(--text-primary)] border-[var(--border-color)]',
            'focus-within:border-[var(--fire-red)] focus-within:outline-none focus-within:ring-2 focus-within:ring-[var(--focus-ring)]',
            'transition-colors duration-200',
            disabled && 'opacity-50 cursor-not-allowed',
            isInvalid && 'border-red-500 focus-within:ring-red-500/25',
          )}
        >
          <div className="flex min-h-0 min-w-0 flex-1 items-center overflow-x-auto">
            <HoverHint
              hint={fieldHoverTitle}
              capturePointer={!!disabled}
              className="min-w-0"
              shape="rounded-xl"
            >
              <DateInput className="flex min-h-0 min-w-0 items-center gap-0 text-sm">
                {(segment) => <DateFieldSegment segment={segment} />}
              </DateInput>
            </HoverHint>
          </div>
          <HoverHint
            hint={fieldHoverTitle}
            capturePointer={!!disabled}
            className="inline-flex shrink-0"
            shape="rounded-lg"
          >
            <Button
              slot="trigger"
              aria-label={openCalendarLabel}
              className="-mr-1 px-1 text-[var(--text-secondary)] hover:text-[var(--fire-red)] transition-colors outline-none focus:ring-2 focus:ring-[var(--focus-ring)] rounded"
            >
              <ChevronDown className="w-4 h-4" aria-hidden />
            </Button>
          </HoverHint>
        </Group>

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

        {isMobileViewport ? (
          <AriaModalOverlay
            isDismissable
            className="fixed inset-0 z-[110] flex items-start justify-center overflow-y-auto bg-black/50 p-4 pt-[max(1rem,env(safe-area-inset-top))] backdrop-blur-sm"
          >
            <AriaModal className="my-auto w-[min(320px,calc(100vw-2rem))] rounded-2xl border border-[var(--border-color)] bg-[var(--card-bg)] p-3 shadow-2xl">
              {calendarDialog}
            </AriaModal>
          </AriaModalOverlay>
        ) : (
          <Popover
            isNonModal
            className="z-50 w-[min(320px,calc(100vw-2rem))] rounded-lg border border-[var(--border-color)] bg-[var(--card-bg)] p-3 shadow-md"
            offset={8}
            placement="bottom start"
          >
            {calendarDialog}
          </Popover>
        )}
      </AriaDatePicker>

      <input type="hidden" name={name} value={value ?? ''} id={inputId} />
    </div>
  );
}
