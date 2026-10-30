/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Download, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { FileInput } from '@/components/ui/file-input';
import { IconButton } from '@/components/ui/icon-button';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { SwitchField } from '@/components/ui/switch';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  adminOrganizationOnboardingDocumentsApi,
  type OnboardingDocumentFulfillmentMode,
  type OrganizationOnboardingDocumentDefinitionDto,
} from '@/lib/api/adminOrganizationOnboardingDocuments';
import { downloadBlob } from '@/lib/utils/downloadBlob';

export interface AdminOrganizationOnboardingDocumentEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
  tenantId: number;
  organizationId: number;
  document: OrganizationOnboardingDocumentDefinitionDto | null;
}

export function AdminOrganizationOnboardingDocumentEditModal({
  isOpen,
  ...props
}: AdminOrganizationOnboardingDocumentEditModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <AdminOrganizationOnboardingDocumentEditModalBody
      key={props.document?.configKey ?? 'create'}
      isOpen={isOpen}
      {...props}
    />
  );
}

function AdminOrganizationOnboardingDocumentEditModalBody({
  onClose,
  onSaved,
  tenantId,
  organizationId,
  document,
}: Omit<AdminOrganizationOnboardingDocumentEditModalProps, 'isOpen'> & { isOpen: boolean }) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [busy, setBusy] = useState(false);
  const [abbreviation, setAbbreviation] = useState(() => document?.abbreviation ?? '');
  const [name, setName] = useState(() => document?.name ?? '');
  const [description, setDescription] = useState(() => document?.description ?? '');
  const [fulfillmentMode, setFulfillmentMode] = useState<OnboardingDocumentFulfillmentMode>(
    () => document?.fulfillmentMode ?? 'CONFIRMED',
  );
  const [mandatory, setMandatory] = useState(() => document?.mandatory ?? true);
  const [active, setActive] = useState(() => document?.active ?? true);
  const [hasTemplate, setHasTemplate] = useState(() => document?.hasTemplate ?? false);
  const [templateFileName, setTemplateFileName] = useState(() => document?.templateFileName ?? '');
  const [pendingFile, setPendingFile] = useState<File | null>(null);

  const fulfillmentModeOptions = [
    {
      value: 'CONFIRMED',
      label: t('admin.tenantStandards.onboardingDocuments.fulfillmentMode.CONFIRMED'),
    },
    {
      value: 'SIGNED_UPLOAD',
      label: t('admin.tenantStandards.onboardingDocuments.fulfillmentMode.SIGNED_UPLOAD'),
    },
  ];

  const hasFile = hasTemplate || pendingFile != null;
  const requiresFile = !hasTemplate;

  const isValid =
    name.trim().length > 0 &&
    abbreviation.trim().length > 0 &&
    (!requiresFile || pendingFile != null);

  const buildPayload = (nextActive: boolean) => ({
    abbreviation: abbreviation.trim(),
    name: name.trim(),
    description: description.trim(),
    fulfillmentMode,
    mandatory,
    active: nextActive,
  });

  const handleSave = async () => {
    if (!isValid) return;
    setBusy(true);
    try {
      const willUpload = pendingFile != null;
      const shouldActivate = active && hasFile;
      let savedConfigKey = document?.configKey ?? '';

      if (document) {
        const updated = await adminOrganizationOnboardingDocumentsApi.update(
          tenantId,
          organizationId,
          savedConfigKey,
          buildPayload(!willUpload && shouldActivate),
        );
        savedConfigKey = updated.configKey;
      } else {
        const created = await adminOrganizationOnboardingDocumentsApi.create(
          tenantId,
          organizationId,
          buildPayload(false),
        );
        savedConfigKey = created.configKey;
      }

      if (willUpload && pendingFile) {
        await adminOrganizationOnboardingDocumentsApi.uploadTemplate(
          tenantId,
          organizationId,
          savedConfigKey,
          pendingFile,
        );
        setHasTemplate(true);
        setTemplateFileName(pendingFile.name);
        setPendingFile(null);
      }

      if (shouldActivate) {
        await adminOrganizationOnboardingDocumentsApi.update(
          tenantId,
          organizationId,
          savedConfigKey,
          buildPayload(true),
        );
      }

      showSuccess(t('admin.tenantStandards.onboardingDocuments.saved'));
      onSaved();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.onboardingDocuments.saveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleDeleteTemplate = async () => {
    if (!document) return;
    setBusy(true);
    try {
      await adminOrganizationOnboardingDocumentsApi.deleteTemplate(
        tenantId,
        organizationId,
        document.configKey,
      );
      setHasTemplate(false);
      setTemplateFileName('');
      setPendingFile(null);
      setActive(false);
      showSuccess(t('admin.tenantStandards.onboardingDocuments.templateDeleted'));
    } catch (e) {
      showError(
        e instanceof Error
          ? e.message
          : t('admin.tenantStandards.onboardingDocuments.templateDeleteFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleDownloadTemplate = async () => {
    if (!document?.hasTemplate) return;
    setBusy(true);
    try {
      const blob = await adminOrganizationOnboardingDocumentsApi.downloadTemplate(
        tenantId,
        organizationId,
        document.configKey,
      );
      downloadBlob(blob, document.templateFileName ?? 'document');
    } catch (e) {
      showError(
        e instanceof Error
          ? e.message
          : t('admin.tenantStandards.onboardingDocuments.templateDownloadFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        document
          ? t('admin.tenantStandards.onboardingDocuments.editTitle')
          : t('admin.tenantStandards.onboardingDocuments.createTitle')
      }
      footer={
        <ModalFooter
          onCancel={onClose}
          cancelText={t('common.cancel')}
          onSave={() => void handleSave()}
          saveText={t('common.save')}
          isValid={isValid}
          isLoading={busy}
        />
      }
    >
      <div className="space-y-3">
        <Input
          label={t('admin.tenantStandards.onboardingDocuments.abbreviationLabel')}
          value={abbreviation}
          onChange={(e) => setAbbreviation(e.target.value.toUpperCase())}
          placeholder={t('admin.tenantStandards.onboardingDocuments.abbreviationPlaceholder')}
          required
        />
        <Input
          label={t('admin.standards.name')}
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <Textarea
          label={t('admin.tenantStandards.onboardingDocuments.descriptionLabel')}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={4}
        />
        <Select
          label={t('admin.tenantStandards.onboardingDocuments.fulfillmentModeLabel')}
          value={fulfillmentMode}
          onChange={(e) => setFulfillmentMode(e.target.value as OnboardingDocumentFulfillmentMode)}
          options={fulfillmentModeOptions}
          required
        />

        <SwitchField
          label={t('admin.tenantStandards.onboardingDocuments.mandatoryLabel')}
          checked={mandatory}
          onCheckedChange={setMandatory}
        />
        <SwitchField
          label={t('admin.standards.active')}
          checked={active}
          onCheckedChange={setActive}
        />

        <div className="space-y-2">
          {hasTemplate && templateFileName ? (
            <div className="flex items-center justify-between gap-2 rounded-lg border border-[var(--border-subtle)] px-3 py-2">
              <span className="min-w-0 truncate text-sm text-[var(--text-primary)]">
                {templateFileName}
              </span>
              <div className="flex items-center gap-1">
                {document?.hasTemplate ? (
                  <IconButton
                    icon={<Download className="h-4 w-4" />}
                    aria-label={t('admin.tenantStandards.onboardingDocuments.templateDownload')}
                    onClick={() => void handleDownloadTemplate()}
                    disabled={busy}
                  />
                ) : null}
                {document ? (
                  <IconButton
                    icon={<Trash2 className="h-4 w-4" />}
                    aria-label={t('common.attachments.removeFile')}
                    onClick={() => void handleDeleteTemplate()}
                    disabled={busy}
                  />
                ) : null}
              </div>
            </div>
          ) : null}
          <FileInput
            variant="field"
            label={t('admin.tenantStandards.onboardingDocuments.templateLabel')}
            accept="application/pdf,image/jpeg,image/png,image/gif,image/webp"
            files={pendingFile ? [pendingFile] : []}
            onChange={(files) => setPendingFile(files[0] ?? null)}
            disabled={busy}
            required
            chooseLabel={t('common.attachments.chooseFile')}
          />
          {requiresFile ? (
            <p className="text-xs text-[var(--text-secondary)]">
              {t('admin.tenantStandards.onboardingDocuments.templateRequired')}
            </p>
          ) : null}
        </div>
      </div>
    </Modal>
  );
}
