package co.edu.poli.dailysync;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import co.edu.poli.dailysync.alarmas.AlarmScheduler;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Usuario;
import co.edu.poli.dailysync.ui.ActividadesFragment;
import co.edu.poli.dailysync.ui.AlarmasFragment;
import co.edu.poli.dailysync.ui.BotonesFragment;
import co.edu.poli.dailysync.ui.ComprasFragment;
import co.edu.poli.dailysync.ui.EjercicioFragment;
import co.edu.poli.dailysync.ui.MenuFragment;
import co.edu.poli.dailysync.ui.MultimediaFragment;
import co.edu.poli.dailysync.ui.PerfilFragment;
import co.edu.poli.dailysync.ui.SaludFragment;
import co.edu.poli.dailysync.ui.WebFragment;

/**
 * Actividad principal. Contiene dos fragmentos al mismo tiempo:
 * el menú (izquierda) y el contenedor de contenido (derecha).
 */
public class MainActivity extends AppCompatActivity
        implements OnMenuItemSelectedListener, PerfilFragment.OnPerfilGuardadoListener {

    /** Extra para abrir directamente una opción del menú (por ejemplo, desde la notificación de una alarma). */
    public static final String EXTRA_ABRIR = "abrir";

    private MenuFragment menuFragment;
    private LocalRepository repositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Mismo comportamiento de borde a borde en todas las versiones, con íconos claros en las barras.
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        aplicarMargenesDeSistema();

        repositorio = new LocalRepository(this);
        menuFragment = (MenuFragment) getSupportFragmentManager().findFragmentById(R.id.menu_container);

        // Solo la primera vez: al rotar, el FragmentManager restaura el fragmento visible.
        if (savedInstanceState == null && esCompartido(getIntent())) {
            AlarmScheduler.reprogramarTodas(this);
            procesarIntentCompartido(getIntent());
        } else if (savedInstanceState == null) {
            Usuario usuario = repositorio.obtenerUsuario();
            String pedida = getIntent().getStringExtra(EXTRA_ABRIR);
            String inicial = usuario == null ? MenuFragment.PERFIL
                    : (pedida != null ? pedida : MenuFragment.ACTIVIDADES);
            menuFragment.marcarSeleccion(inicial);
            mostrarFragmento(crearFragmento(inicial));
            if (usuario == null) {
                Toast.makeText(this, R.string.perfil_crear, Toast.LENGTH_LONG).show();
            }
            // Asegura que las alarmas activas estén programadas en el sistema.
            AlarmScheduler.reprogramarTodas(this);
        }
    }

    /** Intent que abre la app en la opción Alarmas (se usa en la notificación). */
    public static Intent intentAbrirAlarmas(Context context) {
        return new Intent(context, MainActivity.class)
                .putExtra(EXTRA_ABRIR, MenuFragment.ALARMAS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    /** Si la app ya estaba abierta y se toca la notificación, llega aquí (launchMode singleTop). */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (esCompartido(intent)) {
            procesarIntentCompartido(intent);
            return;
        }
        String pedida = intent.getStringExtra(EXTRA_ABRIR);
        if (pedida != null && repositorio.obtenerUsuario() != null) {
            menuFragment.marcarSeleccion(pedida);
            mostrarFragmento(crearFragmento(pedida));
        }
    }

    /** Llamado por el MenuFragment cuando el usuario toca una opción. */
    @Override
    public void onMenuItemSelected(String opcion) {
        mostrarFragmento(crearFragmento(opcion));
    }

    /** Llamado por el PerfilFragment al guardar: actualiza el menú según el género (RF-02). */
    @Override
    public void onPerfilGuardado(Usuario usuario) {
        menuFragment.actualizarOpciones(usuario.esFemenino());
        menuFragment.actualizarUsuario(usuario);
        Toast.makeText(this, R.string.perfil_guardado, Toast.LENGTH_SHORT).show();
    }

    /** Reemplaza solo el fragmento derecho; el menú no se recrea (RF-03). */
    private void mostrarFragmento(Fragment fragmento) {
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.content_container, fragmento)
                .commit();
    }

    private Fragment crearFragmento(String opcion) {
        switch (opcion) {
            case MenuFragment.ACTIVIDADES: return new ActividadesFragment();
            case MenuFragment.ALARMAS:     return new AlarmasFragment();
            case MenuFragment.COMPRAS:     return new ComprasFragment();
            case MenuFragment.SALUD:       return new SaludFragment();
            case MenuFragment.MULTIMEDIA:  return new MultimediaFragment();
            case MenuFragment.WEB:         return new WebFragment();
            case MenuFragment.EJERCICIO:   return new EjercicioFragment();
            case MenuFragment.BOTONES:     return new BotonesFragment();
            case MenuFragment.PERFIL:
            default:                       return new PerfilFragment();
        }
    }

    private static boolean esCompartido(@Nullable Intent intent) {
        return intent != null && Intent.ACTION_SEND.equals(intent.getAction());
    }

    /**
     * Contenido compartido desde otra app con "Compartir" (RF-06): un texto o enlace y/o una imagen.
     * La imagen se copia de inmediato, porque el permiso para leerla es temporal.
     */
    private void procesarIntentCompartido(Intent intent) {
        if (repositorio.obtenerUsuario() == null) {
            menuFragment.marcarSeleccion(MenuFragment.PERFIL);
            mostrarFragmento(crearFragmento(MenuFragment.PERFIL));
            Toast.makeText(this, R.string.compartir_sin_perfil, Toast.LENGTH_LONG).show();
            return;
        }
        String texto = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (texto == null || texto.trim().isEmpty()) {
            texto = intent.getStringExtra(Intent.EXTRA_TEXT);
        }
        if (texto != null) {
            texto = texto.trim();
            if (texto.length() > 150) {
                texto = texto.substring(0, 150);
            }
        }

        String rutaFoto = null;
        Uri imagen = imagenCompartida(intent);
        if (imagen != null) {
            try {
                rutaFoto = repositorio.guardarImagen(imagen, "compra", 1024);
            } catch (Exception e) {
                Toast.makeText(this, R.string.compras_error_foto, Toast.LENGTH_SHORT).show();
            }
        }
        menuFragment.marcarSeleccion(MenuFragment.COMPRAS);
        mostrarFragmento(ComprasFragment.conContenidoCompartido(texto, rutaFoto));
    }

    @Nullable
    @SuppressWarnings("deprecation")
    private static Uri imagenCompartida(Intent intent) {
        String tipo = intent.getType();
        if (tipo == null || !tipo.startsWith("image/")) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri.class);
        }
        return intent.getParcelableExtra(Intent.EXTRA_STREAM);
    }

    /** Evita que el contenido quede debajo de la barra de estado y la de navegación. */
    private void aplicarMargenesDeSistema() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (vista, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
