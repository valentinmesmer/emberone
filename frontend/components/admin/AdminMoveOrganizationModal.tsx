/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useEffect, useMemo, useState } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { useTranslation } from '@/hooks/useTranslation';
import { useAdminOrganizationTree } from '@/lib/store';
import {
  collectAdminOrgSubtreeIds,
  flattenAdminOrgTreeForParentPicker,
} from '@/lib/utils/adminOrgTree';

export interface AdminMoveOrganizationModalProps {
  isOpen: boolean;
  onClose: () => void;
  tenantId: number;
  organizationId: number;
  organizationName: string;
  isLoading?: boolean;
  onMove: (newParentOrganizationId: number | null) => void | Promise<void>;
}

const ROOT_VALUE = '__root__';

export function AdminMoveOrganizationModal({
  isOpen,
  onClose,
  tenantId,
  organizationId,
  organizationName,
  isLoading = false,
  onMove,
}: AdminMoveOrganizationModalProps) {
  const t = useTranslation();
  const [parentValue, setParentValue] = useState<string>(ROOT_VALUE);
  const {
    data: tree,
    isPending: loadingTree,
    isError: loadFailed,
  } = useAdminOrganizationTree(tenantId, isOpen);
  const treeRoots = useMemo(() => tree?.roots ?? [], [tree]);

  useEffect(() => {
    if (isOpen) setParentValue(ROOT_VALUE);
  }, [isOpen]);

  const excludedIds = useMemo(
    () => collectAdminOrgSubtreeIds(treeRoots, organizationId),
    [treeRoots, organizationId],
  );

  const parentOptions = useMemo(() => {
    const candidates = flattenAdminOrgTreeForParentPicker(treeRoots, excludedIds);
    return [
      { value: ROOT_VALUE, label: t('admin.orgMove.rootOption') },
      ...candidates.map((org) => ({
        value: String(org.id),
        label: `${org.labelPrefix}${org.name} (${org.organizationCode})`,
      })),
    ];
  }, [treeRoots, excludedIds, t]);

  const blockers = loadingTree || loadFailed ? [t('admin.orgMove.parentLabel')] : [];

  const handleSave = () => {
    const newParent = parentValue === ROOT_VALUE ? null : Number.parseInt(parentValue, 10);
    if (newParent !== null && !Number.isFinite(newParent)) return;
    void onMove(newParent);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t('admin.orgMove.modalTitle')}
      size="sm"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={handleSave}
          isLoading={isLoading}
          blockers={blockers}
          cancelText={t('common.cancel')}
          saveText={t('admin.orgMove.confirmButton')}
        />
      }
    >
      <div className="space-y-3 py-2">
        <p className="text-sm text-[var(--text-secondary)]">
          {t('admin.orgMove.modalMessage', { name: organizationName })}
        </p>
        {loadingTree ? (
          <LoadingState variant="embedded" size="sm" />
        ) : loadFailed ? (
          <p className="text-sm text-[var(--text-secondary)]">
            {t('admin.errors.orgTreeLoadFailed')}
          </p>
        ) : (
          <Select
            label={t('admin.orgMove.parentLabel')}
            value={parentValue}
            onChange={(event) => setParentValue(event.target.value)}
            options={parentOptions}
            disabled={isLoading}
          />
        )}
      </div>
    </Modal>
  );
}
