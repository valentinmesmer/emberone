/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useRouter } from 'next/navigation';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { getMyAreaWorkflowDetailPath } from '@/lib/my-area/myAreaSections';
import { useOpenCourseOfferingRegistrationWorkflow } from '@/lib/store';
import type { SelfCourseOfferingDto } from '@/types/courseOffering';

interface CourseOfferingRegisterDialogProps {
  offering: SelfCourseOfferingDto;
  onClose: () => void;
}

export function CourseOfferingRegisterDialog({
  offering,
  onClose,
}: CourseOfferingRegisterDialogProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const openRegistration = useOpenCourseOfferingRegistrationWorkflow();

  return (
    <ConfirmDialog
      isOpen
      onClose={onClose}
      title={t('myArea.courseRegistrations.register.title')}
      message={t('myArea.courseRegistrations.register.message', {
        name: offering.trainingCourseTypeName,
      })}
      variant="info"
      confirmText={t('myArea.courseRegistrations.register.action')}
      isLoading={openRegistration.isPending}
      onConfirm={async () => {
        try {
          const workflow = await openRegistration.mutateAsync(offering.id);
          showSuccess(t('myArea.courseRegistrations.register.success'));
          onClose();
          router.push(getMyAreaWorkflowDetailPath(workflow.id));
        } catch {
          showError(t('myArea.courseRegistrations.register.failed'));
        }
      }}
    />
  );
}
