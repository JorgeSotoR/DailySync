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

## Estado de la entrega final (versión 1.0.0)

| Módulo | Estado |
|---|---|
| Pantalla con dos fragmentos | Funcional |
| Perfil (apodo, fecha, género; avatar predeterminado, emoji o foto de galería; resumen del perfil guardado) | Funcional |
| Menú que cambia según el género | Funcional |
| Actividades (crear, completar, eliminar, filtrar por tipo y por fecha, agrupar por día) | Funcional |
| Alarmas (crear, editar, repetir por días, activar/desactivar, eliminar; suenan en bucle con pantalla completa sobre el bloqueo, Detener y Posponer 5 min; AlarmManager + servicio en primer plano; se restauran al reiniciar) | Funcional |
| Lista de compras (texto, foto con cámara o galería, recibir contenido con "Compartir" desde otras apps, marcar, eliminar) | Funcional |
| Salud (calendario con periodo, periodo estimado y ventana fértil; borrar registros) | Funcional |
| Multimedia (video con controles: ejemplo en línea o del teléfono; galería horizontal de consejos) | Funcional |
| Web (URL o búsqueda, WebView con progreso, atajos y botón Atrás) | Funcional |
| Actividad física (burpees, sentadillas, flexiones, abdominales, estiramientos, ciclismo, trote, pasos; sesión guiada por series con descanso de 1 minuto; resumen de hoy, semana y mes) | Funcional |
| Panel de botones (Button, Tonal, Outlined, Text, Toggle, Chips, Switch, CheckBox, Slider, FAB con registro de eventos) | Funcional |

## Cómo abrirlo y generar la APK

1. Abrir la carpeta del proyecto en **Android Studio** (Ladybug 2024.2 o más reciente) con *File > Open*.
2. Esperar a que termine la sincronización de Gradle (la primera vez descarga dependencias).
   Si Android Studio sugiere actualizar el plugin de Android Gradle, se puede aceptar.
3. Ejecutar en un emulador o teléfono con **Run ▶**.
4. Generar la APK con *Build > Build App Bundle(s) / APK(s) > Build APK(s)*.
   El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`.
5. Copiarlo a la carpeta `apk/` con el nombre `DailySync-final.apk` y subirlo al repositorio.

Desde la terminal también se puede usar `./gradlew assembleDebug` (en Windows: `gradlew.bat assembleDebug`).

## Estructura

```
app/src/main/java/co/edu/poli/dailysync/
├── MainActivity.java
├── OnMenuItemSelectedListener.java
├── alarmas/      AlarmScheduler, AlarmReceiver, BootReceiver, AlarmaService, AlarmaActivity, NotificacionAlarma
├── data/LocalRepository.java
├── model/        Usuario, Actividad, TipoActividad, Alarma, ItemCompra, RegistroCiclo, CalculadoraCiclo,
│                 Ejercicio, RegistroEjercicio, ResumenEjercicio
└── ui/           MenuFragment, AvatarView, PerfilFragment, ActividadesFragment, AlarmasFragment,
                  ComprasFragment, SaludFragment, MultimediaFragment (+ ConsejoAdapter), WebFragment, EjercicioFragment, BotonesFragment
apk/              APK de cada entrega
docs/             Documentos de cada entrega; docs/final/ tiene el documento, los diagramas UML y el mockup finales
```

- SDK mínimo 26 (Android 8.0), SDK objetivo 35, Java 17.
