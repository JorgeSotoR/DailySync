package co.edu.poli.dailysync.alarmas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import co.edu.poli.dailysync.R;

/**
 * Pantalla completa que aparece cuando suena una alarma, incluso con el teléfono bloqueado.
 * Muestra la hora y la etiqueta, y permite Detener o Posponer.
 */
public class AlarmaActivity extends AppCompatActivity {

    private static final Locale ES = new Locale("es", "CO");

    /** Se cierra sola cuando la alarma termina (por ejemplo, si se detiene desde la notificación). */
    private final BroadcastReceiver alTerminar = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    public static Intent intent(Context context, String hora, String etiqueta, String repeticion) {
        return new Intent(context, AlarmaActivity.class)
                .putExtra(AlarmaService.EXTRA_HORA, hora)
                .putExtra(AlarmaService.EXTRA_ETIQUETA, etiqueta)
                .putExtra(AlarmaService.EXTRA_REPETICION, repeticion)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarSobrePantallaDeBloqueo();
        setContentView(R.layout.activity_alarma);
        mostrarDatos(getIntent());

        findViewById(R.id.btn_detener).setOnClickListener(v -> {
            AlarmaService.enviar(this, AlarmaService.ACCION_DETENER);
            finish();
        });
        findViewById(R.id.btn_posponer).setOnClickListener(v -> {
            AlarmaService.enviar(this, AlarmaService.ACCION_POSPONER);
            Toast.makeText(this, getString(R.string.alarma_pospuesta, AlarmaService.MINUTOS_POSPONER),
                    Toast.LENGTH_SHORT).show();
            finish();
        });

        // El botón Atrás no cierra la alarma: hay que elegir Detener o Posponer.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // sin acción
            }
        });

        ContextCompat.registerReceiver(this, alTerminar,
                new IntentFilter(AlarmaService.ACCION_TERMINADA), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mostrarDatos(intent);   // sonó otra alarma mientras esta pantalla estaba abierta
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(alTerminar);
        super.onDestroy();
    }

    private void mostrarDatos(Intent intent) {
        ((TextView) findViewById(R.id.tv_hora)).setText(intent.getStringExtra(AlarmaService.EXTRA_HORA));
        ((TextView) findViewById(R.id.tv_etiqueta)).setText(intent.getStringExtra(AlarmaService.EXTRA_ETIQUETA));
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES));
        fecha = fecha.substring(0, 1).toUpperCase(ES) + fecha.substring(1);
        ((TextView) findViewById(R.id.tv_detalle)).setText(
                fecha + " · " + intent.getStringExtra(AlarmaService.EXTRA_REPETICION));
    }

    private void mostrarSobrePantallaDeBloqueo() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
