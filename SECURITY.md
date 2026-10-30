# Sicherheitsrichtlinie

## Unterstützte Versionen

Unterstützt wird nur der jeweils neueste stabile Release (`main`). Sicherheitsfixes landen auf `development` und werden zeitnah als Patch-Release nach `main` gebracht. Einen Long-Term-Support-Branch gibt es nicht.

## Schwachstelle melden

EmberOne verarbeitet mandantenfähige Organisations- und Personendaten hinter Authentifizierung. Bitte **keine** öffentlichen Issues für Sicherheitslücken eröffnen.

Bevorzugt privat über [GitHub Private Vulnerability Reporting](https://github.com/valentinmesmer/emberone/security/advisories/new), alternativ per Mail an **opensource@valentinmesmer.de**.

Bitte angeben:

- Betroffene Komponente (Backend-API-Pfad, Frontend-Route, RLS-/Tenant-Pfad, IdP-Integration)
- Reproduktionsschritte oder Proof-of-Concept (bevorzugt nicht-destruktiv)
- Einschätzung der Auswirkungen (mandantenübergreifendes Lesen/Schreiben, Privilege Escalation, Auth-Bypass)

Du solltest innerhalb weniger Werktage eine Bestätigung erhalten. Bitte gib den Maintainern angemessene Zeit für einen Fix, bevor etwas öffentlich wird.

## Sicherheitsrelevante Bereiche

Änderungen in diesen Bereichen verdienen besonders gründliche Review:

- JWT-/OIDC-Validierung und Claim-Mapping
- `TenantContextHolder` / Postgres-Session-GUCs / FORCE-RLS-Policies
- `SecurityService` und Access-Services (`AdminAccessService`, `OrganizationAccessService`, …)
- Autorisierung von Realtime-Topics
- Object-Storage-Key-Scoping (Profilbilder, Dokumente, Attachments)
