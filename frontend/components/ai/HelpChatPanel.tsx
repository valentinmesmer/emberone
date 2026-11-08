/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { ArrowUp, MessageCircleQuestion, RotateCcw } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import { WorkflowBeginModalHost } from '@/components/my-area/workflows/WorkflowBeginModalHost';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Modal } from '@/components/ui/modal';
import { useTranslation } from '@/hooks/useTranslation';
import { type HelpChatActionTarget, resolveHelpChatActionTarget } from '@/lib/ai/helpChatActions';
import { useAiCapabilities, useHelpChat, useSelf } from '@/lib/store';
import { workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';
import {
  HELP_CHAT_MAX_MESSAGE_LENGTH,
  HELP_CHAT_MAX_MESSAGES,
  type HelpChatActionDto,
} from '@/types/ai';

const SUGGESTION_KEYS = [
  'g263',
  'absence',
  'timeEntry',
  'drivingLicense',
  'course',
  'emergencyContact',
  'name',
  'notifications',
] as const;
const SUGGESTION_COUNT = 3;

function pickSuggestions() {
  return [...SUGGESTION_KEYS].sort(() => Math.random() - 0.5).slice(0, SUGGESTION_COUNT);
}

interface ChatEntry {
  role: 'user' | 'assistant';
  content: string;
  actions?: HelpChatActionDto[];
}

interface StartRequest {
  workflowTypeId: number;
  catalogId: number | null;
  key: number;
}

/**
 * Floating help assistant on every authenticated page. Visible only when the backend offers the
 * function. The conversation lives in React state only (nothing is stored); answers are labelled as
 * AI output; actions start the matching request or open the matching page through a fixed route
 * map. When a page exists in several organizations, the user picks the organization here.
 */
export function HelpChatPanel() {
  const { data: capabilities } = useAiCapabilities();
  if (!capabilities?.helpChat.available) {
    return null;
  }
  return <HelpChatPanelContent />;
}

function HelpChatPanelContent() {
  const t = useTranslation();
  const router = useRouter();
  const { data: self } = useSelf();
  const chat = useHelpChat();
  const [open, setOpen] = useState(false);
  const [entries, setEntries] = useState<ChatEntry[]>([]);
  const [suggestions, setSuggestions] = useState<readonly string[]>(() =>
    SUGGESTION_KEYS.slice(0, SUGGESTION_COUNT),
  );
  const [draft, setDraft] = useState('');
  const [failed, setFailed] = useState(false);
  const [start, setStart] = useState<StartRequest | null>(null);
  const [startOpen, setStartOpen] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  // biome-ignore lint/correctness/useExhaustiveDependencies: scroll whenever the conversation changes
  useEffect(() => {
    endRef.current?.scrollIntoView?.({ block: 'end' });
  }, [entries, chat.isPending]);

  useEffect(() => {
    if (open && !chat.isPending) inputRef.current?.focus();
  }, [open, chat.isPending]);

  const limitReached = entries.length >= HELP_CHAT_MAX_MESSAGES;
  const canSend = draft.trim().length > 0 && !chat.isPending && !limitReached;

  const send = async (preset?: string) => {
    const text = (preset ?? draft).trim();
    if (!text || chat.isPending || limitReached) return;
    const next: ChatEntry[] = [...entries, { role: 'user', content: text }];
    setEntries(next);
    setDraft('');
    setFailed(false);
    try {
      const response = await chat.mutateAsync(next.map(({ role, content }) => ({ role, content })));
      setEntries([
        ...next,
        {
          role: 'assistant',
          content: response.text,
          actions: response.actions.filter((action) => resolveHelpChatActionTarget(action)),
        },
      ]);
    } catch {
      setFailed(true);
    }
  };

  const reset = () => {
    setEntries([]);
    setDraft('');
    setFailed(false);
  };

  // Every opening starts a fresh conversation with new example questions.
  const openChat = () => {
    reset();
    setSuggestions(pickSuggestions());
    setOpen(true);
  };

  const navigate = (href: string) => {
    setOpen(false);
    router.push(href);
  };

  const runAction = (action: HelpChatActionDto, target: HelpChatActionTarget) => {
    if (target.kind === 'route') {
      navigate(target.href);
      return;
    }
    if (target.kind !== 'modal') return;
    setOpen(false);
    setStart((previous) => ({
      workflowTypeId: target.workflowTypeId,
      catalogId: action.catalogId,
      key: (previous?.key ?? 0) + 1,
    }));
    setStartOpen(true);
  };

  const actionLabel = (action: HelpChatActionDto, target: HelpChatActionTarget) => {
    if (action.kind === 'TOPIC') {
      const name = t(`ai.helpChat.topics.${action.topicKey}`);
      return action.create
        ? t('ai.helpChat.actionCreateTopic', { name })
        : t('ai.helpChat.actionOpenTopic', { name });
    }
    if (target.kind === 'route' || action.workflowTypeId === null) {
      return t('ai.helpChat.actionOpenRegistrations');
    }
    const typeName = t(workflowTypeTranslationKey(action.workflowTypeId));
    return action.label
      ? t('ai.helpChat.actionStartCatalog', { name: typeName, entry: action.label })
      : t('ai.helpChat.actionStart', { name: typeName });
  };

  return (
    <>
      <button
        type="button"
        onClick={openChat}
        className="fixed right-4 bottom-[calc(var(--app-mobile-bottom-safe)+0.5rem)] z-40 inline-flex h-12 items-center gap-2 rounded-full bg-[var(--fire-red)] px-4 text-sm font-semibold text-white shadow-lg transition-colors hover:bg-[var(--fire-red-dark)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)] xl:right-8 xl:bottom-8"
      >
        <MessageCircleQuestion className="h-5 w-5" aria-hidden />
        <span className="hidden sm:inline">{t('ai.helpChat.open')}</span>
        <span className="sm:hidden sr-only">{t('ai.helpChat.open')}</span>
      </button>

      <Modal isOpen={open} onClose={() => setOpen(false)} title={t('ai.helpChat.title')} size="md">
        <div className="flex flex-col gap-3">
          {entries.length > 0 && !limitReached ? (
            <div className="flex justify-end">
              <Button
                type="button"
                variant="ghost"
                size="xs"
                icon={<RotateCcw className="h-3.5 w-3.5" aria-hidden />}
                onClick={reset}
              >
                {t('ai.helpChat.reset')}
              </Button>
            </div>
          ) : null}
          <div className="max-h-[55vh] space-y-3 overflow-y-auto" aria-live="polite">
            {entries.length === 0 ? (
              <div className="space-y-4 py-2">
                <p className="text-sm text-[var(--text-secondary)]">{t('ai.helpChat.intro')}</p>
                <div className="space-y-2">
                  <p className="text-xs font-semibold text-[var(--text-secondary)]">
                    {t('ai.helpChat.suggestionsTitle')}
                  </p>
                  <div className="flex flex-col items-start gap-2">
                    {suggestions.map((key) => (
                      <Button
                        key={key}
                        type="button"
                        size="sm"
                        variant="secondary"
                        disabled={chat.isPending}
                        onClick={() => void send(t(`ai.helpChat.suggestions.${key}`))}
                      >
                        {t(`ai.helpChat.suggestions.${key}`)}
                      </Button>
                    ))}
                  </div>
                </div>
              </div>
            ) : null}
            {entries.map((entry, index) => (
              <div
                // biome-ignore lint/suspicious/noArrayIndexKey: append-only conversation
                key={index}
                className={
                  entry.role === 'user'
                    ? 'ml-auto w-fit max-w-[85%] rounded-2xl rounded-br-md bg-[var(--surface-soft)] px-4 py-2.5 text-sm'
                    : 'mr-auto w-fit max-w-[95%] rounded-2xl rounded-bl-md border border-[var(--border-color)] px-4 py-3 text-sm'
                }
              >
                {entry.role === 'assistant' ? (
                  <Badge tone="accent" className="mb-1.5">
                    {t('ai.badge')}
                  </Badge>
                ) : null}
                <p className="whitespace-pre-wrap break-words">{entry.content}</p>
                {entry.actions && entry.actions.length > 0 ? (
                  <div className="mt-3 flex flex-col gap-2">
                    {entry.actions.map((action) => {
                      const target = resolveHelpChatActionTarget(action);
                      if (!target) return null;
                      const key = `${action.kind}:${action.workflowTypeKey ?? action.topicKey}:${action.configKey ?? ''}:${action.create}`;
                      if (target.kind === 'chooseOrganization') {
                        return (
                          <div key={key} className="flex flex-col gap-1.5">
                            <p className="text-xs font-semibold text-[var(--text-secondary)]">
                              {actionLabel(action, target)} · {t('ai.helpChat.chooseOrganization')}
                            </p>
                            <div className="flex flex-wrap gap-2">
                              {target.options.map((option) => (
                                <Button
                                  key={option.id}
                                  type="button"
                                  size="sm"
                                  variant="secondary"
                                  onClick={() => navigate(option.href)}
                                >
                                  {option.name}
                                </Button>
                              ))}
                            </div>
                          </div>
                        );
                      }
                      return (
                        <div key={key}>
                          <Button type="button" size="sm" onClick={() => runAction(action, target)}>
                            {actionLabel(action, target)}
                          </Button>
                        </div>
                      );
                    })}
                  </div>
                ) : null}
              </div>
            ))}
            {chat.isPending ? (
              <div className="mr-auto flex w-fit items-center gap-2 rounded-2xl rounded-bl-md border border-[var(--border-color)] px-4 py-3 text-sm text-[var(--text-secondary)]">
                <span className="flex gap-1" aria-hidden>
                  <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-current [animation-delay:-0.3s]" />
                  <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-current [animation-delay:-0.15s]" />
                  <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-current" />
                </span>
                {t('ai.helpChat.thinking')}
              </div>
            ) : null}
            {failed ? (
              <p className="text-sm text-[var(--status-danger-accent)]" role="alert">
                {t('ai.helpChat.error')}
              </p>
            ) : null}
            <div ref={endRef} />
          </div>

          <div className="space-y-2">
            {limitReached ? (
              <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-[var(--border-color)] bg-[var(--surface-soft)] p-3">
                <p className="text-sm text-[var(--text-secondary)]">
                  {t('ai.helpChat.limitReached')}
                </p>
                <Button
                  type="button"
                  size="sm"
                  icon={<RotateCcw className="h-4 w-4" />}
                  onClick={reset}
                >
                  {t('ai.helpChat.reset')}
                </Button>
              </div>
            ) : (
              <div className="flex items-center gap-2 rounded-2xl border border-[var(--border-color)] bg-[var(--card-bg)] p-2 transition-colors focus-within:border-[var(--fire-red)] focus-within:ring-2 focus-within:ring-[var(--focus-ring)]">
                <textarea
                  ref={inputRef}
                  aria-label={t('ai.helpChat.inputLabel')}
                  value={draft}
                  maxLength={HELP_CHAT_MAX_MESSAGE_LENGTH}
                  rows={1}
                  placeholder={t('ai.helpChat.placeholder')}
                  className="field-sizing-content max-h-32 min-w-0 flex-1 resize-none bg-transparent px-2 py-2 text-sm leading-5 text-[var(--text-primary)] placeholder:text-[var(--text-secondary)] focus:outline-none"
                  onChange={(event) => setDraft(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' && !event.shiftKey) {
                      event.preventDefault();
                      void send();
                    }
                  }}
                />
                <Button
                  type="button"
                  size="sm"
                  className="w-9 shrink-0 px-0"
                  icon={<ArrowUp className="h-4 w-4" aria-hidden />}
                  aria-label={t('ai.helpChat.send')}
                  onClick={() => void send()}
                  disabled={!canSend}
                  isLoading={chat.isPending}
                >
                  <span className="sr-only">{t('ai.helpChat.send')}</span>
                </Button>
              </div>
            )}
            <p className="px-1 text-center text-xs text-[var(--text-secondary)]">
              {t('ai.helpChat.privacyHint')}
            </p>
          </div>
        </div>
      </Modal>

      {self && start ? (
        <WorkflowBeginModalHost
          workflowTypeId={start.workflowTypeId}
          isOpen={startOpen}
          onClose={() => setStartOpen(false)}
          userData={self}
          modalKey={start.key}
          presetCatalogId={start.catalogId}
        />
      ) : null}
    </>
  );
}
