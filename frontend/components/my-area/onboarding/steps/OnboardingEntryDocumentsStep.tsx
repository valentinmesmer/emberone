/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { CheckCircle2, Circle, CircleSlash, Download } from 'lucide-react';
import { forwardRef, useCallback, useEffect, useImperativeHandle, useMemo, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { FileInput } from '@/components/ui/file-input';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { onboardingInvitationApi } from '@/lib/api/onboardingInvitation';
import { buildEntryDocumentConfirmationText } from '@/lib/onboarding/entryDocumentConfirmation';
import { readEntryDocuments } from '@/lib/onboarding/stepPayloads';
import type {
  OnboardingDocumentCatalogItemDto,
  OnboardingEntryDocumentDraft,
  OnboardingInvitationAccess,
} from '@/types/onboardingInvitation';
import { useDownloadOnboardingAttachment } from '../useDownloadOnboardingAttachment';
import type { OnboardingStepHandle, OnboardingStepProps } from './onboardingStepHandle';
import { payloadsEqual } from './onboardingStepHandle';

interface OnboardingEntryDocumentsStepProps extends OnboardingStepProps {
  access: OnboardingInvitationAccess;
  documents: OnboardingDocumentCatalogItemDto[];
  documentScope?: 'tenant' | 'organization';
  onUpload?: (configKey: string, file: File) => Promise<void>;
}

const ACCEPT = 'application/pdf,image/jpeg,image/png,image/gif,image/webp';

const buildEntryDocumentsPayload = (
  documents: OnboardingDocumentCatalogItemDto[],
  confirmedKeys: Set<string>,
  declinedKeys: Set<string>,
  confirmationTextFor: (document: OnboardingDocumentCatalogItemDto) => string,
) => ({
  documents: documents
    .filter((document) => !document.alreadyFulfilled)
    .map((document) => {
      const confirmed = confirmedKeys.has(document.configKey);
      return {
        configKey: document.configKey,
        confirmed,
        declined: declinedKeys.has(document.configKey),
        ...(confirmed ? { confirmationText: confirmationTextFor(document) } : {}),
      };
    }),
});

const isDocumentComplete = (
  document: OnboardingDocumentCatalogItemDto,
  uploaded: boolean,
  confirmed: boolean,
  declined: boolean,
) =>
  document.alreadyFulfilled ||
  (!document.mandatory && declined) ||
  (document.fulfillmentMode === 'SIGNED_UPLOAD' ? uploaded : confirmed);

export const OnboardingEntryDocumentsStep = forwardRef<
  OnboardingStepHandle,
  OnboardingEntryDocumentsStepProps
>(function OnboardingEntryDocumentsStep(
  { access, payload, documents, documentScope = 'tenant', editable, isSaving, onUpload },
  ref,
) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const downloadAttachment = useDownloadOnboardingAttachment();
  const saved = useMemo(() => readEntryDocuments(payload), [payload]);
  const [confirmedKeys, setConfirmedKeys] = useState<Set<string>>(
    () => new Set(saved.filter((item) => item.confirmed).map((item) => item.configKey)),
  );
  const [declinedKeys, setDeclinedKeys] = useState<Set<string>>(
    () => new Set(saved.filter((item) => item.declined).map((item) => item.configKey)),
  );
  const [uploadingKey, setUploadingKey] = useState<string | null>(null);
  const [expandedKeys, setExpandedKeys] = useState<Set<string>>(new Set());
  const [openedKeys, setOpenedKeys] = useState<Set<string>>(
    () => new Set(saved.filter((item) => item.templateOpened).map((item) => item.configKey)),
  );
  const [templateOpenResetKeys, setTemplateOpenResetKeys] = useState<Set<string>>(new Set());

  const confirmationTextFor = useCallback(
    (document: OnboardingDocumentCatalogItemDto) => buildEntryDocumentConfirmationText(t, document),
    [t],
  );

  useEffect(() => {
    setOpenedKeys(
      new Set(saved.filter((item) => item.templateOpened).map((item) => item.configKey)),
    );
  }, [saved]);

  const draftByKey = useMemo(() => {
    const map = new Map<string, OnboardingEntryDocumentDraft>();
    for (const item of saved) {
      map.set(item.configKey, item);
    }
    return map;
  }, [saved]);

  const savedSnapshot = useState(() =>
    buildEntryDocumentsPayload(
      documents,
      new Set(saved.filter((item) => item.confirmed).map((item) => item.configKey)),
      new Set(saved.filter((item) => item.declined).map((item) => item.configKey)),
      confirmationTextFor,
    ),
  )[0];

  useImperativeHandle(
    ref,
    () => ({
      getPayload: () =>
        buildEntryDocumentsPayload(documents, confirmedKeys, declinedKeys, confirmationTextFor),
      hasChanges: () =>
        !payloadsEqual(
          savedSnapshot,
          buildEntryDocumentsPayload(documents, confirmedKeys, declinedKeys, confirmationTextFor),
        ),
    }),
    [confirmedKeys, declinedKeys, documents, savedSnapshot, confirmationTextFor],
  );

  const openCount = documents.filter((document) => {
    const draft = draftByKey.get(document.configKey);
    return !isDocumentComplete(
      document,
      draft?.uploaded === true,
      confirmedKeys.has(document.configKey),
      declinedKeys.has(document.configKey),
    );
  }).length;

  const downloadTemplate = async (document: OnboardingDocumentCatalogItemDto) => {
    await downloadAttachment(() =>
      documentScope === 'organization'
        ? onboardingInvitationApi.downloadOrganizationDocumentTemplate(
            access,
            document.configKey,
            document.templateFileName ?? `${document.name}.pdf`,
          )
        : onboardingInvitationApi.downloadDocumentTemplate(
            access,
            document.configKey,
            document.templateFileName ?? `${document.name}.pdf`,
          ),
    );
    setTemplateOpenResetKeys((prev) => {
      const next = new Set(prev);
      next.delete(document.configKey);
      return next;
    });
    setOpenedKeys((prev) => new Set(prev).add(document.configKey));
  };

  const downloadSignedFile = async (document: OnboardingDocumentCatalogItemDto) => {
    await downloadAttachment(() =>
      documentScope === 'organization'
        ? onboardingInvitationApi.downloadOrganizationSignedDocument(
            access,
            document.configKey,
            draftByKey.get(document.configKey)?.fileName ?? `${document.name}.pdf`,
          )
        : onboardingInvitationApi.downloadSignedDocument(
            access,
            document.configKey,
            draftByKey.get(document.configKey)?.fileName ?? `${document.name}.pdf`,
          ),
    );
  };

  const uploadFile = async (document: OnboardingDocumentCatalogItemDto, files: File[]) => {
    const file = files[0];
    if (!file || !onUpload) return;
    setUploadingKey(document.configKey);
    try {
      await onUpload(document.configKey, file);
      showSuccess(t('myArea.onboarding.wizard.entryDocuments.uploaded'));
    } catch (error) {
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
    } finally {
      setUploadingKey(null);
    }
  };

  if (documents.length === 0) {
    return (
      <p className="text-sm text-[var(--text-secondary)]">
        {t('myArea.onboarding.wizard.entryDocuments.empty')}
      </p>
    );
  }

  return (
    <div className="space-y-4">
      <div className="space-y-1">
        {openCount === 0 ? (
          <p className="text-sm font-medium text-[var(--text-primary)]">
            {t('myArea.onboarding.wizard.entryDocuments.progressDone')}
          </p>
        ) : (
          <>
            {editable ? (
              <p className="text-sm text-[var(--text-secondary)]">
                {t('myArea.onboarding.wizard.entryDocuments.hint')}
              </p>
            ) : null}
            <p className="text-xs font-medium text-[var(--text-primary)]">
              {t('myArea.onboarding.wizard.entryDocuments.progressOpen', { count: openCount })}
            </p>
          </>
        )}
      </div>

      <div className="space-y-3">
        {documents.map((document) => {
          const draft = draftByKey.get(document.configKey);
          const uploaded = draft?.uploaded === true;
          const confirmed = confirmedKeys.has(document.configKey);
          const declined = declinedKeys.has(document.configKey);
          const complete = isDocumentComplete(document, uploaded, confirmed, declined);
          const isUpload = document.fulfillmentMode === 'SIGNED_UPLOAD';
          const expanded = expandedKeys.has(document.configKey);
          const templateOpened =
            !templateOpenResetKeys.has(document.configKey) &&
            (openedKeys.has(document.configKey) || draft?.templateOpened === true);
          const canExpandCompleted =
            complete && editable && !document.alreadyFulfilled && (!isUpload || declined);
          const showTaskPanel = !document.alreadyFulfilled && (!complete || expanded);

          return (
            <ListContainer key={document.configKey} variant="inset" label={document.name}>
              <ListRow
                accent
                tone={declined ? 'default' : complete ? 'success' : 'active'}
                title={document.name}
                subtitle={
                  document.alreadyFulfilled
                    ? t('myArea.onboarding.wizard.entryDocuments.alreadyFulfilled')
                    : complete
                      ? declined
                        ? t('myArea.onboarding.wizard.entryDocuments.declined')
                        : isUpload
                          ? (draft?.fileName ?? t('myArea.onboarding.wizard.fulfilled'))
                          : t('myArea.onboarding.wizard.entryDocuments.agreed')
                      : isUpload
                        ? t('myArea.onboarding.wizard.entryDocuments.uploadTask')
                        : t('myArea.onboarding.wizard.entryDocuments.confirmTask')
                }
                badges={
                  !document.mandatory ? (
                    <Badge tone="neutral" pill>
                      {t('admin.tenantStandards.onboardingDocuments.optionalBadge')}
                    </Badge>
                  ) : undefined
                }
                leading={
                  complete && !declined ? (
                    <CheckCircle2
                      className="h-5 w-5 shrink-0 text-[var(--status-success-accent)]"
                      aria-hidden
                    />
                  ) : declined ? (
                    <CircleSlash
                      className="h-5 w-5 shrink-0 text-[var(--text-secondary)]"
                      aria-hidden
                    />
                  ) : (
                    <Circle className="h-5 w-5 shrink-0 text-[var(--text-secondary)]" aria-hidden />
                  )
                }
                keepActionsOnMobile
                onClick={
                  canExpandCompleted
                    ? () => {
                        const next = new Set(expandedKeys);
                        if (next.has(document.configKey)) next.delete(document.configKey);
                        else next.add(document.configKey);
                        setExpandedKeys(next);
                      }
                    : undefined
                }
                ariaLabel={
                  canExpandCompleted
                    ? declined
                      ? t('myArea.onboarding.wizard.entryDocuments.changeDecision')
                      : t('myArea.onboarding.wizard.entryDocuments.changeAgreement')
                    : undefined
                }
                actions={
                  complete && isUpload && editable ? (
                    <FileInput
                      variant="inline"
                      files={[]}
                      chooseLabel={t('myArea.onboarding.wizard.entryDocuments.replaceFile')}
                      onChange={(files) => void uploadFile(document, files)}
                      disabled={uploadingKey === document.configKey}
                      accept={ACCEPT}
                      hideFileList
                    />
                  ) : !editable && complete && isUpload && uploaded ? (
                    <IconButton
                      icon={<Download className="h-4 w-4" />}
                      aria-label={t('common.attachments.download')}
                      title={draft?.fileName ?? t('common.attachments.download')}
                      onClick={() => void downloadSignedFile(document)}
                    />
                  ) : !editable && complete && !isUpload && document.hasTemplate ? (
                    <IconButton
                      icon={<Download className="h-4 w-4" />}
                      aria-label={t('common.attachments.download')}
                      onClick={() => void downloadTemplate(document)}
                    />
                  ) : undefined
                }
                below={
                  showTaskPanel ? (
                    <EntryDocumentActions
                      document={document}
                      complete={complete}
                      declined={declined}
                      confirmed={confirmed}
                      templateOpened={templateOpened}
                      editable={editable}
                      isSaving={isSaving}
                      uploading={uploadingKey === document.configKey}
                      onDownload={() => void downloadTemplate(document)}
                      onUpload={(files) => void uploadFile(document, files)}
                      onConfirmChange={(checked) => {
                        if (!editable || isSaving) return;
                        const next = new Set(confirmedKeys);
                        if (checked) next.add(document.configKey);
                        else next.delete(document.configKey);
                        setConfirmedKeys(next);
                        if (checked) {
                          const declinedNext = new Set(declinedKeys);
                          declinedNext.delete(document.configKey);
                          setDeclinedKeys(declinedNext);
                        }
                      }}
                      onDecline={() => {
                        if (!editable || isSaving) return;
                        const next = new Set(declinedKeys);
                        next.add(document.configKey);
                        setDeclinedKeys(next);
                        const confirmedNext = new Set(confirmedKeys);
                        confirmedNext.delete(document.configKey);
                        setConfirmedKeys(confirmedNext);
                      }}
                      onWithdrawDecline={() => {
                        if (!editable || isSaving) return;
                        const next = new Set(declinedKeys);
                        next.delete(document.configKey);
                        setDeclinedKeys(next);
                        setOpenedKeys((prev) => {
                          const opened = new Set(prev);
                          opened.delete(document.configKey);
                          return opened;
                        });
                        setTemplateOpenResetKeys((prev) => new Set(prev).add(document.configKey));
                      }}
                    />
                  ) : undefined
                }
              />
            </ListContainer>
          );
        })}
      </div>
    </div>
  );
});

