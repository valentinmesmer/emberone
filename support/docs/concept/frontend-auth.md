# Frontend Auth & Berechtigungen

Design-Notizen für die Frontend-Hälfte der Sicherheitsfläche - Hooks `useAuth()`, `usePermissions()` und `useHasEmberOneUserId()`. Ergänzt Backend [Token & Mandantenkontext](token-and-tenant-context.md) und [Sicherheit & Berechtigungen](security-and-permissions.md).

## Provider-Baum

Aus `app/layout.tsx`:

```
SerwistProvider → ThemeProvider → QueryProvider → ClientAuthProvider → RealtimeProvider
  → PermissionProvider → LocaleProvider → ToastProvider → AuthLayout / pages
```

## Mental Model - zwei Achsen

| Achse | Quelle | UI-Nutzung |
|-------|--------|------------|
| **App-Rolle** | JWT / OIDC-Profil `app_role` | Admin-Chrome, `/admin/*`-Gates |
| **Org-Berechtigungen** | `GET /api/v1/self/permissions` | Feature-UI pro Organisation |

Nicht vermischen. JWT trägt **keine** Org-Berechtigungen zur Autorisierung (wie im Backend).

```
Authentik OIDC ──► accessToken (Bearer)
                      │
         ┌────────────┼────────────┐
         ▼            ▼            ▼
      useAuth()   API-Client   useHasEmberOneUserId()
      app_role    Authorization  userId vorhanden & != 0
         │                         (nur Access-Token)
         ▼
  canAccessAdministration
         │
         ▼
  usePermissions() ←── GET /api/v1/self/permissions
         │              (+ STOMP → eventDispatcher → notifyPermissionsChanged)
         ▼
  hasPermission(orgId, PermissionTypeEnum)  // nur Membership-Map, kein Admin-Bypass
```

## Auth: `useAuth()` / JWT-Claims

### Wichtige Dateien

| Rolle | Pfad |
|-------|------|
| Hook | `hooks/useAuth.ts` → `lib/auth/AuthContext.tsx` |
| EmberOne-User-Gate | `hooks/useHasEmberOneUserId.ts` |
| Claim-Konstanten / Rollen-Helfer | `lib/auth/claims.ts` |
| JWT-Helfer | `lib/auth/jwtPayload.ts` |
| OIDC | `lib/auth/oidcConfig.ts` |
| Logout-Cache | `lib/auth/logoutClientCleanup.ts` (Berechtigungen + SW vor Redirect; OIDC bei Fehler) |
| Routen-Gate | `components/auth/ProtectedRoute.tsx`, `AuthLayout.tsx` |

### Claim-Namen (exakte Strings)

| Konstante / Quelle | Claim | Hinweise |
|--------------------|-------|----------|
| `USER_ID` | `userId` | EmberOne `users.id`; `0` = Platform-Admin (akadmin) → **kein** EmberOne-User in der UI. Auflösung **nur** aus dem Access-Token (`getUserIdFromToken`), nicht aus dem OIDC-Profil. |
| `TENANT_ID` | `tenantId` | Zahl oder numerischer String; Profil zuerst, dann JWT (`readTenantIdFromToken`) |
| Literal `app_role` | `app_role` | Keine Claim-Namenskonstante. Werte in `APP_ROLE_VALUES`: `GLOBAL_ADMIN` \| `TENANT_ADMIN` \| `USER`. Lesen via `readAppRoleFromToken` (Profil zuerst, dann JWT-Payload-Literal). |

Es gibt **keine** `GROUPS`-, `PERMISSIONS`- oder `APP_ROLE`-Claim-Konstanten in `claims.ts`. AuthContext nutzt `groups` / `permissions`-Claims nicht.

Claims für Rolle/Mandant werden **Profil zuerst, dann JWT** aufgelöst. JWT-Parse ist nur UI (keine Signaturprüfung) - das Backend validiert Bearer-Tokens.

### `AuthContextType`-Oberfläche

- Session: `isAuthenticated`, `isLoading`, `isLoggingOut`, `accessToken`, `login` / `logout`, `getAccessToken`
- Claims: `appRole`, `tenantId`, `isGlobalAdmin`, `canAccessAdministration`
- OIDC: `isOidcConfigured` (`false`, wenn Env fehlt/ungültig → kein Redirect zu Authentik)
- Anzeige: `account` (OIDC-Profil); **Anzeigenamen** für den verknüpften User kommen von `useUserInfo()` → `/api/v1/self`, nicht aus dem JWT

### `useHasEmberOneUserId()`

`authenticated && getUserIdFromToken(accessToken) != null`. Liest `userId` **nur** aus dem Access-Token. Behandelt `userId === 0` als fehlend. Steuert React-Query-`enabled` und Chrome, die einen echten EmberOne-User brauchen (Nav, Benachrichtigungen, Mein Bereich). Akadmin kann authentifiziert sein, Self-/Permission-APIs bleiben aus.

### UI-Gating-Ebenen

| Ebene | Mechanismus |
|-------|-------------|
| Session | `ProtectedRoute` → auto `login()` wenn OIDC konfiguriert |
| Öffentliche Pfade | `/auth/callback`, `/verify-notification-email`, `/~offline`, `/source` |
| Admin-Bereich | `canAccessAdministration` auf `/admin/*` |
| Nur global | `isGlobalAdmin` |
| Mandanten-Scope | Nicht-globale Admins: JWT-Mandant muss Route-Mandant entsprechen |
| Verknüpfter User | `useHasEmberOneUserId()` |
| Org-Features | `usePermissions().hasPermission(...)` |

