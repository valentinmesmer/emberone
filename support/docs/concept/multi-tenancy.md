# Multi-Tenancy

Design-Notizen für Mandantenisolation End-to-End. Fehler hier sind **Data-Leak-Klasse**, kein UI-Bug. Package roots: `com.emberone.platform.security` (Holder / Filter / JPA-TM), `platform.tenant`. Eng verwandt: [Token & Mandantenkontext](token-and-tenant-context.md) (JWT-Claims / `TokenService`), [Sicherheit & Berechtigungen](security-and-permissions.md) (Authz).

## Mental Model

Gemeinsame Postgres-Datenbank, **FORCE RLS** auf fast allen Domänentabellen. Effektiver Mandant ist ein Request-/Thread-Wert als transaktionslokale GUCs - kein handgeschriebener Spaltenfilter in jeder Query.

```
JWT (tenantId, userId, app_role)
        │
        ▼
RequestIdFilter ──► X-Request-Id / MDC requestId
        │
        ▼
BearerTokenAuthenticationFilter
        │
        ▼
TenantContextWebFilter ──bindFromJwt──► TenantContextHolder (+ MDC)
        │
        ▼
DeactivatedUserWebFilter ── blockiert deaktivierte User
        │
        ▼  (nur /api/v1/auth/admin/tenants/{id}/...)
AdminPathTenantContextInterceptor ──überschreibt Holder.tenantId aus Pfad
        │
        ▼
@Controller (@PreAuthorize canAccessTenant / ...)
        │
        ▼
@Service @Transactional
        │
        ▼
TenantContextJpaTransactionManager.doBegin
        │  resolve*: Holder zuerst, sonst JWT
        ▼
PostgresSessionContext
  set_config('app.tenant_id', ?, true)   -- transaktionslokal
  set_config('app.user_id',   ?, true)   -- oder RESET wenn null
        │
        ▼
FORCE RLS: tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::BIGINT
(+ emberone_action_log liest app.user_id)
```

**Authz (JWT / `SecurityService`) und effektiver Mandant (Holder → GUCs) sind bewusst getrennt.** Global Admins behalten JWT-Identität, während der Holder auf den Zielmandanten zeigt.

## Isolationsebenen

| Ebene | Mechanismus | Schützt |
|-------|-------------|---------|
| **DB** | FORCE RLS auf `app.tenant_id` | Queries/Writes sehen andere Mandanten nicht, auch nicht als Tabellenowner |
| **App** | `TenantScopeGuard` | Explizites Bind / „Entity im aktiven Mandanten“ → 404 cross-tenant |
| **Authz** | JWT `app_role` + `canAccessTenant` / Org-Permissions | Wer Route/Org betreten darf |

Listen/Anlegen von Mandanten trifft nicht-RLS-Tabelle `tenants` - kein RLS-Bypass für Domänendaten. Global Admins sind nicht `BYPASSRLS`; sie bekommen **Ziel**-Mandant in Holder/GUC.

## Kernkomponenten

| Rolle | Klasse |
|-------|--------|
| ThreadLocal-Speicher | `TenantContextHolder` |
| Bind / resolve / MDC | `TenantContextSupport` |
| HTTP-Bind + Clear | `TenantContextWebFilter` |
| Filter + Primary-TM-Beans | `TenantContextAutoConfiguration` |
| JPA-Txn → GUC | `TenantContextJpaTransactionManager` (`@Primary`) |
| `set_config` / `RESET` | `PostgresSessionContext` |
| Admin-Pfad-Override | `AdminPathTenantContextInterceptor` |
| App-Level-Guard | `TenantScopeGuard` |
| Batch pro Mandant | `TenantBatchRunner` |
| SQL ohne Transaktion erkennen | `TransactionRequiredStatementInspector` (`emberone.db.require-transaction`) |

### Holder vs. JWT

| Consumer | Quelle |
|----------|--------|
| `TokenService` / Rollen / `canAccessTenant` | **Nur JWT** |
| RLS / Audit / `TenantContextSupport.resolve*` | **Holder zuerst, dann JWT** |

`TenantContextSupport.bindFromJwt` setzt nur vorhandene Claims (löscht fehlende nicht). Web-Filter räumt Holder + MDC immer in `finally` auf.

