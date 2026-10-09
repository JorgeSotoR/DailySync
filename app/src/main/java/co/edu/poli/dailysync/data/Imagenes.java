package co.edu.poli.dailysync.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

/** Lectura de imágenes guardadas, reducidas al tamaño necesario para no gastar memoria. */
public final class Imagenes {

    /** Miniaturas ya decodificadas (máximo ~8 MB). */
    private static final LruCache<String, Bitmap> MINIATURAS = new LruCache<String, Bitmap>(8 * 1024 * 1024) {
        @Override
        protected int sizeOf(String clave, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };

    private Imagenes() { }

    /** Decodifica un archivo de imagen con un lado máximo aproximado (en píxeles). */
    public static Bitmap decodificar(String ruta, int ladoMaximo) {
        BitmapFactory.Options limites = new BitmapFactory.Options();
        limites.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ruta, limites);
        int escala = 1;
        while (limites.outWidth / (escala * 2) >= ladoMaximo && limites.outHeight / (escala * 2) >= ladoMaximo) {
            escala *= 2;
        }
        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = escala;
        return BitmapFactory.decodeFile(ruta, opciones);
    }

    /** Miniatura de unos 160 px, guardada en caché para que la lista se desplace con fluidez. */
    public static Bitmap miniatura(String ruta) {
        Bitmap enCache = MINIATURAS.get(ruta);
        if (enCache != null) {
            return enCache;
        }
        Bitmap bitmap = decodificar(ruta, 160);
        if (bitmap != null) {
            MINIATURAS.put(ruta, bitmap);
        }
        return bitmap;
    }
}
