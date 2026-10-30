/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Clock, LogIn } from 'lucide-react';
import { useMemo, useState } from 'react';
import { DateTimePicker } from '@/components/ui/date-time-picker';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useMyOrganizationGroups, useMyOrganizations } from '@/lib/store';
import { filterVisibleOrganizationGroups, getGroupDisplayName } from '@/lib/utils';
import {
  formatToLocalDateTime,
  getOrganizationWallClockNow,
  isEndStrictlyBeforeStart,
  shiftEndWithStartDateChange,
} from '@/lib/utils/date';
import type { CreateUserTimeEntryRequest, UserTimeEntryDto } from '@/types/timeEntry';

// ─── Shared org/group selector ──────────────────────────────────────────────

interface OrgGroupSelectorProps {
  selectedOrgId: number | undefined;
  setSelectedOrgId: (id: number) => void;
  selectedGroupId: number | undefined;
  setSelectedGroupId: (id: number | undefined) => void;
  orgsLoading: boolean;
  orgOptions: { value: string; label: string }[];
  groupOptions: { value: string; label: string }[];
  hideOrganizationSelect?: boolean;
}

function OrgGroupSelector({
  selectedOrgId,
  setSelectedOrgId,
  selectedGroupId,
  setSelectedGroupId,
  orgsLoading,
  orgOptions,
  groupOptions,
  hideOrganizationSelect = false,
}: OrgGroupSelectorProps) {
  const t = useTranslation();

  if (orgsLoading && !hideOrganizationSelect) {
    return <p className="text-sm text-[var(--text-secondary)]">{t('common.loading')}</p>;
  }

  const showGroupSelect = groupOptions.length > 1;

  return (
    <div
      className={
        showGroupSelect
          ? 'grid grid-cols-1 gap-4 sm:grid-cols-2 sm:gap-x-4 sm:gap-y-0'
          : 'grid grid-cols-1'
      }
    >
      {hideOrganizationSelect ? null : (
        <Select
          label={t('myArea.timeEntries.fields.organization')}
          name="organizationId"
          value={selectedOrgId ? String(selectedOrgId) : ''}
          options={orgOptions}
          onChange={(e) => {
            setSelectedOrgId(Number(e.target.value));
            setSelectedGroupId(undefined);
          }}
          required
        />
      )}
      {showGroupSelect ? (
        <Select
          label={t('myArea.timeEntries.fields.group')}
          name="organizationGroupId"
          value={selectedGroupId ? String(selectedGroupId) : ''}
          options={groupOptions}
          onChange={(e) => setSelectedGroupId(e.target.value ? Number(e.target.value) : undefined)}
        />
      ) : null}
    </div>
  );
}

// ─── Stamp modal ─────────────────────────────────────────────────────────────

interface StampModalProps {
  onClose: () => void;
  onSave: (data: CreateUserTimeEntryRequest) => Promise<void>;
  fixedOrganizationId?: number;
}

function StampModalContent({ onClose, onSave, fixedOrganizationId }: StampModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: memberships = [], isLoading: orgsLoading } = useMyOrganizations();
  const [isLoading, setIsLoading] = useState(false);

  const mainOrg = useMemo(
    () => memberships.find((m) => m.organization?.main) ?? memberships[0],
    [memberships],
  );

  const defaultOrgId = fixedOrganizationId ?? mainOrg?.organization?.id;
  /** User-picked org; falls back to main org when memberships load. */
  const [orgOverride, setOrgOverride] = useState<number | undefined>(undefined);
  const selectedOrgId = fixedOrganizationId ?? orgOverride ?? defaultOrgId;

  const [selectedGroupId, setSelectedGroupId] = useState<number | undefined>();
  const [description, setDescription] = useState('');

  const { data: groups = [] } = useMyOrganizationGroups(selectedOrgId);

  const orgOptions = useMemo(
    () =>
      memberships
        .filter((m) => m.organization?.id != null)
        .map((m) => ({
          value: String(m.organization?.id),
          label:
            m.organization?.name +
            (m.organization?.main ? ` (${t('myArea.timeEntries.mainOrg')})` : ''),
        })),
    [memberships, t],
  );

  const groupOptions = useMemo(
    () => [
      { value: '', label: t('myArea.timeEntries.noGroup') },
      ...filterVisibleOrganizationGroups(groups).map((g) => ({
        value: String(g.id),
        label: getGroupDisplayName(g),
      })),
    ],
    [groups, t],
  );

  const resolvedGroupId =
    selectedGroupId != null && groups.some((g) => g.id === selectedGroupId)
      ? selectedGroupId
      : undefined;

  const handleStampIn = async () => {
    if (!selectedOrgId) return;
    const payload: CreateUserTimeEntryRequest = {
      organizationId: selectedOrgId,
      organizationGroupId: resolvedGroupId,
      startTime: getOrganizationWallClockNow(),
      description: description.trim() || undefined,
    };
    setIsLoading(true);
    try {
      await onSave(payload);
      showSuccess(t('myArea.timeEntries.success.stampedIn'));
      onClose();
    } catch (error) {
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('myArea.timeEntries.addTitle')}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleStampIn()}
          isValid={!!selectedOrgId}
          isLoading={isLoading}
          saveText={t('myArea.timeEntries.stampIn')}
        />
      }
    >
      <div className="space-y-6">
        <OrgGroupSelector
          selectedOrgId={selectedOrgId}
          setSelectedOrgId={(id) => {
            setOrgOverride(id);
            setSelectedGroupId(undefined);
          }}
          selectedGroupId={resolvedGroupId}
          setSelectedGroupId={setSelectedGroupId}
          orgsLoading={orgsLoading && !fixedOrganizationId}
          orgOptions={orgOptions}
          groupOptions={groupOptions}
          hideOrganizationSelect={fixedOrganizationId != null}
        />

        <Textarea
          label={t('myArea.timeEntries.fields.description')}
          name="description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={2}
          placeholder={t('myArea.timeEntries.descriptionPlaceholder')}
        />
      </div>
    </Modal>
  );
}

