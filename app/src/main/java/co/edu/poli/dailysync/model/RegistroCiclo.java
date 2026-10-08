package co.edu.poli.dailysync.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Inicio de un periodo registrado en el módulo Salud. */
public class RegistroCiclo {

    public static final int DURACION_POR_DEFECTO = 5;

    private long fechaInicio;   // milisegundos, a medianoche (hora local)
    private int duracionDias;   // días de sangrado

    public RegistroCiclo(LocalDate inicio, int duracionDias) {
        this.fechaInicio = inicio.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        this.duracionDias = duracionDias;
    }

    public long getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getInicio() {
        return Instant.ofEpochMilli(fechaInicio).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Los registros guardados antes de esta versión no tienen duración: se usan 5 días. */
    public int getDuracionDias() {
        return duracionDias > 0 ? duracionDias : DURACION_POR_DEFECTO;
    }
}
