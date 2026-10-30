/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { DrivingLicenseClassesEditor } from '@/components/my-area/drivingLicenses/DrivingLicenseClassesEditor';
import { DatePicker } from '@/components/ui/date-picker';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { DrivingLicenseClassInput, DrivingLicenseDto } from '@/lib/api/drivingLicenses';
import { useOrgDrivingLicenseCatalog, useUpdateDrivingLicense } from '@/lib/store';

function mapClassesFromLicense(license: DrivingLicenseDto): DrivingLicenseClassInput[] {
  return (license.classes ?? []).map((licenseClass) => ({
    tenantDrivingLicenseDefinitionId: licenseClass.tenantDrivingLicenseDefinitionId,
    grantDate: licenseClass.grantDate,
    expirationDate: licenseClass.expirationDate,
  }));
}

export interface DrivingLicenseRecordEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  organizationId: number;
  userId: number;
  license: DrivingLicenseDto;
}

export function DrivingLicenseRecordEditModal({
  isOpen,
  ...props
}: DrivingLicenseRecordEditModalProps) {
  if (!isOpen) {
    return null;
  }

  return <DrivingLicenseRecordEditModalBody key={props.license.id} isOpen={isOpen} {...props} />;
}

function DrivingLicenseRecordEditModalBody({
  isOpen,
  onClose,
  organizationId,
  userId,
  license,
}: DrivingLicenseRecordEditModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const updateLicense = useUpdateDrivingLicense(organizationId, userId);
  const { data: catalog } = useOrgDrivingLicenseCatalog(organizationId, userId, isOpen);

  const [issueDate, setIssueDate] = useState(license.issueDate);
  const [documentExpirationDate, setDocumentExpirationDate] = useState(
    license.documentExpirationDate ?? '',
  );
  const [licenseNumber, setLicenseNumber] = useState(license.licenseNumber ?? '');
  const [issuingAuthority, setIssuingAuthority] = useState(license.issuingAuthority ?? '');
  const [notes, setNotes] = useState(license.notes ?? '');
  const [classes, setClasses] = useState<DrivingLicenseClassInput[]>(() =>
    mapClassesFromLicense(license),
  );

  const classDefinitions = useMemo(
    () => catalog?.definitions.filter((definition) => definition.active) ?? [],
    [catalog?.definitions],
  );

  const canSubmit =
    issueDate.trim().length > 0 &&
    licenseNumber.trim().length > 0 &&
    issuingAuthority.trim().length > 0;

  const handleSave = async () => {
    if (!canSubmit) return;
    try {
      await updateLicense.mutateAsync({
        issueDate,
        documentExpirationDate: documentExpirationDate.trim() || null,
        licenseNumber: licenseNumber.trim(),
        issuingAuthority: issuingAuthority.trim(),
        classes: classes.map((entry) => ({
          tenantDrivingLicenseDefinitionId: entry.tenantDrivingLicenseDefinitionId,
          grantDate: entry.grantDate?.trim() || null,
          expirationDate: entry.expirationDate?.trim() || null,
        })),
        notes: notes.trim() || null,
      });
      showSuccess(t('drivingLicense.record.updateSuccess'));
      onClose();
    } catch (error) {
      showError(error instanceof Error ? error.message : t('drivingLicense.record.updateError'));
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t('drivingLicense.record.editTitle')}
      size="lg"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isValid={canSubmit}
          isLoading={updateLicense.isPending}
          showSave
        />
      }
    >
      <div className="space-y-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <DatePicker
            label={t('drivingLicense.record.issueDateLabel')}
            value={issueDate}
            onChange={(event) => setIssueDate(event.target.value)}
            disabled={updateLicense.isPending}
            required
          />
          <DatePicker
            label={t('drivingLicense.record.documentExpirationDateLabel')}
            value={documentExpirationDate}
            onChange={(event) => setDocumentExpirationDate(event.target.value)}
            disabled={updateLicense.isPending}
          />
        </div>
        <Input
          label={t('drivingLicense.record.licenseNumberLabel')}
          value={licenseNumber}
          onChange={(event) => setLicenseNumber(event.target.value)}
          disabled={updateLicense.isPending}
          required
        />
        <Input
          label={t('drivingLicense.record.issuingAuthorityLabel')}
          value={issuingAuthority}
          onChange={(event) => setIssuingAuthority(event.target.value)}
          disabled={updateLicense.isPending}
          required
        />
        <DrivingLicenseClassesEditor
          definitions={classDefinitions}
          value={classes}
          onChange={setClasses}
          disabled={updateLicense.isPending}
          showArchiveHint
        />
        <Textarea
          label={t('common.record.notesLabel')}
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
          disabled={updateLicense.isPending}
        />
      </div>
    </Modal>
  );
}