// ─── Manual modal ─────────────────────────────────────────────────────────────

interface ManualModalProps {
  onClose: () => void;
  entry?: UserTimeEntryDto;
  onSave: (data: CreateUserTimeEntryRequest) => Promise<void>;
  fixedOrganizationId?: number;
}

function ManualModalContent({ onClose, entry, onSave, fixedOrganizationId }: ManualModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data: memberships = [], isLoading: orgsLoading } = useMyOrganizations();
  const [isLoading, setIsLoading] = useState(false);

  const mainOrg = useMemo(
    () => memberships.find((m) => m.organization?.main) ?? memberships[0],
    [memberships],
  );

  const defaultOrgId = fixedOrganizationId ?? entry?.organizationId ?? mainOrg?.organization?.id;
  const [orgOverride, setOrgOverride] = useState<number | undefined>(undefined);
  const selectedOrgId = fixedOrganizationId ?? orgOverride ?? defaultOrgId;

  const [selectedGroupId, setSelectedGroupId] = useState<number | undefined>(
    () => entry?.organizationGroupId,
  );
  const [startTime, setStartTime] = useState(() =>
    entry?.startTime ? formatToLocalDateTime(entry.startTime) : getOrganizationWallClockNow(),
  );
  const [endTime, setEndTime] = useState(() =>
    entry?.endTime ? formatToLocalDateTime(entry.endTime) : '',
  );
  const [description, setDescription] = useState(entry?.description ?? '');

  const { data: groups = [] } = useMyOrganizationGroups(selectedOrgId);

  const orgOptions = useMemo(
    () =>
      memberships
        .filter((m) => m.organization?.id != null)
        .map((m) => ({
          value: String(m.organization?.id),
          label:
            m.organization?.name +
            (m.organization?.main ? ` (${t('myArea.timeEntries.mainOrg')})` : ''),
        })),
    [memberships, t],
  );

  const groupOptions = useMemo(
    () => [
      { value: '', label: t('myArea.timeEntries.noGroup') },
      ...filterVisibleOrganizationGroups(groups).map((g) => ({
        value: String(g.id),
        label: getGroupDisplayName(g),
      })),
    ],
    [groups, t],
  );

  const resolvedGroupId =
    selectedGroupId != null && groups.some((g) => g.id === selectedGroupId)
      ? selectedGroupId
      : undefined;

  const timeOrderError =
    startTime && endTime && isEndStrictlyBeforeStart(startTime, endTime)
      ? t('common.validation.endNotBeforeStart')
      : undefined;

  const isValid =
    !!selectedOrgId && !!startTime && (!endTime || !isEndStrictlyBeforeStart(startTime, endTime));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isValid || !selectedOrgId) return;
    const payload: CreateUserTimeEntryRequest = {
      organizationId: selectedOrgId,
      organizationGroupId: resolvedGroupId,
      startTime,
      endTime: endTime || undefined,
      description: description.trim() || undefined,
    };
    setIsLoading(true);
    try {
      await onSave(payload);
      showSuccess(
        entry ? t('myArea.timeEntries.success.updated') : t('myArea.timeEntries.success.created'),
      );
      onClose();
    } catch (error) {
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={entry ? t('myArea.timeEntries.editTitle') : t('myArea.timeEntries.addManualTitle')}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => {
            const form = document.getElementById('time-entry-form') as HTMLFormElement;
            form?.requestSubmit();
          }}
          isValid={isValid}
          isLoading={isLoading}
        />
      }
    >
      <form id="time-entry-form" onSubmit={(e) => void handleSubmit(e)}>
        <div className="space-y-6">
          <OrgGroupSelector
            selectedOrgId={selectedOrgId}
            setSelectedOrgId={(id) => {
              setOrgOverride(id);
              setSelectedGroupId(undefined);
            }}
            selectedGroupId={resolvedGroupId}
            setSelectedGroupId={setSelectedGroupId}
            orgsLoading={orgsLoading && !fixedOrganizationId}
            orgOptions={orgOptions}
            groupOptions={groupOptions}
            hideOrganizationSelect={fixedOrganizationId != null}
          />

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 sm:gap-x-4 sm:gap-y-0">
            <DateTimePicker
              label={t('myArea.timeEntries.fields.startTime')}
              name="startTime"
              value={startTime}
              onChange={(e) => {
                const nextStart = e.target.value;
                setEndTime((prevEnd) =>
                  prevEnd ? shiftEndWithStartDateChange(startTime, nextStart, prevEnd) : prevEnd,
                );
                setStartTime(nextStart);
              }}
              required
            />
            <DateTimePicker
              label={t('myArea.timeEntries.fields.endTime')}
              name="endTime"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              error={timeOrderError}
            />
          </div>

          <Textarea
            label={t('myArea.timeEntries.fields.description')}
            name="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            placeholder={t('myArea.timeEntries.descriptionPlaceholder')}
          />
        </div>
      </form>
    </Modal>
  );
}

