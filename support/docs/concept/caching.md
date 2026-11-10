# Caching-Strategie

Design-Notizen zu allen Caching-Stellen: Backend (Spring Cache über Redis mit **After-Commit**-Eviction, Request-Memoizing, geteiltes KI-Rate-Limit, HTTP-Validierung) und Frontend (React Query, Realtime-Invalidierung). Package roots: `com.emberone.platform.cache` / `platform.config` (`CacheConfig`), Domänen-`*cache`-Packages, `frontend/lib/store`. Eng verwandt: [Multi-Tenancy](multi-tenancy.md), [Standard](standard.md), [Sicherheit & Berechtigungen](security-and-permissions.md), [Skalierbarkeit](scalability.md).

Ohne dieses Dokument lernt man „was invalidiert was“ nur durch jeden `*CacheEvictionListener` einzeln.

## Grundsätze

1. **Redis ist der einzige Server-Cache.** Das Backend läuft mit mehreren Instanzen ([Skalierbarkeit](scalability.md)); lokale Caches (Caffeine, Maps) wären zwischen den Knoten inkonsistent und sind verboten. Ein lokaler L1-Cache vor Redis spart nur einen Roundtrip (< 1 ms) und bräuchte eine eigene Cross-Node-Invalidierung; er ist bewusst nicht vorgesehen.
2. **Cache nur, was gelesen oft und geändert selten wird** und wessen Invalidierung sich an einem Domänen-Event festmachen lässt. Volatile Daten (Zähler, Workflow-Zustände, Kapazitäten) werden nicht gecacht.
3. **Jeder Cache hat Evictor und TTL.** Das Event ist die Korrektheit, die TTL nur das Sicherheitsnetz bei verpasstem Event.
4. **Mandantenspezifische Werte tragen `tenantId` im Key**, an erster Stelle (`{tenantId}:{…}`).
5. **Ein Redis-Ausfall ist ein Cache-Miss**, kein Fehler (`ResilientCacheErrorHandler`).

## Backend: Mental Model

```
Domänenservices publizieren Events
        │
        ▼
After-Commit-Listener (AFTER_COMMIT)
        │
        ▼
*CacheEvictionListener → *Evictor / cache.evict(...)
        │
        ▼
Redis: {cacheName}::{key} gelöscht
        │
        ▼
Nächster @Cacheable-Miss → DB-Reload
```

- Produktion: `spring.cache.type: redis` (Timeout 1 s). Die TTL je Cache steht in `application.yml` unter `emberone.cache.ttl` (`CacheProperties`); `CacheConfig` bricht den Start ab, wenn für einen Namen aus `CacheNames.ALL` die TTL fehlt, nicht positiv ist oder ein unbekannter Name konfiguriert ist.
- Tests: `spring.cache.type: simple` (In-Memory, erlaubt `null`). Wer einen nullbaren Loader testet, nutzt einen Cache-Manager mit `setAllowNullValues(false)` wie `EffectiveStandardOrganizationCacheTest`, damit der Test das Redis-Verhalten abbildet.
- **Null-Werte werden nie gecacht** (`disableCachingNullValues`). Ein `@Cacheable`, dessen Loader `null` liefern kann, braucht `unless = "#result == null"`. Sonst scheitert das Put in Redis, wird nur als Warnung geloggt und der Pfad bleibt dauerhaft ungecacht.

## Cache-Inventar

