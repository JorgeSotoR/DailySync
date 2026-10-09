package co.edu.poli.dailysync.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.Locale;

import co.edu.poli.dailysync.R;

/**
 * Panel de botones (RF-09): estilos de botones y controles de Material Design.
 * Cada control registra el evento que genera (clic, pulsación larga, cambio de estado…)
 * y lo muestra en un Snackbar y en el registro de la pantalla.
 */
public class BotonesFragment extends Fragment {

    private static final int EVENTOS_VISIBLES = 6;
    private static final String ESTADO_EVENTOS = "eventos";
    private static final String ESTADO_TOTAL = "total";

    private final SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final Deque<String> eventos = new ArrayDeque<>();
    private int total;
    /** Al rotar, Android restaura el estado de los controles y dispara sus listeners: esos no se registran. */
    private boolean listo;

    private View raiz;
    private TextView tvRegistro;
    private TextView tvTotal;
    private TextView tvVasos;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_botones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        raiz = view.findViewById(R.id.raiz);
        tvRegistro = view.findViewById(R.id.tv_registro);
        tvTotal = view.findViewById(R.id.tv_total);
        tvVasos = view.findViewById(R.id.tv_vasos);

        if (savedInstanceState != null) {
            ArrayList<String> guardados = savedInstanceState.getStringArrayList(ESTADO_EVENTOS);
            if (guardados != null) {
                eventos.addAll(guardados);
            }
            total = savedInstanceState.getInt(ESTADO_TOTAL, 0);
        }

        // Botones: clic y pulsación larga
        MaterialButton guardar = view.findViewById(R.id.btn_guardar);
        guardar.setOnClickListener(v -> registrar("Clic en «Guardar» (Button)"));
        guardar.setOnLongClickListener(v -> {
            registrar("Pulsación larga en «Guardar»");
            return true;
        });
        view.findViewById(R.id.btn_tonal).setOnClickListener(v -> registrar("Clic en «Tonal» (TonalButton)"));
        view.findViewById(R.id.btn_outlined).setOnClickListener(v -> registrar("Clic en «Outlined» (OutlinedButton)"));
        view.findViewById(R.id.btn_texto).setOnClickListener(v -> registrar("Clic en «Texto» (TextButton)"));

        // Selector de una sola opción
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_periodo);
        toggle.addOnButtonCheckedListener((grupo, id, marcado) -> {
            if (marcado) {
                MaterialButton boton = grupo.findViewById(id);
                registrar("Toggle: se eligió «" + boton.getText() + "»");
            }
        });

        // Chips de selección múltiple
        int[] chips = {R.id.chip_agua, R.id.chip_ejercicio, R.id.chip_lectura};
        for (int id : chips) {
            Chip chip = view.findViewById(id);
            chip.setOnCheckedChangeListener((c, marcado) ->
                    registrar("Chip «" + c.getText() + "» " + (marcado ? "seleccionado" : "deseleccionado")));
        }

        // Interruptor y casilla
        MaterialSwitch concentracion = view.findViewById(R.id.sw_concentracion);
        concentracion.setOnCheckedChangeListener((s, activo) ->
                registrar("Switch «Modo concentración» " + (activo ? "activado" : "desactivado")));
        CheckBox recordarme = view.findViewById(R.id.cb_recordarme);
        recordarme.setOnCheckedChangeListener((c, marcado) ->
                registrar("CheckBox «Recordarme» " + (marcado ? "marcado" : "desmarcado")));

        // Deslizador: el texto cambia en vivo y el evento se registra al soltar
        Slider vasos = view.findViewById(R.id.slider_vasos);
        mostrarVasos(vasos.getValue());
        vasos.addOnChangeListener((s, valor, delUsuario) -> mostrarVasos(valor));
        vasos.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                // sin acción
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                registrar("Slider: " + Math.round(slider.getValue()) + " vasos de agua");
            }
        });

        // Botones flotantes
        view.findViewById(R.id.fab_pequeno).setOnClickListener(v -> registrar("Clic en FAB pequeño"));
        ExtendedFloatingActionButton extendido = view.findViewById(R.id.fab_extendido);
        extendido.setOnClickListener(v -> {
            // Alterna entre extendido y compacto para mostrar la animación del componente
            if (extendido.isExtended()) {
                extendido.shrink();
            } else {
                extendido.extend();
            }
            registrar("Clic en ExtendedFAB «Nueva tarea»");
        });

        mostrarRegistro();
    }

    private void mostrarVasos(float valor) {
        tvVasos.setText(getString(R.string.botones_vasos, Math.round(valor)));
    }

    /** Agrega el evento al registro (últimos 6) y lo muestra en un Snackbar. */
    private void registrar(String evento) {
        if (!listo) {
            return;
        }
        total++;
        eventos.addFirst(formatoHora.format(new Date()) + "  " + evento);
        while (eventos.size() > EVENTOS_VISIBLES) {
            eventos.removeLast();
        }
        mostrarRegistro();
        Snackbar.make(raiz, evento, Snackbar.LENGTH_SHORT)
                .setAnchorView(R.id.fab_extendido)
                .show();
    }

    private void mostrarRegistro() {
        if (eventos.isEmpty()) {
            tvRegistro.setText(R.string.botones_registro_vacio);
        } else {
            tvRegistro.setText(String.join("\n", eventos));
        }
        tvTotal.setText(getString(R.string.botones_total, total));
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        listo = true;
    }

    @Override
    public void onDestroyView() {
        listo = false;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(ESTADO_EVENTOS, new ArrayList<>(eventos));
        outState.putInt(ESTADO_TOTAL, total);
    }
}
