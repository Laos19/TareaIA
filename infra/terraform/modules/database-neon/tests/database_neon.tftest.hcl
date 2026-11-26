# Pruebas nativas de Terraform del módulo de base de datos con proveedor simulado.
mock_provider "neon" {
  mock_resource "neon_project" {
    defaults = {
      id            = "proyecto-simulado"
      database_host = "ep-simulado.us-east-1.aws.neon.tech"
      database_name = "tareia"
      database_user = "tareia_owner"
    }
  }
}

variables {
  project_name = "tareia"
}

run "postgres_16_con_nombres_del_proyecto" {
  command = apply

  assert {
    condition     = neon_project.main.pg_version == 16
    error_message = "Se usa PostgreSQL 16 (el mismo de CI)."
  }
  assert {
    condition     = neon_project.main.name == "tareia"
    error_message = "El proyecto Neon debe llamarse tareia."
  }
}

run "jdbc_para_liquibase_con_tls" {
  command = apply

  assert {
    condition     = output.jdbc_url == "jdbc:postgresql://ep-simulado.us-east-1.aws.neon.tech:5432/tareia?sslmode=require"
    error_message = "La URL JDBC debe apuntar al host de Neon y exigir TLS."
  }
}

run "rechaza_nombre_de_bd_invalido" {
  command = plan
  variables {
    database_name = "Tareia-BD"
  }
  expect_failures = [var.database_name]
}
