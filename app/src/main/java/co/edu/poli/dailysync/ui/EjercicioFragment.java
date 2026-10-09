package co.edu.poli.dailysync.ui;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Ejercicio;
import co.edu.poli.dailysync.model.RegistroEjercicio;
import co.edu.poli.dailysync.model.ResumenEjercicio;

/**
 * Registro de actividad física diaria (RF-10).
 * - Ejercicios por repeticiones (burpees, sentadillas…): sesión guiada por series con descanso de 1 minuto.
 * - Ejercicios por duración (ciclismo, estiramientos, trote) y pasos: se registran directamente.
 * - Resumen de hoy, la semana o el mes, e historial del día.
 */
public class EjercicioFragment extends Fragment {

    private static final long DESCANSO_MS = 60_000;
    private static final Locale ES = new Locale("es", "CO");

    private static final String E_EJERCICIO = "ejercicio";
    private static final String E_PERIODO = "periodo";
    private static final String E_SESION = "sesion";
    private static final String E_SERIES = "series";
    private static final String E_REPS = "reps";
    private static final String E_CON_DESCANSO = "con_descanso";
    private static final String E_SERIE_ACTUAL = "serie_actual";
    private static final String E_DESCANSO_RESTANTE = "descanso_restante";

    private final DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("h:mm a", ES);
    private final Handler handler = new Handler(Looper.getMainLooper());

    private LocalRepository repositorio;
    private List<RegistroEjercicio> registros;

    // Vistas
    private View raiz;
    private TextView tvResumen;
    private View llConfiguracion, llRepeticiones, llDuracion, llPasos, btnSinGuia;
    private TextView tvSeries, tvReps, tvMinutos, tvPasos;
    private Slider sliderSeries, sliderReps, sliderMinutos, sliderPasos;
    private MaterialSwitch swDescanso;
    private ExtendedFloatingActionButton fab;
    private View llSesion, flDescanso, btnSerieCompletada, btnSaltar;
    private TextView tvSesionEjercicio, tvSesionSerie, tvSesionIndicacion, tvCuenta;
    private CircularProgressIndicator progresoDescanso;
    private LinearLayout llHistorial;
    private TextView tvHistorialAyuda;

    // Estado
    private Ejercicio ejercicio = Ejercicio.BURPEES;
    private ResumenEjercicio.Periodo periodo = ResumenEjercicio.Periodo.HOY;
    private boolean sesionActiva;
    private int sesionSeries;
    private int sesionReps;
    private boolean sesionConDescanso;
    private int serieActual;
    /** Momento (reloj del sistema) en que termina el descanso; 0 si no hay descanso en curso. */
    private long finDescanso;
    private CountDownTimer temporizador;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ejercicio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repositorio = new LocalRepository(requireContext());
        registros = repositorio.obtenerEjercicios();
        enlazarVistas(view);

        if (savedInstanceState != null) {
            ejercicio = Ejercicio.values()[savedInstanceState.getInt(E_EJERCICIO, 0)];
            periodo = ResumenEjercicio.Periodo.values()[savedInstanceState.getInt(E_PERIODO, 0)];
        }

        crearChipsDeEjercicios(view);
        configurarSliders();
        configurarPeriodo(view);

        fab.setOnClickListener(v -> {
            if (ejercicio.getTipo() == Ejercicio.Tipo.REPETICIONES) {
                iniciarSesion();
            } else {
                registrarDirecto();
            }
        });
        btnSinGuia.setOnClickListener(v -> registrarDirecto());
        btnSerieCompletada.setOnClickListener(v -> serieCompletada());
        btnSaltar.setOnClickListener(v -> terminarDescanso(false));
        view.findViewById(R.id.btn_cancelar_sesion).setOnClickListener(v -> cancelarSesion());

        seleccionarEjercicio(ejercicio);
        mostrarResumen();
        mostrarHistorial();