| Cache-Name | Key | Inhalt | TTL | Loader |
|------------|-----|--------|-----|--------|
| `userPermissions` | `{tenantId}:{userId}` | `UserPermissionDto` (Org → Permissions; **ohne** Locale) | 5 min | `UserPermissionCache` |
| `orgInTenant` | `{tenantId}:{organizationId}` | exists boolean | 60 min | `OrganizationTenantCache` |
| `effectiveStandardByOrgType` | `{tenantId}:{organizationTypeId}` | Standard-ID (null uncached) | 30 min | `EffectiveStandardTypeCache` |
| `effectiveStandardByOrg` | `{tenantId}:{organizationId}` | effektive Standard-ID (null uncached) | 30 min | `EffectiveStandardOrganizationCache` |
| `organizationsForStandard` | `{tenantId}:{standardId}` | Org-ID-Liste | 30 min | `StandardOrganizationIdCache` |
| `enabledModulesByStandard` | `{tenantId}:{standardId}` | `Set<Module>` | 30 min | `EnabledModulesCache` |
| `tenantLocale` | `{tenantId}` | `SupportedLocale` (Default `DE`) | 24 h | `TenantLocaleCache` |
| `tenantFederalState` | `{tenantId}` | `FederalState` | 24 h | `TenantFederalStateCache` |
| `activeOrgMembership` | `{userId}:{organizationId}` | active boolean | 5 min | `ActiveOrganizationMembershipCache` |
| `userActive` | `{userId}` | active boolean (Deaktivierungs-Check pro Request, nur aktive Nutzer werden gecacht) | 60 s | `UserActiveCache` |

Konstanten: `platform.cache.CacheNames` (inklusive `ALL`). Redis-Physische Keys: `{cacheName}::{key}`. Die TTLs sind Defaults aus `application.yml` (`emberone.cache.ttl.<cacheName>`); `CacheConfigTest` prüft, dass die Datei jeden Cache abdeckt.

### Aufbau pro Cache

Jeder Cache liegt im `cache`-Package seiner Domäne und besteht aus denselben Bausteinen:

| Baustein | Name | Aufgabe |
|----------|------|---------|
| Loader | `<Name>Cache` | `@Cacheable`, lädt bei Miss aus der DB |
| Evictor | `<Name>CacheEvictor` | nur `@CacheEvict`, eigene Bean (Self-Invocation umgeht AOP) |
| Listener | `<Domäne>CacheEvictionListener` | übersetzt Domänen-Events in Evictor-Aufrufe |

Loader und Evictor stehen nie in derselben Klasse. Ein Listener nutzt `@AfterCommitEventListener(readOnly = true)`, wenn er vor dem Evict Daten lesen muss (betroffene Orgs, Gruppenmitglieder), und `@AfterCommitCallback`, wenn er nur evictet. Mehrere Caches derselben Domäne dürfen sich einen Listener teilen (`StandardCacheEvictionListener`, `TenantCacheEvictionListener`).

Auth-sensitive Caches (`userPermissions`, `activeOrgMembership`, `userActive`) haben kurze TTLs. Locale ist getrennt von Permissions, damit Locale-Änderungen Permission-Maps nicht flushen.

Keys von `activeOrgMembership` und `userActive` enthalten keine `tenantId`: User- und Organisations-IDs sind global eindeutig, und beide Werte werden nur nach einer Tenant-Prüfung der Aufrufer gelesen.

## Request-Memoizing (kein Cross-Request-Cache)

Mehrfach gelesene Fakten, die zu volatil für Redis sind, werden pro HTTP-Request einmal geladen (`@RequestScope`):

| Bean | Inhalt | Zweck |
|------|--------|-------|
| `RequestScopedUserPermissions` | Permission-Map des angemeldeten Users | mehrere `@PreAuthorize`-Checks pro Request |
| `RequestScopedOpenerMemberships` | „hat aktive Mitgliedschaft“, „hat aktive Hauptmitgliedschaft“ | `WorkflowOpenAccess` läuft für die Liste beginnbarer Workflow-Typen einmal pro Handler (18×), ohne Memoizing liefen dieselben zwei Queries 18-mal |

Diese Beans funktionieren nur im Request-Thread. Aufrufe aus Scheduler- oder Batch-Pfaden dürfen sie nicht nutzen.

`ManagementAccessService` cached bewusst nichts über Requests hinweg (fail-closed, Aktivierungen müssen sofort wirken).

## Berechtigungsprüfung ohne DB-Zugriff bei Cache-Treffer

`UserPermissionService.getUserPermissions` ist absichtlich **nicht** `@Transactional` und lädt den User nicht: Der Tenant kommt aus dem Kontext (`TenantScopeGuard.requireActiveTenantId`), der Key ist `{tenantId}:{userId}`, der Loader läuft unter RLS. Ein unbekannter oder mandantenfremder User ergibt eine leere Map (fail closed). Bei einem Treffer wird weder eine Transaktion geöffnet noch eine Connection genommen.

