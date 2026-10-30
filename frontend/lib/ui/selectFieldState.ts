/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export type SelectFieldOption = {
  value: string;
  label: string;
  disabled?: boolean;
};

export function isSelectPlaceholderValue(optionValue: string): boolean {
  return optionValue === '' || optionValue === '0';
}

/** Options shown in the open menu (required fields omit clearable placeholders). */
export function getSelectDropdownOptions(
  options: SelectFieldOption[],
  required?: boolean,
): SelectFieldOption[] {
  if (!required) {
    return options;
  }
  return options.filter((opt) => !isSelectPlaceholderValue(opt.value));
}

export function getSelectableSelectOptions(options: SelectFieldOption[]): SelectFieldOption[] {
  return options.filter((opt) => !opt.disabled);
}

function findSelectedSelectOption(
  options: SelectFieldOption[],
  value: string | number | readonly string[] | undefined | null,
): SelectFieldOption | undefined {
  if (value === undefined || value === null || Array.isArray(value)) {
    return options.find((opt) => isSelectPlaceholderValue(opt.value));
  }
  const valueStr = String(value);
  return options.find((opt) => opt.value === valueStr);
}

function hasEmptySelectValue(
  value: string | number | readonly string[] | undefined | null,
): boolean {
  if (value === undefined || value === null || Array.isArray(value)) {
    return true;
  }
  return value === '';
}

export function resolveSelectTriggerLabel(params: {
  options: SelectFieldOption[];
  dropdownOptions: SelectFieldOption[];
  value: string | number | readonly string[] | undefined | null;
  selectOptionLabel: string;
  noOptionsLabel: string;
}): { label: string; isPlaceholder: boolean } {
  const { options, dropdownOptions, value, selectOptionLabel, noOptionsLabel } = params;
  const selectedOption = findSelectedSelectOption(options, value);
  const selectableOptions = getSelectableSelectOptions(dropdownOptions);

  if (selectedOption?.label) {
    const isPlaceholder =
      Boolean(selectedOption.disabled) || isSelectPlaceholderValue(selectedOption.value);
    return { label: selectedOption.label, isPlaceholder };
  }

  if (selectableOptions.length === 0) {
    return { label: noOptionsLabel, isPlaceholder: true };
  }

  if (hasEmptySelectValue(value)) {
    return { label: selectOptionLabel, isPlaceholder: true };
  }

  return { label: '', isPlaceholder: true };
}

export function isSelectInteractionDisabled(
  disabled: boolean | undefined,
  dropdownOptions: SelectFieldOption[],
): boolean {
  if (disabled) {
    return true;
  }
  return getSelectableSelectOptions(dropdownOptions).length === 0;
}
