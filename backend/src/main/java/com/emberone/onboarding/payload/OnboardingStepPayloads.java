/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.payload;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OnboardingStepPayloads {

    public static final String CONTACTS = "contacts";
    public static final String DOCUMENTS = "documents";
    public static final String RECORDS = "records";
    public static final String LICENSE = "license";
    public static final String RANK = "rank";
    public static final String TENANT_RANK_ID = "tenantRankId";
    public static final String NONE_DECLARED = "noneDeclared";

    public static final String RELATIONSHIP = "relationship";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String STREET = "street";
    public static final String CITY = "city";
    public static final String POSTAL_CODE = "postalCode";
    public static final String COUNTRY = "country";
    public static final String PHONE = "phone";
    public static final String EMAIL = "email";

    public static final String CONFIG_KEY = "configKey";
    public static final String DOCUMENT_VERSION = "documentVersion";
    public static final String OPENED_AT = "openedAt";
    public static final String CONFIRMATION_TEXT = "confirmationText";
    public static final String CONFIRMED_AT = "confirmedAt";
    public static final String ADMIN_RECORD_REASON = "adminRecordReason";
    public static final String CONFIRMED = "confirmed";
    public static final String DECLINED = "declined";
    public static final String FILE_NAME = "fileName";
    public static final String FILE_NAMES = "fileNames";
    public static final String ATTACHMENTS = "attachments";
    public static final String STORAGE_KEY = "storageKey";
    public static final String CONTENT_TYPE = "contentType";
    public static final String SIZE_BYTES = "sizeBytes";
    public static final String UPLOADED = "uploaded";

    public static final String TENANT_TRAINING_COURSE_TYPE_IDS = "tenantTrainingCourseTypeIds";
    public static final String NAME = "name";
    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String LOCATION = "location";
    public static final String HOURS = "hours";
    public static final String NOTES = "notes";

    public static final String RECORD_KEY = "recordKey";

    public static final String TENANT_CERT_DEFINITION_ID = "tenantCertDefinitionId";
    public static final String COMPLETED_DATE = "completedDate";
    public static final String EXPIRY_DATE = "expiryDate";

    public static final String ISSUE_DATE = "issueDate";
    public static final String DOCUMENT_EXPIRATION_DATE = "documentExpirationDate";
    public static final String LICENSE_NUMBER = "licenseNumber";
    public static final String ISSUING_AUTHORITY = "issuingAuthority";
    public static final String CLASSES = "classes";
    public static final String TENANT_DRIVING_LICENSE_DEFINITION_ID =
            "tenantDrivingLicenseDefinitionId";
    public static final String GRANT_DATE = "grantDate";
    public static final String EXPIRATION_DATE = "expirationDate";

    public static final int MAX_ATTACHMENTS = 10;

    private OnboardingStepPayloads() {}

    public static Map<String, Object> emptyPayload() {
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> readObjectList(
            Map<String, Object> payload, String key) {
        if (payload == null) {
            return List.of();
        }
        Object value = payload.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readObject(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return null;
    }

    public static String readString(Map<String, Object> map, String key) {
        if (map == null) {
            return "";
        }
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static String readNullableString(Map<String, Object> map, String key) {
        String value = readString(map, key);
        return value.isBlank() ? null : value;
    }

    public static boolean readBoolean(Map<String, Object> map, String key) {
        if (map == null) {
            return false;
        }
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return false;
    }

    public static Integer readInteger(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text.trim());
        }
        return null;
    }

    public static Instant readInstant(Map<String, Object> map, String key) {
        String value = readNullableString(map, key);
        if (value == null) {
            return null;
        }
        return Instant.parse(value);
    }

    public static Long readLong(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return null;
    }

    public static List<Long> readLongList(Map<String, Object> map, String key) {
        if (map == null) {
            return List.of();
        }
        Object value = map.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number number) {
                result.add(number.longValue());
            } else if (item instanceof String text && !text.isBlank()) {
                result.add(Long.parseLong(text.trim()));
            }
        }
        return result;
    }

    public static boolean hasKey(Map<String, Object> payload, String key) {
        return payload != null && payload.containsKey(key);
    }

    public static List<Map<String, Object>> readAttachments(Map<String, Object> map) {
        return readObjectList(map, ATTACHMENTS);
    }

    public static List<String> readAttachmentFileNames(Map<String, Object> map) {
        if (map == null) {
            return List.of();
        }
        Object fileNamesValue = map.get(FILE_NAMES);
        if (fileNamesValue instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String text && !text.isBlank()) {
                    result.add(text);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        List<String> fromAttachments = new ArrayList<>();
        for (Map<String, Object> attachment : readAttachments(map)) {
            String fileName = readNullableString(attachment, FILE_NAME);
            if (fileName != null) {
                fromAttachments.add(fileName);
            }
        }
        return fromAttachments;
    }
}
