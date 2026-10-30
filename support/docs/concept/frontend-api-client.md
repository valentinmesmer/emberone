# Frontend API-Client & Runtime Config

Design-Notizen für den manuellen API-Client (kein OpenAPI-/Orval-Codegen) und die Laufzeit-Konfiguration (Base-URL / OIDC / Realtime). Pfade unter `frontend/lib/`. Verwandt: [Frontend Auth](frontend-auth.md), [Frontend PWA / Offline](frontend-pwa-offline.md), [Paginierung von Listen](pagination.md).

## Mental Model

```
.env / runtime-config.json
        │
        ▼
getRuntimeConfig()  ──► ApiClient.baseUrl, OIDC, Geocoder, Realtime
        │
        ▼
apiClient (lib/api/client.ts) + Bearer via tokenGetter
        │
        ▼
*Api Module (lib/api/*.ts)  ──► React Query (lib/store/queries/*)
        │
        ▼
types/* (handgeschriebene DTOs)
```

## API-Client

- Kern: `lib/api/client.ts` - Klasse `ApiClient`, Singleton `apiClient`
- Methoden: `get` / `post` / `put` / `patch` / `delete` / `postFormData` / `getBlob`
- Base-URL: `getRuntimeConfig().NEXT_PUBLIC_API_BASE_URL`
- Auth: `lib/api/authorizedFetch.ts` hängt Header `Authorization: Bearer …` via `getTokenGetter()` (`lib/api/tokenGetter.ts`, von `AuthProvider` gesetzt) an; `skipAuth` möglich
- 401-Recovery: einmaliger Silent-Renew-Versuch (`forceSilentRenew`) + Retry; schlägt der fehl, `redirectToLogin()`
- Fehler: `ApiRequestError` (status, code, message)
- Domänenmodule: `lib/api/{absence,organization,workflows,…}.ts` - Pfade `/api/v1/self/…` bzw. `/api/v1/auth/organizations/…`
- Store: `lib/store/queries/*.ts` → `useQuery` / `useMutation`; Re-Export `lib/store/index.ts`

UI nutzt bevorzugt Store-Hooks, nicht direkt den API-Client.

## Paginierte Listen

Vertrag und Backend-Spiegel: [Paginierung von Listen](pagination.md).

```
defineList (lib/list/<domain>List.ts)
        │
        ▼
useListState(definition)  →  listQuery (ohne page)
        │
        ▼
usePagedList in lib/store/queries/*  →  queryFn({ ...listQuery, page })
        │
        ▼
*Api.list(query)  →  buildQuery (lib/api/paged.ts)  →  PageResponse<T>
```

| Baustein | Ort | Aufgabe |
|----------|-----|---------|
| `defineList` / `toListQuery` | `lib/list/defineList.ts` | Deklarative Filter/Sort/Suche; flache Query-Params (`sort` nur wenn gesetzt und in `definition.sort.options`) |
| `useListState` | `lib/list/useListState.ts` | UI-State (Suche, Filter, `sort`/`setSort`), Debounce für `search`, `reset` (leert auch `sort`), `hasActiveFilters` (ohne Sort) |
| `buildQuery` | `lib/api/paged.ts` | URLSearchParams; Arrays als wiederholte Keys; leere `search`/`sort` weglassen |
| `usePagedList` | `lib/store/usePagedList.ts` | Infinite Scroll (`fetchNextPage`); `listQuery` im React-Query-Key |
| `fetchAllPageContent` | `lib/api/paged.ts` | Alle Seiten für Selects/Lookups (Cap 50 × max. Page-Size) |

**Neues Listen-API anbinden:**

1. Request-Typ `ListQuery & { …domänenfilter }` im API-Modul; `list()` ruft `buildQuery` auf.
2. Hook in `lib/store/queries/`: `usePagedList` mit stabilem `queryKey` + `listQuery` aus der View.
3. Pro Liste eine `*ListDefinition` in `lib/list/` (nicht in der View inline).
4. Kein clientseitiges Nachfiltern paginierter `content`-Arrays.

Beispiele: `lib/list/todoList.ts` + `lib/store/queries/todos.ts`, `lib/list/notificationList.ts` + `lib/store/queries/notifications.ts`.

## Runtime Config

| Key | Verwendung |
|-----|------------|
| `NEXT_PUBLIC_API_BASE_URL` | API-Base |
| `NEXT_PUBLIC_AUTHENTIK_ISSUER` | OIDC Authority |
| `NEXT_PUBLIC_AUTHENTIK_CLIENT_ID` | OIDC Client |
| `NEXT_PUBLIC_AUTHENTIK_SCOPE` | OIDC Scopes |
| `NEXT_PUBLIC_GEOCODER_URL` | Address-Input (Photon-Basis-URL; leer = keine Vorschläge) |
| `NEXT_PUBLIC_REALTIME_ENABLED` | Client-Toggle (Default an; `"false"` schaltet ab), in Docker aktuell nicht aus `runtime-config.json` befüllt |

**Lokal:** `.env` - Next/Turbopack inlined `process.env.NEXT_PUBLIC_*`.

**Docker:** Entrypoint schreibt `public/runtime-config.json` (`frontend/docker/write-runtime-config.cjs`) nur für API/Auth/Geocoder. `NEXT_PUBLIC_REALTIME_ENABLED` wird dort aktuell nicht geschrieben. Client fetcht `/runtime-config.json` (`cache: 'no-store'`), merged über Env-Fallback. SSR nutzt nur `process.env`.

Dateien: `lib/runtimeConfig.ts`, `frontend/docker/write-runtime-config.cjs`, `frontend/docker/docker-entrypoint.sh`.

**PWA:** Der Service Worker behandelt `/runtime-config.json` als `NetworkOnly` und schließt `public/runtime-config.json` vom Precache aus (`globIgnores`) - siehe [Frontend PWA / Offline](frontend-pwa-offline.md).

## Neue API-Fläche hinzufügen

1. DTO in `types/<domain>.ts`
2. Modul `lib/api/<domain>.ts` mit `apiClient` + Pfadkonstanten
3. React-Query-Hooks in `lib/store/queries/<domain>.ts` (+ Export in `lib/store/index.ts`)
4. UI an Store-Hooks anbinden