## Invalidierungsregeln

Bausteine, Namen und die Wahl der Listener-Annotation: siehe [Aufbau pro Cache](#aufbau-pro-cache).

### `userPermissions` - `UserPermissionCacheEvictionListener`

| Event | Effekt |
|-------|--------|
| `UserPermissionsChangedEvent` | Evict dieser `userId` |
| `OrganizationGroupPermissionsChangedEvent` | Evict aller **aktiven** Mitglieder dieser Org-Gruppe |
| `PermissionGroupPermissionsChangedEvent` | Evict aktiver Mitglieder aller Org-Gruppen, die auf diese Permission Group gemappt sind |

Publisher u. a. Membership-Services, `PermissionGroupMappingService`, `PermissionGroupStandardDomain`.

### `orgInTenant` - `OrganizationTenantCacheEvictionListener`

| Event | Effekt |
|-------|--------|
| `OrganizationCreatedEvent` | Key evicten (löscht mögliches negatives `exists=false`) |

Org-Updates ändern Existenz nicht → kein Evict. Es gibt keinen Org-Delete-Pfad; käme einer hinzu, muss er hier evicten (sonst bleibt `exists=true` bis zu 60 min).

### Standards - `StandardCacheEvictionListener`

| Event | Caches |
|-------|--------|
| `OrganizationCreatedEvent` | `effectiveStandardByOrg`; `organizationsForStandard` (Override und/oder Typ-Zuweisung) |
| `OrganizationTypeStandardAssignedEvent` | `effectiveStandardByOrgType`; erbende Orgs `effectiveStandardByOrg`; beide Standards `organizationsForStandard` |
| `OrganizationStandardOverrideChangedEvent` | Org `effectiveStandardByOrg`; vorheriger + neuer `organizationsForStandard` |
| `StandardEnabledModulesChangedEvent` | `enabledModulesByStandard` |
| `StandardDeletedEvent` | `enabledModulesByStandard` und `organizationsForStandard` des gelöschten Standards |

Org-effektiver Standard kann Type-Cache verschachtelt laden (zwei Redis-Einträge). Ein Standard lässt sich nur löschen, wenn weder Typ-Zuweisung noch Org-Override auf ihn zeigen; die `effectiveStandardBy*`-Einträge können ihn deshalb nicht mehr enthalten.

### `tenantLocale` / `tenantFederalState` - `TenantCacheEvictionListener`

| Event | Effekt |
|-------|--------|
| `TenantUpdatedEvent` | Evict `tenantLocale` und `tenantFederalState` je `{tenantId}` (via `TenantFederalStateCacheEvictor`) |

Beim Löschen eines Mandanten (`AdminTenantPurgeBatchService`) wird nichts evictet: Mandanten-IDs werden nie wiederverwendet, die Einträge verfallen ungelesen per TTL.

### `activeOrgMembership` - `ActiveOrganizationMembershipCacheEvictionListener`

| Event | Effekt |
|-------|--------|
| Membership Created / Updated / Ended | Evict `{userId}:{organizationId}` |

### `userActive` - `UserActiveCacheEvictionListener`

| Event | Effekt |
|-------|--------|
| `UserDeactivatedEvent` | Evict `{userId}` |

## Geteiltes KI-Rate-Limit (Redis, kein `@Cacheable`)

`AiRateLimiter` zählt KI-Aufrufe je User in Redis (`aiRateLimit:{userId}`), damit das Limit über alle Instanzen gilt:

- Tumbling Window: Der erste Aufruf startet ein Fenster von einer Minute, weitere zählen darauf, danach verfällt der Key. Abgelehnte Aufrufe zählen mit, verlängern das Fenster aber nicht.
- `INCR` und `PEXPIRE` laufen in einem Lua-Skript. Ein Absturz dazwischen kann keinen Zähler ohne Ablauf hinterlassen.
- **Fail-open:** Ist Redis nicht erreichbar, wird der Aufruf erlaubt und eine Warnung geloggt. Das Limit schützt Kosten und Missbrauch eines optionalen Features und darf es nicht mit Redis ausschalten.
- Konfiguration: `emberone.ai.rate-limit-per-minute` (0 = aus). Siehe [KI-Assistenz](ai-assistance.md).

## HTTP-Caching

| Stelle | Verhalten |
|--------|-----------|
| Profilbild-Download | `Cache-Control: private, no-cache` plus starker `ETag`. Der ETag leitet sich aus dem Objektnamen ab (jeder Upload erzeugt einen neuen UUID-Namen). `If-None-Match` wird beantwortet, **bevor** Object Storage berührt wird (`304`). Siehe [Profilbild](profileimage.md). |
| Übrige Downloads (Attachments, User-Dokumente) | Keine Cache-Header, Stream direkt aus S3. Bewusst nicht erweitert, solange Bandbreite kein Problem ist. |
| Next.js-Assets | Hash-Assets liefert Next mit `immutable`; die API (`/api/*`) und `/runtime-config.json` nie über den Service Worker ([Frontend PWA](frontend-pwa-offline.md)). |

Ein CDN ist wegen der durchgängigen Authentifizierung nicht sinnvoll.

## Bewusst nicht gecacht

| Stelle | Begründung |
|--------|------------|
| Hibernate L2- und Query-Cache | RLS und mandantenspezifische GUCs gelten pro Transaktion. |
| Kataloge (Lehrgangs-, Zertifikats-, Führerschein-Typen) | Pro Org nur wenige indizierte Queries; die Invalidierungsfläche (Typ-Pflege, Auswahl je Standard, Archivierung, Standard-Zuweisung) ist groß, und ein veralteter Eintrag würde die Auswählbarkeit eines archivierten Typs falsch beantworten. Erst bei gemessener Last einführen. |
| Workflow-Zugriffsprüfungen, Unread-Counts, Kapazitäten | Volatil; Zähler kommen per Realtime-Push. |
| `ListFilterOptionsService` | Der Key bräuchte den vollen Kontext (Org, Rechte, User), die Trefferquote wäre gering. |
| LLM-Antworten | Eingabeabhängig und datenschutzrelevant. |
| Profilbild-Bytes | Gehören weder in Redis noch in den Heap; Revalidierung per ETag genügt. |
| Authentik-Gruppen-IDs | Nur bei Onboarding-Provisionierung, kein Hot Path. |
| Scheduler-Locks, Outbox-Claims, Idempotenz, Token-Sperrlisten | ShedLock auf JDBC, `SKIP LOCKED`, DB-Marker bzw. zustandslose JWTs genügen; Redis wäre Overkill. |

## Betrieb von Redis

- Reiner Cache und Pub/Sub: **keine Persistenz** nötig.
- Für Produktion `maxmemory` und `maxmemory-policy` (`volatile-ttl` oder `allkeys-lru`) setzen; das Dev-Compose setzt sie nicht. Das Authentik-Redis ist ein eigener Dienst und bleibt getrennt.
- Pub/Sub ist At-most-once; nach einem Reconnect verlorene Realtime-Events fängt das Frontend ab ([Realtime](realtime.md)).
- Bei Redis-Ausfall wartet jeder Cache-Zugriff bis zum Timeout (1 s), bevor die DB einspringt; pro Request fallen mehrere Zugriffe an. Es gibt keinen Circuit Breaker.
- Evict-Fehler werden nur geloggt. Ein verpasster Evict bleibt bis zur TTL stehen (`userPermissions` 5 min, `orgInTenant` 60 min, Locale 24 h).

## Frontend: React Query

Serverzustand läuft ausschließlich über TanStack React Query (`lib/store`); der Zustand im UI-Store ist reiner UI-Zustand. Der `QueryClient` ist ein Singleton (`lib/store/query-client.ts`).

### Freshness-Stufen

`lib/store/staleTime.ts` definiert `STALE_TIME`. Queries nutzen diese Konstanten statt Zahlenliterale:

| Stufe | Wert | Verwendung |
|-------|------|------------|
| `REALTIME` | 15 s | Work in Progress, den andere oft ändern (Workflow-Entscheidungen, Zähler) |
| `LIVE` | 30 s | Postfächer und Übersichten, die Realtime-Events meist aktualisieren |
| `LIST` | 60 s | Datensätze und Listen eines Users oder einer Organisation |
| `STANDARD` | 5 min | Kataloge, Definitionen, Profildaten; zugleich Client-Default |

Defaults: `gcTime` 10 min, `retry` 1, kein Refetch bei Window-Focus oder Reconnect. Einzelne Queries schalten `refetchOnReconnect` ein, wenn ein verpasstes Realtime-Event schadet (Permissions).

### Query-Keys

- Jede Domäne exportiert eine `xxxKeys`-Factory in `lib/store/queries`. **Rohe String-Arrays gibt es nicht**, auch nicht in `invalidateQueries`. Fehlt ein Teil-Key (z. B. `organizationKeys.groupsRoot`, `qualificationKeys.statusForOrganization`), wird er in der Factory ergänzt.
- Query-Hooks liegen in `lib/store/queries`, auch Hooks für Teilansichten (`useAppointmentAttachments`, `useListFilterOptions`). Komponenten rufen `useQuery` nicht direkt. Einzige Ausnahme ist `useOnboardingApprovedEvidence` (`lib/onboarding`, mit `onboardingApprovedEvidenceKeys`): ein zusammengesetzter `useQueries`-Hook, der Treffer gegen Workflow-Payloads abgleicht.
- Jeder Scope eines Keys bekommt einen eigenen Teil-Key. Ein Scope ohne eigenen Zweig würde unter dem Key eines anderen landen und dessen Daten überschreiben (so lag `organizationCourseOfferings` vorher auf dem Key der Organisations-Aggregate).

### Invalidierung

- Mutationen invalidieren den kleinsten Key, der betroffen ist, und rufen `invalidateQueries` mit `void` auf. Beispiele: Anhänge eines Termins berühren nur `appointmentAttachmentKeys.list` (die Termin-DTOs enthalten keine Anhangsdaten); Workflow-Anträge invalidieren Listen, Zähler, beginnbare Typen und Open-State (`invalidateWorkflowLists`), nicht `workflowKeys.all` mit allen Details.
- Nur ein Realtime-Event ohne IDs (`workflow.updated` ohne `workflowId`) fällt auf das breite Topic `workflows` zurück; es lässt sich nicht enger fassen.
- Realtime-Events werden zentral in `lib/realtime/eventDispatcher.ts` auf Topics in `lib/store/cache-invalidation.ts` abgebildet (gebündelt mit Debounce und Jitter). Neue Topics sind dort der einzige Ort.
- Die Berechtigungen sind eine Query (`permissionKeys.self()`, `useSelfPermissions`). `permissions.changed` und `management.activation.expired` invalidieren sie zusammen mit den Management-Aktivierungen. Es gibt keinen zweiten Speicher (kein `localStorage`, kein In-Process-Pub/Sub). Siehe [Frontend Auth](frontend-auth.md).

### Polling

Feste Intervalle sind die Ausnahme. Wo es ein Realtime-Event gibt, pollt die Query nur, solange Realtime aus oder getrennt ist (`useRealtimePollingFallback`, z. B. Benachrichtigungen, Todos, Workflows, Termine). Festes Polling bleibt dort, wo kein Event existiert oder Zeitablauf selbst der Auslöser ist: ungelesene Dokumente (120 s), Management-Aktivierungen (60 s, laufen zeitgesteuert ab).

### Profilbilder

- Zwei Stufen: `selfImage` / `orgUserImage` liefern die stabile Download-URL, `resolvedImage(url)` lädt den Blob mit Bearer-Token und erzeugt eine Blob-URL (`staleTime: Infinity`, `gcTime` 1 h). Der Server beantwortet Revalidierung per ETag mit `304`.
- Blob-URLs werden freigegeben, wenn React Query den Eintrag entfernt (`revokeResolvedImageOnRemoval`, abonniert in `QueryProvider`) und beim Upload/Löschen (`invalidateSelfProfileImage`).
- Ladefehler werden **nicht** als „kein Bild“ gecacht; die Query steht im Fehlerzustand und wird beim nächsten Mount erneut versucht.

### Logout und Userwechsel

`clearClientCachesBeforeLogout()` leert vor `signoutRedirect()` den kompletten Query-Cache (das gibt auch die Blob-URLs frei) und die Service-Worker-Caches. Dasselbe Aufräumen passiert, wenn das Abmelden fehlschlägt. Der Query-Cache liegt nicht im Browser-Speicher; nach einem Reload ist er ohnehin leer.

### Service Worker

`/api/*` und `/runtime-config.json` laufen same-origin über `NetworkOnly`. Der Rest ist Serwist-Precache und `defaultCache` ([Frontend PWA](frontend-pwa-offline.md)). `skipWaiting` mit `clientsClaim` aktiviert einen neuen Worker sofort; ein Chunk-Mismatch in einer laufenden Session ist die bekannte Folge.

## Invarianten & Fallstricke

1. **Event + After-Commit-Eviction** bevorzugen statt `@CacheEvict` aus dem mutierenden Service.
2. Mandantenspezifische Werte brauchen `tenantId` im Key, an erster Stelle. Ausnahme: `activeOrgMembership` und `userActive` (globale IDs, siehe oben).
3. Locale nicht in `userPermissions` cachen.
4. Negatives Caching (`orgInTenant` false) braucht explizites Create-Evict oder 60-min-sticky Miss.
5. Fehlender Eviction-Listener ist Korrektheits-Bug; TTL begrenzt nur Veraltung. Wer einen Löschpfad für gecachte Entitäten baut, ergänzt Event und Evict im selben Change.
6. `@Cacheable`-Loader, die `null` liefern können, brauchen `unless`.
7. Race beim After-Commit-Evict: Ein paralleler Leser kann zwischen Commit und Evict den alten Wert neu in den Cache schreiben; er hält bis zur TTL. Das ist der Grund für die kurzen TTLs bei Berechtigungen.
8. Zeitabhängige Werte (`Instant.now()` in `userPermissions` und `activeOrgMembership`) wirken bis zu eine TTL zu spät, wenn ein Beginn oder Ende zwischen zwei Events liegt.
9. Ein `@Cacheable`-Loader unter RLS lädt mit dem Tenant des aufrufenden Threads. Aufrufe aus Batch- oder Admin-Kontexten brauchen den passenden Tenant-Kontext, sonst landet ein Wert unter dem falschen Key.
10. Kein Zahlenliteral für `staleTime`, kein rohes Query-Key-Array im Frontend.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Namen / Config | `platform/cache/CacheNames.java`, `platform/config/CacheConfig.java`, `CacheProperties.java`, `application.yml` (`emberone.cache.ttl`), `RedisCacheSerializerFactory`, `ResilientCacheErrorHandler` |
| Permission | `userpermission/cache/`, `userpermission/service/UserPermissionService.java` |
| Request-Memoizing | `platform/security/RequestScopedUserPermissions.java`, `RequestScopedOpenerMemberships.java` |
| Organization | `organization/cache/` |
| Standard | `standard/cache/`, `standard/event/` |
| Membership | `userorganizationmembership/cache/` |
| Locale | `admin/tenant/cache/` |
| KI-Rate-Limit | `platform/ai/AiRateLimiter.java` |
| Profilbild-ETag | `profileimage/service/ProfileImageService.java`, `profileimage/controller/ProfileImageDownloadResponses.java` |
| Frontend-Client, Stufen | `frontend/lib/store/query-client.ts`, `staleTime.ts` |
| Frontend-Invalidierung | `frontend/lib/store/cache-invalidation.ts`, `lib/realtime/eventDispatcher.ts` |
| Frontend-Profilbild, Logout | `frontend/lib/store/queries/profileMedia.ts`, `lib/auth/logoutClientCleanup.ts` |

Siehe auch: [Multi-Tenancy](multi-tenancy.md), [Standard](standard.md), [Sicherheit & Berechtigungen](security-and-permissions.md), [Realtime](realtime.md).
