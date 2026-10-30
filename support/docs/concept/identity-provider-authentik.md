# Identity-Provider-Integration (Authentik)

Design-Notizen für die Authentik-Integration (OIDC/JWT, User-Provisioning). Package root: `com.emberone.identity`. Eng verwandt: [Token & Mandantenkontext](token-and-tenant-context.md), [Benachrichtigungssystem](notification-system.md), [Globale Exception-Behandlung](global-exception-handling.md).

## Mental Model

EmberOne ist derzeit auf **Authentik** als Identity Provider aufgebaut. Provisioning läuft über `IdentityProviderUserProvisioner` / `IdentityProviderKind`; **aktuell ist nur Authentik verdrahtet** (`resolvedKind()` fest). Ein weiterer IdP ist kein Drop-in: neue Provisioner-Implementierung, OIDC-/Claim-Konfiguration und i. d. R. Blueprints - die Domänenlogik selbst muss dafür nicht neu geschrieben werden.

Sync ist **einseitig EmberOne → Authentik**. Die App ist Master für Profilfelder und App-Rolle; Authentik ist Master für Authentifizierung, Passwörter und JWT-Ausstellung. JWT emittiert nur `userId`, `tenantId`, `app_role` (Blueprint `02-emberone-claims.yaml`); Details: [Token & Mandantenkontext](token-and-tenant-context.md). Deprovisioning nach Deaktivierung: `UserDeactivationService` → `scheduleDeprovision` (setzt auch `accountEnabled=false`) → `IdentityDeprovisionScheduler` / `IdentityDeprovisionTransactionService`.

## Zuständigkeitsaufteilung

| Thema | Owner | Hinweise |
|-------|-------|----------|
| User-Stamm-/HR-Daten (Name, Username, Benachrichtigungs-E-Mail) | **EmberOne** | Per API gepusht; Authentik-UI-Felder read-only gesperrt |
| App-Rolle (`GLOBAL_ADMIN` / `TENANT_ADMIN` / `USER`) | **EmberOne** | `idm_users.app_role` → Authentik-Attr `emberone_role` + eine `role:*`-Gruppe |
| Mandanten-/User-Verknüpfungs-Attribute | **EmberOne** | Authentik-Attrs `emberone_user_id`, `emberone_tenant_id` |
| Passwort / Credentials | **Authentik** | Temp-Passwort bei Create via API |
| Login / OIDC / JWT | **Authentik** | Scope-Mapping emittiert `userId`, `tenantId`, `app_role` |
| Lokales Identity-Mapping | **EmberOne** | `idm_users`: `emberone_user_id` ↔ `idp_user_id` |
| Org-Berechtigungen | **EmberOne (DB)** | Nicht in Authentik / JWT |

Es gibt **keinen** eingehenden Authentik→App-User-Sync (keine Webhooks / Pull-Job).

## Sync-Ablauf

```
UserService / AdminUserService
        │
        ▼
IdentityUserService  ──persist──►  idm_users
        │
        │  IdentityProviderUserProvisioner
        ▼
AuthentikUserService
        ├── GET role groups (role:USER|TENANT_ADMIN|GLOBAL_ADMIN)
        ├── create / update users
        ├── set_password on create
        └── merge groups: non-role:* behalten; genau eine EmberOne role:*
                │
                ▼
           Authentik ──login──► JWT (Claims-Blueprint)
```

| App-Aktion | IdP-Op |
|------------|--------|
| Provision (Org hinzufügen / initialer Mandanten-User) | create + set_password → `idp_user_id` speichern → `UserWelcomeNeededEvent` |
| Profil-Update / genehmigter Name / E-Mail-Verifizierung | update (oder create bei fehlender Zeile / `idp_user_id`) |
| Username-Änderung | update + `UsernameChangedEvent` |
| Admin-Rollenwechsel | update (Attrs + Group-Merge) |
| User deaktivieren | Trigger (**nicht** Org `USER_WRITE`): Admin `POST …/users/{id}/deactivate`, Hauptorg-Austritt (`OrganizationMembershipExitService` bei `main=true`), Org-Deaktivierung (`OrganizationDeactivationService`). `UserDeactivationService.deactivateUser` → `IdentityUserService.scheduleDeprovision`: in der DB-Transaktion `accountEnabled=false` und `deprovision_pending_at` setzen; danach `IdentityDeprovisionScheduler` (Cron `emberone.identity.deprovision-cron`, UTC) über `TenantBatchRunner` → `IdentityDeprovisionTransactionService.processSingle`: IdP-Account löschen (bereits gelöscht = Erfolg), `idp_user_id` / `deprovision_pending_at` leeren (kein IdP-Aufruf vor Commit) |

