# Automatizaciones (GitHub Actions)

Cada workflow publica un **reporte en Markdown** en el resumen de la ejecución (*Summary*) y lo sube como artefacto.
`pages.yml` reúne los últimos reportes en GitHub Pages (`/reportes/`).

| Workflow | Cuándo corre | Qué hace | Reporte |
|---|---|---|---|
| `ci.yml` | push a ramas y PR | Typecheck, actionlint, `terraform fmt`; pruebas unitarias (con cobertura), de integración y de sistema de la API; unitarias del dashboard | `reporte-ci-pruebas` |
| `android.yml` | cambios en `android/` | `assembleDebug`, pruebas JVM, Android Lint y pruebas de interfaz Compose en emulador API 34 | `android-apk-y-reportes`, `reporte-android-interfaz` |
| `mutacion.yml` | push a main, PR | Stryker (API) y Pitest (dominio Kotlin) | `reporte-mutacion-api`, `reporte-mutacion-android` |
| `sonarqube.yml` | push a main, PR | SonarQube Cloud con cobertura; tabla de Bugs, Vulnerabilities y Security Hotspots y Quality Gate | `reporte-sonarqube` |
| `semgrep.yml` | push a main, PR, semanal | SAST con 8 conjuntos de reglas; SARIF a *Code Scanning*; falla si hay hallazgos ERROR | `reporte-semgrep` |
| `snyk.yml` | push a main, PR, semanal | Dependencias npm y Gradle, Snyk Code y Terraform | `reporte-snyk` |
| `infra.yml` | push/PR a main, manual | Terraform fmt, validate, tflint, **terraform test**, checkov; pruebas de la app (unitarias, integración, sistema, interfaz, aceptación); **reporte de costos**; plan/apply en producción | `reporte-terraform-calidad`, `reporte-pruebas`, `reporte-costos`, `reporte-terraform-produccion` |
| `release.yml` | push a main | release-please (versión y CHANGELOG); APK, API, dashboard y paquete MCP en el Release; Liquibase en producción; deploy en Render; smoke test | `release-<versión>`, `reporte-despliegue` |
| `docs.yml` | push a main | Diccionario de datos, diagramas Mermaid y secciones del README generados desde el código y la BD | `reporte-documentacion` |
| `pages.yml` | push a main y tras otros workflows | Sitio: Dokka (Kotlin), TypeDoc (TypeScript), base de datos, diagramas, reportes, presentación y PDFs | `reporte-pages` |

## Configuración necesaria en GitHub

Los workflows **no fallan** si falta un secreto: escriben «omitido: falta secret X» en el reporte.
*Settings → Secrets and variables → Actions*:

| Secreto / variable | Para qué | Dónde se obtiene |
|---|---|---|
| `SONAR_TOKEN` (secret) | SonarQube | sonarcloud.io → importar `Laos19/TareaIA` (organización `laos19`, clave `Laos19_TareaIA`) → *My Account → Security*. Desactivar *Automatic Analysis* en el proyecto. |
| `SNYK_TOKEN` (secret) | Snyk | app.snyk.io → *Account settings → Auth Token* |
| `NEON_API_KEY` (secret), `NEON_ORG_ID` (variable) | Terraform: base de datos | console.neon.tech → *Account → API keys*; el id `org-…` en la configuración de la organización |
| `RENDER_API_KEY`, `RENDER_OWNER_ID` (secrets) | Terraform: API y dashboard | dashboard.render.com → *Account settings → API keys*; el owner id `tea-…` |
| `TF_API_TOKEN` (secret), `TF_CLOUD_ORGANIZATION` (variable) | Estado de Terraform en HCP | app.terraform.io → crear workspace `tareia-prod` con *Execution Mode: Local*. Alternativa: `TF_STATE_PG_CONN_STR` |
| `INFRACOST_API_KEY` (secret, opcional) | Costos en vivo de AWS/Azure | infracost.io. Sin él se usa la tabla de precios documentada |
| `LIQUIBASE_URL`, `LIQUIBASE_USER`, `LIQUIBASE_PASSWORD` (secrets) | Migraciones en producción | Salidas `database_jdbc_url`, `database_user` de Terraform y la contraseña de Neon |
| `RENDER_DEPLOY_HOOK_API`, `RENDER_DEPLOY_HOOK_WEB` (secrets) | Despliegue | Render → cada servicio → *Settings → Deploy Hook* |
| `API_URL`, `WEB_URL` (variables) | APK, dashboard y smoke test | `https://tareia-api.onrender.com` y `https://tareia-dashboard.onrender.com` (o las salidas de Terraform) |

Ajustes del repositorio:

- *Settings → Pages → Source: GitHub Actions*.
- *Settings → Actions → General → Allow GitHub Actions to create and approve pull requests* (release-please).
- *Settings → Environments*: crear `production` (opcionalmente con aprobación manual).

## Ejecutar las pruebas en local

```bash
npm ci
```

```bash
DATABASE_URL=postgres://postgres:postgres@localhost:5432/tareia npm test -w backend
```

```bash
npm run test:mutacion -w backend
```

```bash
cd android/verificacion-jvm && gradle test pitest
```

Para la interfaz y la aceptación se necesita la API levantada (`npm run dev:api`) y el dashboard (`npm run dev:dashboard`):

```bash
DASHBOARD_URL=http://localhost:5173 npm run test:e2e -w dashboard
```

```bash
API_URL=http://localhost:8080 npm run test:aceptacion -w acceptance
```
