/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    private String defaultRegion;
    private boolean allowEmpty;

    private static boolean isValidParsed(Phonenumber.PhoneNumber parsedNumber) {
        return phoneUtil.isValidNumber(parsedNumber);
    }

    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        this.defaultRegion = constraintAnnotation.defaultRegion();
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return allowEmpty;
        }

        String trimmed = phoneNumber.trim();

        try {
            if (trimmed.startsWith("+")) {
                return isValidParsed(phoneUtil.parse(trimmed, null));
            }

            if (defaultRegion != null && !defaultRegion.isEmpty()) {
                if (isValidParsed(phoneUtil.parse(trimmed, defaultRegion))) {
                    return true;
                }
                String digitsOnly = trimmed.replaceAll("\\D", "");
                if (!digitsOnly.isEmpty()) {
                    return isValidParsed(phoneUtil.parse("+" + digitsOnly, null));
                }
                return false;
            }

            String digitsOnly = trimmed.replaceAll("\\D", "");
            if (digitsOnly.isEmpty()) {
                return false;
            }
            return isValidParsed(phoneUtil.parse("+" + digitsOnly, null));
        } catch (NumberParseException e) {
            return false;
        }
    }
}
