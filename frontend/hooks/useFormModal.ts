/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useState, useCallback } from 'react';

/**
 * Custom hook for managing form state in modals
 * Handles form initialization and reset based on modal open state
 *
 * @param initialData - Initial form data or a function that returns it
 * @param isOpen - Whether the modal is open
 * @param resetOnOpen - Whether to reset form when modal opens (default: true)
 * @param dependencies - Optional array of dependencies that should trigger form reset when changed
 */
export function useFormModal<T>(
  initialData: T | (() => T),
  isOpen: boolean,
  resetOnOpen: boolean = true,
  dependencies: unknown[] = [],
) {
  const getInitialData = useCallback(() => {
    return typeof initialData === 'function' ? (initialData as () => T)() : initialData;
  }, [initialData]);

  const [formData, setFormData] = useState<T>(getInitialData);
  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);
  const depsKey = JSON.stringify(dependencies);
  const [prevDepsKey, setPrevDepsKey] = useState(depsKey);

  if (isOpen !== prevIsOpen) {
    setPrevIsOpen(isOpen);
    if (isOpen && resetOnOpen) {
      setFormData(getInitialData());
    }
  } else if (isOpen && depsKey !== prevDepsKey) {
    setPrevDepsKey(depsKey);
    setFormData(getInitialData());
  }

  const reset = useCallback(() => {
    setFormData(getInitialData());
  }, [getInitialData]);

  return { formData, setFormData, reset };
}
