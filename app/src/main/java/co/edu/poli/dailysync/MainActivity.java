package co.edu.poli.dailysync;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Usuario;
import co.edu.poli.dailysync.ui.ActividadesFragment;
import co.edu.poli.dailysync.ui.AlarmasFragment;
import co.edu.poli.dailysync.ui.BotonesFragment;
import co.edu.poli.dailysync.ui.ComprasFragment;
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
        if (savedInstanceState == null) {
            Usuario usuario = repositorio.obtenerUsuario();
            String inicial = usuario == null ? MenuFragment.PERFIL : MenuFragment.ACTIVIDADES;
            menuFragment.marcarSeleccion(inicial);
            mostrarFragmento(crearFragmento(inicial));
            if (usuario == null) {
                Toast.makeText(this, R.string.perfil_crear, Toast.LENGTH_LONG).show();
            }
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
            case MenuFragment.BOTONES:     return new BotonesFragment();
            case MenuFragment.PERFIL:
            default:                       return new PerfilFragment();
        }
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
