package co.edu.poli.dailysync.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

/** Un ejercicio realizado: series × repeticiones, minutos o pasos. */
public class RegistroEjercicio {

    private static final Locale ES = new Locale("es", "CO");

    private long id;
    private Ejercicio ejercicio;
    private int series;
    private int repeticiones;
    private int minutos;
    private int pasos;
    private long fecha;   // milisegundos

    private RegistroEjercicio(long id, Ejercicio ejercicio, long fecha) {
        this.id = id;
        this.ejercicio = ejercicio;
        this.fecha = fecha;
    }

    public static RegistroEjercicio deRepeticiones(long id, Ejercicio e, int series, int repeticiones, long fecha) {
        RegistroEjercicio r = new RegistroEjercicio(id, e, fecha);
        r.series = series;
        r.repeticiones = repeticiones;
        return r;
    }

    public static RegistroEjercicio deMinutos(long id, Ejercicio e, int minutos, long fecha) {
        RegistroEjercicio r = new RegistroEjercicio(id, e, fecha);
        r.minutos = minutos;
        return r;
    }

    public static RegistroEjercicio dePasos(long id, int pasos, long fecha) {
        RegistroEjercicio r = new RegistroEjercicio(id, Ejercicio.PASOS, fecha);
        r.pasos = pasos;
        return r;
    }

    public int getTotalRepeticiones() {
        return series * repeticiones;
    }

    /** Por ejemplo "3 × 15 = 45 repeticiones", "30 min" o "8.000 pasos". */
    public String getResumen() {
        switch (ejercicio.getTipo()) {
            case REPETICIONES:
                return series + " × " + repeticiones + " = " + getTotalRepeticiones() + " repeticiones";
            case DURACION:
                return minutos + " min";
            default:
                return String.format(ES, "%,d pasos", pasos);
        }
    }

    public LocalDate getDia() {
        return Instant.ofEpochMilli(fecha).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public long getId() { return id; }
    public Ejercicio getEjercicio() { return ejercicio; }
    public int getSeries() { return series; }
    public int getRepeticiones() { return repeticiones; }
    public int getMinutos() { return minutos; }
    public int getPasos() { return pasos; }
    public long getFecha() { return fecha; }
}
