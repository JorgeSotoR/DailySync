package co.edu.poli.dailysync.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Actividad;
import co.edu.poli.dailysync.model.TipoActividad;

/** Tareas, rutinas y eventos agrupados por día, con checklist y filtro por tipo (RF-04). */
public class ActividadesFragment extends Fragment implements ActividadAdapter.Acciones {

    private static final Locale ES = new Locale("es", "CO");
    private final SimpleDateFormat formatoDia = new SimpleDateFormat("EEEE d 'de' MMMM", ES);
    private final SimpleDateFormat formatoBoton = new SimpleDateFormat("dd/MM/yyyy", ES);

    private LocalRepository repositorio;
    private List<Actividad> actividades;
    private ActividadAdapter adapter;
    private TextView tvVacio;
    private TipoActividad filtro = null;   // null = todas

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_actividades, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repositorio = new LocalRepository(requireContext());
        actividades = repositorio.obtenerActividades();

        tvVacio = view.findViewById(R.id.tv_vacio);
        tvVacio.setText(R.string.actividades_vacio);

        RecyclerView rv = view.findViewById(R.id.rv_actividades);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ActividadAdapter(this);
        rv.setAdapter(adapter);

        ChipGroup chips = view.findViewById(R.id.chips_filtro);
        chips.setOnCheckedStateChangeListener((grupo, ids) -> {
            int id = ids.isEmpty() ? R.id.chip_todas : ids.get(0);
            if (id == R.id.chip_tareas) {
                filtro = TipoActividad.TAREA;
            } else if (id == R.id.chip_rutinas) {
                filtro = TipoActividad.RUTINA;
            } else if (id == R.id.chip_eventos) {
                filtro = TipoActividad.EVENTO;
            } else {
                filtro = null;
            }
            refrescar();
        });

        view.findViewById(R.id.fab_agregar).setOnClickListener(v -> mostrarDialogoNueva());
        refrescar();
    }

    /** Construye las filas: filtra, ordena por fecha e inserta un encabezado por cada día. */
    private void refrescar() {
        List<Actividad> visibles = new ArrayList<>();
        for (Actividad a : actividades) {
            if (filtro == null || a.getTipo() == filtro) {
                visibles.add(a);
            }
        }
        visibles.sort(Comparator.comparingLong(Actividad::getFecha));

        List<ActividadAdapter.Fila> filas = new ArrayList<>();
        String diaAnterior = null;
        for (Actividad a : visibles) {
            String dia = capitalizar(formatoDia.format(new Date(a.getFecha())));
            if (!dia.equals(diaAnterior)) {
                filas.add(ActividadAdapter.Fila.encabezado(dia));
                diaAnterior = dia;
            }
            filas.add(ActividadAdapter.Fila.de(a));
        }
        adapter.setFilas(filas);
        tvVacio.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void mostrarDialogoNueva() {
        View dialogo = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_actividad, null);
        TextInputEditText etTitulo = dialogo.findViewById(R.id.et_titulo);
        Spinner spTipo = dialogo.findViewById(R.id.sp_tipo);
        Button btnFecha = dialogo.findViewById(R.id.btn_fecha);

        List<String> tipos = new ArrayList<>();
        for (TipoActividad t : TipoActividad.values()) {
            tipos.add(t.getEtiqueta());
        }
        ArrayAdapter<String> adaptadorTipos = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, tipos);
        adaptadorTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipo.setAdapter(adaptadorTipos);
        if (filtro != null) {
            spTipo.setSelection(filtro.ordinal());
        }

        Calendar fecha = Calendar.getInstance();
        btnFecha.setText(formatoBoton.format(fecha.getTime()));
        btnFecha.setOnClickListener(v -> new DatePickerDialog(requireContext(), (p, anio, mes, dia) -> {
            fecha.set(anio, mes, dia);
            btnFecha.setText(formatoBoton.format(fecha.getTime()));
        }, fecha.get(Calendar.YEAR), fecha.get(Calendar.MONTH), fecha.get(Calendar.DAY_OF_MONTH)).show());

        AlertDialog alerta = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.actividad_nueva)
                .setView(dialogo)
                .setPositiveButton(R.string.agregar, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();
        // Se asigna el clic después de mostrar el diálogo para poder validar sin cerrarlo.
        alerta.setOnShowListener(d -> alerta.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String titulo = etTitulo.getText() == null ? "" : etTitulo.getText().toString().trim();
            if (TextUtils.isEmpty(titulo)) {
                etTitulo.setError(getString(R.string.error_titulo));
                return;
            }
            TipoActividad tipo = TipoActividad.values()[spTipo.getSelectedItemPosition()];
            actividades.add(new Actividad(System.currentTimeMillis(), titulo, fecha.getTimeInMillis(), tipo));
            repositorio.guardarActividades(actividades);
            refrescar();
            alerta.dismiss();
        }));
        alerta.show();
    }

    @Override
    public void onCompletadaCambiada(Actividad actividad, boolean completada) {
        actividad.marcarCompletada(completada);
        repositorio.guardarActividades(actividades);
    }

    @Override
    public void onEliminar(Actividad actividad) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.eliminar_pregunta, actividad.getTitulo()))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    actividades.remove(actividad);
                    repositorio.guardarActividades(actividades);
                    refrescar();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private static String capitalizar(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(ES) + texto.substring(1);
    }
}
