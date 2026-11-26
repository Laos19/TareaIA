# Module TARE·IA — app Android

Documentación de cada clase, método y propiedad de la app Kotlin (Jetpack Compose), organizada por paquetes:

| Paquete | Capa | Contenido |
|---|---|---|
| `com.tareia.app.dominio` | Dominio (DDD) | Entidades, objetos de valor, patrón State (`EstadoActividad`), Builder (`ActividadBuilder`), Prototype (`PlanEstudio`), interfaces Repository. |
| `com.tareia.app.aplicacion` | Aplicación | Casos de uso `Gestor*`, `Dependencias` (inyección manual) y `ObservadorUso` (Observer). |
| `com.tareia.app.ia` | Inteligencia artificial | Abstract Factory de proveedores (Gemini u OpenAI compatible), Strategy de planificación, OCR con ML Kit. |
| `com.tareia.app.infraestructura` | Infraestructura | `AppDatabase` (Singleton cifrado), repositorios locales, alarmas y calendario. |
| `com.tareia.app.remoto` | Nube | `Nube` (Facade), `ApiTareia` (OkHttp con caché), `ColaTelemetria`, `MapeoNube`. |
| `com.tareia.app.ui` | Presentación | Pantallas y componentes Compose, tema «Tablero periódico». |
