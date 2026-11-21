# TARE·IA — corrección de la incompatibilidad Kotlin / Google Sign-In

## Motivo del fallo

El ZIP recibido usaba Kotlin 1.9.24 con dependencias modernas de Credential Manager y Firebase. El error `Class kotlin.Unit was compiled with an incompatible version` indica un conflicto de metadatos/versión del compilador que puede producir cientos de errores adicionales como `Unresolved reference: trim` u `orEmpty`.

## Cambios (solo configuración de compilación)

- Android Gradle Plugin: **8.6.1 → 8.13.2**.
- Gradle Wrapper: **8.7 → 8.13**.
- Kotlin Gradle Plugin: **1.9.24 → 2.3.21**.
- Jetpack Compose Compiler: se utiliza el plugin `org.jetbrains.kotlin.plugin.compose` **2.3.21**; se retira el antiguo `kotlinCompilerExtensionVersion 1.5.14`.
- Se conservan Java 17, compileSdk/targetSdk 35, las dependencias de Firebase/Auth/Google Identity, el ID del paquete y el procesamiento temporal de Kotlin de `app/build.gradle`.
- No se han modificado los archivos `*.kt`, las pantallas, el modelo de datos ni los recursos XML.
- Se ha conservado `app/google-services.json` del ZIP. **Este archivo pertenece a tu proyecto Firebase: no lo compartas públicamente sin necesidad**.
- Se han excluido carpetas generadas (`build`, `.gradle`, `.idea`) y el `local.properties` propio de otra máquina. Se crean de nuevo al abrir Android Studio.

## Abrir y ejecutar

1. Descomprime **en una carpeta NUEVA** (no mezcles con la versión anterior).
2. Abre la carpeta `TAREIA_v4` desde **File → Open** en Android Studio.
3. Configura **Gradle JDK 17** en Settings → Build, Execution, Deployment → Build Tools → Gradle.
4. Conéctate a Internet para descargar Gradle 8.13 y los nuevos plugins. Android Studio puede pedir aceptar una sincronización/actualización de su versión si es antigua.
5. **Sync Project with Gradle Files**.
6. **Build → Rebuild Project** (o en la terminal `./gradlew assembleDebug --stacktrace`; en PowerShell ` .\gradlew.bat :app:assembleDebug --stacktrace`).
7. Selecciona `app` y el emulador Pixel 7a → ▶ **Run**.

## Si Google Sign-In compila pero muestra error de acceso

1. En Firebase Console → Project settings → Your apps → Android `com.tareia.app`, revisa que hayas registrado el **SHA-1 de la firma debug** de esta computadora. Obtén el certificado mediante ` .\gradlew.bat :app:signingReport`.
2. Agrega también SHA-256 si Firebase/Google te lo solicita.
3. Verifica que en Firebase Authentication → Sign-in method esté activado el proveedor **Google**.
4. Descarga **de nuevo** `google-services.json` de Firebase DESPUÉS de registrar SHA y habilitar el proveedor; sustituye `app/google-services.json` del ZIP por ese archivo actualizado y sincroniza Gradle.
5. Para Google OAuth en Testing, usa una cuenta registrada en Google Auth Platform → Audience → Test users.
6. `default_web_client_id` lo genera `google-services.json`: no sustituyas arbitrariamente por otro Web Client ID creado en Cloud.
7. El servicio de Google Calendar REST aún no está implementado en esta versión; la pantalla de calendario conserva el proveedor Android previo. Esta corrección se concentra en permitir compilar y probar RF-01.

## Verificación y límites

Revisión estática realizada: se comprobó la presencia de GoogleAuthManager.kt, MainActivity.kt, google-services.json y Gradle Wrapper, los ajustes de versiones, y que el código Kotlin original es idéntico al ZIP recibido. **No se ejecutó `:app:assembleDebug`** porque el entorno de preparación no tiene Android SDK/Gradle descargado. Un resultado `BUILD SUCCESSFUL` debe confirmarse en tu Android Studio.

Documentación de compatibilidad:
- https://developer.android.com/build/releases/agp-8-13-0-release-notes
- https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- https://kotlinlang.org/docs/gradle-configure-project.html
