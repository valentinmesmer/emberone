/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { PendingAttachments } from '@/components/attachment';
import { WorkflowInputChangeRequestNote } from '@/components/my-area/workflows/WorkflowInputChangeRequestNote';
import { AddressInput } from '@/components/ui/address-input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { ApiRequestError } from '@/lib/api/client';
import { workflowsApi } from '@/lib/api/workflows';
import { useSubmitAddressChangeWorkflow, useUpdateWorkflowPayload } from '@/lib/store';
import type { UserDto } from '@/types/user';
import {
  type AddressChangeWorkflowPayload,
  WORKFLOW_CLIENT_ERROR_CODES,
  WORKFLOW_TYPE_IDS,
} from '@/types/workflow';

export interface AddressChangeRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  userData: UserDto;
  /** When set, updates an existing workflow payload instead of opening a new one. */
  editWorkflowId?: number | null;
  initialPayload?: AddressChangeWorkflowPayload | null;
  /** Comment from an open input-change request, shown while editing. */
  requestMessage?: string | null;
}

export function AddressChangeRequestModal({
  isOpen,
  onClose,
  userData,
  editWorkflowId = null,
  initialPayload = null,
  requestMessage = null,
}: AddressChangeRequestModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const submitAddressChange = useSubmitAddressChangeWorkflow();
  const updatePayload = useUpdateWorkflowPayload();
  const isEdit = editWorkflowId != null && editWorkflowId > 0;
  const pending = isEdit ? updatePayload.isPending : submitAddressChange.isPending;

  const contact = userData.contactInformation;

  const resolveInitial = () => {
    if (isEdit && initialPayload) {
      return {
        street: initialPayload.street,
        city: initialPayload.city,
        postalCode: initialPayload.postalCode,
        country: initialPayload.country,
        attachmentIds: initialPayload.attachmentIds ?? [],
      };
    }
    return {
      street: contact?.street ?? '',
      city: contact?.city ?? '',
      postalCode: contact?.postalCode ?? '',
      country: contact?.country ?? '',
      attachmentIds: [] as number[],
    };
  };

  const [street, setStreet] = useState(() => resolveInitial().street);
  const [city, setCity] = useState(() => resolveInitial().city);
  const [postalCode, setPostalCode] = useState(() => resolveInitial().postalCode);
  const [country, setCountry] = useState(() => resolveInitial().country);
  const [existingAttachmentIds, setExistingAttachmentIds] = useState(
    () => resolveInitial().attachmentIds,
  );
  const [files, setFiles] = useState<File[]>([]);
  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);
  const depsKey = `${editWorkflowId ?? 'new'}:${JSON.stringify(initialPayload)}:${contact?.street ?? ''}:${contact?.city ?? ''}:${contact?.postalCode ?? ''}:${contact?.country ?? ''}`;
  const [prevDepsKey, setPrevDepsKey] = useState(depsKey);

  if (isOpen !== prevIsOpen) {
    setPrevIsOpen(isOpen);
    if (isOpen) {
      const initial = resolveInitial();
      setStreet(initial.street);
      setCity(initial.city);
      setPostalCode(initial.postalCode);
      setCountry(initial.country);
      setExistingAttachmentIds(initial.attachmentIds);
      setFiles([]);
      setPrevDepsKey(depsKey);
    }
  } else if (isOpen && depsKey !== prevDepsKey) {
    setPrevDepsKey(depsKey);
    const initial = resolveInitial();
    setStreet(initial.street);
    setCity(initial.city);
    setPostalCode(initial.postalCode);
    setCountry(initial.country);
    setExistingAttachmentIds(initial.attachmentIds);
    setFiles([]);
  }

  const addressUnchanged = useMemo(() => {
    if (isEdit) return false;
    const s = street.trim();
    const c = city.trim();
    const pc = postalCode.trim();
    const co = country.trim();
    return (
      s.toLowerCase() === (contact?.street?.trim() ?? '').toLowerCase() &&
      c.toLowerCase() === (contact?.city?.trim() ?? '').toLowerCase() &&
      pc.toLowerCase() === (contact?.postalCode?.trim() ?? '').toLowerCase() &&
      co.toLowerCase() === (contact?.country?.trim() ?? '').toLowerCase()
    );
  }, [
    street,
    city,
    postalCode,
    country,
    isEdit,
    contact?.street,
    contact?.city,
    contact?.postalCode,
    contact?.country,
  ]);

  const canSubmit =
    street.trim().length > 0 &&
    city.trim().length > 0 &&
    postalCode.trim().length > 0 &&
    country.trim().length > 0 &&
    !addressUnchanged &&
    !pending;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    try {
      if (isEdit) {
        if (editWorkflowId == null) return;
        const uploadedIds =
          files.length > 0
            ? (
                await Promise.all(
                  files.map((file) =>
                    workflowsApi.uploadWorkflowDraftAttachment(
                      WORKFLOW_TYPE_IDS.ADDRESS_CHANGE,
                      file,
                    ),
                  ),
                )
              ).map((item) => item.id)
            : [];
        const attachmentIds = [...existingAttachmentIds, ...uploadedIds];
        const payload: AddressChangeWorkflowPayload = {
          street: street.trim(),
          city: city.trim(),
          postalCode: postalCode.trim(),
          country: country.trim(),
          ...(attachmentIds.length > 0 ? { attachmentIds } : {}),
        };
        await updatePayload.mutateAsync({ workflowId: editWorkflowId, payload });
        showSuccess(t('myArea.workflowDetail.editInputSuccess'));
      } else {
        await submitAddressChange.mutateAsync({
          files,
          payload: {
            street: street.trim(),
            city: city.trim(),
            postalCode: postalCode.trim(),
            country: country.trim(),
          },
        });
        showSuccess(t('common.request.successSubmitted'));
      }
      onClose();
    } catch (e) {
      const msg =
        e instanceof ApiRequestError && e.code === WORKFLOW_CLIENT_ERROR_CODES.TYPE_ALREADY_OPEN
          ? t('myArea.addressChange.errorAlreadyOpen')
          : e instanceof Error
            ? e.message
            : isEdit
              ? t('myArea.workflowDetail.editInputError')
              : t('common.request.errorSubmit');
      showError(msg);
    }
  };

  if (!isOpen) {
    return null;
  }

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        isEdit ? t('myArea.workflowDetail.editInputTitle') : t('myArea.addressChange.modalTitle')
      }
      size="md"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSubmit()}
          isValid={canSubmit}
          isLoading={pending}
          saveText={
            isEdit ? t('myArea.workflowDetail.editInputSubmit') : t('common.request.submit')
          }
          showSave
        />
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-[var(--text-secondary)]">
          {isEdit
            ? t('myArea.workflowDetail.editInputIntro')
            : t('myArea.addressChange.modalIntro')}
        </p>

        {isEdit && requestMessage?.trim() ? (
          <WorkflowInputChangeRequestNote message={requestMessage} tone="neutral" />
        ) : null}

        <AddressInput
          street={street}
          city={city}
          postalCode={postalCode}
          country={country}
          onAddressChange={(address) => {
            if (address.street !== undefined) setStreet(address.street);
            if (address.city !== undefined) setCity(address.city);
            if (address.postalCode !== undefined) setPostalCode(address.postalCode);
            if (address.country !== undefined) setCountry(address.country);
          }}
          required
        />

        {!isEdit && addressUnchanged && (
          <p className="text-sm text-amber-700 dark:text-amber-300">
            {t('myArea.addressChange.hintAddressUnchanged')}
          </p>
        )}

        {isEdit && existingAttachmentIds.length > 0 ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.editInputKeepAttachments', {
              count: existingAttachmentIds.length,
            })}
          </p>
        ) : null}

        <PendingAttachments
          variant="field"
          inputId="address-change-attachments"
          files={files}
          onChange={setFiles}
          label={t('myArea.addressChange.attachmentsLabel')}
          hint={t('myArea.addressChange.attachmentsHint')}
          optionalLabel={t('common.request.attachmentsOptional')}
          removeFileLabel={t('common.attachments.removeFile')}
        />
      </div>
    </Modal>
  );
}
