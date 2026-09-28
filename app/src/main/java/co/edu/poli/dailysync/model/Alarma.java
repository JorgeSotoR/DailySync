package co.edu.poli.dailysync.model;

import java.util.Locale;

/** Alarma configurada por el usuario (RF-05). */
public class Alarma {

    private long id;
    private int hora;
    private int minuto;
    private String etiqueta;
    private boolean activa;

    public Alarma(long id, int hora, int minuto, String etiqueta) {
        this.id = id;
        this.hora = hora;
        this.minuto = minuto;
        this.etiqueta = etiqueta;
        this.activa = true;
    }

    /** Hora en formato de 12 horas, por ejemplo "6:30 p.m.". */
    public String getHoraTexto() {
        int h12 = hora % 12 == 0 ? 12 : hora % 12;
        String sufijo = hora < 12 ? "a.m." : "p.m.";
        return String.format(Locale.getDefault(), "%d:%02d %s", h12, minuto, sufijo);
    }

    /** Minutos desde la medianoche, para ordenar la lista. */
    public int getMinutosDelDia() {
        return hora * 60 + minuto;
    }

    public long getId() { return id; }
    public int getHora() { return hora; }
    public int getMinuto() { return minuto; }
    public String getEtiqueta() { return etiqueta; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
