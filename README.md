# DailySync

Aplicación Android nativa (Java) para planificar el día: actividades, alarmas, lista de compras y bienestar.
Proyecto del curso **Herramientas de Programación Móvil I** — Institución Universitaria Politécnico Grancolombiano, grupo G8-HPMB02.

**Integrantes:** Jorge Adrian Soto Reyes, Jhonathan Alarcon Parra, Alvaro Perez Hernandez, Jeison Arevalo Mora.
**Docente:** Víctor Fabián Castro Pérez.

## Arquitectura

Una sola actividad (`MainActivity`) muestra dos fragmentos al mismo tiempo:

| Panel | Clase | Función |
|---|---|---|
| Izquierdo (35 %) | `MenuFragment` | Lista de opciones. Avisa la opción elegida mediante `OnMenuItemSelectedListener`. |
| Derecho (65 %) | Fragmento de la opción | `MainActivity` lo cambia con `FragmentTransaction.replace()`. |

La opción **Salud** solo aparece en el menú cuando el perfil tiene género femenino.
En orientación horizontal se usa `layout-land/activity_main.xml` con pesos 30/70.
Los datos se guardan en el dispositivo (SharedPreferences + JSON con Gson).

## Estado de la entrega 2 (versión 0.2.0)

| Módulo | Estado |
|---|---|
| Pantalla con dos fragmentos | Funcional |
| Perfil (apodo, fecha, género, avatar predeterminado o de galería) | Funcional |
| Menú que cambia según el género | Funcional |
| Actividades (crear, completar, eliminar, filtrar por tipo, agrupar por día) | Parcial: falta filtro por fecha |
| Alarmas (crear, activar/desactivar, eliminar) | Parcial: falta AlarmManager y notificaciones |
| Lista de compras (texto, marcar, eliminar) | Parcial: faltan fotos e intents compartidos |
| Salud (registro del ciclo y estimación del próximo periodo) | Funcional |
| Multimedia, Web, Botones | En construcción |

## Cómo abrirlo y generar la APK

1. Abrir la carpeta del proyecto en **Android Studio** (Ladybug 2024.2 o más reciente) con *File > Open*.
2. Esperar a que termine la sincronización de Gradle (la primera vez descarga dependencias).
   Si Android Studio sugiere actualizar el plugin de Android Gradle, se puede aceptar.
3. Ejecutar en un emulador o teléfono con **Run ▶**.
4. Generar la APK con *Build > Build App Bundle(s) / APK(s) > Build APK(s)*.
   El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`.
5. Copiarlo a la carpeta `apk/` con el nombre `DailySync-entrega2.apk` y subirlo al repositorio.

Desde la terminal también se puede usar `./gradlew assembleDebug` (en Windows: `gradlew.bat assembleDebug`).

## Estructura

```
app/src/main/java/co/edu/poli/dailysync/
├── MainActivity.java
├── OnMenuItemSelectedListener.java
├── data/LocalRepository.java
├── model/        Usuario, Actividad, TipoActividad, Alarma, ItemCompra, RegistroCiclo
└── ui/           MenuFragment, PerfilFragment, ActividadesFragment, AlarmasFragment,
                  ComprasFragment, SaludFragment, MultimediaFragment, WebFragment, BotonesFragment
apk/              APK de cada entrega
docs/             Documento de la entrega, diagramas UML y mockup
```

- SDK mínimo 26 (Android 8.0), SDK objetivo 35, Java 17.
