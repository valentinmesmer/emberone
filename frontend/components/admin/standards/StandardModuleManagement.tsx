/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { useTranslation } from '@/hooks/useTranslation';
import { type Module, TOGGLEABLE_MODULES } from '@/types/permission';

interface Props {
  state: StandardContentState;
  embedded?: boolean;
}

const SAVE_DEBOUNCE_MS = 400;

function setsEqual(a: Set<Module>, b: Set<Module>): boolean {
  if (a.size !== b.size) return false;
  for (const key of a) {
    if (!b.has(key)) return false;
  }
  return true;
}

export function StandardModuleManagement({ state, embedded = false }: Props) {
  const t = useTranslation();
  const { editable, enabledModules, updateEnabledModules } = state;

  const [selected, setSelected] = useState<Set<Module>>(enabledModules);
  const selectedRef = useRef(selected);
  const lastSavedRef = useRef<Set<Module>>(enabledModules);
  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    selectedRef.current = selected;
  }, [selected]);

  useEffect(() => {
    if (setsEqual(enabledModules, lastSavedRef.current)) return;
    setSelected(new Set(enabledModules));
    lastSavedRef.current = new Set(enabledModules);
  }, [enabledModules]);

  const flushSave = useCallback(async () => {
    const toSave = selectedRef.current;
    if (setsEqual(toSave, lastSavedRef.current)) return;

    const ok = await updateEnabledModules(toSave);
    if (ok) {
      lastSavedRef.current = new Set(toSave);
    } else {
      setSelected(new Set(lastSavedRef.current));
    }
  }, [updateEnabledModules]);

  const scheduleSave = useCallback(() => {
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    saveTimeoutRef.current = setTimeout(() => {
      saveTimeoutRef.current = null;
      void flushSave();
    }, SAVE_DEBOUNCE_MS);
  }, [flushSave]);

  useEffect(() => {
    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
        void flushSave();
      }
    };
  }, [flushSave]);

  const toggle = (module: Module) => {
    if (!editable) return;

    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(module)) next.delete(module);
      else next.add(module);
      return next;
    });
    scheduleSave();
  };

  return (
    <OrganizationManageSection
      embedded={embedded}
      title={t('admin.standards.modules.title')}
      subtitle={t('admin.standards.modules.hint')}
    >
      <StandardCatalogCheckboxList
        listLabel={t('admin.standards.modules.title')}
        idPrefix="standard-module"
        items={TOGGLEABLE_MODULES.map((module) => ({
          id: module,
          label: t(`admin.standards.modules.names.${module}`),
        }))}
        selected={selected}
        onToggle={(module) => toggle(module as Module)}
        disabled={!editable}
      />
    </OrganizationManageSection>
  );
}