### JPA / Transaktionsscoping

Bei jedem `doBegin` löst der Primary-TM effektive IDs auf und ruft `PostgresSessionContext.apply` auf. GUCs sind **transaktionslokal** (`is_local=true`). Holder-Änderungen mitten in der Tx **stimmen** GUCs nicht nach, außer eine **neue** Transaktion startet (`REQUIRES_NEW`).

Bootstrap-Muster, das darauf baut:

```
syncBaseline @REQUIRES_NEW      // doBegin kann null-GUC sehen
  bind(tenantId)                 // nach begin
  syncForActiveTenant @REQUIRES_NEW  // neuer begin → GUC korrekt; Advisory-Lock pro Mandant
```

Repository-Interfaces tragen auf Typebene `@Transactional(readOnly = true)`. Damit bekommt jede deklarierte Query-Methode auch außerhalb eines transaktionalen Beans (`@PreAuthorize`, Interceptoren, `@AfterCommitCallback`, WebSocket) eine eigene Transaktion und damit `app.tenant_id` / `app.user_id`. Geerbte CRUD-Methoden behalten die Annotationen von `SimpleJpaRepository`.

Schreibmethoden deklarieren ein schreibendes `@Transactional` auf der Methode:

- `@Modifying`,
- abgeleitete `delete*` / `remove*`,
- `@Lock`-Methoden und native bzw. JPQL-Queries mit `UPDATE`, `DELETE`, `INSERT` oder `FOR UPDATE` (`RETURNING` tritt nur zusammen mit einem dieser Statements auf).

Lock-Methoden verwenden `Propagation.MANDATORY`, weil die Sperre in der Transaktion des Aufrufers liegen muss. Ohne umgebende Transaktion wirft der Aufruf, statt die Sperre stillschweigend zu verlieren.

Erzwungen wird das durch `RepositoryTransactionArchitectureTest`. `TransactionRequiredStatementInspector` meldet SQL, das ohne Transaktion läuft. Mit `emberone.db.require-transaction` (Env `EMBERONE_DB_REQUIRE_TRANSACTION`) wirft er bei `true` (in den Tests aktiv), sonst loggt er eine Warnung einmal pro distinktem SQL, mit der Aufrufstelle als Stacktrace.

### Admin-Pfad-Override

Pattern: `^/api/v1/auth/admin/tenants/(\d+)(?:/|$)`  
Setzt **nur** `Holder.tenantId` (+ MDC). **`userId` unberührt.**

| URI | Effekt |
|-----|--------|
| `/admin/tenants/42/standards` | Override auf 42 |
| `/admin/tenants` (Liste/Create) | Kein Override |
| `/admin/users`, `/admin/management-activations` | Kein Override; GET für Global-Admins mit Cross-Tenant-Lesen (siehe unten) |

Authz: `@PreAuthorize("@securityService.canAccessTenant(#tenantId)")` - Global Admin jeder Mandant; Mandanten-Admin nur JWT-Mandant. Interceptor scoped **Daten**; PreAuthorize **Wer darf rein**.

## Cross-Tenant-Lesen für Global-Admins

Ausnahme von „ein Mandant pro Request“, bewusst eng:

