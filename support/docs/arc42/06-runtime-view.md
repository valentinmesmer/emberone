# 6. Laufzeitsicht

Repräsentative End-to-End-Abläufe. Details zu einzelnen Schritten: Concept-Docs.

## 6.1 Login und API-Aufruf (OIDC → JWT → Tenant-Kontext)

```mermaid
sequenceDiagram
  actor U as Nutzer
  participant FE as Frontend
  participant IdP as Authentik
  participant BE as Backend
  participant DB as PostgreSQL

  U->>FE: App öffnen
  FE->>IdP: OIDC Authorize
  IdP->>FE: Redirect + Tokens (JWT)
  FE->>BE: REST + Authorization Bearer
  BE->>IdP: JWKS / Token validieren
  Note over BE: Filter: RequestId → Bearer → TenantContext → DeactivatedUser → TenantBaselineSync
  BE->>BE: TenantContextWebFilter bindet Holder aus JWT
  BE->>BE: @PreAuthorize / SecurityService (Org-Permissions aus DB)
  BE->>DB: Tx begin → set_config app.tenant_id / app.user_id
  DB-->>BE: RLS-gefilterte Daten
  BE-->>FE: JSON-Antwort
```

**Kernpunkte:** JWT trägt nur `userId`, `tenantId`, `app_role` (keine Gruppen/Org-Permissions). Org-Permissions lädt `SecurityService` serverseitig aus der DB. Der effektive Mandant für Queries ist der Holder-/GUC-Pfad, nicht allein der Claim-Filter in Application-Code. Filter-Reihenfolge und Admin-Pfad-Override: [token-and-tenant-context](../concept/token-and-tenant-context.md).

## 6.2 Workflow öffnen und genehmigen

Beispiel: Nutzer startet einen typisierten Workflow (z. B. Adressänderung / Zertifizierungsantrag); Genehmiger entscheidet.

```mermaid
sequenceDiagram
  actor Opener as Ersteller
  actor Approver as Genehmiger
  participant FE as Frontend
  participant BE as WorkflowService
  participant H as WorkflowHandler
  participant Bus as Lifecycle Events
  participant Side as Todo / Notification / Realtime

  Opener->>FE: Submit
  FE->>BE: POST open (typspezifisch)
  BE->>H: validateOpen / resolveContextOrganization
  BE->>BE: effective definition + participants
  BE->>BE: persist instance (+ auto-approve)
  BE->>H: onOpened
  BE->>Bus: lifecycle events
  Bus->>Side: Todos, Mails, STOMP

  Approver->>FE: Inbox / Claim / Decide
  FE->>BE: POST .../workflows/{id}/decisions (APPROVED / REJECTED)
  BE->>H: Domänen-Nebeneffekte
  BE->>Bus: terminal / decision events
  Bus->>Side: Updates an Clients
```

**Kernpunkte:** Die Engine ist generisch; Fachregeln sitzen im Handler und in Standard-/Org-Mappings. Genehmigung/Ablehnung läuft über `POST /api/v1/auth/workflows/{id}/decisions` (`DecisionType`: `APPROVED` / `REJECTED`), nicht über getrennte Approve-/Reject-Endpunkte. Nebenkanäle hängen an Events, nicht im Engine-Kern. Siehe [workflow-engine](../concept/workflow-engine.md).

## 6.3 Realtime-Subscribe auf Org-/Self-Topics

```mermaid
sequenceDiagram
  participant FE as Frontend STOMP
  participant WS as /ws
  participant Auth as RealtimeAuthChannelInterceptor
  participant N as Domain Notifier

  FE->>WS: CONNECT (JWT)
  WS->>Auth: authentifizieren
  FE->>WS: SUBSCRIBE /topic/...
  WS->>Auth: Topic-Autorisierung (Self/Org/Permission)
  Note over N: Domänenänderung nach Commit
  N->>WS: convertAndSend topic
  WS-->>FE: MESSAGE
```

**Kernpunkte:** Subscribe ist berechtigungspflichtig; Notifier publishen nach Domänenänderungen. Siehe [realtime](../concept/realtime.md).