        if (savedInstanceState != null && savedInstanceState.getBoolean(E_SESION, false)) {
            restaurarSesion(savedInstanceState);
        }
    }

    private void enlazarVistas(View v) {
        raiz = v.findViewById(R.id.raiz);
        tvResumen = v.findViewById(R.id.tv_resumen);
        llConfiguracion = v.findViewById(R.id.ll_configuracion);
        llRepeticiones = v.findViewById(R.id.ll_repeticiones);
        llDuracion = v.findViewById(R.id.ll_duracion);
        llPasos = v.findViewById(R.id.ll_pasos);
        btnSinGuia = v.findViewById(R.id.btn_sin_guia);
        tvSeries = v.findViewById(R.id.tv_series);
        tvReps = v.findViewById(R.id.tv_reps);
        tvMinutos = v.findViewById(R.id.tv_minutos);
        tvPasos = v.findViewById(R.id.tv_pasos);
        sliderSeries = v.findViewById(R.id.slider_series);
        sliderReps = v.findViewById(R.id.slider_reps);
        sliderMinutos = v.findViewById(R.id.slider_minutos);
        sliderPasos = v.findViewById(R.id.slider_pasos);
        swDescanso = v.findViewById(R.id.sw_descanso);
        fab = v.findViewById(R.id.fab_iniciar);
        llSesion = v.findViewById(R.id.ll_sesion);
        flDescanso = v.findViewById(R.id.fl_descanso);
        btnSerieCompletada = v.findViewById(R.id.btn_serie_completada);
        btnSaltar = v.findViewById(R.id.btn_saltar);
        tvSesionEjercicio = v.findViewById(R.id.tv_sesion_ejercicio);
        tvSesionSerie = v.findViewById(R.id.tv_sesion_serie);
        tvSesionIndicacion = v.findViewById(R.id.tv_sesion_indicacion);
        tvCuenta = v.findViewById(R.id.tv_cuenta);
        progresoDescanso = v.findViewById(R.id.progreso_descanso);
        llHistorial = v.findViewById(R.id.ll_historial);
        tvHistorialAyuda = v.findViewById(R.id.tv_historial_ayuda);
    }

    // ---------- Configuración ----------

    private void crearChipsDeEjercicios(View view) {
        ChipGroup grupo = view.findViewById(R.id.chips_ejercicio);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Ejercicio e : Ejercicio.values()) {
            Chip chip = (Chip) inflater.inflate(R.layout.item_chip_ejercicio, grupo, false);
            chip.setId(View.generateViewId());
            chip.setText(e.getEmoji() + " " + e.getNombre());
            chip.setTag(e);
            grupo.addView(chip);
            if (e == ejercicio) {
                chip.setChecked(true);
            }
        }
        grupo.setOnCheckedStateChangeListener((g, ids) -> {
            if (!ids.isEmpty()) {
                Chip elegido = g.findViewById(ids.get(0));
                seleccionarEjercicio((Ejercicio) elegido.getTag());
            }
        });
    }

    private void configurarSliders() {
        sliderSeries.addOnChangeListener((s, valor, usuario) ->
                tvSeries.setText(getString(R.string.ejercicio_series, Math.round(valor))));
        sliderReps.addOnChangeListener((s, valor, usuario) ->
                tvReps.setText(getString(R.string.ejercicio_repeticiones, Math.round(valor))));
        sliderMinutos.addOnChangeListener((s, valor, usuario) ->
                tvMinutos.setText(getString(R.string.ejercicio_minutos, Math.round(valor))));
        sliderPasos.addOnChangeListener((s, valor, usuario) ->
                tvPasos.setText(getString(R.string.ejercicio_pasos, String.format(ES, "%,d", Math.round(valor)))));
        tvSeries.setText(getString(R.string.ejercicio_series, Math.round(sliderSeries.getValue())));
        tvReps.setText(getString(R.string.ejercicio_repeticiones, Math.round(sliderReps.getValue())));
        tvMinutos.setText(getString(R.string.ejercicio_minutos, Math.round(sliderMinutos.getValue())));
        tvPasos.setText(getString(R.string.ejercicio_pasos, String.format(ES, "%,d", Math.round(sliderPasos.getValue()))));
    }

    private void configurarPeriodo(View view) {
        MaterialButtonToggleGroup toggle = view.findViewById(R.id.toggle_periodo);
        int[] botones = {R.id.btn_hoy, R.id.btn_semana, R.id.btn_mes};
        toggle.check(botones[periodo.ordinal()]);
        toggle.addOnButtonCheckedListener((g, id, marcado) -> {
            if (!marcado) {
                return;
            }
            if (id == R.id.btn_semana) {
                periodo = ResumenEjercicio.Periodo.SEMANA;
            } else if (id == R.id.btn_mes) {
                periodo = ResumenEjercicio.Periodo.MES;
            } else {
                periodo = ResumenEjercicio.Periodo.HOY;
            }
            mostrarResumen();
        });
    }

    /** Muestra los controles que corresponden al tipo de ejercicio elegido. */
    private void seleccionarEjercicio(Ejercicio e) {
        ejercicio = e;
        boolean repeticiones = e.getTipo() == Ejercicio.Tipo.REPETICIONES;
        llRepeticiones.setVisibility(repeticiones ? View.VISIBLE : View.GONE);
        llDuracion.setVisibility(e.getTipo() == Ejercicio.Tipo.DURACION ? View.VISIBLE : View.GONE);
        llPasos.setVisibility(e.getTipo() == Ejercicio.Tipo.PASOS ? View.VISIBLE : View.GONE);
        btnSinGuia.setVisibility(repeticiones ? View.VISIBLE : View.GONE);
        fab.setText(repeticiones ? R.string.ejercicio_iniciar : R.string.ejercicio_registrar);
    }

    // ---------- Registro ----------

    private void registrarDirecto() {
        long ahora = System.currentTimeMillis();
        long id = ahora;
        RegistroEjercicio r;
        switch (ejercicio.getTipo()) {
            case REPETICIONES:
                r = RegistroEjercicio.deRepeticiones(id, ejercicio,
                        Math.round(sliderSeries.getValue()), Math.round(sliderReps.getValue()), ahora);
                break;
            case DURACION:
                r = RegistroEjercicio.deMinutos(id, ejercicio, Math.round(sliderMinutos.getValue()), ahora);
                break;
            default:
                r = RegistroEjercicio.dePasos(id, Math.round(sliderPasos.getValue()), ahora);
        }
        guardar(r, getString(R.string.ejercicio_registrado, ejercicio.getNombre() + " · " + r.getResumen()));
    }

    private void guardar(RegistroEjercicio r, String mensaje) {
        registros.add(r);
        repositorio.guardarEjercicios(registros);
        mostrarResumen();
        mostrarHistorial();
        Snackbar.make(raiz, mensaje, Snackbar.LENGTH_LONG).setAnchorView(fab).show();
    }

    private void mostrarResumen() {
        ResumenEjercicio resumen = ResumenEjercicio.calcular(registros, periodo, LocalDate.now());
        tvResumen.setText(resumen.texto(periodo));
    }

    /** Ejercicios de hoy, del más reciente al más antiguo. Pulsación larga para borrar. */
    private void mostrarHistorial() {
        llHistorial.removeAllViews();
        LocalDate hoy = LocalDate.now();
        List<RegistroEjercicio> deHoy = new ArrayList<>();
        for (RegistroEjercicio r : registros) {
            if (r.getDia().equals(hoy)) {
                deHoy.add(r);
            }
        }
        deHoy.sort((a, b) -> Long.compare(b.getFecha(), a.getFecha()));

        int margen = Math.round(3 * getResources().getDisplayMetrics().density);
        for (RegistroEjercicio r : deHoy) {
            TextView fila = new TextView(requireContext());
            fila.setBackgroundResource(R.drawable.bg_tarjeta);
            int relleno = Math.round(10 * getResources().getDisplayMetrics().density);
            fila.setPadding(relleno, relleno, relleno, relleno);
            fila.setTextSize(14);
            String hora = Instant.ofEpochMilli(r.getFecha()).atZone(ZoneId.systemDefault())
                    .toLocalTime().format(formatoHora);
            fila.setText(r.getEjercicio().getEmoji() + " " + r.getEjercicio().getNombre()
                    + "\n" + r.getResumen() + " · " + hora);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, margen, 0, margen);
            fila.setOnLongClickListener(v -> {
                confirmarBorrado(r);
                return true;
            });
            llHistorial.addView(fila, lp);
        }
        tvHistorialAyuda.setText(deHoy.isEmpty()
                ? R.string.ejercicio_historial_vacio : R.string.ejercicio_historial_ayuda);
    }

    private void confirmarBorrado(RegistroEjercicio r) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.ejercicio_borrar, r.getEjercicio().getNombre() + " · " + r.getResumen()))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    registros.remove(r);
                    repositorio.guardarEjercicios(registros);
                    mostrarResumen();
                    mostrarHistorial();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    // ---------- Sesión guiada por series ----------

    private void iniciarSesion() {
        sesionActiva = true;
        sesionSeries = Math.round(sliderSeries.getValue());
        sesionReps = Math.round(sliderReps.getValue());
        sesionConDescanso = swDescanso.isChecked();
        serieActual = 1;
        finDescanso = 0;
        raiz.setKeepScreenOn(true);   // que la pantalla no se apague durante el entrenamiento
        mostrarSesion();
    }

    private void serieCompletada() {
        if (serieActual >= sesionSeries) {
            terminarSesion();
        } else if (sesionConDescanso) {
            iniciarDescanso(DESCANSO_MS);
        } else {
            serieActual++;
            mostrarSesion();
        }
    }

    private void iniciarDescanso(long duracion) {
        finDescanso = SystemClock.elapsedRealtime() + duracion;
        mostrarSesion();
        cancelarTemporizador();
        temporizador = new CountDownTimer(duracion, 250) {
            @Override
            public void onTick(long restante) {
                mostrarCuenta(restante);
            }

            @Override
            public void onFinish() {
                terminarDescanso(true);
            }
        };
        mostrarCuenta(duracion);
        temporizador.start();
    }

    private void mostrarCuenta(long restanteMs) {
        int segundos = (int) Math.ceil(restanteMs / 1000.0);
        tvCuenta.setText(String.format(Locale.ROOT, "%d:%02d", segundos / 60, segundos % 60));
        progresoDescanso.setProgressCompat(segundos, false);
    }

    private void terminarDescanso(boolean avisar) {
        cancelarTemporizador();
        finDescanso = 0;
        serieActual++;
        if (avisar) {
            avisarFinDescanso();
        }
        mostrarSesion();
    }

    private void terminarSesion() {
        RegistroEjercicio r = RegistroEjercicio.deRepeticiones(System.currentTimeMillis(), ejercicio,
                sesionSeries, sesionReps, System.currentTimeMillis());
        cerrarSesion();
        guardar(r, getString(R.string.ejercicio_terminado, r.getResumen()));
    }

    private void cancelarSesion() {
        cerrarSesion();
    }

    private void cerrarSesion() {
        cancelarTemporizador();
        sesionActiva = false;
        finDescanso = 0;
        raiz.setKeepScreenOn(false);
        llSesion.setVisibility(View.GONE);
        llConfiguracion.setVisibility(View.VISIBLE);
        fab.show();
    }

    private void mostrarSesion() {
        llConfiguracion.setVisibility(View.GONE);
        llSesion.setVisibility(View.VISIBLE);
        fab.hide();

        boolean descansando = finDescanso > 0;
        tvSesionEjercicio.setText(ejercicio.getEmoji() + " " + ejercicio.getNombre());
        tvSesionSerie.setText(getString(R.string.ejercicio_serie, serieActual, sesionSeries));
        tvSesionIndicacion.setText(descansando
                ? getString(R.string.ejercicio_descansando)
                : getString(R.string.ejercicio_haz, sesionReps, ejercicio.getNombre().toLowerCase(ES)));
        flDescanso.setVisibility(descansando ? View.VISIBLE : View.GONE);
        btnSaltar.setVisibility(descansando ? View.VISIBLE : View.GONE);
        btnSerieCompletada.setVisibility(descansando ? View.GONE : View.VISIBLE);
        ((MaterialButton) btnSerieCompletada).setText(serieActual >= sesionSeries
                ? R.string.ejercicio_registrar : R.string.ejercicio_serie_completada);
    }

    /** Pitido corto y vibración cuando termina el minuto de descanso. */
    private void avisarFinDescanso() {
        try {
            ToneGenerator tono = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90);
            tono.startTone(ToneGenerator.TONE_PROP_BEEP2, 500);
            handler.postDelayed(tono::release, 800);
        } catch (RuntimeException ignorada) {
            // algunos dispositivos no permiten crear el generador de tonos
        }
        Vibrator vibrador = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? requireContext().getSystemService(VibratorManager.class).getDefaultVibrator()
                : requireContext().getSystemService(Vibrator.class);
        if (vibrador != null && vibrador.hasVibrator()) {
            vibrador.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void cancelarTemporizador() {
        if (temporizador != null) {
            temporizador.cancel();
            temporizador = null;
        }
    }

    // ---------- Rotación de pantalla ----------

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(E_EJERCICIO, ejercicio.ordinal());
        outState.putInt(E_PERIODO, periodo.ordinal());
        outState.putBoolean(E_SESION, sesionActiva);
        outState.putInt(E_SERIES, sesionSeries);
        outState.putInt(E_REPS, sesionReps);
        outState.putBoolean(E_CON_DESCANSO, sesionConDescanso);
        outState.putInt(E_SERIE_ACTUAL, serieActual);
        outState.putLong(E_DESCANSO_RESTANTE,
                finDescanso > 0 ? Math.max(0, finDescanso - SystemClock.elapsedRealtime()) : -1);
    }

    private void restaurarSesion(Bundle estado) {
        sesionActiva = true;
        sesionSeries = estado.getInt(E_SERIES, 1);
        sesionReps = estado.getInt(E_REPS, 1);
        sesionConDescanso = estado.getBoolean(E_CON_DESCANSO, true);
        serieActual = estado.getInt(E_SERIE_ACTUAL, 1);
        raiz.setKeepScreenOn(true);
        long restante = estado.getLong(E_DESCANSO_RESTANTE, -1);
        if (restante > 0) {
            iniciarDescanso(restante);
        } else if (restante == 0) {
            terminarDescanso(false);
        } else {
            mostrarSesion();
        }
    }

    @Override
    public void onDestroyView() {
        cancelarTemporizador();
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
