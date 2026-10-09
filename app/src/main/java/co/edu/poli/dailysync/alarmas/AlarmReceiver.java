package co.edu.poli.dailysync.alarmas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDateTime;
import java.util.List;

import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Alarma;

/**
 * Recibe el aviso de AlarmManager: inicia AlarmaService (sonido + pantalla de alarma)
 * y programa la siguiente repetición.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!AlarmScheduler.ACCION_ALARMA.equals(intent.getAction())) {
            return;
        }
        long id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, -1);
        boolean pospuesta = intent.getBooleanExtra(AlarmScheduler.EXTRA_POSPUESTA, false);
        LocalRepository repositorio = new LocalRepository(context);
        List<Alarma> alarmas = repositorio.obtenerAlarmas();

        for (Alarma alarma : alarmas) {
            if (alarma.getId() != id) {
                continue;
            }
            // Una alarma pospuesta suena aunque la original (de "una vez") ya esté desactivada.
            if (!pospuesta && !alarma.isActiva()) {
                return;
            }
            AlarmaService.iniciar(context, alarma);
            if (!pospuesta) {
                if (alarma.getDias().isEmpty()) {
                    alarma.setActiva(false);   // alarma de una sola vez
                    repositorio.guardarAlarmas(alarmas);
                } else {
                    // Siguiente repetición, desde un minuto después para no repetir el disparo actual.
                    AlarmScheduler.programarDesde(context, alarma, LocalDateTime.now().plusMinutes(1));
                }
            }
            return;
        }
    }
}