OIDC via `oidc-client-ts` (Code-Flow, `localStorage`, Silent Renew). Kein REST-„Login“-Endpoint.

## Berechtigungen: `usePermissions()`

### Wichtige Dateien

| Rolle | Pfad |
|-------|------|
| Hook + Provider | `lib/permission/PermissionContext.tsx` |
| Typen / Enum | `types/permission.ts` |
| API | `lib/api/permission.ts` → `GET /api/v1/self/permissions` |
| In-Process Pub/Sub | `lib/permission/permissionsRealtime.ts` (`subscribePermissionsChanged` / `notifyPermissionsChanged`) |
| STOMP + Dispatch | `lib/realtime/RealtimeContext.tsx` → `eventDispatcher` (`permissions.changed` → `notifyPermissionsChanged`) |

### Form

```ts
UserPermissionDto {
  permissions: Record<number, PermissionTypeEnum[]>; // orgId → permission ids
  tenantLocale?: SupportedLocale; // speist i18n
}
```

`PermissionTypeEnum`-numerische IDs **müssen** Backend `PermissionType` entsprechen (manueller Vertrag). Backend serialisiert numerisch via `@JsonValue`.

Helfer:

- `hasPermission(organizationId, permission)` - prüft **nur** die Membership-Map aus der Self-Permissions-API
- `hasPermissionInAnyOrganization(permission)`

**Admin-Bypass-Asymmetrie:** Im Backend umgehen `GLOBAL_ADMIN` / `TENANT_ADMIN` Org-Permission-Checks (`OrganizationAccessService.hasOrganizationPermission`). Im Frontend macht `hasPermission` **keinen** Admin-Bypass - Admins sehen Org-Feature-UI nur, wenn die Map den Eintrag enthält (Admin-Chrome läuft über `canAccessAdministration` / `isGlobalAdmin`, nicht über Org-`hasPermission`).

Cache: `localStorage`-Key `emberone_user_permissions` (`lib/permission/permissionsStorageKey.ts`, Hydrate + Write-through). Kein Fetch ohne Auth oder ohne EmberOne-`userId`. Bei Fehler: Cache leeren. Bei Logout: `clearClientCachesBeforeLogout()` in `AuthContext` leert Berechtigungen und Serwist-/SW-Caches vor `signoutRedirect()`; der OIDC-User-Key (`oidc.user:…`) entfernt die Bibliothek beim Redirect, bei Fehler zusätzlich `removeUser()` + `clearAuthLocalStorageAfterLogoutFailure()`. Realtime: STOMP `/user/queue/permissions` → `eventDispatcher` (`permissions.changed`) → `notifyPermissionsChanged()` → Refresh + Query-Invalidierung.

### Umgang mit Abweichungen

Es gibt **keine** Laufzeit-Sync-Prüfung zwischen Frontend-Enum und Backend.

| Situation | Verhalten |
|-----------|-----------|
| Backend neue ID, Frontend-Enum veraltet | API kann Zahl liefern; UI-Checks nutzen bekannte Konstanten → neue Fähigkeit **verborgen** |
| Frontend hat ID entfernt | Checks immer `false` |
| Backend unbekannte ID | Backend `fromId` wirft |

ID-Tabelle bei neuen Permissions manuell pflegen. Labels: `de.json` → `permissions.<ENUM_NAME>` (Name, nicht numerische ID).

Typisch: Load-Gate → `hasPermission` → `NoticeCard` / Redirect / Nav ausblenden.

## Invarianten & Fallstricke

1. Zwei Achsen: `app_role` ≠ Org-`PermissionType`.
2. `userId === 0` ist eingeloggt, aber kein Self-Service-EmberOne-User; `userId` nur aus Access-Token.
3. Permissions-API (+ Realtime) vertrauen, nicht JWT-Permission-Claims.
4. Frontend-`hasPermission` ≠ Backend-Org-Check (kein Admin-Bypass in der UI-Map).
5. `PermissionTypeEnum`-IDs beim Hinzufügen von Permissions mit Backend abgleichen.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Auth-Kontext | `lib/auth/AuthContext.tsx`, `hooks/useAuth.ts` |
| Claims | `lib/auth/claims.ts`, `lib/auth/jwtPayload.ts` |
| EmberOne-User-Gate | `hooks/useHasEmberOneUserId.ts`, `lib/utils/tokenUserId.ts` |
| Berechtigungen | `lib/permission/PermissionContext.tsx`, `types/permission.ts` |
| Realtime-Bridge | `lib/permission/permissionsRealtime.ts`, `lib/realtime/eventDispatcher.ts` |
| Routen-Shell | `components/auth/AuthLayout.tsx`, `ProtectedRoute.tsx` |

Siehe auch: [Frontend i18n](frontend-i18n.md) (`tenantLocale` aus Permissions), [Frontend PWA / Offline](frontend-pwa-offline.md) (Logout-Cache-Clear), [Token & Mandantenkontext](token-and-tenant-context.md).
