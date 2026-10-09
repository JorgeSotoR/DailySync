package co.edu.poli.dailysync.alarmas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Android borra las alarmas programadas al reiniciar el teléfono o actualizar la app; aquí se restauran. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String accion = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(accion) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(accion)) {
            AlarmScheduler.reprogramarTodas(context);
        }
    }
}