- **Wer/Wo:** `GlobalAdminReadScopeInterceptor` setzt `TenantContextHolder.globalAdminRead` nur, wenn das JWT `GLOBAL_ADMIN` ist, die Methode `GET`/`HEAD` ist und die URI unter `/api/v1/auth/admin/` liegt **ohne** `tenants/{id}` im Pfad (heute `/admin/users`, `/admin/management-activations`, `/admin/tenants`-Liste). Tenant-Admins, User und alle Nicht-Admin-Routen (Self, Org) bekommen das Flag nie, auch nicht als Global-Admin.
- **DB:** `TenantContextJpaTransactionManager` setzt dann `app.global_admin = 'true'` (transaktionslokal, ohne Flag wird nichts gesetzt). Dedizierte `FOR SELECT`-Policies (`*_global_admin_read` am Ende von `V1`) auf `idm_users`, `users`, `organizations`, `user_organization_memberships` und `org_management_activations` lesen darauf. Neue Tabellen bekommen **keine** solche Policy, bis eine Admin-Liste sie braucht.
- **Schreiben bleibt mandantengebunden:** Es gibt keine INSERT/UPDATE/DELETE-Policy dafür. Global-Admins schreiben über `/admin/tenants/{tenantId}/…` (Pfad-Override auf den Ziel-Mandanten), zum Beispiel Organisationen anlegen, `PATCH …/tenants/{id}/users/{userId}/app-role`, `POST …/tenants/{id}/users/{userId}/deactivate`, `POST …/tenants/{id}/management-activations/{id}/end`.
- **Betriebsvoraussetzung:** RLS wirkt nur, wenn die App mit einem Konto ohne `SUPERUSER` und ohne `BYPASSRLS` verbunden ist (Superuser ignorieren auch `FORCE ROW LEVEL SECURITY`). Deshalb gibt es zwei Konten ohne Superuser: der Migrations-Owner `emberone_migrator` führt Flyway aus (`FLYWAY_USERNAME`/`FLYWAY_PASSWORD`), besitzt Datenbank und Schema und damit alle Tabellen. Die App läuft als `emberone_app` (`DATASOURCE_USERNAME`/`DATASOURCE_PASSWORD`) mit `SELECT/INSERT/UPDATE/DELETE` auf Tabellen und `USAGE` auf Sequenzen, ohne DDL. Die Rollen legt `bootstrap-db-roles.sh` idempotent an (lokal aus `start-docker.sh`/`reset-docker.sh`; im Betrieb im Deployment des Betreibers, nicht Teil dieses Repos). Die Rechte setzt Flyway selbst über den Callback `afterMigrate.sql`, der nach jedem Migrate läuft und damit auch neue Tabellen abdeckt. Ein separates Grant-Skript gibt es nicht. Die Policies wurden gegen eine Wegwerf-Datenbank mit Nicht-Superuser-Konto geprüft (eigener Mandant, kein GUC = nichts, Flag = alle Mandanten lesbar, fremde Mandanten schreiben = 0 Zeilen); das Backend startet mit dem App-Konto ohne Fehler.

## Postgres-Session / RLS

| GUC | Zweck |
|-----|-------|
| `app.tenant_id` | RLS-Prädikat auf fast allen Domänentabellen |
| `app.user_id` | `emberone_action_log()`-Akteur (`null` = System/Batch) |
| `app.global_admin` | `true` = zusätzliche SELECT-Policies erlauben Lesen über Mandanten hinweg (nur Global-Admin, nur GET auf Admin-Routen ohne Mandant im Pfad) |
| `app.purging` | `true` = kein `action_log` (Tenant-Löschung) |
| `app.retention` | `true` = kein `action_log` (`RetentionHandlerInvoker`, gebatchte Hard-Deletes) |
| `app.object_storage_platform` | `true` = Outbox-Zeilen ohne `tenant_id` (nach Mandanten-Purge) |

Policy-Form (überall):

```sql
USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::BIGINT)
```

plus `ENABLE` + **`FORCE` RLS** (Tabellenowner weiter gefiltert).

**Kein RLS:** `tenants`, `tenant_personal_number_sequences` (Plattform-Katalog / Sequenzen).

### Keine zusätzlichen Mandantenfilter in Queries

Repository-Queries, Derived Queries und `Specification`s filtern **nicht** zusätzlich nach `tenant_id = :tenantId`; die Policy auf `app.tenant_id` begrenzt die Zeilen bereits. Ein expliziter Filter bleibt nur, wenn RLS allein nicht reicht, und trägt dann einen Kommentar `// Explicit tenant filter: …`:

- **Mandanten-Purge** (`AdminTenantPurgeBatchService`, `IdentityUserRepository.findAllByTenantIdOrderByIdAsc`): löscht nach Ziel-Mandant, auch auf Tabellen ohne RLS.
- **Caches mit `tenantId` im Key** (`OrganizationTenantCache`, `StandardOrganizationIdCache`, `EnabledModulesCache`, `EffectiveStandardTypeCache`): das Ergebnis darf nicht vom GUC der ladenden Transaktion abhängen.
- **Tabellen mit `*_global_admin_read`-Policy**, wenn die Query auf Cross-Tenant-Admin-Routen oder in der Berechtigungsauswertung läuft (`OrganizationRepository.findActiveByTenantIdOrderByName`, `existsByIdAndTenantId` auf `organizations`/`users`, `ManagementActivationRepository` für offene Aktivierungen).  
**89 Tabellen** FORCE RLS in `V1__Initial_setup.sql` (Orgs, Users, wf_*, std_*, cert_*, ntf_*, todo_*, action_log, …).

