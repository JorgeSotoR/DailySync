package co.edu.poli.dailysync;

/**
 * Comunicación entre el MenuFragment y la MainActivity.
 * El menú no conoce a los demás fragmentos: solo avisa qué opción se eligió.
 */
public interface OnMenuItemSelectedListener {
    void onMenuItemSelected(String opcion);
}
