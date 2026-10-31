# Token & Mandantenkontext

Design-Notizen für `TokenService` (Claim-Zugriff aus dem Request-JWT; stellt keine Tokens aus) und wie JWT-Claims in die Mandantenisolation einfließen. Package: `com.emberone.platform.security` (+ Tenant-Context-Typen im gleichen Bereich). Ergänzt [Sicherheit & Berechtigungen](security-and-permissions.md).

## Mental Model

```
Authentik (OIDC) ──stellt aus──► JWT
                               │ Claims: userId, tenantId, app_role
                               ▼
              Spring OAuth2 Resource Server (Nimbus / CompositeJwtDecoder)
                               │
                               ▼
              TenantContextSupport.bindFromJwt (aufgerufen vom Filter)
                               │
              TenantContextHolder (tenantId, userId)
                               │
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
  TokenService           SecurityService        JPA / Postgres RLS
  (liest JWT)            (Rollen + Perms)        (Holder → GUCs)
```

**`TokenService` stellt keine Tokens aus.** Er liest nur das JWT der aktuellen Anfrage aus Spring Security. Ausstellung und Claim-Mapping liegen in Authentik.

## JWT-Claims

Konstanten in `EmberOneJwtClaims`:

| Konstante | Claim-String | Bedeutung |
|-----------|--------------|-----------|
| `USER_ID` | `userId` | EmberOne `users.id`; `0` = Platform-Admin (`PLATFORM_ADMIN_USER_ID`) |
| `TENANT_ID` | `tenantId` | Mandanten-Scope für normale User / Mandanten-Admins |
| `APP_ROLE` | `app_role` | `GLOBAL_ADMIN` \| `TENANT_ADMIN` \| `USER` |

Keine weiteren EmberOne-JWT-Claims (`EmberOneJwtClaims` / Frontend `lib/auth/claims.ts`). Org-Permissions kommen aus der DB, nicht aus dem Token.

Authentik-Blueprint (`support/dev/authentik-blueprints/02-emberone-claims.yaml`) mappt:

- `userId` ← User-Attribut `emberone_user_id`
- `tenantId` ← `emberone_tenant_id`
- `app_role` ← höchste von `GLOBAL_ADMIN` > `TENANT_ADMIN` > `USER` aus `role:*`-Gruppen und/oder `emberone_role`

### App-Rollen (`EmberOneAppRole`)

Exakte Strings: `"GLOBAL_ADMIN"`, `"TENANT_ADMIN"`, `"USER"`. Rollencheck ist exakte Gleichheit auf `app_role` (`EmberOneTokenChecks.checkUserRole`).

### User-facing-API-Gate

`/api/v1/self/**` und `/api/v1/auth/**` erfordern `EmberOneUserTokenClaims.isValidForUserFacingApi(jwt)`:

- bekannter `app_role`, **und**
- `GLOBAL_ADMIN` **oder** vorhandene `tenantId`

## `TokenService`-API

| Methode | Verhalten |
|---------|-----------|
| `getUserId()` / `requireUserId()` | Aus JWT `userId` (inkl. `0` = Plattform-Admin) |
| `getReferencedUserId()` | Dieselbe Id für nullable User-FKs. Plattform-Admin (`0`, keine `users`-Zeile) → `null`. Akteur bleibt in `action_log.user_id` |
| `getTenantId()` / `requireTenantId()` | Aus JWT `tenantId` |
| `hasRole(String)` | Exakter Match auf `app_role` |
| `getAppRole()` | Optionaler Claim-Wert |

**Wichtige Nuance:** `TokenService` liest das JWT über `TenantContextSupport.currentJwt()` (nicht direkt `SecurityContextHolder`). Er liest **nicht** `TenantContextHolder` für die Tenant-ID. Relevant für Global Admins in anderem Mandanten via Admin-Pfad-Override (siehe unten).

## Lebenszyklus Mandantenkontext

Filter-Reihenfolge (`SecurityConfig`): `RequestIdFilter` → `BearerTokenAuthenticationFilter` → `TenantContextWebFilter` → `DeactivatedUserWebFilter`.

