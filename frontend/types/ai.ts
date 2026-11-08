/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

/** Mirrors backend `com.emberone.platform.ai` DTOs (optional AI assistance). */

/** Proof types (workflow type names) that support extraction from a document. */
export type AiProofType =
  | 'CERTIFICATION_RECORD'
  | 'TRAINING_COURSE_RECORD'
  | 'DRIVING_LICENSE_RECORD'
  | 'USER_DOCUMENT_SUBMISSION'
  | 'ADDRESS_CHANGE'
  | 'NAME_CHANGE'
  | 'RANK_REQUEST';

/** Admin form: the proof is read on behalf of a member of the organization. */
export interface AiExtractionTarget {
  organizationId: number;
  userId: number;
}

export interface AiProofTypeCapabilityDto {
  available: boolean;
}

export interface AiCapabilitiesDto {
  enabled: boolean;
  helpChat: { available: boolean };
  documentExtraction: {
    available: boolean;
    types: Partial<Record<AiProofType, AiProofTypeCapabilityDto>>;
  };
}

export const HELP_CHAT_MAX_MESSAGES = 10;
export const HELP_CHAT_MAX_MESSAGE_LENGTH = 2000;

export interface HelpChatMessageDto {
  role: 'user' | 'assistant';
  content: string;
}

export type HelpTopicArea = 'SELF' | 'ORGANIZATION' | 'ADMIN';

export interface HelpChatOrganizationDto {
  id: number;
  name: string;
}

/**
 * Validated help chat suggestion. `WORKFLOW` starts a request; `TOPIC` opens a page (optionally its
 * create dialog) in one of `organizations` (organization area) or the admin tenant `tenantId`.
 */
export interface HelpChatActionDto {
  kind: 'WORKFLOW' | 'TOPIC';
  workflowTypeId: number | null;
  workflowTypeKey: string | null;
  configKey: string | null;
  /** Id of the catalog entry (certification definition, course type, license class). */
  catalogId: number | null;
  label: string | null;
  topicKey: string | null;
  area: HelpTopicArea | null;
  create: boolean;
  organizations: HelpChatOrganizationDto[];
  tenantId: number | null;
}

export interface HelpChatResponseDto {
  text: string;
  actions: HelpChatActionDto[];
}

export interface ExtractionHintDto {
  code: string;
  field: string | null;
}

export interface DocumentExtractionResultDto {
  /** Validated suggestions keyed by the form field names of the target request. */
  fields: Record<string, unknown>;
  hints: ExtractionHintDto[];
}
