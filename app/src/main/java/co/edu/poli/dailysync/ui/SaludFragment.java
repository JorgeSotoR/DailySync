package co.edu.poli.dailysync.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.CalculadoraCiclo;
import co.edu.poli.dailysync.model.RegistroCiclo;

/**
 * Registro del ciclo menstrual con calendario propio. Solo aparece en el menú si el perfil es femenino (RF-02).
 * Colores: rosa = periodo registrado, borde rosa punteado = periodo estimado, verde = ventana fértil estimada.
 */
public class SaludFragment extends Fragment implements DiaAdapter.Acciones {

    private static final Locale ES = new Locale("es", "CO");
    private static final String ESTADO_MES = "mes";
    private static final String ESTADO_SELECCION = "seleccion";
    private static final int MESES_HACIA_ADELANTE = 3;

    private final DateTimeFormatter formatoMes = DateTimeFormatter.ofPattern("MMMM yyyy", ES);
    private final DateTimeFormatter formatoDia = DateTimeFormatter.ofPattern("d 'de' MMMM", ES);

    private LocalRepository repositorio;
    private List<RegistroCiclo> registros;
    private CalculadoraCiclo calculadora;
    private DiaAdapter adapter;

    private TextView tvMes;
    private TextView tvSeleccion;
    private TextView tvEstado;
    private TextView tvHistorial;
    private TextView tvAyuda;
    private View btnSiguiente;

    private YearMonth mesVisible = YearMonth.now();
    private LocalDate seleccionado = LocalDate.now();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_salud, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repositorio = new LocalRepository(requireContext());
        registros = repositorio.obtenerRegistrosCiclo();
        calculadora = new CalculadoraCiclo(registros);

        if (savedInstanceState != null) {
            mesVisible = YearMonth.parse(savedInstanceState.getString(ESTADO_MES, mesVisible.toString()));
            seleccionado = LocalDate.parse(savedInstanceState.getString(ESTADO_SELECCION, seleccionado.toString()));
        }

        tvMes = view.findViewById(R.id.tv_mes);
        tvSeleccion = view.findViewById(R.id.tv_seleccion);
        tvEstado = view.findViewById(R.id.tv_estado);
        tvHistorial = view.findViewById(R.id.tv_historial);
        tvAyuda = view.findViewById(R.id.tv_ayuda);
        btnSiguiente = view.findViewById(R.id.btn_mes_siguiente);

        RecyclerView rv = view.findViewById(R.id.rv_dias);
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        adapter = new DiaAdapter(this);
        rv.setAdapter(adapter);

        view.findViewById(R.id.btn_mes_anterior).setOnClickListener(v -> cambiarMes(-1));
        btnSiguiente.setOnClickListener(v -> cambiarMes(1));
        view.findViewById(R.id.btn_registrar).setOnClickListener(v -> registrarInicio());

