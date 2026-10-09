package co.edu.poli.dailysync.alarmas;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import co.edu.poli.dailysync.R;

/**
 * Canal de notificaciones de las alarmas y verificación de permisos.
 * El canal es silencioso: el sonido lo reproduce AlarmaService en bucle hasta que se detiene.
 */
public final class NotificacionAlarma {

    /** "v2": los ajustes de un canal no se pueden cambiar después de creado, por eso se usa un id nuevo. */
    public static final String CANAL = "alarmas_v2";
    private static final String CANAL_ANTERIOR = "alarmas";

    private NotificacionAlarma() { }

    public static void crearCanal(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        nm.deleteNotificationChannel(CANAL_ANTERIOR);   // canal de la versión anterior (con sonido propio)

        NotificationChannel canal = new NotificationChannel(CANAL,
                context.getString(R.string.canal_alarmas), NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription(context.getString(R.string.canal_alarmas_descripcion));
        canal.setSound(null, null);
        canal.enableVibration(false);
        canal.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(canal);
    }

    /** Permiso para mostrar notificaciones (Android 13+). */
    public static boolean tienePermiso(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Permiso para abrir la pantalla de alarma sobre la pantalla de bloqueo (Android 14+). */
    public static boolean puedePantallaCompleta(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true;
        }
        return context.getSystemService(NotificationManager.class).canUseFullScreenIntent();
    }
}
