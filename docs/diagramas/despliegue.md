# Diagrama de despliegue

> Generado automáticamente por `scripts/docs/build-diagrams.mjs`. No editar a mano.

Infraestructura aprovisionada con Terraform (módulos: database (database-neon), app (app-render)) y automatizada con GitHub Actions. Costo mensual en capa gratuita: USD 0 (ver infra/terraform/cost-reference).

```mermaid
flowchart TB
  estudiante(["Estudiante<br/>teléfono Android"])
  admin(["Administración<br/>navegador"])
  dev(["Equipo<br/>git push"])
  subgraph GitHub
    repo["Repositorio Laos19/TareaIA"]
    gha["GitHub Actions<br/>ci · android · mutación · sonar · semgrep · snyk · infra · release · docs · pages"]
    pages["GitHub Pages<br/>documentación técnica"]
    rel["Releases<br/>APK · API · dashboard · MCP"]
  end
  subgraph Render["Render (free)"]
    dash["Static Site<br/>dashboard/dist"]
    api["Web Service Node 22<br/>backend/dist/server.js"]
  end
  subgraph Neon["Neon (free)"]
    pg[("PostgreSQL 16")]
  end
  subgraph Google["Google / Firebase"]
    fb["Firebase Auth + AI Logic"]
    gcal["Google Calendar"]
  end
  hcp["HCP Terraform<br/>estado remoto"]
  dev --> repo --> gha
  gha -- "terraform apply" --> Render
  gha -- "terraform apply" --> Neon
  gha -. "estado" .-> hcp
  gha -- "liquibase update" --> pg
  gha -- "APK + artefactos" --> rel
  gha -- "deploy pages" --> pages
  gha -- "deploy hook" --> api
  estudiante -- "descarga APK" --> rel
  estudiante -- "HTTPS /api" --> api
  estudiante -- "Google Sign-In · Gemini" --> fb
  estudiante -- "eventos" --> gcal
  admin -- HTTPS --> dash
  dash -- "HTTPS /api" --> api
  api -- "TLS 5432" --> pg
```
