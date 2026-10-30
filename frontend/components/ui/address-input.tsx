/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect, useId, useRef, useState } from 'react';
import { HoverHint } from '@/components/ui/hover-hint';
import { Input } from '@/components/ui/input';
import { useFieldHoverTitle } from '@/hooks/useDisabledHoverTitle';
import { useTranslation } from '@/hooks/useTranslation';
import { getRuntimeConfig } from '@/lib/runtimeConfig';
import { cn } from '@/lib/utils/cn';

const MIN_QUERY_LENGTH = 3;
const DEBOUNCE_MS = 400;
const FETCH_TIMEOUT_MS = 3000;
const SUGGESTION_LIMIT = 5;
const SUGGESTION_CACHE_MAX = 50;

/** Module-local LRU: identical Photon queries are not re-fetched while the tab is open. */
const suggestionCache = new Map<string, AddressSuggestion[]>();

function normalizeGeocoderQuery(query: string): string {
  return query.trim().replace(/\s+/g, ' ');
}

function geocoderCacheKey(geocoderUrl: string, query: string): string {
  return `${geocoderUrl}\u0001${query}`;
}

function getCachedSuggestions(cacheKey: string): AddressSuggestion[] | undefined {
  const hit = suggestionCache.get(cacheKey);
  if (!hit) return undefined;
  suggestionCache.delete(cacheKey);
  suggestionCache.set(cacheKey, hit);
  return hit;
}

function setCachedSuggestions(cacheKey: string, suggestions: AddressSuggestion[]) {
  if (suggestionCache.has(cacheKey)) suggestionCache.delete(cacheKey);
  suggestionCache.set(cacheKey, suggestions);
  while (suggestionCache.size > SUGGESTION_CACHE_MAX) {
    const oldest = suggestionCache.keys().next().value;
    if (oldest === undefined) break;
    suggestionCache.delete(oldest);
  }
}

interface AddressSuggestion {
  street: string;
  city: string;
  postalCode: string;
  country: string;
  label: string;
}

type SuggestionStatus = 'idle' | 'loading' | 'ready';

function readString(value: unknown): string {
  return typeof value === 'string' ? value : '';
}

/** Maps one Photon GeoJSON feature (untrusted, every field optional) to a suggestion. */
function toSuggestion(feature: unknown): AddressSuggestion | null {
  if (!feature || typeof feature !== 'object') return null;
  const properties = (feature as { properties?: unknown }).properties;
  if (!properties || typeof properties !== 'object') return null;
  const p = properties as Record<string, unknown>;

  // POIs carry no `street`; their `name` is the closest thing to a street line.
  const streetName = readString(p.street) || readString(p.name);
  if (!streetName) return null;

  const houseNumber = readString(p.housenumber);
  const street = houseNumber ? `${streetName} ${houseNumber}` : streetName;
  const city = readString(p.city) || readString(p.district) || readString(p.state);
  const postalCode = readString(p.postcode);
  const country = readString(p.country);
  const label = [street, [postalCode, city].filter(Boolean).join(' '), country]
    .filter(Boolean)
    .join(', ');

  return { street, city, postalCode, country, label };
}

function parseSuggestions(json: unknown): AddressSuggestion[] {
  const features = (json as { features?: unknown })?.features;
  if (!Array.isArray(features)) return [];
  const seen = new Set<string>();
  const result: AddressSuggestion[] = [];
  for (const feature of features) {
    const suggestion = toSuggestion(feature);
    // Photon returns one feature per OSM object, so the same address can repeat.
    if (!suggestion || seen.has(suggestion.label)) continue;
    seen.add(suggestion.label);
    result.push(suggestion);
  }
  return result;
}

/**
 * Debounced Photon lookup. Kept local: the address field is the only consumer.
 * Without a geocoder URL it stays idle, which degrades to a plain input.
 * Stale suggestions stay visible while the next request is in flight so typing
 * does not blank the list for debounce + network latency.
 */
