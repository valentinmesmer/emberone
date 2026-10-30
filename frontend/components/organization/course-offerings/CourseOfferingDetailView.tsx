/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Ban, BookmarkPlus, CircleCheck, Copy, Pencil, Send, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { CourseOfferingAudienceEditor } from '@/components/organization/course-offerings/CourseOfferingAudienceEditor';
import { CourseOfferingCancelDialog } from '@/components/organization/course-offerings/CourseOfferingCancelDialog';
import { CourseOfferingParticipantsSection } from '@/components/organization/course-offerings/CourseOfferingParticipantsSection';
import { CourseOfferingPrerequisitesEditor } from '@/components/organization/course-offerings/CourseOfferingPrerequisitesEditor';
import { CourseOfferingSessionsSection } from '@/components/organization/course-offerings/CourseOfferingSessionsSection';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DateTimePicker } from '@/components/ui/date-time-picker';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { Input } from '@/components/ui/input';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { SwitchField } from '@/components/ui/switch';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useCompleteCourseOffering,
  useCourseOfferingCompletion,
  useDeleteCourseOffering,
  useInstantiateCourseOfferingTemplate,
  useOfferingRegistrationSummary,
  useOrganizationById,
  useOrganizationCourseOffering,
  usePublishCourseOffering,
  useSaveCourseOfferingAsTemplate,
  useUpdateCourseOffering,
} from '@/lib/store';
import { validateRequiredFields } from '@/lib/utils';
import {
  canCancelCourseOffering,
  canDeleteCourseOffering,
  canEditCourseOffering,
  canPublishCourseOffering,
  courseOfferingCompleteBlockReason,
  courseOfferingCompleteBlockReasonLabelKey,
  courseOfferingPublishBlockReason,
  courseOfferingPublishBlockReasonLabelKey,
  courseOfferingStatusLabelKey,
  getOrganizationCourseOfferingsPath,
  isCourseOfferingTemplate,
} from '@/lib/utils/courseOffering';
import { formatDateTime } from '@/lib/utils/date';
import type { UpdateCourseOfferingRequest } from '@/types/courseOffering';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
  offeringId: number;
}

