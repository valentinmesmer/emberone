/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { LoadingState } from '@/components/ui/loading';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { SectionActions, SectionHeader } from '@/components/ui/section-header';
import { Select } from '@/components/ui/select';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  formatStandardSwitchImpactMessage,
  switchImpactRequiresConfirmation,
} from '@/lib/admin/standardImpact';
import { type OrganizationStandardDto, type StandardDto, standardsApi } from '@/lib/api/standards';

interface OrganizationStandardCardProps {
  tenantId: number;
  organizationId: number;
  onStandardChanged?: () => void;
}

/**
 * Org-detail card showing the standard that drives an organization: either inherited from its org
 * type or an explicit per-org override. Lets the tenant admin set or clear the override.
 */
export function OrganizationStandardCard({
  tenantId,
  organizationId,
  onStandardChanged,
}: OrganizationStandardCardProps) {
  const t = useTranslation();
  const { showSuccess, showError } = useToast();

  const [info, setInfo] = useState<OrganizationStandardDto | null>(null);
  const [standards, setStandards] = useState<StandardDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [editOpen, setEditOpen] = useState(false);
  const [selected, setSelected] = useState<string>('');
  const [saving, setSaving] = useState(false);
  const [impactOpen, setImpactOpen] = useState(false);
  const [impactMessage, setImpactMessage] = useState('');
  const [pendingStandardId, setPendingStandardId] = useState<number | null | undefined>(undefined);

  const customStandards = useMemo(() => standards.filter((s) => !s.system), [standards]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [standardInfo, list] = await Promise.all([
        standardsApi.getOrganizationStandard(tenantId, organizationId),
        standardsApi.listStandards(tenantId),
      ]);
      setInfo(standardInfo);
      setStandards(list);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.orgOverride.loadFailed'));
    } finally {
      setLoading(false);
    }
  }, [tenantId, organizationId, showError, t]);

  useEffect(() => {
    const id = window.setTimeout(() => {
      void load();
    }, 0);
    return () => window.clearTimeout(id);
  }, [load]);

  function openEdit() {
    setSelected(info?.overrideStandardId != null ? String(info.overrideStandardId) : '');
    setEditOpen(true);
  }

  async function applyStandard(nextStandardId: number | null) {
    setSaving(true);
    try {
      const updated = await standardsApi.setOrganizationStandard(tenantId, organizationId, {
        standardId: nextStandardId,
      });
      setInfo(updated);
      setEditOpen(false);
      showSuccess(t('admin.standards.orgOverride.saved'));
      onStandardChanged?.();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.orgOverride.saveFailed'));
    } finally {
      setSaving(false);
    }
  }

  async function submit() {
    const nextStandardId = selected ? Number.parseInt(selected, 10) : null;
    const resolvedStandardId = Number.isFinite(nextStandardId as number) ? nextStandardId : null;
    setSaving(true);
    try {
      const impact = await standardsApi.getOrganizationStandardImpact(
        tenantId,
        organizationId,
        resolvedStandardId,
      );
      if (switchImpactRequiresConfirmation(impact)) {
        setPendingStandardId(resolvedStandardId);
        setImpactMessage(formatStandardSwitchImpactMessage(impact, t));
        setImpactOpen(true);
        return;
      }
      await applyStandard(resolvedStandardId);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.orgOverride.saveFailed'));
    } finally {
      setSaving(false);
    }
  }

  async function confirmImpact() {
    if (pendingStandardId === undefined) return;
    setImpactOpen(false);
    await applyStandard(pendingStandardId);
    setPendingStandardId(undefined);
  }

  const inheritLabel = info?.typeDefaultStandardName
    ? t('admin.standards.orgOverride.inheritOptionWithName').replace(
        '{name}',
        info.typeDefaultStandardName,
      )
    : t('admin.standards.orgOverride.inheritOption');

  const options = [
    { value: '', label: inheritLabel },
    ...customStandards.map((s) => ({ value: String(s.id), label: s.name })),
  ];

  return (
    <Card className="p-3 sm:p-4">
      <SectionHeader
        className="mb-2"
        title={t('admin.standards.orgOverride.title')}
        action={
          <SectionActions layout="inline">
            <Button
              type="button"
              variant="primary"
              size="xs"
              fullWidth="responsive"
              onClick={openEdit}
              disabled={loading}
            >
              {t('admin.standards.orgOverride.change')}
            </Button>
          </SectionActions>
        }
      />

      {loading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : info ? (
        <div className="space-y-1.5">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-base font-semibold text-[var(--text-primary)] sm:text-lg">
              {info.effectiveStandardName ?? t('admin.standards.orgOverride.noneAssigned')}
            </span>
            <Badge tone={info.overridden ? 'accent' : 'neutral'}>
              {info.overridden
                ? t('admin.standards.orgOverride.badgeOverride')
                : t('admin.standards.orgOverride.badgeInherited')}
            </Badge>
          </div>
          <p className="text-xs text-[var(--text-secondary)]">
            {t('admin.standards.orgOverride.subtitle')}
          </p>
          {info.overridden ? (
            <p className="text-xs text-[var(--text-secondary)]">
              {info.typeDefaultStandardName
                ? t('admin.standards.orgOverride.typeDefaultHint').replace(
                    '{name}',
                    info.typeDefaultStandardName,
                  )
                : t('admin.standards.orgOverride.typeDefaultNone')}
            </p>
          ) : null}
        </div>
      ) : null}

      {editOpen ? (
        <Modal
          isOpen
          onClose={() => setEditOpen(false)}
          title={t('admin.standards.orgOverride.modalTitle')}
          footer={
            <ModalFooter
              onCancel={() => setEditOpen(false)}
              onSave={() => void submit()}
              isLoading={saving}
              cancelText={t('common.cancel')}
              saveText={t('common.save')}
            />
          }
        >
          <div className="space-y-3">
            <p className="text-sm text-[var(--text-secondary)]">
              {t('admin.standards.orgOverride.modalHint')}
            </p>
            <Select
              label={t('admin.standards.orgOverride.selectLabel')}
              value={selected}
              options={options}
              onChange={(e) => setSelected(e.target.value)}
              disabled={saving}
            />
            {customStandards.length === 0 ? (
              <p className="text-xs text-[var(--toast-warning-text)]">
                {t('admin.standards.orgOverride.noCustomStandards')}
              </p>
            ) : null}
          </div>
        </Modal>
      ) : null}

      <ConfirmDialog
        isOpen={impactOpen}
        onClose={() => {
          setImpactOpen(false);
          setPendingStandardId(undefined);
        }}
        onConfirm={() => void confirmImpact()}
        title={t('admin.standards.impact.switch.title')}
        message={impactMessage}
        confirmText={t('admin.standards.impact.switch.confirm')}
        cancelText={t('common.cancel')}
        variant="danger"
        isLoading={saving}
      />
    </Card>
  );
}