1. `RequestIdFilter` setzt/echo `X-Request-Id` + MDC `requestId` (vor Bearer).
2. JWT authentifiziert (`BearerTokenAuthenticationFilter`).
3. `TenantContextWebFilter` ruft `TenantContextSupport.bindFromJwt(...)` auf und bindet damit `TenantContextHolder.setTenantId` / `setUserId` aus JWT.
4. `DeactivatedUserWebFilter` blockiert deaktivierte EmberOne-User (nach Tenant-Bind).
5. Admin-Routen `/api/v1/auth/admin/tenants/{tenantId}/...` - `AdminPathTenantContextInterceptor` kann Holder-Mandant aus dem Pfad **überschreiben** (Global Admin in anderem Mandanten).
6. `TenantContextSupport.resolveTenantId` / `resolveUserId` **bevorzugen Holder vor JWT** (für RLS / Audit).
7. `TenantContextJpaTransactionManager` setzt Postgres-Session-GUCs `app.tenant_id` / `app.user_id`.

`TokenService` liest nur JWT; RLS/Audit/die meisten Services via `TenantContextSupport` bevorzugen Holder vor JWT (volle Holder-vs-JWT-Tabelle: [Multi-Tenancy](multi-tenancy.md)). Der Permission-Map-Loader nutzt `TokenService.getUserId()` + DB.

Fehler hier sind **Data-Leak-Klasse**: immer wissen, ob JWT-Identität oder effektiver (ggf. überschriebener) Mandant gemeint ist.

## Validierung / Konfiguration

- `SecurityConfig` - OAuth2 Resource Server JWT mit `NimbusJwtDecoder.withIssuerLocation(...)` oder `CompositeJwtDecoder` für mehrere Issuer.
- Zusätzlich `aud`-Validierung gegen `spring.security.oauth2.resourceserver.jwt.audiences` / `OAUTH2_AUDIENCES` (muss dem Authentik-`client_id` entsprechen, lokal typisch `emberone`).
- Silent Renew / Login-Flow ist Frontend-OIDC (`oidc-client-ts`); Backend validiert nur Bearer-Tokens.

## Bezug Frontend

Frontend spiegelt Claim-Namen in `lib/auth/claims.ts`; die Behandlung `userId === 0` (bzw. `'0'`) als „kein EmberOne-User“ liegt in `lib/utils/tokenUserId.ts` (`getUserIdFromToken`). Siehe [Frontend Auth & Berechtigungen](frontend-auth.md).

## Invarianten & Fallstricke

1. Org-Permissions nicht ins JWT - DB-abgeleitet und gecacht.
2. `TokenService.getTenantId()` nicht nutzen, wenn **effektiver** Admin-Pfad-Mandant nötig - `TenantContextSupport`.
3. `userId == 0` ist Platform-Admin: authentifiziert, aber kein EmberOne-Self-Service-User.
4. Ausstellungs-/Claim-Bugs sind Authentik-seitig; App-Bugs meist Holder-vs-JWT-Verwechslung oder falsche Filter-Reihenfolge.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Token-Read-API | `platform/security/TokenService.java` |
| Claim-Konstanten | `platform/security/EmberOneJwtClaims.java` |
| Rollen / Checks | `EmberOneAppRole.java`, `EmberOneTokenChecks.java`, `EmberOneUserTokenClaims.java` |
| Holder / Support / Filter | `TenantContextHolder.java`, `TenantContextSupport.java`, `TenantContextWebFilter.java`, `DeactivatedUserWebFilter.java` |
| Admin-Pfad-Override | `AdminPathTenantContextInterceptor.java` |
| JWT-Decoder / Security | `SecurityConfig.java`, `CompositeJwtDecoder.java` |
| Claim-Blueprint | `support/dev/authentik-blueprints/02-emberone-claims.yaml` |

Siehe auch: [Multi-Tenancy](multi-tenancy.md) (Isolation End-to-End), [Sicherheit & Berechtigungen](security-and-permissions.md), [Frontend Auth & Berechtigungen](frontend-auth.md), [Identity Provider (Authentik)](identity-provider-authentik.md).
