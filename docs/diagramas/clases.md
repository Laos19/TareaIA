# Diagrama de clases

> Generado automáticamente por `scripts/docs/build-diagrams.mjs`. No editar a mano.

Clases de la API (Fastify). Las flechas indican dependencias inyectadas por constructor (repositorios → servicios → rutas).

| Clase | Archivo | Miembros |
|---|---|---|
| `AuthService` | `backend/src/auth/AuthService.ts` | 8 |
| `VerificadorFirebase` | `backend/src/auth/VerificadorGoogle.ts` | 2 |
| `MemoryCache` | `backend/src/cache/MemoryCache.ts` | 8 |
| `Database` | `backend/src/db/Database.ts` | 5 |
| `HttpError` | `backend/src/errors.ts` | 2 |
| `AcademicoRepository` | `backend/src/repositories/AcademicoRepository.ts` | 12 |
| `DashboardRepository` | `backend/src/repositories/DashboardRepository.ts` | 3 |
| `TelemetriaRepository` | `backend/src/repositories/TelemetriaRepository.ts` | 3 |
| `ActividadService` | `backend/src/services/ActividadService.ts` | 4 |
| `DashboardService` | `backend/src/services/DashboardService.ts` | 5 |
| `TelemetriaService` | `backend/src/services/TelemetriaService.ts` | 6 |

```mermaid
classDiagram
  direction LR
  class AuthService {
    -db : Database
    -verificador : VerificadorIdentidad
    +login(email, password, datos)
    +loginGoogle(idToken, datos)
    -abrirSesion(usuarioId, datos)
    +tocarSesion(sid, usuarioId, minutosInactividad)
    +cerrarSesion(sid, usuarioId)
    +perfil(usuarioId)
  }
  class VerificadorFirebase {
    -projectId : string
    +verificar(idToken)
  }
  class MemoryCache {
    +aciertos
    +fallos
    -ttlMs : number
    -ahora : () = number
    +get(clave)
    +set(clave, valor)
    +obtener(clave, cargar)
    +invalidar(clave)
  }
  class Database {
    +pool : pg.Pool
    +query(text, params)
    +tx(fn)
    +ping()
    +close()
  }
  class HttpError {
    +status : number
    +code : 
  }
  class AcademicoRepository {
    -db : Database
    +listarCursos(usuarioId, desde)
    +buscarCurso(usuarioId, id)
    +guardarCurso(usuarioId, c)
    +listarActividades(usuarioId, desde)
    +buscarActividad(usuarioId, id)
    +buscarDuplicado(usuarioId, a)
    +guardarActividad(usuarioId, a)
    +cambiarEstado(usuarioId, id, estado)
    +marcarPapelera(usuarioId, tabla, id, enPapelera)
    +pendientes(usuarioId)
    +historial(usuarioId, tabla, id)
  }
  class DashboardRepository {
    -db : Database
    +obtener()
    +refrescar(minutosInactividad)
  }
  class TelemetriaRepository {
    -db : Database
    +insertarEventos(sid, usuarioId, eventos)
    +insertarLogs(sid, logs)
  }
  class ActividadService {
    -repo : AcademicoRepository
    +guardar(usuarioId, entrada)
    +accion(usuarioId, id, accion)
    +sincronizar(usuarioId, cursos, actividades, desde)
  }
  class DashboardService {
    +cache : MemoryCache Dashboard
    -repo : DashboardRepository
    -minutosInactividad : number
    +obtener()
    +refrescar()
  }
  class TelemetriaService {
    -repo : TelemetriaRepository
    -ahora : () = number
    +depurarEventos(entrada)
    +fechaValida(fecha)
    +registrarEventos(sid, usuarioId, entrada)
    +registrarLogs(sid, entrada)
  }
  AuthService --> Database : usa
  class HttpError:::error
  AcademicoRepository --> Database : usa
  DashboardRepository --> Database : usa
  TelemetriaRepository --> Database : usa
  ActividadService --> AcademicoRepository : usa
  DashboardService --> DashboardRepository : usa
  TelemetriaService --> TelemetriaRepository : usa
```
