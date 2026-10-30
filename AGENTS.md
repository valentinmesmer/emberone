# AGENTS.md

For humans: [`CONTRIBUTING.md`](CONTRIBUTING.md). Docs: [`support/docs/concept/`](support/docs/concept/00-index.md), [`support/docs/arc42/`](support/docs/arc42/00-index.md).

Multi-tenant fire-service app. Spring Boot monolith + Next.js. OIDC via Authentik. Postgres FORCE RLS.

**Code / commits: English.** Docs: German. Conventional Commits. Do not invent IdP or deploy behavior.

Reuse existing patterns (same package, API surface, concept doc). No second way, no workarounds; if blocked, ask.

## Rules

- New domain tables: FORCE RLS. Tenant via `TenantContextHolder` → GUC `app.tenant_id`. Org authz via `SecurityService`, not a JWT claim or query filter:

  ```java
  // RLS uses the session GUC. Do not add WHERE tenant_id = :tenantId
  @PreAuthorize("@securityService.hasUserWritePermission(#organizationId)")
  ```

- Keep Self / Org / Admin APIs and UI routes separate (`/api/v1/self` ↔ `/my-area`, `/api/v1/auth/organizations/{id}` ↔ `/organizations/[id]`, `/api/v1/auth/admin` ↔ `/admin`).
- Default branch is `development` (prereleases `vX.Y.Z-dev.N`). Stable GitHub releases and `:latest` / `:X` / `:X.Y` images only from `main` (merge commit from `development`, no squash). Prereleases tag `:X.Y.Z-dev.N` and `:dev` only. Pin production to the immutable version tag; do not delete old GHCR versions.
- New `PermissionType`: same numeric id in backend **and** frontend.
- Workflows: handler per type, do not fork `WorkflowService`.
- Frontend: `useTranslation` + `locales/de.json`; UI via `components/ui/*`; data via store hooks; thin `page.tsx`.
- List GETs: paginated (`ListQuery` + `ListDefinition` + `Specification`); exception only `@BoundedList`. Frontend: `defineList` + `useListState` + `ListControls`. No client `.filter`/`.sort` on paginated data; no legacy params like `openOnly` or `archived`.
- Frontend actions (one pattern per intent): create → labeled `Button` in `PageHeader` / `SectionHeader` (`SectionActions layout="inline"`); edit current surface → `SectionEditAction` (section cards) or `DetailCardHeader` + `DetailCardAction` (entity detail cards); row edit/delete → `ListRow.actions`; navigate → `href`/chevron or hub tiles; rare/destructive/admin lifecycle → `ActionsSection` + `ListRow` (uniform look; no tone accents).
- API/permission/lifecycle change → update the concept doc. Security/tenant/RLS → `*SecurityTest`. AGPL headers on new sources.

## Check

Run only what the change touches. Full suite before a PR: [`CONTRIBUTING.md`](CONTRIBUTING.md).

```bash
python3 support/scripts/add-copyright-headers.py --check   # new sources
cd backend && ./gradlew test --tests '*FooServiceTest'
cd frontend && bun lint
```

No secrets. No copyrighted or incompatibly licensed code. Vulnerabilities: [`SECURITY.md`](SECURITY.md).
