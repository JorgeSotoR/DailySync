package co.edu.poli.dailysync.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import co.edu.poli.dailysync.OnMenuItemSelectedListener;
import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Usuario;

/** Fragmento izquierdo: lista de opciones de navegación. */
public class MenuFragment extends Fragment {

    public static final String PERFIL = "Perfil";
    public static final String ACTIVIDADES = "Actividades";
    public static final String ALARMAS = "Alarmas";
    public static final String COMPRAS = "Compras";
    public static final String SALUD = "Salud";
    public static final String MULTIMEDIA = "Multimedia";
    public static final String WEB = "Web";
    public static final String BOTONES = "Botones";

    private static final String[] TODAS = {PERFIL, ACTIVIDADES, ALARMAS, COMPRAS, SALUD, MULTIMEDIA, WEB, BOTONES};
    private static final String ESTADO_SELECCION = "seleccion";

    private final List<String> opciones = new ArrayList<>();
    private OnMenuItemSelectedListener listener;
    private MenuAdapter adapter;
    private String seleccionada = PERFIL;
    private boolean mostrarSalud;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnMenuItemSelectedListener) {
            listener = (OnMenuItemSelectedListener) context;
        } else {
            throw new IllegalStateException("La actividad debe implementar OnMenuItemSelectedListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            seleccionada = savedInstanceState.getString(ESTADO_SELECCION, PERFIL);
        }
        Usuario usuario = new LocalRepository(requireContext()).obtenerUsuario();
        mostrarSalud = usuario != null && usuario.esFemenino();
        construirOpciones();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView rv = view.findViewById(R.id.rv_menu);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MenuAdapter(opciones, seleccionada, opcion -> {
            seleccionada = opcion;
            adapter.setSeleccionada(opcion);
            listener.onMenuItemSelected(opcion);
        });
        rv.setAdapter(adapter);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_SELECCION, seleccionada);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /** Muestra u oculta la opción Salud según el género del perfil (RF-02). */
    public void actualizarOpciones(boolean mostrarSalud) {
        this.mostrarSalud = mostrarSalud;
        construirOpciones();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /** Resalta una opción sin disparar el evento de navegación. */
    public void marcarSeleccion(String opcion) {
        seleccionada = opcion;
        if (adapter != null) {
            adapter.setSeleccionada(opcion);
        }
    }

    private void construirOpciones() {
        opciones.clear();
        for (String opcion : TODAS) {
            if (!SALUD.equals(opcion) || mostrarSalud) {
                opciones.add(opcion);
            }
        }
    }
}