`emberone_action_log` ist `SECURITY DEFINER`; liest GUCs, Fallback Zeilen-`tenant_id`. `action_log` speichert bei `UPDATE` nur `changed_columns` (Spaltennamen), keine Feldwerte; bei `CREATE`/`DELETE` ebenfalls keine Werte.

## Nicht-HTTP-Bind-Pfade

| Pfad | Mechanismus |
|------|-------------|
| Global Admin in anderem Mandanten | Pfad-Interceptor + `canAccessTenant` |
| Admin-Services | Oft `TenantScopeGuard.bind` + `requireActiveTenantMatches` |
| Neuer-Mandant-Seed | Bind dann nested `REQUIRES_NEW` Sync |
| Scheduler | `TenantBatchRunner.forEachTenant` |
| Öffentliche E-Mail-Verifizierung | Body `tenantId` → `TenantScopeGuard.bind` |
| WebSocket-Authorize | `bindFromPrincipal` um DB-Check |

## Invarianten & Fallstricke

1. Immer wissen, ob **JWT-Identität** (`TokenService`) oder **effektiver Mandant** (`TenantContextSupport` / Holder) gemeint ist.
2. Bind nach `doBegin` ohne `REQUIRES_NEW` → falsche GUC für Rest der Transaktion.
3. `clear()` mitten im Request wischt Holder für alles danach - scoped Bind-Helfer bevorzugen.
4. Global-Admin-JWT kann `tenantId` omiten; effektiver Mandant dann aus Pfad/Bind.
5. TTL/Cache-Keys müssen `tenantId` enthalten, wo Werte mandantenspezifisch sind (siehe [Caching](caching.md)).
6. SQL ohne Transaktion sieht keine Tenant-Daten: Reads liefern keine Zeilen, Writes scheitern am RLS-`WITH CHECK`. Eigenständige Repository-Aufrufe bekommen durch das Type-Level-`readOnly` ihre eigene Transaktion; schreibende Methoden deklarieren ihre Transaktion selbst (siehe oben).
7. Lock-Queries (`@Lock`, `FOR UPDATE`) brauchen eine umgebende Transaktion. `Propagation.MANDATORY` erzwingt das.
8. REQUIRED-Repository-Aufrufe in programmatischen `TransactionSynchronization.afterCommit`- bzw. `afterCompletion`-Callbacks treten der bereits beendeten Transaktion bei und laufen ohne GUC. Der Inspector erkennt das nicht. Stattdessen `REQUIRES_NEW` oder `@AfterCommitEventListener` verwenden.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Holder / Support / Filter | `TenantContextHolder.java`, `TenantContextSupport.java`, `TenantContextWebFilter.java`, `DeactivatedUserWebFilter.java`, `RequestIdFilter` |
| Repository-Transaktionen (Test) | `backend/src/test/java/com/emberone/platform/tenant/RepositoryTransactionArchitectureTest.java` |
| SQL ohne Transaktion | `platform/security/TransactionRequiredStatementInspector.java`, `platform/db/EmberOneDbProperties.java` |

| Admin-Pfad | `AdminPathTenantContextInterceptor.java`, `TenantAdminWebConfiguration.java` |
| JPA-TM / GUCs | `TenantContextJpaTransactionManager.java`, `PostgresSessionContext.java` |
| Guard / Batch | `TenantScopeGuard.java`, `platform/tenant/TenantBatchRunner.java` |
| Schema / RLS | `db/migration/postgresql/V1__Initial_setup.sql` |

Siehe auch: [Token & Mandantenkontext](token-and-tenant-context.md), [Sicherheit & Berechtigungen](security-and-permissions.md), [Realtime](realtime.md).
