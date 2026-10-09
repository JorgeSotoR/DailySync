package co.edu.poli.dailysync.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Totales de actividad física en un periodo: hoy, esta semana (lunes a hoy) o este mes. */
public class ResumenEjercicio {

    public enum Periodo { HOY, SEMANA, MES }

    private static final Locale ES = new Locale("es", "CO");

    public final int repeticiones;
    public final int minutos;
    public final int pasos;
    public final int ejercicios;
    public final int diasActivos;

    private ResumenEjercicio(int repeticiones, int minutos, int pasos, int ejercicios, int diasActivos) {
        this.repeticiones = repeticiones;
        this.minutos = minutos;
        this.pasos = pasos;
        this.ejercicios = ejercicios;
        this.diasActivos = diasActivos;
    }

    public static LocalDate inicioDe(Periodo periodo, LocalDate hoy) {
        switch (periodo) {
            case SEMANA: return hoy.with(DayOfWeek.MONDAY);
            case MES:    return hoy.withDayOfMonth(1);
            default:     return hoy;
        }
    }

    public static ResumenEjercicio calcular(List<RegistroEjercicio> registros, Periodo periodo, LocalDate hoy) {
        LocalDate desde = inicioDe(periodo, hoy);
        int reps = 0, mins = 0, pasos = 0, cantidad = 0;
        Set<LocalDate> dias = new HashSet<>();
        for (RegistroEjercicio r : registros) {
            LocalDate dia = r.getDia();
            if (dia.isBefore(desde) || dia.isAfter(hoy)) {
                continue;
            }
            reps += r.getTotalRepeticiones();
            mins += r.getMinutos();
            pasos += r.getPasos();
            cantidad++;
            dias.add(dia);
        }
        return new ResumenEjercicio(reps, mins, pasos, cantidad, dias.size());
    }

    /** Texto para la tarjeta de resumen. */
    public String texto(Periodo periodo) {
        if (ejercicios == 0) {
            return periodo == Periodo.HOY
                    ? "Aún no registras actividad hoy. ¡Elige un ejercicio y empieza!"
                    : "Sin actividad registrada en este periodo.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 ").append(String.format(ES, "%,d", repeticiones)).append(" repeticiones\n");
        sb.append("⏱ ").append(minutos).append(" min de cardio y estiramiento\n");
        sb.append("👟 ").append(String.format(ES, "%,d", pasos)).append(" pasos\n");
        sb.append(ejercicios).append(ejercicios == 1 ? " ejercicio registrado" : " ejercicios registrados");
        if (periodo != Periodo.HOY) {
            sb.append(" · ").append(diasActivos).append(diasActivos == 1 ? " día activo" : " días activos");
        }
        return sb.toString();
    }
}
