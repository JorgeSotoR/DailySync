package co.edu.poli.dailysync.alarmas;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.model.Alarma;

/**
 * Servicio en primer plano que hace sonar la alarma en bucle (con vibración) hasta que el usuario
 * toca Detener o Posponer, o hasta que pasan 3 minutos.
 * Muestra una notificación de pantalla completa que abre AlarmaActivity sobre la pantalla de bloqueo.
 */
public class AlarmaService extends Service {

    public static final String ACCION_INICIAR = "co.edu.poli.dailysync.alarma.INICIAR";
    public static final String ACCION_DETENER = "co.edu.poli.dailysync.alarma.DETENER";
    public static final String ACCION_POSPONER = "co.edu.poli.dailysync.alarma.POSPONER";
    /** Se envía a AlarmaActivity para que se cierre cuando la alarma termina. */
    public static final String ACCION_TERMINADA = "co.edu.poli.dailysync.alarma.TERMINADA";

    public static final String EXTRA_ID = "id";
    public static final String EXTRA_CODIGO = "codigo";
    public static final String EXTRA_HORA = "hora";
    public static final String EXTRA_ETIQUETA = "etiqueta";
    public static final String EXTRA_REPETICION = "repeticion";

    public static final int MINUTOS_POSPONER = 5;
    private static final long DURACION_MAXIMA_MS = 3 * 60 * 1000;
    private static final int ID_NOTIFICACION = 4100;
    private static final long[] PATRON_VIBRACION = {0, 700, 500};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable detenerPorTiempo = this::detener;

    private MediaPlayer reproductor;
    private Vibrator vibrador;

    private long alarmaId;
    private int codigo;

    /** Inicia el servicio desde AlarmReceiver con los datos de la alarma. */
    public static void iniciar(Context context, Alarma alarma) {
        Intent intent = new Intent(context, AlarmaService.class)
                .setAction(ACCION_INICIAR)
                .putExtra(EXTRA_ID, alarma.getId())
                .putExtra(EXTRA_CODIGO, alarma.getRequestCode())
                .putExtra(EXTRA_HORA, alarma.getHoraTexto())
                .putExtra(EXTRA_ETIQUETA, alarma.getEtiqueta())
                .putExtra(EXTRA_REPETICION, alarma.getTextoRepeticion());
        ContextCompat.startForegroundService(context, intent);
    }

    /** Envía Detener o Posponer al servicio (desde la pantalla de alarma). */
    public static void enviar(Context context, String accion) {
        context.startService(new Intent(context, AlarmaService.class).setAction(accion));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String accion = intent != null ? intent.getAction() : null;
        if (ACCION_INICIAR.equals(accion)) {
            sonar(intent);
        } else if (ACCION_POSPONER.equals(accion)) {
            AlarmScheduler.posponer(this, alarmaId, codigo, MINUTOS_POSPONER);
            detener();
        } else {
            detener();
        }
        return START_NOT_STICKY;
    }

    private void sonar(Intent intent) {
        alarmaId = intent.getLongExtra(EXTRA_ID, -1);
        codigo = intent.getIntExtra(EXTRA_CODIGO, 0);
        String hora = intent.getStringExtra(EXTRA_HORA);
        String etiqueta = intent.getStringExtra(EXTRA_ETIQUETA);
        String repeticion = intent.getStringExtra(EXTRA_REPETICION);

        NotificacionAlarma.crearCanal(this);
        Notification notificacion = construirNotificacion(hora, etiqueta, repeticion);
        int tipo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK : 0;
        ServiceCompat.startForeground(this, ID_NOTIFICACION, notificacion, tipo);

        detenerSonido();   // por si ya estaba sonando otra alarma
        iniciarSonido();
        iniciarVibracion();
        handler.removeCallbacks(detenerPorTiempo);
        handler.postDelayed(detenerPorTiempo, DURACION_MAXIMA_MS);
    }

    private Notification construirNotificacion(String hora, String etiqueta, String repeticion) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent pantalla = AlarmaActivity.intent(this, hora, etiqueta, repeticion);
        PendingIntent abrirPantalla = PendingIntent.getActivity(this, codigo, pantalla, flags);
        PendingIntent detener = PendingIntent.getService(this, 1,
                new Intent(this, AlarmaService.class).setAction(ACCION_DETENER), flags);
        PendingIntent posponer = PendingIntent.getService(this, 2,
                new Intent(this, AlarmaService.class).setAction(ACCION_POSPONER), flags);

        return new NotificationCompat.Builder(this, NotificacionAlarma.CANAL)
                .setSmallIcon(R.drawable.ic_alarma)
                .setColor(ContextCompat.getColor(this, R.color.naranja))
                .setContentTitle(hora + " · " + etiqueta)
                .setContentText(repeticion)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setFullScreenIntent(abrirPantalla, true)   // abre la pantalla de alarma
                .setContentIntent(abrirPantalla)
                .setDeleteIntent(detener)                   // si se descarta la notificación, se detiene
                .addAction(0, getString(R.string.alarma_posponer), posponer)
                .addAction(0, getString(R.string.alarma_detener), detener)
                .build();
    }

    private void iniciarSonido() {
        AudioAttributes atributos = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        Uri[] opciones = {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        };
        for (Uri sonido : opciones) {
            if (sonido == null) {
                continue;
            }
            try {
                reproductor = new MediaPlayer();
                reproductor.setAudioAttributes(atributos);
                reproductor.setDataSource(this, sonido);
                reproductor.setLooping(true);
                reproductor.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
                reproductor.prepare();
                reproductor.start();
                return;
            } catch (Exception e) {
                // El tono no existe o no se puede leer: se intenta con el siguiente.
                reproductor.release();
                reproductor = null;
            }
        }
    }

    private void iniciarVibracion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibrador = getSystemService(VibratorManager.class).getDefaultVibrator();
        } else {
            vibrador = getSystemService(Vibrator.class);
        }
        if (vibrador != null && vibrador.hasVibrator()) {
            vibrador.vibrate(VibrationEffect.createWaveform(PATRON_VIBRACION, 0),
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        }
    }

    private void detenerSonido() {
        if (reproductor != null) {
            try {
                reproductor.stop();
            } catch (IllegalStateException ignorada) {
                // ya estaba detenido
            }
            reproductor.release();
            reproductor = null;
        }
        if (vibrador != null) {
            vibrador.cancel();
        }
    }

    private void detener() {
        handler.removeCallbacks(detenerPorTiempo);
        detenerSonido();
        sendBroadcast(new Intent(ACCION_TERMINADA).setPackage(getPackageName()));
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(detenerPorTiempo);
        detenerSonido();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