function useAddressSuggestions(query: string, geocoderUrl: string, enabled: boolean) {
  const [result, setResult] = useState<{ query: string; suggestions: AddressSuggestion[] }>({
    query: '',
    suggestions: [],
  });

  const normalized = normalizeGeocoderQuery(query);
  const isActive = enabled && !!geocoderUrl && normalized.length >= MIN_QUERY_LENGTH;

  useEffect(() => {
    if (!isActive) return;

    let stale = false;
    const controller = new AbortController();
    const timer = setTimeout(() => {
      const cacheKey = geocoderCacheKey(geocoderUrl, normalized);
      const cached = getCachedSuggestions(cacheKey);
      if (cached) {
        setResult({ query: normalized, suggestions: cached });
        return;
      }

      const url =
        `${geocoderUrl}?q=${encodeURIComponent(normalized)}` +
        `&lang=de&limit=${SUGGESTION_LIMIT}&layer=house&layer=street`;

      const timeoutId = setTimeout(() => {
        if (stale) return;
        setResult({ query: normalized, suggestions: [] });
        controller.abort();
      }, FETCH_TIMEOUT_MS);

      fetch(url, { signal: controller.signal })
        .then((res) => (res.ok ? (res.json() as Promise<unknown>) : Promise.reject(new Error())))
        .then((json) => {
          if (stale) return;
          const suggestions = parseSuggestions(json);
          setCachedSuggestions(cacheKey, suggestions);
          setResult({ query: normalized, suggestions });
        })
        .catch(() => {
          if (stale || controller.signal.aborted) return;
          setResult({ query: normalized, suggestions: [] });
        })
        .finally(() => clearTimeout(timeoutId));
    }, DEBOUNCE_MS);

    return () => {
      stale = true;
      clearTimeout(timer);
      controller.abort();
    };
  }, [normalized, geocoderUrl, isActive]);

  const isFresh = isActive && result.query === normalized;
  const hasStale = isActive && result.suggestions.length > 0;
  // Prefer fresh results; fall back to the last list so the dropdown stays useful while typing.
  const status: SuggestionStatus = !isActive
    ? 'idle'
    : isFresh
      ? 'ready'
      : hasStale
        ? 'ready'
        : 'loading';

  return {
    suggestions: isFresh || hasStale ? result.suggestions : [],
    status,
  };
}

export interface AddressInputProps {
  label?: string;
  street?: string;
  city?: string;
  postalCode?: string;
  country?: string;
  onAddressChange?: (address: {
    street?: string;
    city?: string;
    postalCode?: string;
    country?: string;
  }) => void;
  error?: string;
  className?: string;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  title?: string;
  /** When false, the country field is hidden (Ort/PLZ remain). Default: true */
  showCountry?: boolean;
}