function EntryDocumentActions({
  document,
  complete,
  declined,
  confirmed,
  templateOpened,
  editable,
  isSaving,
  uploading,
  onDownload,
  onUpload,
  onConfirmChange,
  onDecline,
  onWithdrawDecline,
}: {
  document: OnboardingDocumentCatalogItemDto;
  complete: boolean;
  declined: boolean;
  confirmed: boolean;
  templateOpened: boolean;
  editable: boolean;
  isSaving: boolean;
  uploading: boolean;
  onDownload: () => void;
  onUpload: (files: File[]) => void;
  onConfirmChange: (checked: boolean) => void;
  onDecline: () => void;
  onWithdrawDecline: () => void;
}) {
  const t = useTranslation();
  const isUpload = document.fulfillmentMode === 'SIGNED_UPLOAD';
  const showSteps = !complete && document.hasTemplate;

  if (declined && editable) {
    return (
      <div className="space-y-2 py-1">
        <p className="text-xs text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.entryDocuments.withdrawDeclineHint')}
        </p>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          fullWidth
          onClick={onWithdrawDecline}
          disabled={isSaving}
        >
          {t('myArea.onboarding.wizard.entryDocuments.withdrawDecline')}
        </Button>
      </div>
    );
  }

  const downloadButton = document.hasTemplate ? (
    <Button
      type="button"
      variant="secondary"
      size="sm"
      fullWidth
      icon={<Download className="h-3.5 w-3.5" aria-hidden />}
      onClick={onDownload}
    >
      {isUpload
        ? t('myArea.onboarding.wizard.entryDocuments.downloadTemplate')
        : t('myArea.onboarding.wizard.entryDocuments.openDocument')}
    </Button>
  ) : null;

  const requiresOpen = document.hasTemplate && !templateOpened;
  const confirmControl = (
    <div className="space-y-1">
      <Checkbox
        label={t('myArea.onboarding.wizard.entryDocuments.confirmLabel')}
        checked={confirmed}
        onCheckedChange={onConfirmChange}
        disabled={!editable || isSaving || requiresOpen}
      />
      {requiresOpen ? (
        <p className="text-xs text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.entryDocuments.openBeforeConfirm')}
        </p>
      ) : null}
    </div>
  );

  const uploadControl = editable ? (
    <div className="space-y-1">
      <FileInput
        files={[]}
        chooseLabel={t('common.attachments.chooseFile')}
        hint={t('myArea.onboarding.wizard.entryDocuments.uploadHint')}
        onChange={onUpload}
        disabled={uploading || requiresOpen}
        accept={ACCEPT}
      />
      {requiresOpen ? (
        <p className="text-xs text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.entryDocuments.openBeforeConfirm')}
        </p>
      ) : null}
    </div>
  ) : null;

  const declineControl =
    !document.mandatory && editable && !complete ? (
      <div className="space-y-2 border-t border-[var(--border-color)]/30 pt-3">
        <Button
          type="button"
          variant="secondary"
          size="sm"
          fullWidth
          icon={<CircleSlash className="h-3.5 w-3.5" aria-hidden />}
          onClick={onDecline}
          disabled={isSaving}
        >
          {t('myArea.onboarding.wizard.entryDocuments.decline')}
        </Button>
        <p className="text-xs text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.entryDocuments.declineHint')}
        </p>
      </div>
    ) : null;

  if (!showSteps) {
    return (
      <div className="space-y-3">
        {isUpload ? uploadControl : confirmControl}
        {declineControl}
      </div>
    );
  }

  return (
    <div className="space-y-4 py-1">
      <div className="space-y-2">
        <p className="text-xs font-medium text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.entryDocuments.readStep')}
        </p>
        {downloadButton}
      </div>
      <div className="space-y-2">
        <p className="text-xs font-medium text-[var(--text-secondary)]">
          {isUpload
            ? t('myArea.onboarding.wizard.entryDocuments.uploadStep')
            : t('myArea.onboarding.wizard.entryDocuments.confirmStep')}
        </p>
        {isUpload ? uploadControl : confirmControl}
        {declineControl}
      </div>
    </div>
  );
}
