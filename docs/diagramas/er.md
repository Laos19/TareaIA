# Diagrama entidad-relación

> Generado automáticamente por `scripts/docs/build-diagrams.mjs`. No editar a mano.

11 tablas del esquema `public` (leído de la base de datos migrada). El diccionario de datos completo está en [docs/database](../database/).

```mermaid
erDiagram
  actividad {
    uuid id PK
    bigint usuario_id FK
    uuid curso_id FK
    character titulo
    text descripcion
    character tipo
    character origen
    character estado
    date fecha_limite
    character id_externo
    character evento_calendario_id
    timestamp completada_en
    timestamp eliminado_en
    integer version
    timestamp creado_en
    timestamp actualizado_en
  }
  apunte {
    uuid id PK
    bigint usuario_id FK
    uuid curso_id FK
    character carpeta
    character titulo
    character tipo
    text contenido
    timestamp eliminado_en
    integer version
    timestamp creado_en
    timestamp actualizado_en
  }
  curso {
    uuid id PK
    bigint usuario_id FK
    character nombre
    character abreviatura
    smallint prioridad
    smallint dificultad
    timestamp eliminado_en
    integer version
    timestamp creado_en
    timestamp actualizado_en
  }
  evento {
    bigint id PK
    uuid sesion_id FK
    bigint usuario_id FK
    character tipo
    character pantalla
    jsonb datos
    timestamp creado_en
  }
  franja_horaria {
    bigint id PK
    bigint usuario_id FK
    smallint dia_semana
    time hora_inicio
    time hora_fin
    character tipo
  }
  historial_cambio {
    bigint id PK
    character tabla
    text registro_id
    bigint usuario_id FK
    character operacion
    jsonb antes
    jsonb despues
    timestamp cambiado_en
  }
  log_cliente {
    bigint id PK
    uuid sesion_id FK
    character nivel
    character etiqueta
    character mensaje
    character version_app
    timestamp creado_en
  }
  plan_estudio {
    uuid id PK
    bigint usuario_id FK
    character estado
    character estrategia
    uuid origen_id FK
    timestamp creado_en
  }
  sesion {
    uuid id PK
    bigint usuario_id FK
    character origen
    character dispositivo
    character version_app
    timestamp iniciada_en
    timestamp ultima_actividad
    timestamp cerrada_en
    character motivo_cierre
  }
  sesion_estudio {
    bigint id PK
    uuid plan_id FK
    uuid actividad_id FK
    date fecha
    smallint minuto_inicio
    smallint duracion_min
  }
  usuario {
    bigint id PK
    character firebase_uid
    character email
    character nombre
    character rol
    character password_hash
    boolean activo
    timestamp creado_en
    timestamp ultimo_acceso
  }
  usuario ||--o{ actividad : "usuario_id"
  curso ||--o{ actividad : "curso_id"
  usuario ||--o{ apunte : "usuario_id"
  curso ||--o{ apunte : "curso_id"
  usuario ||--o{ curso : "usuario_id"
  sesion ||--o{ evento : "sesion_id"
  usuario ||--o{ evento : "usuario_id"
  usuario ||--o{ franja_horaria : "usuario_id"
  usuario ||--o{ historial_cambio : "usuario_id"
  sesion ||--o{ log_cliente : "sesion_id"
  usuario ||--o{ plan_estudio : "usuario_id"
  plan_estudio ||--o{ plan_estudio : "origen_id"
  usuario ||--o{ sesion : "usuario_id"
  plan_estudio ||--o{ sesion_estudio : "plan_id"
  actividad ||--o{ sesion_estudio : "actividad_id"
```
