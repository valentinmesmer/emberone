/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { useTranslation } from '@/hooks/useTranslation';
import { type AdminOrganizationTreeNodeDto, adminApi } from '@/lib/api/admin';
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
  const [treeRoots, setTreeRoots] = useState<AdminOrganizationTreeNodeDto[]>([]);
  const [loadingTree, setLoadingTree] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [parentValue, setParentValue] = useState<string>(ROOT_VALUE);

  const loadTree = useCallback(async () => {
    setLoadingTree(true);
    setLoadFailed(false);
    try {
      const tree = await adminApi.getAdminOrganizationTree(tenantId);
      setTreeRoots(tree.roots);
    } catch {
      setLoadFailed(true);
      setTreeRoots([]);
    } finally {
      setLoadingTree(false);
    }
  }, [tenantId]);

  useEffect(() => {
    if (!isOpen) return;
    setParentValue(ROOT_VALUE);
    void loadTree();
  }, [isOpen, loadTree]);

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
          isValid={!loadingTree && !loadFailed}
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