export function CourseOfferingDetailView({ organizationId, offeringId }: Props) {
  const t = useTranslation();
  const router = useRouter();
  const { showSuccess, showError } = useToast();
  const { hasPermission } = usePermissions();

  const canRead = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_READ);
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_WRITE);

  const { data: organization, isLoading: orgLoading } = useOrganizationById(
    canRead ? organizationId : null,
  );
  const {
    data: offering,
    isLoading: offeringLoading,
    isError,
  } = useOrganizationCourseOffering(canRead ? organizationId : null, offeringId, canRead);
  const { data: completion } = useCourseOfferingCompletion(
    organizationId,
    offeringId,
    canRead && offering?.status === 'PUBLISHED',
  );

  const { data: summary } = useOfferingRegistrationSummary(
    organizationId,
    offeringId,
    canRead && offering?.status === 'PUBLISHED',
  );

  const updateOffering = useUpdateCourseOffering(organizationId, offeringId);
  const deleteOffering = useDeleteCourseOffering(organizationId);
  const publishOffering = usePublishCourseOffering(organizationId, offeringId);
  const saveAsTemplate = useSaveCourseOfferingAsTemplate(organizationId, offeringId);
  const instantiateTemplate = useInstantiateCourseOfferingTemplate(organizationId);
  const completeOffering = useCompleteCourseOffering(organizationId, offeringId);

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);
  const [completeOpen, setCompleteOpen] = useState(false);
  const [publishOpen, setPublishOpen] = useState(false);
  const [templateNameOpen, setTemplateNameOpen] = useState(false);
  const [instantiateOpen, setInstantiateOpen] = useState(false);
  const [templateName, setTemplateName] = useState('');
  const [firstSessionDate, setFirstSessionDate] = useState('');

  const [form, setForm] = useState<UpdateCourseOfferingRequest>({
    content: '',
    notes: '',
    maxParticipants: 20,
    hasExam: false,
    requiresHomeOrgApproval: false,
    registrationStart: null,
    registrationEnd: null,
    templateName: null,
  });

  if (orgLoading || offeringLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!canRead) {
    return <p className="text-[var(--text-secondary)]">{t('organization.manage.noPermission')}</p>;
  }

  if (!organization || isError || !offering) {
    return <ErrorState message={t('courseOfferings.detail.notFound')} className="mt-4" />;
  }

  const editable = canWrite && canEditCourseOffering(offering.status);
  const isTemplate = isCourseOfferingTemplate(offering.status);
  const publishBlock = courseOfferingPublishBlockReason(offering);
  const publishReady = publishBlock == null;
  const canComplete = canWrite && offering.status === 'PUBLISHED';
  const undecidedCount =
    completion?.participants.filter((participant) => participant.finalPassed == null).length ?? 0;
  const completeBlock = courseOfferingCompleteBlockReason(
    offering,
    undecidedCount,
    completion != null,
  );
  const completeBlocked = completeBlock != null;

  const openEdit = () => {
    setForm({
      content: offering.content,
      notes: offering.notes,
      templateName: offering.templateName,
      registrationStart: offering.registrationStart,
      registrationEnd: offering.registrationEnd,
      maxParticipants: offering.maxParticipants,
      hasExam: offering.hasExam,
      requiresHomeOrgApproval: offering.requiresHomeOrgApproval,
    });
    setEditOpen(true);
  };

  const minParticipants = Math.max(1, summary?.participants ?? 0);
  // Quotas sum up to the maximum, so the maximum follows the quotas and is locked while they exist.
  const quotasSet = offering.audiences.some((entry) => entry.quota != null);

  const editBasicsValid = isTemplate
    ? (form.templateName?.trim().length ?? 0) > 0
    : form.maxParticipants >= minParticipants;

  const saveBasics = async () => {
    if (isTemplate) {
      if (
        !validateRequiredFields(
          { templateName: form.templateName ?? '' },
          t,
          showError,
          'common.validation.fillRequired',
        )
      ) {
        return;
      }
    } else if (form.maxParticipants < minParticipants) {
      showError(t('courseOfferings.detail.maxParticipantsMin', { min: minParticipants }));
      return;
    }
    try {
      await updateOffering.mutateAsync(form);
      showSuccess(t('courseOfferings.detail.saved'));
      setEditOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.detail.saveFailed'));
    }
  };

  const handleDelete = async () => {
    try {
      await deleteOffering.mutateAsync(offeringId);
      showSuccess(t('courseOfferings.detail.deleted'));
      router.push(getOrganizationCourseOfferingsPath(organizationId));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.detail.deleteFailed'));
    }
  };

  const handlePublish = async () => {
    try {
      await publishOffering.mutateAsync();
      showSuccess(t('courseOfferings.detail.published'));
      setPublishOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.detail.publishFailed'));
    }
  };

  const handleComplete = async () => {
    try {
      await completeOffering.mutateAsync();
      showSuccess(t('courseOfferings.completion.completeSuccess'));
      setCompleteOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.completion.completeFailed'));
    }
  };

  const handleSaveAsTemplate = async () => {
    if (!validateRequiredFields({ templateName }, t, showError, 'common.validation.fillRequired')) {
      return;
    }
    try {
      await saveAsTemplate.mutateAsync(templateName.trim());
      showSuccess(t('courseOfferings.templates.saved'));
      setTemplateNameOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.templates.saveFailed'));
    }
  };

  const handleInstantiate = async () => {
    if (
      !validateRequiredFields({ firstSessionDate }, t, showError, 'common.validation.fillRequired')
    ) {
      return;
    }
    try {
      const created = await instantiateTemplate.mutateAsync({
        templateId: offeringId,
        firstSessionDate,
      });
      showSuccess(t('courseOfferings.templates.instantiated'));
      setInstantiateOpen(false);
      router.push(`${getOrganizationCourseOfferingsPath(organizationId)}/${created.id}`);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.templates.instantiateFailed'));
    }
  };

  return (
    <div className="space-y-3">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          title={offering.trainingCourseTypeName}
          badges={
            <Badge tone={offering.status === 'PUBLISHED' ? 'success' : 'neutral'}>
              {t(courseOfferingStatusLabelKey(offering.status))}
            </Badge>
          }
          actions={
            editable ? (
              <DetailCardAction
                label={t('common.edit')}
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={openEdit}
              />
            ) : undefined
          }
        />
        <dl className="mt-3 grid grid-cols-1 gap-2 text-sm sm:grid-cols-2">
          {isTemplate && offering.templateName ? (
            <div>
              <dt className="text-[var(--text-secondary)]">
                {t('courseOfferings.fields.templateName')}
              </dt>
              <dd>{offering.templateName}</dd>
            </div>
          ) : null}
          <div>
            <dt className="text-[var(--text-secondary)]">
              {t('courseOfferings.fields.maxParticipants')}
            </dt>
            <dd>{offering.maxParticipants}</dd>
          </div>
          {!isTemplate ? (
            <>
              <div>
                <dt className="text-[var(--text-secondary)]">
                  {t('courseOfferings.fields.registrationStart')}
                </dt>
                <dd>
                  {offering.registrationStart ? formatDateTime(offering.registrationStart) : '-'}
                </dd>
              </div>
              <div>
                <dt className="text-[var(--text-secondary)]">
                  {t('courseOfferings.fields.registrationEnd')}
                </dt>
                <dd>{offering.registrationEnd ? formatDateTime(offering.registrationEnd) : '-'}</dd>
              </div>
            </>
          ) : null}
          {offering.content ? (
            <div className="sm:col-span-2">
              <dt className="text-[var(--text-secondary)]">
                {t('courseOfferings.fields.content')}
              </dt>
              <dd>{offering.content}</dd>
            </div>
          ) : null}
          {offering.notes ? (
            <div className="sm:col-span-2">
              <dt className="text-[var(--text-secondary)]">{t('courseOfferings.fields.notes')}</dt>
              <dd>{offering.notes}</dd>
            </div>
          ) : null}
        </dl>
      </Card>

      <CourseOfferingSessionsSection
        organizationId={organizationId}
        offering={offering}
        canWrite={canWrite}
      />
      <CourseOfferingAudienceEditor
        organizationId={organizationId}
        offering={offering}
        canWrite={canWrite}
      />
      <CourseOfferingPrerequisitesEditor organizationId={organizationId} offering={offering} />

      {!isTemplate &&
      (offering.status === 'PUBLISHED' ||
        offering.status === 'COMPLETED' ||
        offering.status === 'CANCELLED') ? (
        <CourseOfferingParticipantsSection
          organizationId={organizationId}
          offeringId={offeringId}
        />
      ) : null}

      {canWrite ? (
        <ActionsSection title={t('courseOfferings.actions.title')}>
          {isTemplate ? (
            <ListRow
              leading={<ActionRowIcon icon={Copy} />}
              title={t('courseOfferings.templates.instantiate')}
              subtitle={t('courseOfferings.templates.instantiateHint')}
              onClick={() => setInstantiateOpen(true)}
            />
          ) : null}
          {!isTemplate && editable ? (
            <ListRow
              leading={<ActionRowIcon icon={BookmarkPlus} />}
              title={t('courseOfferings.templates.saveAsTemplate')}
              subtitle={t('courseOfferings.templates.saveAsTemplateHint')}
              onClick={() => {
                setTemplateName('');
                setTemplateNameOpen(true);
              }}
            />
          ) : null}
          {canPublishCourseOffering(offering.status) ? (
            <ListRow
              leading={<ActionRowIcon icon={Send} />}
              title={t('courseOfferings.actions.publish')}
              subtitle={
                publishReady
                  ? t('courseOfferings.actions.publishHint')
                  : t(courseOfferingPublishBlockReasonLabelKey(publishBlock))
              }
              onClick={publishReady ? () => setPublishOpen(true) : undefined}
              className={publishReady ? undefined : 'opacity-60'}
            />
          ) : null}
          {canComplete ? (
            <ListRow
              leading={<ActionRowIcon icon={CircleCheck} />}
              title={t('courseOfferings.completion.completeAction')}
              subtitle={
                completeBlock != null
                  ? t(courseOfferingCompleteBlockReasonLabelKey(completeBlock))
                  : t('courseOfferings.completion.completeHint')
              }
              onClick={completeBlocked ? undefined : () => setCompleteOpen(true)}
              className={completeBlocked ? 'opacity-60' : undefined}
            />
          ) : null}
          {canCancelCourseOffering(offering.status) ? (
            <ListRow
              leading={<ActionRowIcon icon={Ban} />}
              title={t('courseOfferings.cancel.action')}
              subtitle={t('courseOfferings.cancel.actionHint')}
              onClick={() => setCancelOpen(true)}
            />
          ) : null}
          {canDeleteCourseOffering(offering.status) ? (
            <ListRow
              leading={<ActionRowIcon icon={Trash2} />}
              title={t('courseOfferings.actions.delete')}
              subtitle={t('courseOfferings.actions.deleteHint')}
              onClick={() => setDeleteOpen(true)}
            />
          ) : null}
        </ActionsSection>
      ) : null}

      <CourseOfferingCancelDialog
        organizationId={organizationId}
        offeringId={offeringId}
        isOpen={cancelOpen}
        onClose={() => setCancelOpen(false)}
      />

      {editOpen ? (
        <Modal
          isOpen
          onClose={() => setEditOpen(false)}
          title={t('courseOfferings.detail.editTitle')}
          footer={
            <ModalFooter
              onCancel={() => setEditOpen(false)}
              onSave={() => void saveBasics()}
              isValid={editBasicsValid}
              isLoading={updateOffering.isPending}
            />
          }
        >
          <div className="space-y-4">
            <Textarea
              label={t('courseOfferings.fields.content')}
              value={form.content ?? ''}
              onChange={(e) => setForm((prev) => ({ ...prev, content: e.target.value }))}
            />
            <Textarea
              label={t('courseOfferings.fields.notes')}
              value={form.notes ?? ''}
              onChange={(e) => setForm((prev) => ({ ...prev, notes: e.target.value }))}
            />
            {isTemplate ? (
              <Input
                label={t('courseOfferings.fields.templateName')}
                value={form.templateName ?? ''}
                onChange={(e) => setForm((prev) => ({ ...prev, templateName: e.target.value }))}
                required
              />
            ) : (
              <>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <DateTimePicker
                    label={t('courseOfferings.fields.registrationStart')}
                    name="registrationStart"
                    value={form.registrationStart ?? ''}
                    onChange={(e) =>
                      setForm((prev) => ({ ...prev, registrationStart: e.target.value || null }))
                    }
                  />
                  <DateTimePicker
                    label={t('courseOfferings.fields.registrationEnd')}
                    name="registrationEnd"
                    value={form.registrationEnd ?? ''}
                    onChange={(e) =>
                      setForm((prev) => ({ ...prev, registrationEnd: e.target.value || null }))
                    }
                  />
                </div>
                <Input
                  label={t('courseOfferings.fields.maxParticipants')}
                  type="number"
                  min={minParticipants}
                  disabled={quotasSet}
                  title={
                    quotasSet
                      ? t('courseOfferings.detail.maxParticipantsLockedByQuotas')
                      : undefined
                  }
                  className={quotasSet ? 'opacity-50' : undefined}
                  hint={
                    !quotasSet && minParticipants > 1
                      ? t('courseOfferings.detail.maxParticipantsMin', { min: minParticipants })
                      : undefined
                  }
                  value={String(form.maxParticipants)}
                  onChange={(e) =>
                    setForm((prev) => ({
                      ...prev,
                      maxParticipants: parseInt(e.target.value, 10) || 0,
                    }))
                  }
                  required
                />
                <SwitchField
                  label={t('courseOfferings.fields.hasExam')}
                  checked={form.hasExam ?? false}
                  onCheckedChange={(checked) => setForm((prev) => ({ ...prev, hasExam: checked }))}
                />
                <SwitchField
                  label={t('courseOfferings.fields.requiresHomeOrgApproval')}
                  checked={form.requiresHomeOrgApproval ?? false}
                  onCheckedChange={(checked) =>
                    setForm((prev) => ({ ...prev, requiresHomeOrgApproval: checked }))
                  }
                />
              </>
            )}
          </div>
        </Modal>
      ) : null}

      <ConfirmDialog
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => void handleDelete()}
        title={t('courseOfferings.actions.deleteTitle')}
        message={t('courseOfferings.actions.deleteConfirm')}
        confirmText={t('common.delete')}
        variant="danger"
        isLoading={deleteOffering.isPending}
      />

      <ConfirmDialog
        isOpen={publishOpen}
        onClose={() => setPublishOpen(false)}
        onConfirm={() => void handlePublish()}
        title={t('courseOfferings.actions.publishTitle')}
        message={t('courseOfferings.actions.publishConfirm')}
        confirmText={t('courseOfferings.actions.publish')}
        variant="info"
        isLoading={publishOffering.isPending}
      />

      <ConfirmDialog
        isOpen={completeOpen}
        onClose={() => setCompleteOpen(false)}
        onConfirm={() => void handleComplete()}
        title={t('courseOfferings.completion.completeConfirmTitle')}
        message={t('courseOfferings.completion.completeConfirmMessage')}
        confirmText={t('courseOfferings.completion.completeAction')}
        variant="info"
        isLoading={completeOffering.isPending}
      />

      {templateNameOpen ? (
        <Modal
          isOpen
          onClose={() => setTemplateNameOpen(false)}
          title={t('courseOfferings.templates.saveAsTemplate')}
          footer={
            <ModalFooter
              onCancel={() => setTemplateNameOpen(false)}
              onSave={() => void handleSaveAsTemplate()}
              isValid={templateName.trim().length > 0}
              isLoading={saveAsTemplate.isPending}
            />
          }
        >
          <Input
            label={t('courseOfferings.fields.templateName')}
            value={templateName}
            onChange={(e) => setTemplateName(e.target.value)}
            required
          />
        </Modal>
      ) : null}

      {instantiateOpen ? (
        <Modal
          isOpen
          onClose={() => setInstantiateOpen(false)}
          title={t('courseOfferings.templates.instantiate')}
          footer={
            <ModalFooter
              onCancel={() => setInstantiateOpen(false)}
              onSave={() => void handleInstantiate()}
              isValid={firstSessionDate !== ''}
              isLoading={instantiateTemplate.isPending}
            />
          }
        >
          <Input
            label={t('courseOfferings.templates.firstSessionDate')}
            type="date"
            value={firstSessionDate}
            onChange={(e) => setFirstSessionDate(e.target.value)}
            required
          />
        </Modal>
      ) : null}
    </div>
  );
}
