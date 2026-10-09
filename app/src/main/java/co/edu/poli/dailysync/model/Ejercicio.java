package co.edu.poli.dailysync.model;

/** Catálogo de ejercicios del registro de actividad física. */
public enum Ejercicio {
    BURPEES("Burpees", "🔥", Tipo.REPETICIONES),
    SENTADILLAS("Sentadillas", "🏋", Tipo.REPETICIONES),
    FLEXIONES("Flexiones", "💪", Tipo.REPETICIONES),
    ABDOMINALES("Abdominales", "⚡", Tipo.REPETICIONES),
    ESTIRAMIENTOS("Estiramientos", "🤸", Tipo.DURACION),
    CICLISMO("Ciclismo", "🚴", Tipo.DURACION),
    TROTE("Trote", "🏃", Tipo.DURACION),
    PASOS("Pasos", "👟", Tipo.PASOS);

    /** Cómo se mide el ejercicio. */
    public enum Tipo { REPETICIONES, DURACION, PASOS }

    private final String nombre;
    private final String emoji;
    private final Tipo tipo;

    Ejercicio(String nombre, String emoji, Tipo tipo) {
        this.nombre = nombre;
        this.emoji = emoji;
        this.tipo = tipo;
    }

    public String getNombre() { return nombre; }
    public String getEmoji() { return emoji; }
    public Tipo getTipo() { return tipo; }
}