export function AddressInput({
  label,
  street = '',
  city = '',
  postalCode = '',
  country = '',
  onAddressChange,
  error,
  className = '',
  placeholder,
  required,
  disabled,
  title,
  showCountry = true,
}: AddressInputProps) {
  const t = useTranslation();
  const [geocoderUrl, setGeocoderUrl] = useState('');
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const hoverTitle = useFieldHoverTitle(disabled, title);
  const listboxId = useId();

  const streetLabel = label ?? t('setup.user.street');

  const { suggestions, status } = useAddressSuggestions(query, geocoderUrl, isOpen);

  useEffect(() => {
    void getRuntimeConfig().then((cfg) => {
      setGeocoderUrl(cfg.NEXT_PUBLIC_GEOCODER_URL || '');
    });
  }, []);

  // A shrinking result list must not leave the highlight pointing past its end.
  const highlightedIndex = activeIndex < suggestions.length ? activeIndex : -1;

  useEffect(() => {
    if (!isOpen) return;
    const onPointerDown = (e: PointerEvent) => {
      const target = e.target as Element | null;
      if (inputRef.current?.contains(target)) return;
      if (target?.closest('[role="option"]')) return;
      setIsOpen(false);
    };
    document.addEventListener('pointerdown', onPointerDown);
    return () => document.removeEventListener('pointerdown', onPointerDown);
  }, [isOpen]);

  const handleSelect = (suggestion: AddressSuggestion) => {
    setIsOpen(false);
    onAddressChange?.({
      street: suggestion.street,
      city: suggestion.city,
      postalCode: suggestion.postalCode,
      country: suggestion.country,
    });
  };

  const handleStreetChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setQuery(value);
    setIsOpen(true);
    setActiveIndex(-1);
    if (!value) {
      onAddressChange?.({ street: '', city: '', postalCode: '', country: '' });
      return;
    }
    onAddressChange?.({
      street: value,
      city,
      postalCode,
      country,
    });
  };

  const handleStreetKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Escape') {
      setIsOpen(false);
      return;
    }
    if (suggestions.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setIsOpen(true);
      setActiveIndex((i) => ((i < suggestions.length ? i : -1) + 1) % suggestions.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setIsOpen(true);
      setActiveIndex((i) =>
        i <= 0 || i > suggestions.length - 1 ? suggestions.length - 1 : i - 1,
      );
    } else if (e.key === 'Enter' && isOpen && highlightedIndex >= 0) {
      e.preventDefault();
      handleSelect(suggestions[highlightedIndex]);
    }
  };

  const streetInputClassName = cn(`
    w-full px-4 py-3 rounded-xl border-2
    bg-[var(--card-bg)] text-[var(--text-primary)]
    border-[var(--border-color)]
    focus:border-[var(--fire-red)] focus:outline-none focus:ring-4 focus:ring-[var(--fire-red)]/10
    transition-all duration-300
    placeholder:text-[var(--text-secondary)]
    ${error ? 'border-red-500 focus:ring-red-500/10' : ''}
    ${disabled ? 'opacity-50 cursor-not-allowed' : ''}
  `);

  const hasSuggestionUi = !!geocoderUrl;
  const isListVisible =
    hasSuggestionUi && isOpen && (status === 'loading' || (status === 'ready' && !!query.trim()));

  return (
    <div className={cn('w-full space-y-4', className)}>
      <fieldset
        className="relative space-y-2 border-0 p-0"
        aria-label={streetLabel}
        onBlur={(e) => {
          if (!e.currentTarget.contains(e.relatedTarget)) setIsOpen(false);
        }}
      >
        <span className="block text-sm font-semibold text-[var(--text-primary)]">
          {streetLabel}
          {required && <span className="text-[var(--fire-red)] ml-1">*</span>}
        </span>
        <HoverHint
          hint={hoverTitle}
          capturePointer={!!disabled}
          className="block w-full"
          shape="rounded-xl"
        >
          <input
            ref={inputRef}
            type="text"
            name="street"
            autoComplete="off"
            value={street}
            onChange={handleStreetChange}
            onKeyDown={hasSuggestionUi ? handleStreetKeyDown : undefined}
            placeholder={placeholder}
            disabled={disabled}
            required={required}
            className={streetInputClassName}
            role="combobox"
            aria-expanded={isListVisible}
            aria-controls={hasSuggestionUi ? listboxId : undefined}
            aria-autocomplete={hasSuggestionUi ? 'list' : undefined}
            aria-activedescendant={
              isListVisible && highlightedIndex >= 0
                ? `${listboxId}-${highlightedIndex}`
                : undefined
            }
          />
        </HoverHint>

        {isListVisible && (
          // Above modal overlay (z-[100]) so suggestions stay tappable inside full-screen modals.
          <div className="absolute left-0 right-0 top-full z-[110] mt-1 overflow-hidden rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)] shadow-lg">
            {status === 'loading' && (
              <p className="px-4 py-3 text-sm text-[var(--text-secondary)]">
                {t('setup.user.addressSuggestionsLoading')}
              </p>
            )}
            {status === 'ready' && suggestions.length === 0 && (
              <p className="px-4 py-3 text-sm text-[var(--text-secondary)]">
                {t('setup.user.addressSuggestionsEmpty')}
              </p>
            )}
            {status === 'ready' && suggestions.length > 0 && (
              <ul id={listboxId} aria-label={t('setup.user.addressSuggestions')}>
                {suggestions.map((suggestion, index) => (
                  <li key={suggestion.label} role="presentation">
                    <button
                      id={`${listboxId}-${index}`}
                      type="button"
                      role="option"
                      aria-selected={index === highlightedIndex}
                      onMouseDown={(e) => e.preventDefault()}
                      onClick={() => handleSelect(suggestion)}
                      onMouseEnter={() => setActiveIndex(index)}
                      className={cn(
                        'block w-full px-4 py-2.5 text-left text-sm text-[var(--text-primary)] transition-colors',
                        index === highlightedIndex && 'bg-[var(--fire-red)]/20 font-semibold',
                      )}
                    >
                      {suggestion.label}
                    </button>
                  </li>
                ))}
              </ul>
            )}
            {/* ODbL requires attributing the OpenStreetMap data Photon serves. */}
            <p className="border-t border-[var(--border-color)] px-4 py-1.5 text-[11px] text-[var(--text-secondary)]">
              © OpenStreetMap contributors
            </p>
          </div>
        )}
      </fieldset>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Input
          label={t('setup.user.postalCode')}
          name="postalCode"
          autoComplete="off"
          value={postalCode}
          onChange={(e) =>
            onAddressChange?.({
              street,
              city,
              postalCode: e.target.value,
              country,
            })
          }
          placeholder={t('setup.user.postalCodePlaceholder')}
          required={required}
          disabled={disabled}
          title={title}
        />
        <Input
          label={t('setup.user.city')}
          name="city"
          autoComplete="off"
          value={city}
          onChange={(e) =>
            onAddressChange?.({
              street,
              postalCode,
              city: e.target.value,
              country,
            })
          }
          placeholder={t('setup.user.cityPlaceholder')}
          required={required}
          disabled={disabled}
          title={title}
        />
      </div>

      {showCountry && (
        <Input
          label={t('setup.user.country')}
          name="country"
          autoComplete="off"
          value={country}
          onChange={(e) =>
            onAddressChange?.({
              street,
              city,
              postalCode,
              country: e.target.value,
            })
          }
          placeholder={t('setup.user.countryPlaceholder')}
          required={required}
          disabled={disabled}
          title={title}
        />
      )}

      {error && <p className="text-sm text-red-600 dark:text-red-400 font-medium">{error}</p>}
    </div>
  );
}
