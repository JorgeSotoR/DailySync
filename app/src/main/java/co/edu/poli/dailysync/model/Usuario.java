package co.edu.poli.dailysync.model;

/** Perfil del usuario. Se guarda localmente como JSON (RNF-04). */
public class Usuario {

    public static final String GENERO_FEMENINO = "Femenino";
    public static final String GENERO_MASCULINO = "Masculino";
    public static final String GENERO_NO_ESPECIFICADO = "No especificado";

    public static final String AVATAR_1 = "avatar_1";
    public static final String AVATAR_2 = "avatar_2";

    private String apodo;
    private long fechaNacimiento;   // milisegundos
    private String genero;
    /** "avatar_1", "avatar_2" o la ruta de una imagen guardada en almacenamiento interno. */
    private String avatar;

    public Usuario(String apodo, long fechaNacimiento, String genero, String avatar) {
        this.apodo = apodo;
        this.fechaNacimiento = fechaNacimiento;
        this.genero = genero;
        this.avatar = avatar;
    }

    /** Regla del RF-02: el módulo Salud solo se muestra si el género es femenino. */
    public boolean esFemenino() {
        return GENERO_FEMENINO.equals(genero);
    }

    public String getApodo() { return apodo; }
    public long getFechaNacimiento() { return fechaNacimiento; }
    public String getGenero() { return genero; }
    public String getAvatar() { return avatar; }
}
