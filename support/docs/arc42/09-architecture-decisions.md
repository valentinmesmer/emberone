# 9. Architekturentscheidungen

ADR-Log für teure oder tragende Entscheidungen. Ausführliche Domänenbegründungen bleiben in den Concept-Docs; hier der Entscheidungsüberblick.

| ID | Entscheidung | Status | Begründung |
|----|--------------|--------|------------|
| [ADR-001](#adr-001---modularer-monolith-statt-microservices) | Modularer Monolith (Spring Boot) | akzeptiert | Konsistenz Workflows/Notifications/RLS; Package-Modularität ohne Netzwerkkosten |
| [ADR-002](#adr-002---postgres-force-rls-für-mandantenisolation) | Postgres FORCE RLS + Session-GUCs | akzeptiert | Defense-in-depth gegen Cross-Tenant-Leaks; App-Filter allein reicht nicht |
| [ADR-003](#adr-003---authentik-als-externer-idp-permissions-in-der-db) | Authentik als IdP (SPI für Provisioning); Org-Permissions in DB | akzeptiert | Login/Rollen extern; dynamische Org-Permissions nicht im JWT autoritativ |
| [ADR-004](#adr-004---generische-workflow-engine--typspezifische-handler) | Generische Workflow-Engine + Handler-SPI | akzeptiert | Ein Lebenszyklus für viele Prozesse; Fachlogik isoliert in Handlern |
| [ADR-005](#adr-005---zentrale-facades-bewusst-dokumentieren-statt-sofort-zerlegen) | Zentrale Facades (`SecurityService`, `WorkflowService`, …) | akzeptiert (mit Schuld) | Hohe Kopplung ist derzeit tragfähig; Zerlegung folgt dokumentierten Grenzen |
| [ADR-006](#adr-006---nextjs-frontend-mit-selforgadmin-routen) | Next.js mit Self/Org/Admin-Routen | akzeptiert | Spiegelt Backend-Controller-Muster; klares UI-Gating |
| [ADR-007](#adr-007---redis-cache-mit-after-commit-eviction) | Redis-Cache mit event-getriebener After-Commit-Eviction | akzeptiert | Performance für Permission-/Org-Lookups ohne stale Writes; TTL nur Sicherheitsnetz |
| [ADR-008](#adr-008---stomp-über-websocket-mit-redis-fan-out) | STOMP/WebSocket, lokaler Broker, Redis-Pub/Sub-Fan-out; thin Events auf Topics | akzeptiert | Live-Updates ohne externen Broker; Autorisierung erst beim Re-Fetch |
| [ADR-009](#adr-009---einheitlicher-paginierter-listenvertrag) | Ein Listen-Vertrag (`ListQuery`/`ListDefinition`/`PageDto`), Ausnahme `@BoundedList` | akzeptiert | Wachsende Collections skalieren; ein Weg statt Parallel-Parameter |
| [ADR-010](#adr-010---release-modell-mit-semantic-release-und-unveränderlichen-image-tags) | `development`/`main`, Semantic Release, unveränderliche Image-Tags | akzeptiert | Reproduzierbare Rollbacks und AGPL-Nachweis; Version = Git-Tag |
| [ADR-011](#adr-011---retention-per-hard-delete-statt-anonymisierung) | Retention nur per Hard Delete, Fristen je Bundesland-Profil | akzeptiert | Anonymisierung in kleinen Wehren re-identifizierbar; Handler je Löschklasse |
| [ADR-012](#adr-012---pwa-ohne-offline-datenschicht) | PWA mit Precache und Offline-Fallback, kein Offline-First | akzeptiert | API-Daten sind auth-scoped; keine Sync-Queue-Komplexität |

Das Kopplungsrisiko liegt primär in **zentralen Facades** mit hoher Fan-in-Nutzung, nicht in Package-Grenzen allein - daher ADR-005.

---

## ADR-001 - Modularer Monolith statt Microservices

- **Kontext:** Viele Domänen (Org, Workflow, Qualifikation, …) teilen Transaktionen und Security-Kontext.
- **Entscheidung:** Ein Backend-Deployable mit Domain-Packages.
- **Konsequenzen:** Einfachere Konsistenz; Disziplin bei Package-Grenzen nötig.

## ADR-002 - Postgres FORCE RLS für Mandantenisolation

- **Kontext:** Shared-DB Multi-Tenancy; Fehler = Datenleck.
- **Entscheidung:** RLS erzwingen; `app.tenant_id` / `app.user_id` pro Transaktion setzen.
- **Konsequenzen:** Jede neue Tabelle braucht RLS-Policy; Admin-Pfade überschreiben Holder bewusst. Details: [multi-tenancy](../concept/multi-tenancy.md).

## ADR-003 - Authentik als externer IdP; Permissions in der DB

- **Kontext:** Identitäten und Login sollen nicht selbst gebaut werden; Org-Permissions ändern sich häufig.
- **Entscheidung:** OIDC/JWT derzeit über Authentik; Claims nur `userId` / `tenantId` / `app_role`. App-Zugriff auf den IdP über `IdentityProviderUserProvisioner` / `IdentityProviderKind` (Sync einseitig EmberOne→IdP). Org-`PermissionType`-Auflösung über Mitgliedschaften/Mappings in EmberOne (nicht im JWT).
- **Konsequenzen:** Aktuell nur Authentik verdrahtet (`resolvedKind()` fest). Weitere IdPs brauchen Enum + Provisioner + OIDC-/Claim-Setup (Blueprints heute Authentik-spezifisch). Caching/Eviction für Permissions kritisch. Details: [security-and-permissions](../concept/security-and-permissions.md), [identity-provider-authentik](../concept/identity-provider-authentik.md).

## ADR-004 - Generische Workflow-Engine + typspezifische Handler

- **Kontext:** Viele Freigabeprozesse mit gleichem Lebenszyklus.
- **Entscheidung:** `WorkflowService` + `WorkflowHandler` pro `WorkflowType`; Konfiguration über Standards/Mappings.
- **Konsequenzen:** Engine bleibt schlank; neue Prozesse = neuer Handler + Config, kein Fork der Engine. Details: [workflow-engine](../concept/workflow-engine.md).

## ADR-005 - Zentrale Facades bewusst; dokumentieren statt sofort zerlegen

- **Kontext:** Zentrale Typen mit hoher Kopplung: `Workflow`, `TokenService`, `Standard`, `Organization`, `SecurityService`, …
- **Entscheidung:** Zentrale Facades beibehalten, aber mit Concept-Docs und klaren Unter-Services (`OrganizationAccessService`, Handler, …) entschärfen; keine Big-Bang-Zerlegung ohne Schnittstellenvertrag.
- **Konsequenzen:** Onboarding braucht die Docs; Refactors priorisieren Hotspots mit Tests. Siehe auch [11-risks](11-risks-and-technical-debt.md).

## ADR-006 - Next.js Frontend mit Self/Org/Admin-Routen

- **Kontext:** Drei API-Flächen im Backend.
- **Entscheidung:** Routengruppen `/my-area`, `/organizations/[id]`, `/admin` + spiegelnde Permission-Hooks.
- **Konsequenzen:** UI-Struktur folgt Security-Modell; Abweichungen Frontend↔Backend sind ein bekanntes Risiko (concept/frontend-auth).

## ADR-007 - Redis-Cache mit After-Commit-Eviction

- **Kontext:** Permission-, Org-, Standard- und Tenant-Lookups laufen auf jedem Request. Ein veralteter Cache wäre bei Berechtigungen ein Security-Fehler.
- **Entscheidung:** Spring Cache über Redis mit benannten Caches (`CacheNames`) und pro-Cache-TTL. Invalidierung ereignisgetrieben: Domänenservices publizieren Events, `@AfterCommitEventListener` (AFTER_COMMIT, `REQUIRES_NEW`) ruft Evictors auf. Kein `@CacheEvict` im mutierenden Service. Null-Werte werden nicht gecacht. Auth-sensitive Caches (`userPermissions`, `activeOrgMembership`, `userActive`) haben kurze TTLs; Locale liegt getrennt von Permissions.
- **Konsequenzen:** Ein fehlender Eviction-Listener ist ein Korrektheitsfehler; die TTL begrenzt nur die Veraltung. Mandantenspezifische Keys enthalten `tenantId`, wo Kollisionen möglich sind. Tests laufen mit `spring.cache.type: simple`. Details und Inventar: [caching](../concept/caching.md).

## ADR-008 - STOMP über WebSocket mit Redis-Fan-out

- **Kontext:** Inbox, Notifications, Todos, Workflows und Org-Änderungen sollen live aktualisieren; das Backend läuft potenziell mit mehreren Instanzen.
- **Entscheidung:** STOMP über natives WebSocket (`/ws`, kein SockJS) mit lokalem In-Memory-Broker je Instanz. Publish läuft nach DB-Commit über `RealtimePublisher` und Redis Pub/Sub (`emberone:realtime`); jede Instanz liefert lokal aus. Auf `/topic` gehen nur **thin** Events (Invalidierung), zielgerichtete Updates über `/user/queue/*` (dort auch rich Payloads). Auth bei CONNECT (JWT) und SUBSCRIBE (`TopicAuthorizationService`); Sessions werden bei Token-Ablauf mit Code 4001 geschlossen. Feature-Flag `emberone.realtime.enabled`; Frontend fällt auf Polling zurück.
- **Konsequenzen:** Kein externer Message-Broker nötig, dafür Abhängigkeit von Redis für Mehrinstanz-Betrieb. Mandantenweite List-Topics sind nur mandantengeprüft; Audience- und Permission-Regeln greifen beim REST-Re-Fetch. Neue Entity-Topics brauchen einen `TopicAuthorizer`. Details: [realtime](../concept/realtime.md).

## ADR-009 - Einheitlicher paginierter Listenvertrag

- **Kontext:** Wachsende Collections (Mitglieder, Termine, Todos, Zeiteinträge, …) dürfen weder ungebremst geladen noch pro Endpoint anders gefiltert werden.
- **Entscheidung:** Jeder Collection-GET nutzt `ListQuery` + `ListDefinition` (Sort-Whitelist, Suchfelder) + `Specification` und liefert `PageDto` (Default 50, Maximum 200). Filter sind eigene Query-Parameter pro Endpoint, keine Legacy-Parameter wie `openOnly`/`archived`. Ausnahme nur mit `@BoundedList` und englischer Begründung (Kataloge, natürlich begrenzte Sub-Ressourcen). Frontend: `defineList` + `useListState` + `ListControls`; kein clientseitiges `.filter()`/`.sort()` auf paginierten Daten.
- **Konsequenzen:** `BoundedListConventionTest` und `check-list-legacy-params.sh` erzwingen die Regel. Gruppierte Übersichten filtern serverseitig und sind keine Seite. Keine generische Filter-DSL. Details: [pagination](../concept/pagination.md).

## ADR-010 - Release-Modell mit Semantic Release und unveränderlichen Image-Tags

- **Kontext:** Open-Source-Betrieb durch Dritte; Betreiber brauchen reproduzierbare Rollbacks, und AGPL-3.0 §13 verlangt, dass die laufende Version auf ihren Quellcode verweist.
- **Entscheidung:** Default-Branch `development` erzeugt Prereleases (`vX.Y.Z-dev.N`), stabile Releases (`vX.Y.Z`) nur durch Merge-Commit von `development` nach `main` (kein Squash). Semantic Release leitet die Version aus Conventional Commits ab; der Git-Tag ist maßgeblich. Backend- und Frontend-Image haben dieselbe Version und werden nur nach veröffentlichtem Release aus dem Tag gebaut. `X.Y.Z[-dev.N]` ist unveränderlich, `latest`/`X`/`X.Y`/`dev` sind beweglich. Alte GHCR-Versionen werden nicht gelöscht.
- **Konsequenzen:** Produktion pinnt auf `X.Y.Z`, Staging darf `dev`. Das Frontend-Image bäckt Version und Quell-URL ein (`/source`). Forks setzen `SOURCE_REPO`. Details: [07-deployment-view](07-deployment-view.md).

## ADR-011 - Retention per Hard Delete statt Anonymisierung

- **Kontext:** Personenbezogene Daten müssen nach Fristen gelöscht werden (DSGVO, BayDSG); Verantwortlicher ist der jeweilige Tenant, die Fristen hängen vom Bundesland ab.
- **Entscheidung:** Nur Hard Delete, auch für den User-Datensatz. Anonymisierung wird verworfen (in kleinen Wehren re-identifizierbar), Soft Delete ist kein Ersatz. Modell nach DIN 66398: eine Löschklasse = ein Startzeitpunkt = eine Frist = ein `RetentionHandler`; Fristen liefert das `RetentionProfile` des Bundeslands (aktuell nur Bayern, Platzhalterfristen bis zur DSB-Freigabe). Handler löschen mengenbasiert in Batches, nächtlich je Tenant und Handler in eigener Transaktion. Das Audit-Log speichert keine Wertkopien. S3-Objekte und IdP-Accounts werden erst nach Commit über Outbox mit Retry gelöscht.
- **Konsequenzen:** Statistik und Archivierung über die Frist hinaus liegen beim Tenant außerhalb der App. Betreiber müssen Fristen vor produktivem Einsatz mit dem DSB abstimmen. Neue Tabellen mit Personenbezug brauchen Abdeckung durch einen Handler (Abdeckungstest). Details: [retention-and-erasure](../concept/retention-and-erasure.md), [user-delete-foreign-keys](../concept/user-delete-foreign-keys.md).

## ADR-012 - PWA ohne Offline-Datenschicht

- **Kontext:** Die App soll installierbar sein und bei Verbindungsverlust nicht abstürzen. API-Daten sind mandanten- und auth-scoped.
- **Entscheidung:** PWA mit Serwist: Precache der Build-Assets und Offline-Fallback `/~offline`. `/api/*` und `/runtime-config.json` sind `NetworkOnly`. Kein Offline-Editing, keine Sync-Queue. Beim Logout werden alle Cache-Storage-Einträge geleert. Der Service Worker ist in Development deaktiviert.
- **Konsequenzen:** Keine Offline-Nutzung von Live-Daten, dafür kein Risiko veralteter oder fremder Auth-Daten im Cache. `runtime-config.json` wird bewusst nicht precached, weil der Container sie pro Deploy schreibt. Details: [frontend-pwa-offline](../concept/frontend-pwa-offline.md), [frontend-auth](../concept/frontend-auth.md).