// ─── Mode picker modal ────────────────────────────────────────────────────────

interface ModePickerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSelect: (mode: 'stamp' | 'manual') => void;
}

export function ModePickerModal({ isOpen, onClose, onSelect }: ModePickerModalProps) {
  const t = useTranslation();
  if (!isOpen) return null;

  return (
    <Modal isOpen onClose={onClose} title={t('myArea.timeEntries.addTitle')}>
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 sm:items-stretch sm:gap-4">
        <button
          type="button"
          onClick={() => onSelect('stamp')}
          className="flex h-full min-h-0 flex-col items-center justify-center gap-3 rounded-xl border border-[var(--border-color)] p-5 text-center transition-colors hover:border-[var(--fire-red)]/60 hover:bg-[var(--hover-bg)] sm:min-h-[9.5rem]"
        >
          <LogIn className="h-8 w-8 shrink-0 text-[var(--fire-red)]" />
          <div className="max-w-[16rem]">
            <p className="text-sm font-semibold text-[var(--text-primary)]">
              {t('myArea.timeEntries.modeStamp')}
            </p>
            <p className="mt-1 text-pretty text-xs leading-snug text-[var(--text-secondary)]">
              {t('myArea.timeEntries.modeStampHint')}
            </p>
          </div>
        </button>
        <button
          type="button"
          onClick={() => onSelect('manual')}
          className="flex h-full min-h-0 flex-col items-center justify-center gap-3 rounded-xl border border-[var(--border-color)] p-5 text-center transition-colors hover:border-[var(--fire-red)]/60 hover:bg-[var(--hover-bg)] sm:min-h-[9.5rem]"
        >
          <Clock className="h-8 w-8 shrink-0 text-[var(--fire-red)]" />
          <div className="max-w-[16rem]">
            <p className="text-sm font-semibold text-[var(--text-primary)]">
              {t('myArea.timeEntries.modeManual')}
            </p>
            <p className="mt-1 text-pretty text-xs leading-snug text-[var(--text-secondary)]">
              {t('myArea.timeEntries.modeManualHint')}
            </p>
          </div>
        </button>
      </div>
    </Modal>
  );
}

// ─── Public wrapper ───────────────────────────────────────────────────────────

interface TimeEntryFormModalProps {
  isOpen: boolean;
  mode: 'stamp' | 'manual';
  onClose: () => void;
  entry?: UserTimeEntryDto;
  onSave: (data: CreateUserTimeEntryRequest) => Promise<void>;
  fixedOrganizationId?: number;
}

export function TimeEntryFormModal({
  isOpen,
  mode,
  onClose,
  entry,
  onSave,
  fixedOrganizationId,
}: TimeEntryFormModalProps) {
  if (!isOpen) return null;

  if (mode === 'stamp') {
    return (
      <StampModalContent
        key="stamp-new"
        onClose={onClose}
        onSave={onSave}
        fixedOrganizationId={fixedOrganizationId}
      />
    );
  }

  return (
    <ManualModalContent
      key={entry?.id ?? 'manual-new'}
      onClose={onClose}
      entry={entry}
      onSave={onSave}
      fixedOrganizationId={fixedOrganizationId}
    />
  );
}
