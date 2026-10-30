/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { AsYouType, type CountryCode, parsePhoneNumber } from 'libphonenumber-js/core';
import metadata from 'libphonenumber-js/max/metadata';
import type React from 'react';
import { useState } from 'react';
import { HoverHint } from '@/components/ui/hover-hint';
import { useFieldHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';
import { DEFAULT_PHONE_REGION } from '@/lib/validation/constants';
import { isValidOptionalPhone } from '@/lib/validation/phone';

function validatePhoneValue(
  phoneValue: string,
  validate: boolean,
  defaultCountry: CountryCode,
  t: (key: string) => string,
): { isValid: boolean; validationError: string | null } {
  if (!phoneValue || !validate) {
    return { isValid: true, validationError: null };
  }

  if (!isValidOptionalPhone(phoneValue, defaultCountry)) {
    return { isValid: false, validationError: t('common.validation.invalidPhoneNumber') };
  }
  return { isValid: true, validationError: null };
}

export interface PhoneInputProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'type' | 'value' | 'onChange'> {
  label?: string;
  error?: string;
  value?: string;
  onChange?: (value: string) => void;
  defaultCountry?: CountryCode;
  /**
   * Whether to validate the phone number format
   */
  validate?: boolean;
}

export function PhoneInput({
  label,
  error,
  className = '',
  value = '',
  onChange,
  defaultCountry = DEFAULT_PHONE_REGION,
  validate = true,
  required,
  disabled,
  title,
  ...props
}: PhoneInputProps) {
  const t = useTranslation();
  const hoverTitle = useFieldHoverTitle(disabled, title);
  const [displayValue, setDisplayValue] = useState(value);
  const [prevValue, setPrevValue] = useState(value);
  const initialValidation = validatePhoneValue(value, validate, defaultCountry, t);
  const [isValid, setIsValid] = useState(initialValidation.isValid);
  const [validationError, setValidationError] = useState<string | null>(
    initialValidation.validationError,
  );
  const inputId = props.id || props.name;

  if (value !== prevValue) {
    setPrevValue(value);
    setDisplayValue(value);
    const nextValidation = validatePhoneValue(value, validate, defaultCountry, t);
    setIsValid(nextValidation.isValid);
    setValidationError(nextValidation.validationError);
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const inputValue = e.target.value;

    // Format as user types - use default country for formatting, but allow any country code
    // For international numbers starting with +, libphonenumber will auto-detect the country
    const formatter = new AsYouType(defaultCountry, metadata);
    const formatted = formatter.input(inputValue);
    setDisplayValue(formatted);

    // Extract the phone number without formatting for storage
    try {
      if (inputValue.trim()) {
        let phoneNumber: ReturnType<typeof parsePhoneNumber> | undefined;
        if (inputValue.startsWith('+')) {
          // Try to parse as international number (any country)
          try {
            phoneNumber = parsePhoneNumber(inputValue, metadata);
          } catch {
            phoneNumber = parsePhoneNumber(inputValue, defaultCountry, metadata);
          }
        } else {
          phoneNumber = parsePhoneNumber(inputValue, defaultCountry, metadata);
        }

        if (phoneNumber && isValidOptionalPhone(phoneNumber.number, defaultCountry)) {
          onChange?.(phoneNumber.number);
          setIsValid(true);
          setValidationError(null);
        } else {
          // If parsing with default country fails, try as international
          if (!inputValue.startsWith('+')) {
            try {
              const cleaned = inputValue.replace(/\D/g, '');
              const intlNumber = parsePhoneNumber(`+${cleaned}`, metadata);
              if (intlNumber && isValidOptionalPhone(intlNumber.number, defaultCountry)) {
                onChange?.(intlNumber.number);
                setIsValid(true);
                setValidationError(null);
                return;
              }
            } catch {
              // Continue with original input
            }
          }
          onChange?.(inputValue);
          if (validate) {
            setIsValid(false);
            setValidationError(t('common.validation.invalidPhoneNumber'));
          }
        }
      } else {
        onChange?.('');
        setIsValid(true);
        setValidationError(null);
      }
    } catch {
      onChange?.(inputValue);
      if (validate && inputValue.trim().length > 0) {
        setIsValid(false);
        setValidationError(t('common.validation.invalidPhoneNumber'));
      } else {
        setIsValid(true);
        setValidationError(null);
      }
    }
  };

  const displayError = error || validationError;
  const hasError = !isValid || !!error;

  return (
    <div className="w-full space-y-2">
      {label && (
        <label htmlFor={inputId} className="block text-sm font-semibold text-[var(--text-primary)]">
          {label}
          {required && <span className="text-[var(--fire-red)] ml-1">*</span>}
        </label>
      )}
      <HoverHint
        hint={hoverTitle}
        capturePointer={!!disabled}
        className="block w-full"
        shape="rounded-xl"
      >
        <input
          id={inputId}
          type="tel"
          value={displayValue}
          onChange={handleChange}
          disabled={disabled}
          className={cn(
            `
          w-full px-4 py-3 rounded-xl border-2
          bg-[var(--card-bg)] text-[var(--text-primary)]
          border-[var(--border-color)]
          focus:border-[var(--fire-red)] focus:outline-none focus:ring-4 focus:ring-[var(--fire-red)]/10
          transition-all duration-300
          placeholder:text-[var(--text-secondary)]
          ${hasError ? 'border-red-500 focus:ring-red-500/10' : ''}
        `,
            className,
          )}
          required={required}
          {...props}
          title={undefined}
        />
      </HoverHint>
      {displayError && (
        <p className="text-sm text-red-600 dark:text-red-400 font-medium">{displayError}</p>
      )}
    </div>
  );
}
