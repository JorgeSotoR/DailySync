package co.edu.poli.dailysync.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.RegistroCiclo;

/**
 * Registro del ciclo menstrual. Solo aparece en el menú si el perfil es femenino (RF-02).
 * La duración del ciclo es el promedio de los registros; si hay menos de dos, se usan 28 días.
 */
public class SaludFragment extends Fragment {

    private static final int CICLO_POR_DEFECTO = 28;
    private static final Locale ES = new Locale("es", "CO");

    private final SimpleDateFormat formato = new SimpleDateFormat("d 'de' MMMM", ES);

    private LocalRepository repositorio;
    private List<RegistroCiclo> registros;
    private TextView tvEstado;
    private TextView tvHistorial;
    private long fechaSeleccionada;

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
        tvEstado = view.findViewById(R.id.tv_estado);
        tvHistorial = view.findViewById(R.id.tv_historial);

        CalendarView calendario = view.findViewById(R.id.calendario);
        calendario.setMaxDate(System.currentTimeMillis());
        fechaSeleccionada = inicioDelDia(calendario.getDate());
        calendario.setOnDateChangeListener((v, anio, mes, dia) -> {
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(anio, mes, dia);
            fechaSeleccionada = c.getTimeInMillis();
        });

        view.findViewById(R.id.btn_registrar).setOnClickListener(v -> registrarInicio());
        mostrarEstado();
    }

    private void registrarInicio() {
        for (RegistroCiclo r : registros) {
            if (r.getFechaInicio() == fechaSeleccionada) {
                return;   // ya registrado
            }
        }
        registros.add(new RegistroCiclo(fechaSeleccionada));
        registros.sort(Comparator.comparingLong(RegistroCiclo::getFechaInicio));
        repositorio.guardarRegistrosCiclo(registros);
        mostrarEstado();
    }

    private void mostrarEstado() {
        if (registros.isEmpty()) {
            tvEstado.setText(R.string.salud_sin_registros);
            tvHistorial.setText("");
            return;
        }
        long ultimo = registros.get(registros.size() - 1).getFechaInicio();
        int duracion = duracionPromedio();
        long hoy = inicioDelDia(System.currentTimeMillis());
        long diaDelCiclo = TimeUnit.MILLISECONDS.toDays(hoy - ultimo) + 1;

        Calendar proximo = Calendar.getInstance();
        proximo.setTimeInMillis(ultimo);
        proximo.add(Calendar.DAY_OF_MONTH, duracion);
        long faltan = TimeUnit.MILLISECONDS.toDays(proximo.getTimeInMillis() - hoy);

        String texto = "Día " + diaDelCiclo + " del ciclo (duración estimada: " + duracion + " días)\n";
        if (faltan > 0) {
            texto += "Próximo periodo: en " + faltan + " días (" + formato.format(proximo.getTime()) + ")";
        } else if (faltan == 0) {
            texto += "Próximo periodo: estimado para hoy";
        } else {
            texto += "El periodo estimado para el " + formato.format(proximo.getTime())
                    + " no se ha registrado";
        }
        tvEstado.setText(texto);

        StringBuilder historial = new StringBuilder("Registros: ");
        int desde = Math.max(0, registros.size() - 4);
        for (int i = registros.size() - 1; i >= desde; i--) {
            historial.append(formato.format(new Date(registros.get(i).getFechaInicio())));
            if (i > desde) {
                historial.append(" · ");
            }
        }
        tvHistorial.setText(historial.toString());
    }

    /** Promedio de días entre inicios registrados (solo ciclos entre 21 y 45 días). */
    private int duracionPromedio() {
        long suma = 0;
        int ciclos = 0;
        for (int i = 1; i < registros.size(); i++) {
            long dias = TimeUnit.MILLISECONDS.toDays(
                    registros.get(i).getFechaInicio() - registros.get(i - 1).getFechaInicio());
            if (dias >= 21 && dias <= 45) {
                suma += dias;
                ciclos++;
            }
        }
        return ciclos == 0 ? CICLO_POR_DEFECTO : Math.round((float) suma / ciclos);
    }

    private static long inicioDelDia(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
