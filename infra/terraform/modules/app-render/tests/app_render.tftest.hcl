# Pruebas nativas de Terraform (terraform test) con proveedor simulado:
# validan la configuración de Render sin crear recursos ni usar credenciales.
mock_provider "render" {}

variables {
  name_prefix  = "tareia"
  repo_url     = "https://github.com/Laos19/TareaIA"
  database_url = "postgresql://usuario:clave@host/tareia?sslmode=require"
}

run "nombres_y_cors" {
  command = plan

  assert {
    condition     = render_web_service.api.name == "tareia-api"
    error_message = "La API debe llamarse tareia-api."
  }
  assert {
    condition     = render_static_site.web.name == "tareia-dashboard"
    error_message = "El dashboard debe llamarse tareia-dashboard."
  }
  assert {
    condition     = output.cors_origins == "https://tareia-dashboard.onrender.com"
    error_message = "CORS debe permitir solo el dashboard por defecto."
  }
}

run "api_saludable_y_gratuita" {
  command = plan

  assert {
    condition     = render_web_service.api.health_check_path == "/api/health"
    error_message = "Render debe usar /api/health para desplegar sin caída."
  }
  assert {
    condition     = render_web_service.api.plan == "free"
    error_message = "El plan por defecto debe ser free (costo USD 0)."
  }
  assert {
    condition     = render_web_service.api.runtime_source.native_runtime.auto_deploy_trigger == "off"
    error_message = "El despliegue lo dispara release.yml después de migrar la BD (auto deploy off)."
  }
}

run "secretos_fuera_del_repositorio" {
  command = plan

  assert {
    condition     = render_web_service.api.env_vars["JWT_SECRET"].generate_value == true
    error_message = "JWT_SECRET debe generarlo Render, nunca venir del repositorio."
  }
  assert {
    condition     = render_web_service.api.env_vars["FIREBASE_PROJECT_ID"].value == "tare-ia"
    error_message = "La API debe conocer el proyecto Firebase para verificar los tokens de Google."
  }
}

run "dashboard_con_cabeceras_de_seguridad_y_cache" {
  command = plan

  assert {
    condition     = contains([for h in render_static_site.web.headers : h.name], "X-Content-Type-Options")
    error_message = "El dashboard debe enviar X-Content-Type-Options."
  }
  assert {
    condition     = anytrue([for h in render_static_site.web.headers : h.path == "/assets/*" && strcontains(h.value, "immutable")])
    error_message = "Los recursos del dashboard deben tener caché inmutable."
  }
  assert {
    condition     = render_static_site.web.publish_path == "dashboard/dist"
    error_message = "Se publica dashboard/dist."
  }
}

run "cors_extra" {
  command = plan
  variables {
    extra_cors_origins = ["https://laos19.github.io"]
  }
  assert {
    condition     = output.cors_origins == "https://tareia-dashboard.onrender.com,https://laos19.github.io"
    error_message = "Los orígenes extra se agregan después del dashboard."
  }
}

run "rechaza_region_invalida" {
  command = plan
  variables {
    region = "lima"
  }
  expect_failures = [var.region]
}

run "rechaza_prefijo_invalido" {
  command = plan
  variables {
    name_prefix = "TareIA"
  }
  expect_failures = [var.name_prefix]
}
