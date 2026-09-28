package co.edu.poli.dailysync.model;

/** Tarea, rutina o evento del día (RF-04). */
public class Actividad {

    private long id;
    private String titulo;
    private long fecha;      // milisegundos
    private TipoActividad tipo;
    private boolean completada;

    public Actividad(long id, String titulo, long fecha, TipoActividad tipo) {
        this.id = id;
        this.titulo = titulo;
        this.fecha = fecha;
        this.tipo = tipo;
        this.completada = false;
    }

    public void marcarCompletada(boolean completada) {
        this.completada = completada;
    }

    public long getId() { return id; }
    public String getTitulo() { return titulo; }
    public long getFecha() { return fecha; }
    public TipoActividad getTipo() { return tipo; }
    public boolean isCompletada() { return completada; }
}
