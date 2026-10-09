package co.edu.poli.dailysync.alarmas;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import co.edu.poli.dailysync.MainActivity;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Alarma;

/**
 * Programa y cancela las alarmas en el sistema con AlarmManager (RF-05).
 * Cada alarma se programa solo para su próximo disparo; al sonar, AlarmReceiver programa la siguiente.
 */
public final class AlarmScheduler {

    public static final String ACCION_ALARMA = "co.edu.poli.dailysync.ALARMA";
    public static final String EXTRA_ID = "alarma_id";
    public static final String EXTRA_POSPUESTA = "pospuesta";

    private static final int FLAGS = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

    private AlarmScheduler() { }

    /** Programa el próximo disparo de la alarma. Devuelve la hora programada en milisegundos. */
    public static long programar(Context context, Alarma alarma) {
        return programarDesde(context, alarma, LocalDateTime.now());
    }

    @SuppressLint({"ScheduleExactAlarm", "MissingPermission"})   // se verifica con puedeProgramarExactas()
    static long programarDesde(Context context, Alarma alarma, LocalDateTime desde) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        long cuando = alarma.proximoDisparo(desde)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        PendingIntent operacion = intentDisparo(context, alarma);

        if (puedeProgramarExactas(context)) {
            // setAlarmClock es exacta, funciona en modo de ahorro (Doze) y muestra el ícono de alarma en la barra.
            PendingIntent mostrar = PendingIntent.getActivity(context, alarma.getRequestCode(),
                    MainActivity.intentAbrirAlarmas(context), FLAGS);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(cuando, mostrar), operacion);
        } else {
            // Sin permiso de alarmas exactas: el sistema puede retrasarla unos minutos.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, operacion);
        }
        return cuando;
    }

    public static void cancelar(Context context, Alarma alarma) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        am.cancel(intentDisparo(context, alarma));
        am.cancel(intentPosposicion(context, alarma.getId(), alarma.getRequestCode()));
    }

    /** Vuelve a hacer sonar la alarma dentro de unos minutos (botón Posponer). */
    @SuppressLint({"ScheduleExactAlarm", "MissingPermission"})   // se verifica con puedeProgramarExactas()
    public static void posponer(Context context, long alarmaId, int codigo, int minutos) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        long cuando = System.currentTimeMillis() + minutos * 60_000L;
        PendingIntent operacion = intentPosposicion(context, alarmaId, codigo);
        if (puedeProgramarExactas(context)) {
            PendingIntent mostrar = PendingIntent.getActivity(context, codigo,
                    MainActivity.intentAbrirAlarmas(context), FLAGS);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(cuando, mostrar), operacion);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, operacion);
        }
    }

    /** PendingIntent distinto al de la alarma normal (otro requestCode) para no reemplazarla. */
    private static PendingIntent intentPosposicion(Context context, long alarmaId, int codigo) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACCION_ALARMA);
        intent.putExtra(EXTRA_ID, alarmaId);
        intent.putExtra(EXTRA_POSPUESTA, true);
        return PendingIntent.getBroadcast(context, ~codigo, intent, FLAGS);
    }

    /** Vuelve a programar todas las alarmas activas (al abrir la app y después de reiniciar el teléfono). */
    public static void reprogramarTodas(Context context) {
        List<Alarma> alarmas = new LocalRepository(context).obtenerAlarmas();
        for (Alarma a : alarmas) {
            if (a.isActiva()) {
                programar(context, a);
            } else {
                cancelar(context, a);
            }
        }
    }

    public static boolean puedeProgramarExactas(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    private static PendingIntent intentDisparo(Context context, Alarma alarma) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACCION_ALARMA);
        intent.putExtra(EXTRA_ID, alarma.getId());
        return PendingIntent.getBroadcast(context, alarma.getRequestCode(), intent, FLAGS);
    }
}