        refrescar();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_MES, mesVisible.toString());
        outState.putString(ESTADO_SELECCION, seleccionado.toString());
    }

    // ---------- Calendario ----------

    private void cambiarMes(int delta) {
        YearMonth limite = YearMonth.now().plusMonths(MESES_HACIA_ADELANTE);
        YearMonth nuevo = mesVisible.plusMonths(delta);
        if (!nuevo.isAfter(limite)) {
            mesVisible = nuevo;
            pintarCalendario();
        }
    }

    /** Arma la cuadrícula del mes: celdas vacías hasta el primer día (semana de lunes a domingo). */
    private void pintarCalendario() {
        List<LocalDate> dias = new ArrayList<>();
        int vacios = mesVisible.atDay(1).getDayOfWeek().getValue() - 1;   // lunes = 1
        for (int i = 0; i < vacios; i++) {
            dias.add(null);
        }
        for (int d = 1; d <= mesVisible.lengthOfMonth(); d++) {
            dias.add(mesVisible.atDay(d));
        }
        adapter.setDatos(dias, calculadora.tiposDeDia(), seleccionado);
        tvMes.setText(capitalizar(mesVisible.format(formatoMes)));
        btnSiguiente.setEnabled(mesVisible.isBefore(YearMonth.now().plusMonths(MESES_HACIA_ADELANTE)));
    }

    @Override
    public void onDiaTocado(LocalDate dia) {
        if (dia.isAfter(LocalDate.now())) {
            Toast.makeText(requireContext(), R.string.salud_futuro, Toast.LENGTH_SHORT).show();
            return;
        }
        seleccionado = dia;
        refrescar();
    }

    @Override
    public void onDiaMantenido(LocalDate dia) {
        RegistroCiclo registro = calculadora.registroQueContiene(dia);
        if (registro == null) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.salud_borrar_titulo)
                .setMessage(getString(R.string.salud_borrar_pregunta, registro.getInicio().format(formatoDia)))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    registros.remove(registro);
                    guardarYRefrescar();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    // ---------- Registro ----------

    private void registrarInicio() {
        if (calculadora.registroQueContiene(seleccionado) != null) {
            Toast.makeText(requireContext(), R.string.salud_ya_registrado, Toast.LENGTH_SHORT).show();
            return;
        }
        NumberPicker selector = new NumberPicker(requireContext());
        selector.setMinValue(1);
        selector.setMaxValue(10);
        selector.setValue(calculadora.duracionPeriodo());
        FrameLayout contenedor = new FrameLayout(requireContext());
        contenedor.addView(selector, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.salud_duracion)
                .setView(contenedor)
                .setPositiveButton(R.string.agregar, (d, w) -> {
                    registros.add(new RegistroCiclo(seleccionado, selector.getValue()));
                    guardarYRefrescar();
                    Toast.makeText(requireContext(), R.string.salud_registrado, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void guardarYRefrescar() {
        registros.sort((a, b) -> a.getInicio().compareTo(b.getInicio()));
        repositorio.guardarRegistrosCiclo(registros);
        calculadora = new CalculadoraCiclo(registros);
        refrescar();
    }

    // ---------- Textos ----------

    private void refrescar() {
        pintarCalendario();
        tvSeleccion.setText(getString(R.string.salud_seleccionado, seleccionado.format(formatoDia)));

        if (!calculadora.hayRegistros()) {
            tvEstado.setText(R.string.salud_sin_registros);
            tvHistorial.setVisibility(View.GONE);
            tvAyuda.setVisibility(View.GONE);
            return;
        }
        LocalDate hoy = LocalDate.now();
        LocalDate proximo = calculadora.proximoInicio();
        long faltan = ChronoUnit.DAYS.between(hoy, proximo);

        StringBuilder texto = new StringBuilder();
        texto.append("Día ").append(calculadora.diaDelCiclo(hoy)).append(" del ciclo\n");
        texto.append("Duración estimada del ciclo: ").append(calculadora.duracionCiclo()).append(" días\n");
        if (faltan > 0) {
            texto.append("Próximo periodo: en ").append(faltan).append(faltan == 1 ? " día (" : " días (")
                    .append(proximo.format(formatoDia)).append(")");
        } else if (faltan == 0) {
            texto.append("Próximo periodo: estimado para hoy");
        } else {
            texto.append("El periodo estimado para el ").append(proximo.format(formatoDia))
                    .append(" aún no se ha registrado");
        }
        tvEstado.setText(texto.toString());

        StringBuilder historial = new StringBuilder();
        int desde = Math.max(0, registros.size() - 4);
        for (int i = registros.size() - 1; i >= desde; i--) {
            historial.append(registros.get(i).getInicio().format(formatoDia));
            if (i > desde) {
                historial.append(" · ");
            }
        }
        tvHistorial.setText(getString(R.string.salud_historial, historial.toString()));
        tvHistorial.setVisibility(View.VISIBLE);
        tvAyuda.setVisibility(View.VISIBLE);
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(ES) + texto.substring(1);
    }
}
