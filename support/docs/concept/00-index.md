# Konzept-Dokumentation - EmberOne

Domänenorientierte Design-Dokumente für tragende Abstraktionen. Sie ergänzen die strukturelle [arc42](../arc42/00-index.md)-Architekturdokumentation.

## Genre-Contract

Jedes Konzept-Dokument soll enthalten:

1. **Design-Notizen**-Öffnung (kurz: Zweck, Package/Modul, Abgrenzung)
2. **Mental Model** (Zustandsautomat, Tabelle oder kurzes Diagramm)
3. **Kernentitäten** oder ein Inventar (Typen, Jobs, Topics, …)
4. **Quellcode-Karte** (wo der Code liegt)
5. Querverweise auf verwandte Konzept-Docs

Prosa auf Deutsch; Identifier (Klassen, Enums, Properties, Pfade) auf Englisch wie im Code. Kein erfundenes IdP- oder Deploy-Verhalten - nur was im Repo bzw. in den Betriebsdocs steht.

---

## Plattform & Sicherheit

| Dokument | Inhalt |
|----------|--------|
| [Multi-Tenancy](multi-tenancy.md) | Holder → JPA → Postgres-GUCs / FORCE RLS, Admin-Pfad-Override |
| [Sicherheit & Berechtigungen](security-and-permissions.md) | `SecurityService`-Fassade, Access-Services, `PermissionType`, Self-/Organization-/Admin-Muster |
| [Token & Mandantenkontext](token-and-tenant-context.md) | `TokenService`, JWT-Claims, `TenantContextHolder` vs. JWT, Admin-Pfad-Override |
| [Identity Provider (Authentik)](identity-provider-authentik.md) | Derzeit Authentik; Provisioning-SPI, Kind noch fest verdrahtet |
| [Permission Groups & Mapping](permission-groups.md) | Katalog, Org-Gruppen, Mappings, Auflösung vs. SecurityService |
| [Effektive User-Permissions](userpermission.md) | `CachedUserPermissionLoader`, Self-API, Cache vs. Locale |
| [Globale Exception-Behandlung](global-exception-handling.md) | Fehlerantwort-Vertrag, wann neuer Handler |
| [Bundesland](federal-state.md) | `FederalState`, `FederalStateSettings`, Bayern-Baseline, `RetentionProfile` |
| [Lifecycle & Statusfelder](lifecycle-and-status-fields.md) | `active` / `archived` / `enabled` / `end_date` - wann welches Feld |
| [Zeitbehandlung](time-handling.md) | Org-Wanduhr vs. System-Instant, API-Formate, Frontend-Regeln |
| [Paginierung (Backend)](pagination.md) | `PageDto` / `PageResponse`, Default 50 / Max 200, `fetchAllPageContent` |

## Organisation & Benutzer

| Dokument | Inhalt |
|----------|--------|
| [Organisation & Mitgliedschaft](organization.md) | Org-Baum, Org-Gruppen, Mitgliedschaftsebenen, Berechtigungskaskade in Gruppen |
| [Org-/Gruppen-Mitgliedschaften](user-memberships.md) | Org-Membership vs. Group-Membership, Permission-Voraussetzung |
| [User](user.md) | Kern-User, Kontakt, Provisioning, Self-Profil |
| [Admin](admin.md) | Tenant-/User-/Standard-/Katalog-Admin-APIs |
| [Onboarding](onboarding.md) | Steps am Standard, Einladung/Wizard, Promote, Org-Eintrittsdokumente, Admin-Erfassung |
| [Standard](standard.md) | Organisations-Setup-Blueprints, `StandardDomain`-SPI, Materialisierung, effektive Auflösung, Baseline-Sync |
| [Management-Modul](management.md) | Verwaltende Organisationen, Audience-Konfiguration, Aktivierung, Zugriffsauswertung, Aktivierungsprotokoll |
| [Module & Standard Module Config](module-and-standard-module-config.md) | `Module`-Enum, Disabled-Rows, EnabledModuleResolver |
| [Abwesenheit & Notfallkontakt](absence-and-emergencycontact.md) | Absences inkl. Org-Links; Emergency Contacts inkl. Priority |
| [Profilbild](profileimage.md) | Ein Bild pro User, S3-Key-Hardening |

