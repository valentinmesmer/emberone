# Globale Exception-Behandlung

Design-Notizen für die sieben `@RestControllerAdvice`-Klassen; kein gemeinsames `ProblemDetail` / `ApiError`-DTO. Package roots: `com.emberone.platform.exceptionhandler` / `platform.exception`, plus package-scoped `*exceptionhandler` in Workflow / Todo / Notification / Permission / Document. Eng verwandt: [Sicherheit & Berechtigungen](security-and-permissions.md), [Identity Provider (Authentik)](identity-provider-authentik.md), [Workflow-Engine](workflow-engine.md).

Bodies sind ad-hoc `Map<String, String>` (oder leeres `204`). Das Frontend-`apiClient` liest `error || message` und optional `code`.

## Mental Model

```
Domänen-Exception (meist von Platform-Basis abgeleitet)
        │
        ▼
Package-scoped @RestControllerAdvice  (@Order(0) wenn es gewinnen muss)
        │  (wenn kein Match)
        ▼
CoreGlobalExceptionHandler  (anwendungsweit)
```

## Fehlerantwort-Vertrag

### Kanonische Form

```http
HTTP/<status>
Content-Type: application/json

{ "error": "<menschenlesbare Nachricht>" }
```

| Status | Wann |
|--------|------|
| 400 | `ValidationException`, erlaubte `IllegalArgumentException`, Bean-Validation |
| 403 | Platform-`AccessDeniedException`, Spring `AuthorizationDeniedException`, `SecurityException` (Dokumente) |
| 404 | `ResourceNotFoundException` |
| 409 | `ConflictException` |
| 502 | `IdentityException` (generische Identity-Meldung) |
| 500 | Unerwartet / sanitisierte Meldungen |
| 204 | Profilbild fehlt (leerer Body) |

Unerwartet generisch: `"An unexpected error occurred"`.  
Identity: `"The identity service is temporarily unavailable."`

### Varianten

| Variante | Form | Wo |
|----------|------|-----|
| Workflow-Validation | `{ "error", "code"? }` | `WorkflowValidationException` |
| Todo | `{ "message" }` | Todo-Package (historisch; `error` bevorzugen) |
| Bean-Validation | `{ "field": "msg", ... }` | `MethodArgumentNotValidException` |
| Profilbild fehlt | 204 leer | `ProfileImageExceptionHandler` |

Stabile Workflow-Codes: vollständige Liste in `WorkflowValidationException` und Frontend `WORKFLOW_CLIENT_ERROR_CODES` (`frontend/types/workflow.ts`) - dort pflegen und spiegeln. U. a. Type-/Target-Concurrency (`WORKFLOW_TYPE_ALREADY_OPEN`, `CERTIFICATION_DEFINITION_ALREADY_OPEN`, `TRAINING_COURSE_TYPE_ALREADY_OPEN`, `ORGANIZATION_JOIN_ALREADY_OPEN`, `MAIN_ORGANIZATION_CHANGE_ALREADY_OPEN`, `ORGANIZATION_LEAVE_ALREADY_OPEN`, `ORGANIZATION_GROUP_LEAVE_ALREADY_OPEN`, `ORGANIZATION_GROUP_JOIN_ALREADY_OPEN`, `ONBOARDING_SUBMISSION_ALREADY_OPEN`, `ORGANIZATION_MEMBERSHIP_END_ALREADY_OPEN`), Cross-Typ-Exclusivity (`OPEN_EXCLUSIVITY_ALREADY_OPEN`, `ORGANIZATION_MEMBERSHIP_EXIT_ALREADY_OPEN`, Fallback `OPEN_CONCURRENCY_KEY_ALREADY_OPEN`), Leadership-Guards (`LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE`, `LEADERSHIP_GROUP_LEAVE_NOT_ALLOWED`, `LEADERSHIP_GROUP_JOIN_NOT_ALLOWED`), Kontinuität/Deaktivierung (`USER_ACCOUNT_NOT_SUSTAINABLE`, `USER_ALREADY_DEACTIVATED`).

