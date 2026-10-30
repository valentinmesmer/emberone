/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { FieldDisplay } from '@/components/ui/field-display';
import { formatAddress } from '@/lib/utils/address';
import { normalizeWorkflowStatus } from '@/lib/utils/workflowApprovalSlot';
import {
  type AddressChangeWorkflowPayload,
  WORKFLOW_TYPE_IDS,
  type WorkflowDto,
} from '@/types/workflow';
import { parseAddressChangePayload } from '../payloadParsers';
import type {
  WorkflowInputDisplayContext,
  WorkflowInputDisplayPlugin,
  WorkflowInputSelfContext,
} from '../types';
import { WorkflowInputSectionCard } from '../WorkflowInputSectionCard';

function resolveAddressChangePrevious(
  payload: AddressChangeWorkflowPayload,
  wf: WorkflowDto,
  self?: WorkflowInputSelfContext,
): { street: string; city: string; postalCode: string; country: string } {
  if (
    payload.previousStreet != null ||
    payload.previousCity != null ||
    payload.previousPostalCode != null ||
    payload.previousCountry != null
  ) {
    return {
      street: payload.previousStreet ?? '',
      city: payload.previousCity ?? '',
      postalCode: payload.previousPostalCode ?? '',
      country: payload.previousCountry ?? '',
    };
  }
  if (
    self != null &&
    wf.openedByUserId === self.id &&
    normalizeWorkflowStatus(wf.status) === 'OPEN'
  ) {
    return {
      street: self.contactInformation?.street?.trim() ?? '',
      city: self.contactInformation?.city?.trim() ?? '',
      postalCode: self.contactInformation?.postalCode?.trim() ?? '',
      country: self.contactInformation?.country?.trim() ?? '',
    };
  }
  return { street: '', city: '', postalCode: '', country: '' };
}

function AddressChangeInputDisplay({
  ctx,
  payload,
}: {
  ctx: WorkflowInputDisplayContext;
  payload: AddressChangeWorkflowPayload;
}) {
  const { workflow, self, t } = ctx;
  const previous = resolveAddressChangePrevious(payload, workflow, self);

  return (
    <WorkflowInputSectionCard>
      <div className="flex flex-col gap-4 sm:grid sm:grid-cols-2">
        <div className="space-y-3">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.addressChangeBeforeSection')}
          </h3>
          <FieldDisplay
            label={t('myArea.fields.address')}
            value={formatAddress(
              previous.street,
              previous.city,
              previous.postalCode,
              previous.country,
            )}
          />
        </div>
        <div className="space-y-3">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
            {t('myArea.workflowDetail.addressChangeAfterSection')}
          </h3>
          <FieldDisplay
            label={t('myArea.fields.address')}
            value={formatAddress(payload.street, payload.city, payload.postalCode, payload.country)}
          />
        </div>
      </div>
    </WorkflowInputSectionCard>
  );
}

export const addressChangeInputDisplayPlugin: WorkflowInputDisplayPlugin<AddressChangeWorkflowPayload> =
  {
    workflowTypeId: WORKFLOW_TYPE_IDS.ADDRESS_CHANGE,
    parsePayload: parseAddressChangePayload,
    render(ctx, payload) {
      return <AddressChangeInputDisplay ctx={ctx} payload={payload} />;
    },
  };