## Domäne

| Dokument | Inhalt |
|----------|--------|
| [Workflow-Engine](workflow-engine.md) | `Workflow` / `WorkflowService`, Typen, Handler, Teilnehmer/Slots/Entscheidungen, Konfigurationsebenen |
| [Qualifikation](qualification.md) | Übungsstatus-Zustandsautomat, Org-Entscheidungen/Intervalle, Voraussetzungen, Sync |
| [Todo](todo.md) | Aufgaben-Inbox, Typen, Workflow-/Cert-Treiber |
| [Training & Zertifizierung](trainingcourse-and-certification.md) | Kataloge, Submissions/Assignments/Records, Event-Grants, Reminder |
| [Führerschein](driving-license.md) | Führerscheinklassen-Katalog, User-Dokument mit Klassen, Workflow, Qualifikations-Voraussetzung |
| [Dienstgrad und Funktion](rank-and-function.md) | Dienstgrad-Historie, Funktionen aus Standard-Grants, Onboarding/`RANK_REQUEST` |
| [Appointment](appointment.md) | Termine vs. Events, Serien, RSVP-Modi (`AppointmentResponseMode`), Promote |
| [Event (Dienst/Einsatz)](event.md) | Service-/OperationEvent, Attendance, Promote aus Appointment |
| [Lehrgangsangebot](courseoffering.md) | Angebote und Termine, Zielgruppe, Voraussetzungen, Vorlagen, Anmeldung/Absage, Kontingente, Abschluss, Org- und Mein-Bereich-UI |
| [Zeiteintrag](timeentry.md) | Freie Time-Entries, Relation zu Event-Stunden |

## Dokumente & Retention

| Dokument | Inhalt |
|----------|--------|
| [Attachment & Dokument-Promotion](attachment-and-document-promotion.md) | EntityAttachment, Buckets, Draft→UserDocument |
| [User-Dokumente](userdocument.md) | Persönliches Archiv, Tags, Links, Self-/Org-API |
| [Aufbewahrung und Löschung](retention-and-erasure.md) | Löschklassen, Bayern-Profil, Querschnitt, Tenant-Purge, Begründungen, **Betrieb** (Backup/Logs/Restore) |
| [User-Löschung: Fremdschlüssel](user-delete-foreign-keys.md) | CREATE- + nachgezogene FKs: CASCADE / SET NULL je Tabelle |

## Infra

| Dokument | Inhalt |
|----------|--------|
| [Caching](caching.md) | Redis-Caches, TTLs, After-Commit-Eviction-Listener |
| [Logging](logging.md) | SLF4J-Levels, fachliches Logging, MDC, ECS JSON, Request-Korrelation |
| [Realtime / WebSocket](realtime.md) | STOMP `/ws`, Topic-Namensgebung, Subscribe-Auth, Domain-Notifier |
| [Benachrichtigungssystem](notification-system.md) | Outbox, Typ-Registry, Kanäle, neuen Typ hinzufügen |
| [Geplante Jobs](scheduled-jobs.md) | Alle `@Scheduled`-Jobs, Crons, `TenantBatchRunner` |
| [Skalierbarkeit](scalability.md) | Instanzunabhängiger Betrieb, Request-Pfad, Datenwachstum |

## Frontend

| Dokument | Inhalt |
|----------|--------|
| [Frontend Auth & Berechtigungen](frontend-auth.md) | `useAuth`, `useHasEmberOneUserId`, JWT-Claims, `usePermissions` |
| [Frontend i18n](frontend-i18n.md) | `useTranslation`, Locale-Quelle, Key-Konventionen, `locales/de.json` |
| [Frontend Component-Library](frontend-component-library.md) | `components/ui/*`, `ListControls` |
| [Frontend Detail-Routing](frontend-detail-routing.md) | Dünne `page.tsx` → `*DetailView` |
| [Frontend PWA / Offline](frontend-pwa-offline.md) | Serwist, Precache, Offline-Fallback |
| [Frontend API-Client](frontend-api-client.md) | `apiClient`, Runtime Config, Store-Hooks |
| [Paginierung von Listen](pagination.md) | `ListQuery`, `defineList`, `ListControls`, `@BoundedList` |
