/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { CourseOfferingTemplateList } from '@/components/organization/course-offerings/CourseOfferingTemplateList';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Modal } from '@/components/ui/modal';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useDeleteCourseOffering } from '@/lib/store';
import { getOrganizationCourseOfferingDetailPath } from '@/lib/utils';
import type { CourseOfferingDto } from '@/types/courseOffering';

interface Props {
  organizationId: number;
  canWrite: boolean;
  onClose: () => void;
}

/** Templates of the organization; opening one leads to its detail page, where it is edited. */
export function CourseOfferingTemplatesModal({ organizationId, canWrite, onClose }: Props) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const deleteTemplate = useDeleteCourseOffering(organizationId);
  const [deleteTarget, setDeleteTarget] = useState<CourseOfferingDto | null>(null);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteTemplate.mutateAsync(deleteTarget.id);
      showSuccess(t('courseOfferings.templates.deleted'));
      setDeleteTarget(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.templates.deleteFailed'));
    }
  };

  return (
    <>
      <Modal isOpen onClose={onClose} title={t('courseOfferings.templates.title')} size="lg">
        <CourseOfferingTemplateList
          organizationId={organizationId}
          onSelect={(template) =>
            router.push(getOrganizationCourseOfferingDetailPath(organizationId, template.id))
          }
          onDelete={canWrite ? setDeleteTarget : undefined}
        />
      </Modal>
      <ConfirmDialog
        isOpen={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void handleDelete()}
        title={t('courseOfferings.templates.deleteTitle')}
        message={t('courseOfferings.templates.deleteConfirm')}
        confirmText={t('common.delete')}
        variant="danger"
        isLoading={deleteTemplate.isPending}
      />
    </>
  );
}