## Platform-Basis-Exceptions

| Klasse | Typisches HTTP |
|--------|----------------|
| `ResourceNotFoundException` | 404 |
| `ValidationException` | 400 |
| `AccessDeniedException` | 403 |
| `ConflictException` | 409 |

Die meisten Domänen-Exceptions erben davon. Gemeinsame Helfer: `ClientErrorMessageSupport` (nur `IllegalArgumentException`-Msgs aus `com.emberone.*` exponieren), `AccessDeniedLogSupport`, `ClientDisconnectSupport`.

## Handler-Inventar

| Handler | Scope | Besonderes |
|---------|-------|------------|
| `CoreGlobalExceptionHandler` | global | Volle Matrix + Catch-all + Identity 502 + Disconnect |
| `PermissionGlobalExceptionHandler` | permission-Packages | Lokale Teilmenge wie Core |
| `WorkflowGlobalExceptionHandler` | `com.emberone.workflow` | `@Order(0)` + optional `code` |
| `TodoGlobalExceptionHandler` | `com.emberone.todo` | `{message}` statt `{error}` |
| `NotificationGlobalExceptionHandler` | `com.emberone.notification` | nur 404 |
| `DocumentGlobalExceptionHandler` | `userdocument` / `attachment` / `profileimage` / `workflowsupporting` | `SecurityException` → 403 |
| `ProfileImageExceptionHandler` | zwei Image-Controller | 204 bei fehlendem Avatar |

## Neuer Handler vs. Wiederverwendung

### Wiederverwendung (Default)

1. Eine der vier Platform-Basen erweitern.
2. Aus Services/Controllern werfen.
3. **Core** mappt - kein neues Advice.

### Package-scoped Advice wenn

| Bedarf | Beispiel |
|--------|----------|
| Anderer HTTP-Status als Core | Profilbild → 204 statt 404 |
| Zusätzliche Response-Felder | Workflow → `code` |
| Domänentyp, den Core nicht mappt | `WorkflowValidationException` |
| Package-lokale JDK-Exceptions | Document / Permission |

Richtlinien:

- Enges `basePackages` / `basePackageClasses`.
- `@Order(0)`, wenn das Advice Core für denselben Typ schlagen muss.
- **Kein** weiteres Catch-all `Exception.class` außerhalb Core.
- Body-Key **`error`** beibehalten, außer triftiger Grund.
- Maschinelle Codes wie Workflow (`code` + Frontend-Konstanten).

## Frontend-Konsum

`frontend/lib/api/authorizedFetch.ts` (`ApiRequestError` wird über `client.ts` re-exportiert):

```ts
type ErrorBody = { error?: string; message?: string; code?: string };
// → ApiRequestError(message, status, code?)
```

Bean-Validation-Feldmaps erscheinen oft als generisches `HTTP error! status: 400`, unless der Caller den Body parst. Profilbild-204 gilt als Erfolg / leere Payload.

## Nicht-Advice-Pfade

- OAuth2 Resource-Server-Auth-Fehler: Framework-Defaults (nicht `{error}`-Map).
- `ResponseStatusException` in Admin/Identity; Core-Catch-all kann 500 liefern - Platform-Basis bevorzugen.
- Realtime-STOMP-Fehler sind kein HTTP-`@RestControllerAdvice`.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Core + Helfer | `platform/exceptionhandler/` |
| Platform-Basen | `platform/exception/` |
| Workflow | `workflow/exceptionhandler/` |
| Todo / notification / permission / document / profile image | jeweiliges `exceptionhandler/` oder `exception/` |
| Frontend-Error-Codes | `frontend/types/workflow.ts` (`WORKFLOW_CLIENT_ERROR_CODES`) |
| Frontend-Parse | `frontend/lib/api/authorizedFetch.ts` |

Siehe auch: [Identity Provider (Authentik)](identity-provider-authentik.md), [Sicherheit & Berechtigungen](security-and-permissions.md).
