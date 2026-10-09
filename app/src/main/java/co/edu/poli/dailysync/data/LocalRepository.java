package co.edu.poli.dailysync.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

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
import co.edu.poli.dailysync.model.RegistroEjercicio;
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
    private static final String K_EJERCICIOS = "ejercicios";

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

    public List<RegistroEjercicio> obtenerEjercicios() {
        return leerLista(K_EJERCICIOS, new TypeToken<List<RegistroEjercicio>>() { }.getType());
    }

    public void guardarEjercicios(List<RegistroEjercicio> lista) {
        guardarLista(K_EJERCICIOS, lista);
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

    /** Guarda la foto de perfil (máximo 512 px por lado). */
    public String guardarImagen(Uri uri) throws IOException {
        return guardarImagen(uri, "avatar", LADO_MAXIMO_AVATAR);
    }

    /**
     * Copia una imagen (galería, cámara u otra app) al almacenamiento interno de DailySync,
     * reducida a un lado máximo y con la rotación corregida según los datos EXIF de la cámara.
     * Devuelve la ruta del archivo guardado.
     */
    public String guardarImagen(Uri uri, String prefijo, int ladoMaximo) throws IOException {
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, limites);
        }
        int escala = 1;
        while (limites.outWidth / (escala * 2) >= ladoMaximo
                && limites.outHeight / (escala * 2) >= ladoMaximo) {
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

        int grados = gradosDeRotacion(uri);
        if (grados != 0) {
            Matrix matriz = new Matrix();
            matriz.postRotate(grados);
            Bitmap rotada = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matriz, true);
            if (rotada != bitmap) {
                bitmap.recycle();
                bitmap = rotada;
            }
        }

        File archivo = new File(context.getFilesDir(), prefijo + "_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(archivo)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out);
        }
        bitmap.recycle();
        return archivo.getAbsolutePath();
    }

    /** Las fotos de la cámara suelen guardarse "acostadas" con una marca de orientación EXIF. */
    private int gradosDeRotacion(Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return 0;
            }
            int orientacion = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientacion) {
                case ExifInterface.ORIENTATION_ROTATE_90:  return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default:                                   return 0;
            }
        } catch (IOException | RuntimeException e) {
            return 0;
        }
    }

    /** Borra un archivo de imagen guardado por la app (por ejemplo, al eliminar un producto). */
    public void eliminarImagen(String ruta) {
        if (ruta != null && ruta.startsWith(context.getFilesDir().getAbsolutePath())) {
            //noinspection ResultOfMethodCallIgnored
            new File(ruta).delete();
        }
    }
}
