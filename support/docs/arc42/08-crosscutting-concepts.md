# 8. Querschnittliche Konzepte

Übergreifende Mechanismen sind in [`support/docs/concept/`](../concept/00-index.md) ausgearbeitet. Dieses Kapitel ist der **Index** aus arc42-Sicht.

## 8.1 Domänenmodell

| Thema | Dokument |
|-------|----------|
| Organisation & Mitgliedschaft | [organization.md](../concept/organization.md) |
| Standard / Materialisierung | [standard.md](../concept/standard.md) |
| Onboarding | [onboarding.md](../concept/onboarding.md) |
| Workflow-Engine | [workflow-engine.md](../concept/workflow-engine.md) |
| Qualifikation | [qualification.md](../concept/qualification.md) |
| Permission Groups & Mapping | [permission-groups.md](../concept/permission-groups.md) |
| Todo | [todo.md](../concept/todo.md) |
| Attachment & Dokument-Promotion | [attachment-and-document-promotion.md](../concept/attachment-and-document-promotion.md) |
| Training & Zertifizierung | [trainingcourse-and-certification.md](../concept/trainingcourse-and-certification.md) |
| Führerschein | [driving-license.md](../concept/driving-license.md) |
| Appointment | [appointment.md](../concept/appointment.md) |
| Module & Standard Module Config | [module-and-standard-module-config.md](../concept/module-and-standard-module-config.md) |
| Admin | [admin.md](../concept/admin.md) |
| Event (Dienst/Einsatz) | [event.md](../concept/event.md) |
| User | [user.md](../concept/user.md) |
| Org-/Gruppen-Mitgliedschaften | [user-memberships.md](../concept/user-memberships.md) |
| User-Dokumente | [userdocument.md](../concept/userdocument.md) |
| Abwesenheit & Notfallkontakt | [absence-and-emergencycontact.md](../concept/absence-and-emergencycontact.md) |
| Zeiteintrag | [timeentry.md](../concept/timeentry.md) |
| Zeitbehandlung | [time-handling.md](../concept/time-handling.md) |
| Lifecycle & Statusfelder | [lifecycle-and-status-fields.md](../concept/lifecycle-and-status-fields.md) |
| Bundesland | [federal-state.md](../concept/federal-state.md) |
| Profilbild | [profileimage.md](../concept/profileimage.md) |
| Effektive User-Permissions | [userpermission.md](../concept/userpermission.md) |

Vollständiger Index: [`support/docs/concept/00-index.md`](../concept/00-index.md).

## 8.2 Persistenz

