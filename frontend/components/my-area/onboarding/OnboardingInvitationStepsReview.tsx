/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Check, Eye } from 'lucide-react';
import type { ReactNode } from 'react';
import { onboardingStepTypeKey } from '@/components/admin/standards/onboardingStepUtils';
import { formatGrantedTrainingCourseTypeNames } from '@/components/my-area/trainingCourses/TrainingCourseTypeSelectionField';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { List, ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import { onboardingInvitationApi } from '@/lib/api/onboardingInvitation';
import type { OnboardingStepType } from '@/lib/api/standards';
import { getEmergencyContactRelationshipLabelKey } from '@/lib/my-area/emergencyContactOptions';
import { readOnboardingAttachmentFileNames } from '@/lib/onboarding/attachmentUtils';
import {
  type OnboardingPromotedDocumentScope,
  resolveOnboardingPromotedDocumentScope,
  viewOnboardingPromotedUserDocument,
} from '@/lib/onboarding/onboardingPromotedDocumentView';
import {
  readCertifications,
  readDrivingLicense,
  readEmergencyContacts,
  readEntryDocuments,
  readNoneDeclared,
  readOnboardingRankId,
  readTrainingCourses,
} from '@/lib/onboarding/stepPayloads';
import {
  matchPromotedCertificationRecord,
  matchPromotedDrivingLicense,
  matchPromotedTrainingCourseRecord,
  type OnboardingApprovedEvidence,
  promotedUserDocumentIdForFileIndex,
  useOnboardingApprovedEvidence,
} from '@/lib/onboarding/useOnboardingApprovedEvidence';
import { formatAddress } from '@/lib/utils/address';
import { cn } from '@/lib/utils/cn';
import { formatDateLong } from '@/lib/utils/date';
import type {
  OnboardingDocumentCatalogItemDto,
  OnboardingInvitationAccess,
  OnboardingInvitationCatalogDto,
  OnboardingInvitationStatus,
  OnboardingInvitationStepDto,
} from '@/types/onboardingInvitation';
import { type OnboardingProofItem, OnboardingProofList } from './OnboardingProofList';
import { useViewOnboardingAttachment } from './useViewOnboardingAttachment';

interface OnboardingInvitationStepsReviewProps {
  access: OnboardingInvitationAccess;
  status: OnboardingInvitationStatus;
  invitationId: number;
  targetOrganizationId: number;
  subjectUserId: number;
  steps: OnboardingInvitationStepDto[];
  catalog: OnboardingInvitationCatalogDto;
  alreadyFulfilledStepTypes?: OnboardingStepType[];
}

interface ApprovedDocumentReviewContext {
  invitationId: number;
  promotedScope: OnboardingPromotedDocumentScope;
  evidence: OnboardingApprovedEvidence;
}

export function OnboardingInvitationStepsReview({
  access,
  status,
  invitationId,
  targetOrganizationId,
  subjectUserId,
  steps,
  catalog,
  alreadyFulfilledStepTypes = [],
}: OnboardingInvitationStepsReviewProps) {
  const t = useTranslation();
  const { view, viewingKey } = useViewOnboardingAttachment();
  const usePromotedDocuments = status === 'APPROVED';
  const promotedScope = resolveOnboardingPromotedDocumentScope(
    access,
    targetOrganizationId,
    subjectUserId,
  );
  const evidence = useOnboardingApprovedEvidence({
    enabled: usePromotedDocuments,
    invitationId,
    organizationId: targetOrganizationId,
    subjectUserId,
    scopeKind: access.kind,
  });
  const approvedContext: ApprovedDocumentReviewContext | null = usePromotedDocuments
    ? { invitationId, promotedScope, evidence }
    : null;

  if (steps.length === 0 && alreadyFulfilledStepTypes.length === 0) {
    return (
      <p className="text-sm text-[var(--text-secondary)]">
        {t('myArea.onboarding.wizard.noSteps')}
      </p>
    );
  }

  return (
    <div className="w-full space-y-4">
      {steps.map((step) => (
        <StepReviewBody
          key={step.stepType}
          access={access}
          step={step}
          catalog={catalog}
          viewingKey={viewingKey}
          onView={view}
          approvedContext={approvedContext}
        />
      ))}
      {alreadyFulfilledStepTypes.length > 0 ? (
        <Card density="compact" title={t('myArea.onboarding.detail.alreadyFulfilledStepsTitle')}>
          <p className="mb-3 text-sm text-[var(--text-secondary)]">
            {t('myArea.onboarding.detail.alreadyFulfilledStepsHint')}
          </p>
          <List
            variant="inset"
            rowSeparator="inset"
            label={t('myArea.onboarding.detail.alreadyFulfilledStepsTitle')}
          >
            {alreadyFulfilledStepTypes.map((stepType) => (
              <ListRow
                key={stepType}
                compact
                accent={false}
                leading={<Check className="h-4 w-4 text-[var(--fire-red)]" aria-hidden />}
                title={t(`myArea.onboarding.wizard.stepTitles.${onboardingStepTypeKey(stepType)}`)}
                badges={
                  <Badge tone="neutral" pill>
                    {t('myArea.onboarding.wizard.fulfilled')}
                  </Badge>
                }
              />
            ))}
          </List>
        </Card>
      ) : null}
    </div>
  );
}

function StepShell({ step, children }: { step: OnboardingInvitationStepDto; children: ReactNode }) {
  const t = useTranslation();
  return (
    <Card
      density="compact"
      title={t(`myArea.onboarding.wizard.stepTitles.${onboardingStepTypeKey(step.stepType)}`)}
      action={
        step.fulfilled ? undefined : (
          <Badge tone="warning" pill>
            {t('myArea.onboarding.wizard.unfulfilled')}
          </Badge>
        )
      }
    >
      {children}
    </Card>
  );
}

function StepReviewBody({
  access,
  step,
  catalog,
  viewingKey,
  onView,
  approvedContext,
}: {
  access: OnboardingInvitationAccess;
  step: OnboardingInvitationStepDto;
  catalog: OnboardingInvitationCatalogDto;
  viewingKey: string | null;
  onView: (key: string, run: (targetWindow: Window) => Promise<void>) => Promise<void>;
  approvedContext: ApprovedDocumentReviewContext | null;
}) {
  switch (step.stepType) {
    case 'TRAINING_COURSES':
      return (
        <TrainingCoursesReview
          access={access}
          step={step}
          payload={step.payload}
          catalog={catalog}
          viewingKey={viewingKey}
          onView={onView}
          approvedContext={approvedContext}
        />
      );
    case 'CERTIFICATIONS':
      return (
        <CertificationsReview
          access={access}
          step={step}
          payload={step.payload}
          catalog={catalog}
          viewingKey={viewingKey}
          onView={onView}
          approvedContext={approvedContext}
        />
      );
    case 'DRIVING_LICENSE':
      return (
        <DrivingLicenseReview
          access={access}
          step={step}
          payload={step.payload}
          catalog={catalog}
          viewingKey={viewingKey}
          onView={onView}
          approvedContext={approvedContext}
        />
      );
    case 'EMERGENCY_CONTACTS':
      return <EmergencyContactsReview step={step} payload={step.payload} />;
    case 'ENTRY_DOCUMENTS':
      return (
        <EntryDocumentsReview
          access={access}
          step={step}
          payload={step.payload}
          documents={catalog.documents ?? []}
          documentScope="tenant"
          viewingKey={viewingKey}
          onView={onView}
          approvedContext={approvedContext}
        />
      );
    case 'ORGANIZATION_ENTRY_DOCUMENTS':
      return (
        <EntryDocumentsReview
          access={access}
          step={step}
          payload={step.payload}
          documents={catalog.organizationDocuments ?? []}
          documentScope="organization"
          viewingKey={viewingKey}
          onView={onView}
          approvedContext={approvedContext}
        />
      );
    case 'RANK':
      return <RankReview step={step} payload={step.payload} ranks={catalog.ranks ?? []} />;
  }
}

function RecordBlock({ children, showDivider }: { children: ReactNode; showDivider: boolean }) {
  return (
    <div
      className={cn('space-y-3', showDivider && 'border-t border-[var(--border-color)]/30 pt-3')}
    >
      {children}
    </div>
  );
}

function TrainingCoursesReview({
  access,
  step,
  payload,
  catalog,
  viewingKey,
  onView,
  approvedContext,
}: {
  access: OnboardingInvitationAccess;
  step: OnboardingInvitationStepDto;
  payload: Record<string, unknown>;
  catalog: OnboardingInvitationCatalogDto;
  viewingKey: string | null;
  onView: (key: string, run: (targetWindow: Window) => Promise<void>) => Promise<void>;
  approvedContext: ApprovedDocumentReviewContext | null;
}) {
  const t = useTranslation();
  const records = readTrainingCourses(payload);
  if (readNoneDeclared(payload) && records.length === 0) {
    return (
      <StepShell step={step}>
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.trainingCourses.noneDeclaredReview')}
        </p>
      </StepShell>
    );
  }
  if (records.length === 0) {
    return (
      <StepShell step={step}>
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.trainingCourses.empty')}
        </p>
      </StepShell>
    );
  }

  return (
    <StepShell step={step}>
      <div className="space-y-3">
        {records.map((record, index) => {
          const typeIds = record.tenantTrainingCourseTypeIds;
          const courseName =
            record.name?.trim() ||
            (typeIds.length === 1
              ? formatGrantedTrainingCourseTypeNames(typeIds, catalog.trainingCourseTypes)
              : null);
          const promotedRecord =
            approvedContext != null
              ? matchPromotedTrainingCourseRecord(
                  record,
                  approvedContext.invitationId,
                  approvedContext.evidence.trainingRecords,
                )
              : undefined;
          const proofItems = recordProofItems(
            readOnboardingAttachmentFileNames(record),
            (fileName, fileIndex) => {
              const key = proofKey('TRAINING_COURSES', record.recordKey, fileIndex);
              const promotedDocumentId = promotedUserDocumentIdForFileIndex(
                promotedRecord?.userDocumentIds,
                fileIndex,
              );
              return {
                key,
                fileName,
                onView:
                  approvedContext != null
                    ? promotedDocumentId != null
                      ? () =>
                          void onView(key, (targetWindow) =>
                            viewOnboardingPromotedUserDocument(
                              approvedContext.promotedScope,
                              promotedDocumentId,
                              fileName,
                              targetWindow,
                            ),
                          )
                      : undefined
                    : () =>
                        void onView(key, (targetWindow) =>
                          onboardingInvitationApi.viewRecordAttachment(
                            access,
                            'TRAINING_COURSES',
                            record.recordKey,
                            fileName,
                            targetWindow,
                            fileIndex,
                          ),
                        ),
              };
            },
          );
          return (
            <RecordBlock key={record.recordKey} showDivider={index > 0}>
              {courseName ? (
                <FieldDisplay label={t('trainingCourse.submissionName.label')} value={courseName} />
              ) : null}
              {typeIds.length > 1 ? (
                <div className="space-y-1.5">
                  <p className="text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
                    {t('trainingCourse.grantedTypes.label')}
                  </p>
                  <ul className="space-y-1">
                    {typeIds.map((typeId) => (
                      <li key={typeId} className="text-sm font-medium text-[var(--text-primary)]">
                        {formatGrantedTrainingCourseTypeNames(
                          [typeId],
                          catalog.trainingCourseTypes,
                        )}
                      </li>
                    ))}
                  </ul>
                </div>
              ) : null}
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <FieldDisplay
                  label={t('trainingCourse.record.startDateLabel')}
                  value={formatDateLong(record.startDate)}
                />
                <FieldDisplay
                  label={t('trainingCourse.record.endDateLabel')}
                  value={formatDateLong(record.endDate)}
                />
                <FieldDisplay
                  label={t('trainingCourse.record.locationLabel')}
                  value={record.location}
                />
                {record.hours != null ? (
                  <FieldDisplay
                    label={t('trainingCourse.record.hoursLabel')}
                    value={t('trainingCourse.record.hours', { hours: record.hours })}
                  />
                ) : null}
              </div>
              {record.notes?.trim() ? (
                <div className="border-t border-[var(--border-color)]/30 pt-3">
                  <FieldDisplay label={t('common.record.notesLabel')} value={record.notes} />
                </div>
              ) : null}
              <RecordDocuments items={proofItems} viewingKey={viewingKey} />
            </RecordBlock>
          );
        })}
      </div>
    </StepShell>
  );
}

function CertificationsReview({
  access,
  step,
  payload,
  catalog,
  viewingKey,
  onView,
  approvedContext,
}: {
  access: OnboardingInvitationAccess;
  step: OnboardingInvitationStepDto;
  payload: Record<string, unknown>;
  catalog: OnboardingInvitationCatalogDto;
  viewingKey: string | null;
  onView: (key: string, run: (targetWindow: Window) => Promise<void>) => Promise<void>;
  approvedContext: ApprovedDocumentReviewContext | null;
}) {
  const t = useTranslation();
  const records = readCertifications(payload);
  if (readNoneDeclared(payload) && records.length === 0) {
    return (
      <StepShell step={step}>
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.certifications.noneDeclaredReview')}
        </p>
      </StepShell>
    );
  }
  if (records.length === 0) {
    return (
      <StepShell step={step}>
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.certifications.empty')}
        </p>
      </StepShell>
    );
  }

  return (
    <StepShell step={step}>
      <div className="space-y-3">
        {records.map((record, index) => {
          const definitionName =
            catalog.certifications.find(
              (definition) => definition.id === record.tenantCertDefinitionId,
            )?.name ?? `#${record.tenantCertDefinitionId}`;
          const promotedRecord =
            approvedContext != null
              ? matchPromotedCertificationRecord(
                  record,
                  approvedContext.invitationId,
                  approvedContext.evidence.certificationRecords,
                )
              : undefined;
          const proofItems = recordProofItems(
            readOnboardingAttachmentFileNames(record),
            (fileName, fileIndex) => {
              const key = proofKey('CERTIFICATIONS', record.recordKey, fileIndex);
              const promotedDocumentId = promotedUserDocumentIdForFileIndex(
                promotedRecord?.userDocumentIds,
                fileIndex,
              );
              return {
                key,
                fileName,
                onView:
                  approvedContext != null
                    ? promotedDocumentId != null
                      ? () =>
                          void onView(key, (targetWindow) =>
                            viewOnboardingPromotedUserDocument(
                              approvedContext.promotedScope,
                              promotedDocumentId,
                              fileName,
                              targetWindow,
                            ),
                          )
                      : undefined
                    : () =>
                        void onView(key, (targetWindow) =>
                          onboardingInvitationApi.viewRecordAttachment(
                            access,
                            'CERTIFICATIONS',
                            record.recordKey,
                            fileName,
                            targetWindow,
                            fileIndex,
                          ),
                        ),
              };
            },
          );
          return (
            <RecordBlock key={record.recordKey} showDivider={index > 0}>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <FieldDisplay
                  label={t('certifications.submit.definitionLabel')}
                  value={definitionName}
                />
                <FieldDisplay
                  label={t('certifications.submit.completedDateLabel')}
                  value={formatDateLong(record.completedDate)}
                />
              </div>
              {record.notes?.trim() ? (
                <div className="border-t border-[var(--border-color)]/30 pt-3">
                  <FieldDisplay
                    label={t('certifications.submit.notesLabel')}
                    value={record.notes}
                  />
                </div>
              ) : null}
              <RecordDocuments items={proofItems} viewingKey={viewingKey} />
            </RecordBlock>
          );
        })}
      </div>
    </StepShell>
  );
}

function DrivingLicenseReview({
  access,
  step,
  payload,
  catalog,
  viewingKey,
  onView,
  approvedContext,
}: {
  access: OnboardingInvitationAccess;
  step: OnboardingInvitationStepDto;
  payload: Record<string, unknown>;
  catalog: OnboardingInvitationCatalogDto;
  viewingKey: string | null;
  onView: (key: string, run: (targetWindow: Window) => Promise<void>) => Promise<void>;
  approvedContext: ApprovedDocumentReviewContext | null;
}) {
  const t = useTranslation();
  const license = readDrivingLicense(payload);
  if (readNoneDeclared(payload) && license == null) {
    return (
      <StepShell step={step}>
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.drivingLicense.noneDeclaredReview')}
        </p>
      </StepShell>
    );
  }
  if (license == null) {
    return (
      <StepShell step={step}>
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.drivingLicense.empty')}
        </p>
      </StepShell>
    );
  }

  const promotedLicense =
    approvedContext != null
      ? matchPromotedDrivingLicense(
          license,
          approvedContext.invitationId,
          approvedContext.evidence.drivingLicense,
        )
      : null;
  const proofItems = recordProofItems(
    readOnboardingAttachmentFileNames(license),
    (fileName, fileIndex) => {
      const key = proofKey('DRIVING_LICENSE', 'license', fileIndex);
      const promotedDocumentId = promotedUserDocumentIdForFileIndex(
        promotedLicense?.userDocumentIds,
        fileIndex,
      );
      return {
        key,
        fileName,
        onView:
          approvedContext != null
            ? promotedDocumentId != null
              ? () =>
                  void onView(key, (targetWindow) =>
                    viewOnboardingPromotedUserDocument(
                      approvedContext.promotedScope,
                      promotedDocumentId,
                      fileName,
                      targetWindow,
                    ),
                  )
              : undefined
            : () =>
                void onView(key, (targetWindow) =>
                  onboardingInvitationApi.viewDrivingLicenseAttachment(
                    access,
                    fileName,
                    targetWindow,
                    fileIndex,
                  ),
                ),
      };
    },
  );

  return (
    <StepShell step={step}>
      <div className="space-y-3">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <FieldDisplay
            label={t('drivingLicense.record.issueDateLabel')}
            value={formatDateLong(license.issueDate)}
          />
          {license.documentExpirationDate?.trim() ? (
            <FieldDisplay
              label={t('drivingLicense.record.documentExpirationDateLabel')}
              value={formatDateLong(license.documentExpirationDate)}
            />
          ) : null}
          {license.licenseNumber.trim() ? (
            <FieldDisplay
              label={t('drivingLicense.record.licenseNumberLabel')}
              value={license.licenseNumber}
            />
          ) : null}
          {license.issuingAuthority.trim() ? (
            <FieldDisplay
              label={t('drivingLicense.record.issuingAuthorityLabel')}
              value={license.issuingAuthority}
            />
          ) : null}
        </div>
        {license.classes.length > 0 ? (
          <div className="border-t border-[var(--border-color)]/30 pt-3">
            <p className="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
              {t('drivingLicense.classes.label')}
            </p>
            <div className="flex flex-wrap gap-1.5">
              {license.classes.map((licenseClass) => {
                const className =
                  catalog.drivingLicenseClasses.find(
                    (definition) => definition.id === licenseClass.tenantDrivingLicenseDefinitionId,
                  )?.name ?? `#${licenseClass.tenantDrivingLicenseDefinitionId}`;
                return (
                  <Badge key={licenseClass.tenantDrivingLicenseDefinitionId} tone="neutral">
                    {className}
                  </Badge>
                );
              })}
            </div>
          </div>
        ) : null}
        {license.notes?.trim() ? (
          <div className="border-t border-[var(--border-color)]/30 pt-3">
            <FieldDisplay label={t('common.record.notesLabel')} value={license.notes} />
          </div>
        ) : null}
        <RecordDocuments items={proofItems} viewingKey={viewingKey} showWhenEmpty />
      </div>
    </StepShell>
  );
}

function RankReview({
  step,
  payload,
  ranks,
}: {
  step: OnboardingInvitationStepDto;
  payload: Record<string, unknown>;
  ranks: { id: number; name: string }[];
}) {
  const t = useTranslation();
  const rankId = readOnboardingRankId(payload);
  const rankRecord =
    payload.rank && typeof payload.rank === 'object'
      ? (payload.rank as Record<string, unknown>)
      : undefined;
  const fileNames = readOnboardingAttachmentFileNames(rankRecord ?? {});

  if (readNoneDeclared(payload) || rankId == null) {
    return (
      <StepShell step={step}>
        <p className="text-sm text-[var(--text-secondary)]">
          {t('onboarding.rank.noneDeclaredReview')}
        </p>
      </StepShell>
    );
  }

  const rankName =
    ranks.find((rank) => rank.id === rankId)?.name ?? t('ranks.userSection.unknownRank');

  return (
    <StepShell step={step}>
      <div className="space-y-3">
        <FieldDisplay label={t('onboarding.rank.select')} value={rankName} />
        {fileNames.length > 0 ? (
          <RecordDocuments
            items={recordProofItems(fileNames, (fileName, fileIndex) => ({
              key: proofKey('RANK', 'rank', fileIndex),
              fileName,
            }))}
            viewingKey={null}
          />
        ) : null}
      </div>
    </StepShell>
  );
}

function EmergencyContactsReview({
  step,
  payload,
}: {
  step: OnboardingInvitationStepDto;
  payload: Record<string, unknown>;
}) {
  const t = useTranslation();
  const contacts = readEmergencyContacts(payload);
  if (contacts.length === 0) {
    return (
      <StepShell step={step}>
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.emergencyContacts.empty')}
        </p>
      </StepShell>
    );
  }

  return (
    <StepShell step={step}>
      <div className="space-y-3">
        {contacts.map((contact, index) => (
          <RecordBlock
            key={`${contact.relationship}-${contact.firstName}-${contact.lastName}-${contact.phone}-${contact.email}`}
            showDivider={index > 0}
          >
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <FieldDisplay
                label={t('myArea.emergencyContact.fields.firstName')}
                value={contact.firstName}
              />
              <FieldDisplay
                label={t('myArea.emergencyContact.fields.lastName')}
                value={contact.lastName}
              />
              <FieldDisplay
                label={t('myArea.emergencyContact.fields.relationship')}
                value={t(getEmergencyContactRelationshipLabelKey(contact.relationship))}
              />
              <FieldDisplay
                label={t('myArea.emergencyContact.fields.phone')}
                value={contact.phone}
              />
              <FieldDisplay
                label={t('myArea.emergencyContact.fields.address')}
                value={formatAddress(
                  contact.street,
                  contact.city,
                  contact.postalCode,
                  contact.country,
                )}
              />
              <FieldDisplay
                label={t('myArea.emergencyContact.fields.email')}
                value={contact.email}
              />
            </div>
          </RecordBlock>
        ))}
      </div>
    </StepShell>
  );
}

function EntryDocumentsReview({
  access,
  step,
  payload,
  documents,
  documentScope = 'tenant',
  viewingKey,
  onView,
  approvedContext,
}: {
  access: OnboardingInvitationAccess;
  step: OnboardingInvitationStepDto;
  payload: Record<string, unknown>;
  documents: OnboardingDocumentCatalogItemDto[];
  documentScope?: 'tenant' | 'organization';
  viewingKey: string | null;
  onView: (key: string, run: (targetWindow: Window) => Promise<void>) => Promise<void>;
  approvedContext: ApprovedDocumentReviewContext | null;
}) {
  const t = useTranslation();
  const drafts = readEntryDocuments(payload);
  const draftByKey = new Map(drafts.map((draft) => [draft.configKey, draft]));

  if (documents.length === 0) {
    return (
      <StepShell step={step}>
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.onboarding.wizard.entryDocuments.empty')}
        </p>
      </StepShell>
    );
  }

  return (
    <StepShell step={step}>
      <List
        variant="inset"
        rowSeparator="inset"
        label={t(`myArea.onboarding.wizard.stepTitles.${onboardingStepTypeKey(step.stepType)}`)}
      >
        {documents.map((document) => {
          const draft = draftByKey.get(document.configKey);
          const isUpload = document.fulfillmentMode === 'SIGNED_UPLOAD';
          const uploaded = draft?.uploaded === true;
          const confirmed = draft?.confirmed === true;
          const declined = draft?.declined === true;
          const fromThisInvitation = isUpload ? uploaded : confirmed;
          const subtitle = declined
            ? t('myArea.onboarding.wizard.entryDocuments.declined')
            : fromThisInvitation
              ? isUpload
                ? (draft?.fileName ?? t('myArea.onboarding.wizard.fulfilled'))
                : t('myArea.onboarding.wizard.entryDocuments.agreed')
              : document.alreadyFulfilled
                ? t('myArea.onboarding.wizard.entryDocuments.alreadyFulfilled')
                : t('myArea.onboarding.wizard.unfulfilled');

          const viewableUpload = isUpload && uploaded;
          const viewableTemplate =
            !isUpload && document.hasTemplate && (confirmed || document.alreadyFulfilled);
          const viewKey =
            viewableUpload || viewableTemplate
              ? proofKey(step.stepType, document.configKey, 0)
              : null;
          const viewFileName = viewableUpload
            ? (draft?.fileName ?? `${document.name}.pdf`)
            : viewableTemplate
              ? (document.templateFileName ?? `${document.name}.pdf`)
              : null;
          const promotedUserDocumentId = document.promotedUserDocumentId ?? undefined;
          const canViewApprovedUpload =
            approvedContext != null && viewableUpload && promotedUserDocumentId != null;
          const canViewDraftUpload = approvedContext == null && viewableUpload;
          const canViewTemplate = viewableTemplate;
          const showViewAction =
            viewKey &&
            viewFileName &&
            (canViewApprovedUpload || canViewDraftUpload || canViewTemplate);

          return (
            <ListRow
              key={document.configKey}
              compact
              keepActionsOnMobile
              title={document.name}
              subtitle={subtitle}
              badges={
                !document.mandatory ? (
                  <Badge tone="neutral" pill>
                    {t('admin.tenantStandards.onboardingDocuments.optionalBadge')}
                  </Badge>
                ) : undefined
              }
              actions={
                showViewAction ? (
                  <Button
                    type="button"
                    variant="ghost"
                    size="xs"
                    className="h-7 w-7 min-w-7 shrink-0 p-0"
                    isLoading={viewingKey === viewKey}
                    onClick={() =>
                      void onView(viewKey, (targetWindow) => {
                        if (canViewApprovedUpload && promotedUserDocumentId != null) {
                          return viewOnboardingPromotedUserDocument(
                            approvedContext.promotedScope,
                            promotedUserDocumentId,
                            viewFileName,
                            targetWindow,
                          );
                        }
                        if (viewableUpload) {
                          return documentScope === 'organization'
                            ? onboardingInvitationApi.viewOrganizationSignedDocument(
                                access,
                                document.configKey,
                                viewFileName,
                                targetWindow,
                              )
                            : onboardingInvitationApi.viewSignedDocument(
                                access,
                                document.configKey,
                                viewFileName,
                                targetWindow,
                              );
                        }
                        return documentScope === 'organization'
                          ? onboardingInvitationApi.viewOrganizationDocumentTemplate(
                              access,
                              document.configKey,
                              viewFileName,
                              targetWindow,
                            )
                          : onboardingInvitationApi.viewDocumentTemplate(
                              access,
                              document.configKey,
                              viewFileName,
                              targetWindow,
                            );
                      })
                    }
                    aria-label={t('myArea.workflowDetail.viewAttachment')}
                    title={t('myArea.workflowDetail.viewAttachment')}
                  >
                    <Eye className="h-3.5 w-3.5" aria-hidden />
                  </Button>
                ) : undefined
              }
            />
          );
        })}
      </List>
    </StepShell>
  );
}

function RecordDocuments({
  items,
  viewingKey,
  showWhenEmpty = false,
}: {
  items: OnboardingProofItem[];
  viewingKey: string | null;
  showWhenEmpty?: boolean;
}) {
  if (items.length === 0 && !showWhenEmpty) return null;
  return (
    <div className="border-t border-[var(--border-color)]/30 pt-3">
      <OnboardingProofList items={items} viewingKey={viewingKey} showWhenEmpty={showWhenEmpty} />
    </div>
  );
}

function recordProofItems(
  fileNames: string[],
  build: (fileName: string, fileIndex: number) => OnboardingProofItem,
): OnboardingProofItem[] {
  return fileNames.map((fileName, fileIndex) => build(fileName, fileIndex));
}

function proofKey(stepType: OnboardingStepType, id: string, index: number): string {
  return `${stepType}:${id}:${index}`;
}
