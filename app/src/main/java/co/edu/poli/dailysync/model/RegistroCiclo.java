package co.edu.poli.dailysync.model;

/** Inicio de un periodo registrado en el módulo Salud. */
public class RegistroCiclo {

    private long fechaInicio;   // milisegundos, a medianoche

    public RegistroCiclo(long fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public long getFechaInicio() {
        return fechaInicio;
    }
}
