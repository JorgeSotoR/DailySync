package co.edu.poli.dailysync.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculos del ciclo menstrual a partir de los inicios registrados.
 * No depende de Android, por eso se puede probar con JUnit.
 *
 * Reglas (estimaciones informativas):
 *  - Duración del ciclo: promedio de los días entre inicios consecutivos (solo ciclos de 21 a 45 días);
 *    si no hay datos suficientes, 28 días.
 *  - Próximo periodo: último inicio + duración del ciclo.
 *  - Ovulación estimada: 14 días antes del próximo periodo.
 *  - Ventana fértil: desde 5 días antes de la ovulación hasta 1 día después.
 */
public class CalculadoraCiclo {

    public enum TipoDia { NINGUNO, PERIODO, PERIODO_ESTIMADO, FERTIL }

    public static final int CICLO_POR_DEFECTO = 28;
    private static final int CICLOS_A_PROYECTAR = 3;

    private final List<RegistroCiclo> registros;

    public CalculadoraCiclo(List<RegistroCiclo> registros) {
        this.registros = new ArrayList<>(registros);
        this.registros.sort(Comparator.comparing(RegistroCiclo::getInicio));
    }

    public boolean hayRegistros() {
        return !registros.isEmpty();
    }

    public RegistroCiclo getUltimo() {
        return registros.isEmpty() ? null : registros.get(registros.size() - 1);
    }

    public int duracionCiclo() {
        long suma = 0;
        int ciclos = 0;
        for (int i = 1; i < registros.size(); i++) {
            long dias = ChronoUnit.DAYS.between(registros.get(i - 1).getInicio(), registros.get(i).getInicio());
            if (dias >= 21 && dias <= 45) {
                suma += dias;
                ciclos++;
            }
        }
        return ciclos == 0 ? CICLO_POR_DEFECTO : Math.round((float) suma / ciclos);
    }

    /** Duración promedio del sangrado según los registros. */
    public int duracionPeriodo() {
        if (registros.isEmpty()) {
            return RegistroCiclo.DURACION_POR_DEFECTO;
        }
        int suma = 0;
        for (RegistroCiclo r : registros) {
            suma += r.getDuracionDias();
        }
        return Math.round((float) suma / registros.size());
    }

    /** Día del ciclo para una fecha (1 = día de inicio del último periodo). 0 si no aplica. */
    public long diaDelCiclo(LocalDate fecha) {
        RegistroCiclo ultimo = getUltimo();
        if (ultimo == null || fecha.isBefore(ultimo.getInicio())) {
            return 0;
        }
        return ChronoUnit.DAYS.between(ultimo.getInicio(), fecha) + 1;
    }

    /**
     * Próximo inicio estimado a partir de una fecha. Si la fecha estimada ya pasó sin registro,
     * se devuelve esa fecha atrasada para que la interfaz pueda avisarlo.
     */
    public LocalDate proximoInicio() {
        RegistroCiclo ultimo = getUltimo();
        return ultimo == null ? null : ultimo.getInicio().plusDays(duracionCiclo());
    }

    /** Clasifica cada día relevante: periodos registrados, periodos estimados y ventanas fértiles. */
    public Map<LocalDate, TipoDia> tiposDeDia() {
        Map<LocalDate, TipoDia> mapa = new HashMap<>();
        if (registros.isEmpty()) {
            return mapa;
        }
        int ciclo = duracionCiclo();
        int periodo = duracionPeriodo();
        RegistroCiclo ultimo = getUltimo();

        // Ventanas fértiles y periodos estimados de los próximos ciclos.
        for (int k = 1; k <= CICLOS_A_PROYECTAR; k++) {
            LocalDate inicio = ultimo.getInicio().plusDays((long) ciclo * k);
            LocalDate ovulacion = inicio.minusDays(14);
            for (int d = -5; d <= 1; d++) {
                mapa.put(ovulacion.plusDays(d), TipoDia.FERTIL);
            }
            for (int d = 0; d < periodo; d++) {
                mapa.put(inicio.plusDays(d), TipoDia.PERIODO_ESTIMADO);
            }
        }
        // Los periodos registrados tienen prioridad sobre cualquier estimación.
        for (RegistroCiclo r : registros) {
            for (int d = 0; d < r.getDuracionDias(); d++) {
                mapa.put(r.getInicio().plusDays(d), TipoDia.PERIODO);
            }
        }
        return mapa;
    }

    /** Registro cuyo periodo contiene la fecha, o null. */
    public RegistroCiclo registroQueContiene(LocalDate fecha) {
        for (RegistroCiclo r : registros) {
            LocalDate inicio = r.getInicio();
            if (!fecha.isBefore(inicio) && fecha.isBefore(inicio.plusDays(r.getDuracionDias()))) {
                return r;
            }
        }
        return null;
    }
}
