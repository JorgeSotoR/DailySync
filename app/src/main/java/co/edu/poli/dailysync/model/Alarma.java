package co.edu.poli.dailysync.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Alarma configurada por el usuario (RF-05). */
public class Alarma {

    private static final String[] LETRAS = {"L", "M", "X", "J", "V", "S", "D"};

    private long id;
    private int hora;
    private int minuto;
    private String etiqueta;
    private boolean activa;
    /** Días de repetición: 1 = lunes … 7 = domingo. Vacía = suena una sola vez. */
    private List<Integer> dias;

    public Alarma(long id, int hora, int minuto, String etiqueta, List<Integer> dias) {
        this.id = id;
        this.hora = hora;
        this.minuto = minuto;
        this.etiqueta = etiqueta;
        this.activa = true;
        setDias(dias);
    }

    /** Hora en formato de 12 horas, por ejemplo "6:30 p.m.". */
    public String getHoraTexto() {
        return textoHora(hora, minuto);
    }

    public static String textoHora(int hora, int minuto) {
        int h12 = hora % 12 == 0 ? 12 : hora % 12;
        String sufijo = hora < 12 ? "a.m." : "p.m.";
        return String.format(Locale.getDefault(), "%d:%02d %s", h12, minuto, sufijo);
    }

    /** "Una vez", "Todos los días", "Lunes a viernes", "Fines de semana" o letras como "L X V". */
    public String getTextoRepeticion() {
        List<Integer> d = getDias();
        if (d.isEmpty()) {
            return "Una vez";
        }
        if (d.size() == 7) {
            return "Todos los días";
        }
        if (d.equals(Arrays.asList(1, 2, 3, 4, 5))) {
            return "Lunes a viernes";
        }
        if (d.equals(Arrays.asList(6, 7))) {
            return "Fines de semana";
        }
        StringBuilder sb = new StringBuilder();
        for (int dia : d) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(LETRAS[dia - 1]);
        }
        return sb.toString();
    }

    /**
     * Próxima fecha y hora en que debe sonar, estrictamente después de {@code ahora}.
     * Sin días de repetición: hoy a esa hora, o mañana si ya pasó.
     */
    public LocalDateTime proximoDisparo(LocalDateTime ahora) {
        LocalTime horaAlarma = LocalTime.of(hora, minuto);
        LocalDate fecha = ahora.toLocalDate();
        List<Integer> d = getDias();
        for (int i = 0; i <= 7; i++) {
            LocalDateTime candidato = LocalDateTime.of(fecha.plusDays(i), horaAlarma);
            boolean diaValido = d.isEmpty() || d.contains(candidato.getDayOfWeek().getValue());
            if (diaValido && candidato.isAfter(ahora)) {
                return candidato;
            }
        }
        return LocalDateTime.of(fecha.plusDays(1), horaAlarma);   // no debería ocurrir
    }

    /** Código entero único por alarma, usado en los PendingIntent y en la notificación. */
    public int getRequestCode() {
        return (int) (id ^ (id >>> 32));
    }

    public void actualizar(int hora, int minuto, String etiqueta, List<Integer> dias) {
        this.hora = hora;
        this.minuto = minuto;
        this.etiqueta = etiqueta;
        setDias(dias);
    }

    private void setDias(List<Integer> dias) {
        this.dias = dias == null ? new ArrayList<>() : new ArrayList<>(dias);
        Collections.sort(this.dias);
    }

    /** Minutos desde la medianoche, para ordenar la lista. */
    public int getMinutosDelDia() {
        return hora * 60 + minuto;
    }

    /** Las alarmas guardadas por la versión anterior no tienen días: se tratan como "una vez". */
    public List<Integer> getDias() {
        return dias == null ? new ArrayList<>() : dias;
    }

    public long getId() { return id; }
    public int getHora() { return hora; }
    public int getMinuto() { return minuto; }
    public String getEtiqueta() { return etiqueta; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
