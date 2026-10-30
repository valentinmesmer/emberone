/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { parseDate } from '@internationalized/date';
import { useMemo, useState } from 'react';
import { DatePicker } from '@/components/ui/date-picker';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  ABSENCE_REASON_OPTION_VALUES,
  getAbsenceReasonLabelKey,
} from '@/lib/my-area/absenceReasonOptions';
import { useMyOrganizations } from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import {
  isEndStrictlyBeforeStart,
  laterOfDateStrings,
  shiftEndDateWithStartDateChange,
} from '@/lib/utils/date';
import type { CreateUserAbsenceRequest, UserAbsenceDto } from '@/types/absence';
import { AbsenceReason } from '@/types/enums/absenceReason';

interface AbsenceFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  absence?: UserAbsenceDto;
  onSave: (data: CreateUserAbsenceRequest) => Promise<void>;
}

function getLocalDateIsoString(date: Date = new Date()): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function isAbsenceFormValid(params: {
  selectedOrgIds: Set<number>;
  startDate: string;
  endDate: string;
}): boolean {
  if (params.selectedOrgIds.size < 1) return false;
  if (!params.startDate.trim() || !params.endDate.trim()) return false;
  return !isEndStrictlyBeforeStart(params.startDate, params.endDate);
}

function AbsenceFormModalContent({
  onClose,
  absence,
  onSave,
}: Omit<AbsenceFormModalProps, 'isOpen'>) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: memberships = [], isLoading: orgsLoading } = useMyOrganizations();
  const [isLoading, setIsLoading] = useState(false);

  /** User-picked orgs; null means fall back to main org (create) or absence orgs (edit). */
  const [selectedOrgIdsOverride, setSelectedOrgIdsOverride] = useState<Set<number> | null>(() =>
    absence ? new Set(absence.organizationIds ?? []) : null,
  );
  const [startDate, setStartDate] = useState(() => absence?.startDate ?? getLocalDateIsoString());
  const [endDate, setEndDate] = useState(() => absence?.endDate ?? getLocalDateIsoString());
  const [reason, setReason] = useState<AbsenceReason>(absence?.reason ?? AbsenceReason.PERSONAL);
  const [comment, setComment] = useState(absence?.comment ?? '');

  const mainOrgId = useMemo(() => {
    const main = memberships.find((m) => m.organization?.main) ?? memberships[0];
    return main?.organization?.id;
  }, [memberships]);

  const selectedOrgIds = useMemo(() => {
    if (selectedOrgIdsOverride != null) return selectedOrgIdsOverride;
    return mainOrgId != null ? new Set([mainOrgId]) : new Set<number>();
  }, [selectedOrgIdsOverride, mainOrgId]);

  const membershipOptions = useMemo(() => {
    const fromMembership = memberships.flatMap((m) => {
      if (m.organization?.id == null || m.organization.name == null) return [];
      return [{ id: m.organization.id, name: m.organization.name }];
    });
    if (!absence?.organizationIds?.length) {
      return fromMembership;
    }
    const known = new Set(fromMembership.map((o) => o.id));
    const extra = absence.organizationIds
      .filter((id) => !known.has(id))
      .map((id) => ({
        id,
        name: `${t('myArea.absence.orgUnknown')} (${id})`,
      }));
    return [...fromMembership, ...extra];
  }, [memberships, absence, t]);

  const reasonOptions = useMemo(
    () =>
      ABSENCE_REASON_OPTION_VALUES.map((r) => ({
        value: r,
        label: t(getAbsenceReasonLabelKey(r)),
      })),
    [t],
  );

  const isFormValid = useMemo(
    () => isAbsenceFormValid({ selectedOrgIds, startDate, endDate }),
    [selectedOrgIds, startDate, endDate],
  );

  const endDateMinValue = useMemo(() => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(startDate)) return undefined;
    try {
      return parseDate(startDate);
    } catch {
      return undefined;
    }
  }, [startDate]);

  const dateOrderError =
    startDate && endDate && isEndStrictlyBeforeStart(startDate, endDate)
      ? t('common.validation.endNotBeforeStart')
      : undefined;

  const handleStartDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const next = e.target.value;
    const prevStart = startDate;
    setStartDate(next);
    setEndDate((prevEnd) => {
      if (!prevEnd || !next) return prevEnd;
      if (
        /^\d{4}-\d{2}-\d{2}$/.test(prevEnd) &&
        /^\d{4}-\d{2}-\d{2}$/.test(next) &&
        /^\d{4}-\d{2}-\d{2}$/.test(prevStart)
      ) {
        return shiftEndDateWithStartDateChange(prevStart, next, prevEnd);
      }
      return laterOfDateStrings(prevEnd, next);
    });
  };

  const toggleOrg = (orgId: number) => {
    setSelectedOrgIdsOverride((prev) => {
      const next = new Set(prev ?? (mainOrgId != null ? [mainOrgId] : []));
      if (next.has(orgId)) {
        next.delete(orgId);
      } else {
        next.add(orgId);
      }
      return next;
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isFormValid) {
      return;
    }

    const organizationIds = Array.from(selectedOrgIds).sort((a, b) => a - b);
    const payload: CreateUserAbsenceRequest = {
      organizationIds,
      startDate,
      endDate,
      reason,
      comment: comment.trim() || undefined,
    };

    setIsLoading(true);
    try {
      await onSave(payload);
      showSuccess(
        absence ? t('myArea.absence.success.updated') : t('myArea.absence.success.created'),
      );
      onClose();
    } catch (error) {
      console.error('Error saving absence:', error);
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={absence ? t('myArea.absence.editTitle') : t('myArea.absence.addTitle')}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => {
            const form = document.getElementById('absence-form') as HTMLFormElement;
            form?.requestSubmit();
          }}
          isValid={isFormValid}
          isLoading={isLoading}
        />
      }
    >
      <form id="absence-form" onSubmit={(e) => void handleSubmit(e)}>
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <DatePicker
              label={t('myArea.absence.fields.startDate')}
              name="startDate"
              value={startDate}
              onChange={handleStartDateChange}
              required
            />
            <DatePicker
              label={t('myArea.absence.fields.endDate')}
              name="endDate"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              minValue={endDateMinValue}
              required
              error={dateOrderError}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 sm:items-end">
            <Select
              label={t('myArea.absence.fields.reason')}
              name="reason"
              value={reason}
              options={reasonOptions}
              onChange={(e) => setReason(e.target.value as AbsenceReason)}
            />
          </div>

          <fieldset className="space-y-2">
            <legend className="text-sm font-semibold text-[var(--text-primary)]">
              {t('myArea.absence.fields.organizations')}
            </legend>
            <p className="text-xs text-[var(--text-secondary)]">
              {t('myArea.absence.organizationsHint')}
            </p>
            {orgsLoading ? (
              <p className="text-sm text-[var(--text-secondary)]">{t('common.loading')}</p>
            ) : membershipOptions.length === 0 ? (
              <p className="text-sm text-[var(--text-secondary)]">
                {t('myArea.absence.noOrganizations')}
              </p>
            ) : (
              <fieldset
                className="max-h-48 space-y-1 overflow-y-auto rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)] p-2"
                aria-label={t('myArea.absence.fields.organizations')}
              >
                {membershipOptions.map((org) => {
                  const checked = selectedOrgIds.has(org.id);
                  const inputId = `absence-org-${org.id}`;
                  return (
                    <div
                      key={org.id}
                      className={cn(
                        'rounded-lg px-2 py-1 transition-colors duration-200',
                        checked
                          ? 'bg-[var(--fire-red)]/10 ring-1 ring-[var(--fire-red)]/25'
                          : 'hover:bg-[var(--hover-bg)]',
                      )}
                    >
                      <label
                        htmlFor={inputId}
                        className="flex cursor-pointer items-center gap-3 py-1.5"
                      >
                        <div className="relative flex shrink-0 items-center">
                          <input
                            id={inputId}
                            type="checkbox"
                            checked={checked}
                            onChange={() => toggleOrg(org.id)}
                            className={cn(
                              'h-5 w-5 cursor-pointer appearance-none rounded border-2 transition-all duration-200',
                              checked
                                ? 'border-[var(--fire-red)] bg-[var(--fire-red)]'
                                : 'border-[var(--border-color)] bg-[var(--card-bg)]',
                              'focus:outline-none focus:ring-2 focus:ring-[var(--fire-red)]/40 focus:ring-offset-2 focus:ring-offset-[var(--focus-ring-offset)]',
                            )}
                          />
                          {checked && (
                            <svg
                              className="pointer-events-none absolute left-0.5 top-0.5 h-4 w-4 text-white"
                              fill="none"
                              viewBox="0 0 24 24"
                              stroke="currentColor"
                              strokeWidth={3}
                              aria-hidden
                            >
                              <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                d="M5 13l4 4L19 7"
                              />
                            </svg>
                          )}
                        </div>
                        <span className="min-w-0 flex-1 text-sm font-medium text-[var(--text-primary)]">
                          {org.name}
                        </span>
                      </label>
                    </div>
                  );
                })}
              </fieldset>
            )}
          </fieldset>

          <Textarea
            label={t('myArea.absence.fields.comment')}
            name="comment"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            rows={4}
            placeholder={t('myArea.absence.commentPlaceholder')}
            hint={t('myArea.absence.commentHealthHint')}
          />
        </div>
      </form>
    </Modal>
  );
}

export function AbsenceFormModal({ isOpen, onClose, absence, onSave }: AbsenceFormModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <AbsenceFormModalContent
      key={absence?.id ?? 'new'}
      onClose={onClose}
      absence={absence}
      onSave={onSave}
    />
  );
}