Reine Adress-Workflow-Updates syncen **nicht** nach Authentik.

## Feign / Jackson

Kein Spring Cloud `@EnableFeignClients` - manuelles Feign-Target.

| Baustein | Rolle |
|----------|-------|
| `AuthentikApiClient` | Feign `@RequestLine`-API |
| `AuthentikFeignConfig` | Client wenn `emberone.identity.provider=authentik` (Default) |
| `AuthentikApiTokenFeignRequestInterceptor` | `Authorization: Bearer {token}` |
| Feign `JsonMapper` | **SNAKE_CASE** (getrennt vom camelCase Primary-Mapper der App) |

```yaml
authentik.api.base-url: ${AUTHENTIK_API_BASE_URL}
authentik.api.token: ${AUTHENTIK_API_TOKEN}
emberone.identity.provider: authentik
```

`IdentityProviderConfiguration` wählt `AuthentikUserService` als `@Primary` `IdentityProviderUserProvisioner`. `IdentityProviderProperties.resolvedKind()` liefert derzeit hardcodiert `AUTHENTIK`. Weitere IdPs: neues `IdentityProviderKind`-Enum und eine `IdentityProviderUserProvisioner`-Implementierung ergänzen.

## Rollen-Gruppen & Blueprints

Authentik-Gruppen (müssen existieren): `role:USER`, `role:TENANT_ADMIN`, `role:GLOBAL_ADMIN`.

| Blueprint | Zweck |
|-----------|-------|
| `01-emberone-base.yaml` | Rollen-Gruppen; seed `akadmin` |
| `02-emberone-claims.yaml` | OAuth2-Scope → JWT `userId` / `tenantId` / `app_role` |
| `03-emberone-apps.yaml` | OAuth2-Provider hängt Scope-Mapping an |
| `04-emberone-single-role-group-policy.yaml` | Höchstens eine `role:*`-Gruppe |

App-seitig `mergeEmberOneRoleMembership` spiegelt die Single-Role-Regel. JWT-Claim-Vorrang: GLOBAL > TENANT > USER (aus Gruppen und/oder `emberone_role`).

## Invarianten & Fallstricke

1. JWT-Claims kommen aus von der App gepushten Authentik-Attrs/Gruppen - kein Live-Postgres-Read zur Token-Zeit.
2. Fehlender API-Token oder fehlende `role:*`-Gruppen → Provisioning schlägt fehl (`IdentityException` → 502 via Core-Handler).
3. Gesperrte Profilfelder nicht in Authentik-UI editieren - Drift bis zum nächsten App-Push.
4. Feign nutzt snake_case Jackson; Primary-`JsonMapper` der App nicht für Authentik-DTOs wiederverwenden.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Client / Feign | `identity/client/`, `identity/config/AuthentikFeignConfig.java` |
| Provisioner-SPI | `identity/provider/IdentityProviderUserProvisioner.java` |
| Authentik-Impl | `identity/service/AuthentikUserService.java` |
| Lokale Brücke | `identity/service/IdentityUserService.java`, `identity/domain/IdentityUser.java` |
| Deferred Deprovision | `identity/schedule/IdentityDeprovisionScheduler.java`, `IdentityDeprovisionService`, `IdentityDeprovisionTransactionService` |
| Deaktivierung | `user/service/UserDeactivationService.java` (`deactivateUser` → `scheduleDeprovision`) |
| Call Sites (Deaktivierung) | `AdminUserService`, `OrganizationMembershipExitService`, `OrganizationDeactivationService` |
| Call Sites (Sync) | `UserService`, `AdminUserService` |
| Blueprints | `support/dev/authentik-blueprints/` |

Siehe auch: [Token & Mandantenkontext](token-and-tenant-context.md), [Benachrichtigungssystem](notification-system.md), [Globale Exception-Behandlung](global-exception-handling.md).
