/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import type { CreateTenantRequest, TenantAdminPatchRequest } from '@/lib/api/admin';
import type { FederalState } from '@/lib/federal-state';
import { TenantMasterDataFormFields } from './TenantMasterDataFormFields';
import type { TenantMasterDataFormState } from './tenantMasterDataFormModel';
import {
  isTenantFormComplete,
  masterDataFormToCreateRequest,
  masterDataFormToPatchRequest,
} from './tenantMasterDataFormModel';

type TenantMasterDataModalProps =
  | {
      mode: 'create';
      isOpen: boolean;
      onClose: () => void;
      isLoading: boolean;
      initialValues: TenantMasterDataFormState;
      configuredFederalStates?: FederalState[];
      onCreate: (body: CreateTenantRequest) => Promise<void>;
    }
  | {
      mode: 'edit';
      tenantId: number;
      isOpen: boolean;
      onClose: () => void;
      isLoading: boolean;
      initialValues: TenantMasterDataFormState;
      onPatch: (tenantId: number, body: TenantAdminPatchRequest) => Promise<void>;
    };

export function TenantMasterDataModal(props: TenantMasterDataModalProps) {
  const t = useTranslation();
  const [form, setForm] = useState<TenantMasterDataFormState>(() => ({ ...props.initialValues }));

  const isCreate = props.mode === 'create';
  const formComplete = isTenantFormComplete(form);

  const handleSave = async () => {
    if (!formComplete) return;
    if (props.mode === 'create') {
      await props.onCreate(masterDataFormToCreateRequest(form));
    } else {
      await props.onPatch(props.tenantId, masterDataFormToPatchRequest(form));
    }
  };

  return (
    <Modal
      isOpen={props.isOpen}
      onClose={props.onClose}
      title={isCreate ? t('admin.newTenantModalTitle') : t('admin.tenantForm.editModalTitle')}
      size="sm"
      footer={
        <ModalFooter
          onCancel={props.onClose}
          onSave={() => void handleSave()}
          isValid={formComplete}
          isLoading={props.isLoading}
          saveText={isCreate ? t('common.add') : t('common.save')}
        />
      }
    >
      <div className="space-y-4">
        {isCreate ? (
          <p className="text-sm text-[var(--text-secondary)]">
            {t('admin.newTenantModalDescription')}
          </p>
        ) : null}
        <TenantMasterDataFormFields
          form={form}
          setForm={setForm}
          showLocaleField={isCreate}
          showFederalStateField={isCreate}
          configuredFederalStates={
            props.mode === 'create' ? props.configuredFederalStates : undefined
          }
          showAbbreviationField={isCreate}
        />
      </div>
    </Modal>
  );
}
