package co.edu.poli.dailysync.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import co.edu.poli.dailysync.model.Actividad;
import co.edu.poli.dailysync.model.Alarma;
import co.edu.poli.dailysync.model.ItemCompra;
import co.edu.poli.dailysync.model.RegistroCiclo;
import co.edu.poli.dailysync.model.Usuario;

/**
 * Guarda y lee los datos de la aplicación en el almacenamiento interno (RNF-04).
 * Los objetos se convierten a JSON con Gson y se guardan en SharedPreferences.
 */
public class LocalRepository {

    private static final String PREFS = "dailysync_prefs";
    private static final String K_USUARIO = "usuario";
    private static final String K_ACTIVIDADES = "actividades";
    private static final String K_ALARMAS = "alarmas";
    private static final String K_COMPRAS = "compras";
    private static final String K_CICLO = "ciclo";

    /** Tamaño máximo (en píxeles) de la imagen de perfil guardada. */
    private static final int LADO_MAXIMO_AVATAR = 512;

    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public LocalRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- Usuario ----------

    public void guardarUsuario(Usuario usuario) {
        prefs.edit().putString(K_USUARIO, gson.toJson(usuario)).apply();
    }

    public Usuario obtenerUsuario() {
        String json = prefs.getString(K_USUARIO, null);
        return json == null ? null : gson.fromJson(json, Usuario.class);
    }

    // ---------- Listas ----------

    public List<Actividad> obtenerActividades() {
        return leerLista(K_ACTIVIDADES, new TypeToken<List<Actividad>>() { }.getType());
    }

    public void guardarActividades(List<Actividad> lista) {
        guardarLista(K_ACTIVIDADES, lista);
    }

    public List<Alarma> obtenerAlarmas() {
        return leerLista(K_ALARMAS, new TypeToken<List<Alarma>>() { }.getType());
    }

    public void guardarAlarmas(List<Alarma> lista) {
        guardarLista(K_ALARMAS, lista);
    }

    public List<ItemCompra> obtenerCompras() {
        return leerLista(K_COMPRAS, new TypeToken<List<ItemCompra>>() { }.getType());
    }

    public void guardarCompras(List<ItemCompra> lista) {
        guardarLista(K_COMPRAS, lista);
    }

    public List<RegistroCiclo> obtenerRegistrosCiclo() {
        return leerLista(K_CICLO, new TypeToken<List<RegistroCiclo>>() { }.getType());
    }

    public void guardarRegistrosCiclo(List<RegistroCiclo> lista) {
        guardarLista(K_CICLO, lista);
    }

    private void guardarLista(String clave, List<?> lista) {
        prefs.edit().putString(clave, gson.toJson(lista)).apply();
    }

    private <T> List<T> leerLista(String clave, Type tipo) {
        String json = prefs.getString(clave, null);
        if (json == null) {
            return new ArrayList<>();
        }
        List<T> lista = gson.fromJson(json, tipo);
        return lista != null ? lista : new ArrayList<>();
    }

    // ---------- Imágenes ----------

    /**
     * Copia una imagen elegida en la galería al almacenamiento interno de la app,
     * reducida a un máximo de 512 px por lado. Devuelve la ruta del archivo.
     */
    public String guardarImagen(Uri uri) throws IOException {
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, limites);
        }
        int escala = 1;
        while (limites.outWidth / (escala * 2) >= LADO_MAXIMO_AVATAR
                && limites.outHeight / (escala * 2) >= LADO_MAXIMO_AVATAR) {
            escala *= 2;
        }
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = escala;
        Bitmap bitmap;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(in, null, opciones);
        }
        if (bitmap == null) {
            throw new IOException("No se pudo leer la imagen");
        }
        File archivo = new File(context.getFilesDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(archivo)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        }
        bitmap.recycle();
        return archivo.getAbsolutePath();
    }
}
