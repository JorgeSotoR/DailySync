package co.edu.poli.dailysync.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Alarma;

/**
 * Lista de alarmas con interruptor (RF-05).
 * Pendiente para la siguiente entrega: programarlas con AlarmManager y mostrar la notificación.
 */
public class AlarmasFragment extends Fragment implements AlarmaAdapter.Acciones {

    private LocalRepository repositorio;
    private List<Alarma> alarmas;
    private AlarmaAdapter adapter;
    private TextView tvVacio;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alarmas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repositorio = new LocalRepository(requireContext());
        alarmas = repositorio.obtenerAlarmas();
        ordenar();

        tvVacio = view.findViewById(R.id.tv_vacio);
        tvVacio.setText(R.string.alarmas_vacio);

        RecyclerView rv = view.findViewById(R.id.rv_alarmas);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AlarmaAdapter(alarmas, this);
        rv.setAdapter(adapter);

        view.findViewById(R.id.fab_agregar).setOnClickListener(v -> elegirHora());
        actualizarVacio();
    }

    private void elegirHora() {
        Calendar ahora = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (picker, hora, minuto) -> pedirEtiqueta(hora, minuto),
                ahora.get(Calendar.HOUR_OF_DAY), ahora.get(Calendar.MINUTE), false).show();
    }

    private void pedirEtiqueta(int hora, int minuto) {
        EditText etEtiqueta = new EditText(requireContext());
        etEtiqueta.setHint(R.string.alarma_etiqueta);
        etEtiqueta.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        FrameLayout contenedor = new FrameLayout(requireContext());
        int margen = (int) (20 * getResources().getDisplayMetrics().density);
        contenedor.setPadding(margen, margen / 2, margen, 0);
        contenedor.addView(etEtiqueta);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(new Alarma(0, hora, minuto, "").getHoraTexto())
                .setView(contenedor)
                .setPositiveButton(R.string.agregar, (d, w) -> {
                    String etiqueta = etEtiqueta.getText().toString().trim();
                    if (TextUtils.isEmpty(etiqueta)) {
                        etiqueta = getString(R.string.alarma_por_defecto);
                    }
                    alarmas.add(new Alarma(System.currentTimeMillis(), hora, minuto, etiqueta));
                    ordenar();
                    repositorio.guardarAlarmas(alarmas);
                    adapter.notifyDataSetChanged();
                    actualizarVacio();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    @Override
    public void onActivaCambiada(Alarma alarma, boolean activa) {
        alarma.setActiva(activa);
        repositorio.guardarAlarmas(alarmas);
    }

    @Override
    public void onEliminar(Alarma alarma) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.eliminar_pregunta, alarma.getHoraTexto()))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    alarmas.remove(alarma);
                    repositorio.guardarAlarmas(alarmas);
                    adapter.notifyDataSetChanged();
                    actualizarVacio();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void ordenar() {
        alarmas.sort(Comparator.comparingInt(Alarma::getMinutosDelDia));
    }

    private void actualizarVacio() {
        tvVacio.setVisibility(alarmas.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