- JPA + PostgreSQL; Schema über Flyway
- Mandantenisolation: FORCE RLS auf Domänentabellen (derzeit 89 in `V1__Initial_setup.sql`) + Session-GUCs `app.tenant_id` / `app.user_id` - [multi-tenancy.md](../concept/multi-tenancy.md)
- Repository-Transaktionen (Typebene `readOnly`, schreibende und Lock-Methoden) sichern die GUCs auch außerhalb transaktionaler Beans - [multi-tenancy.md](../concept/multi-tenancy.md#jpa--transaktionsscoping)
- Object Storage (S3) für Binärdaten - Anbindung über `platform.storage`
- User-Löschung / FK-Verhalten: [user-delete-foreign-keys.md](../concept/user-delete-foreign-keys.md)
- Aufbewahrung / Hard-Delete-Klassen: [retention-and-erasure.md](../concept/retention-and-erasure.md) (auch unter 8.5)

## 8.3 Authentifizierung & Autorisierung

Architektur-Kern (Details in den Concept-Docs):

- JWT-Claims für Auth: nur `userId`, `tenantId`, `app_role` - keine Gruppen-/Org-Permissions im Token
- Filter-Kette: `RequestIdFilter` → Bearer → `TenantContextWebFilter` → `DeactivatedUserWebFilter` → `TenantBaselineSyncFilter`
- Org-AuthZ über `SecurityService` / Access-Services aus der DB; App-Rolle aus JWT
- IdP: Authentik fest verdrahtet; Provisioning einseitig App→IdP

| Schicht | Dokument |
|---------|----------|
| JWT / TokenService / Tenant-Claims | [token-and-tenant-context.md](../concept/token-and-tenant-context.md) |
| Mandantenisolation (Holder → GUCs / RLS) | [multi-tenancy.md](../concept/multi-tenancy.md) |
| SecurityService & PermissionType | [security-and-permissions.md](../concept/security-and-permissions.md) |
| Authentik / Provisioning | [identity-provider-authentik.md](../concept/identity-provider-authentik.md) |
| Frontend Auth & `usePermissions` | [frontend-auth.md](../concept/frontend-auth.md) |

## 8.4 Fehlerbehandlung / Exception Handling

Einheitlicher API-Fehlervertrag und Handler-Regeln: [global-exception-handling.md](../concept/global-exception-handling.md).

## 8.5 Caching, Realtime, Notifications, Jobs, Retention

| Thema | Dokument |
|-------|----------|
| Redis-Caches & Eviction | [caching.md](../concept/caching.md) |
| STOMP / Topic-Auth | [realtime.md](../concept/realtime.md) |
| Notification-Outbox & Kanäle | [notification-system.md](../concept/notification-system.md) |
| `@Scheduled`-Jobs | [scheduled-jobs.md](../concept/scheduled-jobs.md) |
| Skalierbarkeit (Multi-Instanz, Request-Pfad, Datenwachstum) | [scalability.md](../concept/scalability.md) |
| Aufbewahrung und Löschung | [retention-and-erasure.md](../concept/retention-and-erasure.md) |

## 8.6 Logging & Monitoring

Konzept: [logging.md](../concept/logging.md) (Geltungsbereich, Levels, MDC, ECS JSON, Request-Korrelation).

- MDC: `tenantId` / `userId` / `requestId` (Tenant-Kontext inkl. HTTP, Batch, WebSocket)
- Structured Console: Spring Boot ECS JSON (Default in `application.yml`); Test-`application.yml` überschreibt das ECS-Format nicht - für plain lokal `CONSOLE_LOG_STRUCTURED_FORMAT` leer setzen; `X-Request-Id` Accept / Generate / Echo
- Domänen- und Security-Tests als primäres Qualitätsnetz in CI
- Nicht im Logging-Konzept: Frontend-Console, OpenTelemetry/Trace-IDs, Log-Shipper/Retention/Alerting (Deploy/Betrieb)

## 8.7 Frontend / i18n

- Frontend: [frontend-i18n.md](../concept/frontend-i18n.md)
- Frontend Component-Library: [frontend-component-library.md](../concept/frontend-component-library.md)
- Frontend Detail-Routing: [frontend-detail-routing.md](../concept/frontend-detail-routing.md)
- Frontend PWA / Offline: [frontend-pwa-offline.md](../concept/frontend-pwa-offline.md)
- Frontend API-Client: [frontend-api-client.md](../concept/frontend-api-client.md)
- Backend: Message-Bundles (`messages_de.properties`) und Mail-Templates unter `resources/templates`

## 8.8 Tests

- Backend: `./gradlew build` inkl. Jacoco; umfangreiche `*SecurityTest` / WebMvc-Tests
- Frontend: `bun lint`, `bun build` in CI
- Change-Detection in GitHub Actions begrenzt Jobs auf betroffene Pfade

## 8.9 API-Dokumentation (OpenAPI / Swagger)

- Dependency: `springdoc-openapi-starter-webmvc-ui` (OpenAPI 3 + Swagger UI)
- Aktiv nur wenn `SPRINGDOC_ENABLED=true` (Default `false` in `application.yml` für api-docs und swagger-ui)
- Kanonische UI unter `/swagger-ui.html` / `/swagger-ui/index.html`; OpenAPI-JSON unter `/v3/api-docs`
- `SwaggerUiAliasController` (`platform.config`) leitet `/api/swagger-ui*` auf die springdoc-Pfade um (Symmetrie zu `/api/v1/**`)

## 8.10 Build, Deployment, CI/CD

Siehe [07-deployment-view](07-deployment-view.md) und [support/dev/DEV.md](../../dev/DEV.md).
