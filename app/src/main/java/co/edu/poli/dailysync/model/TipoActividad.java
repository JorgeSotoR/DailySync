package co.edu.poli.dailysync.model;

public enum TipoActividad {
    TAREA("Tarea"),
    RUTINA("Rutina"),
    EVENTO("Evento");

    private final String etiqueta;

    TipoActividad(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
