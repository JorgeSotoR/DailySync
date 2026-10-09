package co.edu.poli.dailysync.ui;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.alarmas.AlarmScheduler;
import co.edu.poli.dailysync.alarmas.NotificacionAlarma;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Alarma;

/**
 * Alarmas (RF-05): crear, editar, repetir por días, activar/desactivar y borrar.
 * Cada cambio se programa en el sistema con AlarmScheduler y suena con una notificación.
 */
public class AlarmasFragment extends Fragment implements AlarmaAdapter.Acciones {

    /** Chips de días en orden: lunes (1) … domingo (7). */
    private static final int[] CHIPS_DIAS = {
            R.id.chip_dia_l, R.id.chip_dia_m, R.id.chip_dia_x, R.id.chip_dia_j,
            R.id.chip_dia_v, R.id.chip_dia_s, R.id.chip_dia_d
    };

    private LocalRepository repositorio;
    private final List<Alarma> alarmas = new ArrayList<>();
    private AlarmaAdapter adapter;
    private TextView tvVacio;
    private TextView tvAviso;
    private TextView tvAyuda;

    /** Permiso de notificaciones (Android 13+). */
    private final ActivityResultLauncher<String> permisoNotificaciones =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> actualizarAviso());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alarmas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repositorio = new LocalRepository(requireContext());
        NotificacionAlarma.crearCanal(requireContext());

        tvVacio = view.findViewById(R.id.tv_vacio);
        tvVacio.setText(R.string.alarmas_vacio);
        tvAviso = view.findViewById(R.id.tv_aviso);
        tvAyuda = view.findViewById(R.id.tv_ayuda);
        tvAviso.setOnClickListener(v -> abrirAjustes());

        RecyclerView rv = view.findViewById(R.id.rv_alarmas);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AlarmaAdapter(alarmas, this);
        rv.setAdapter(adapter);

        view.findViewById(R.id.fab_agregar).setOnClickListener(v -> mostrarDialogo(null));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Se recarga al volver: una alarma de "una vez" pudo desactivarse al sonar.
        alarmas.clear();
        alarmas.addAll(repositorio.obtenerAlarmas());
        ordenar();
        adapter.notifyDataSetChanged();
        actualizarVacio();
        actualizarAviso();
    }

    // ---------- Crear / editar ----------

    private void mostrarDialogo(@Nullable Alarma existente) {
        View vista = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_alarma, null);
        Button btnHora = vista.findViewById(R.id.btn_hora);
        TextInputEditText etEtiqueta = vista.findViewById(R.id.et_etiqueta);
        ChipGroup chipsDias = vista.findViewById(R.id.chips_dias);

        // hora[0] = hora, hora[1] = minuto
        int[] hora = new int[2];
        if (existente != null) {
            hora[0] = existente.getHora();
            hora[1] = existente.getMinuto();
            etEtiqueta.setText(existente.getEtiqueta());
            for (int dia : existente.getDias()) {
                chipsDias.check(CHIPS_DIAS[dia - 1]);
            }
        } else {
            Calendar ahora = Calendar.getInstance();
            ahora.add(Calendar.HOUR_OF_DAY, 1);
            hora[0] = ahora.get(Calendar.HOUR_OF_DAY);
            hora[1] = 0;
        }
        btnHora.setText(Alarma.textoHora(hora[0], hora[1]));
        btnHora.setOnClickListener(v -> new TimePickerDialog(requireContext(), (picker, h, m) -> {
            hora[0] = h;
            hora[1] = m;
            btnHora.setText(Alarma.textoHora(h, m));
        }, hora[0], hora[1], false).show());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existente == null ? R.string.alarma_nueva : R.string.alarma_editar)
                .setView(vista)
                .setPositiveButton(R.string.alarma_guardar, (d, w) -> {
                    String etiqueta = etEtiqueta.getText() == null ? "" : etEtiqueta.getText().toString().trim();
                    if (TextUtils.isEmpty(etiqueta)) {
                        etiqueta = getString(R.string.alarma_por_defecto);
                    }
                    List<Integer> dias = new ArrayList<>();
                    for (int i = 0; i < CHIPS_DIAS.length; i++) {
                        if (chipsDias.getCheckedChipIds().contains(CHIPS_DIAS[i])) {
                            dias.add(i + 1);
                        }
                    }
                    guardar(existente, hora[0], hora[1], etiqueta, dias);
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void guardar(@Nullable Alarma existente, int hora, int minuto, String etiqueta, List<Integer> dias) {
        Alarma alarma;
        if (existente == null) {
            alarma = new Alarma(System.currentTimeMillis(), hora, minuto, etiqueta, dias);
            alarmas.add(alarma);
        } else {
            alarma = existente;
            alarma.actualizar(hora, minuto, etiqueta, dias);
            alarma.setActiva(true);
        }
        ordenar();
        repositorio.guardarAlarmas(alarmas);
        adapter.notifyDataSetChanged();
        actualizarVacio();
        programarYAvisar(alarma);
        pedirPermisoSiHaceFalta();
    }

    // ---------- Acciones de la lista ----------

    @Override
    public void onActivaCambiada(Alarma alarma, boolean activa) {
        alarma.setActiva(activa);
        repositorio.guardarAlarmas(alarmas);
        if (activa) {
            programarYAvisar(alarma);
            pedirPermisoSiHaceFalta();
        } else {
            AlarmScheduler.cancelar(requireContext(), alarma);
            Toast.makeText(requireContext(), R.string.alarma_desactivada, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEditar(Alarma alarma) {
        mostrarDialogo(alarma);
    }

    @Override
    public void onEliminar(Alarma alarma) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.eliminar_pregunta, alarma.getHoraTexto() + " · " + alarma.getEtiqueta()))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    AlarmScheduler.cancelar(requireContext(), alarma);
                    alarmas.remove(alarma);
                    repositorio.guardarAlarmas(alarmas);
                    adapter.notifyDataSetChanged();
                    actualizarVacio();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    // ---------- Programación y permisos ----------

    private void programarYAvisar(Alarma alarma) {
        long cuando = AlarmScheduler.programar(requireContext(), alarma);
        String falta = textoTiempoRestante(cuando - System.currentTimeMillis());
        Toast.makeText(requireContext(), getString(R.string.alarma_sonara, falta), Toast.LENGTH_SHORT).show();
    }

    /** Por ejemplo "2 h 5 min" o "1 d 3 h". */
    static String textoTiempoRestante(long millis) {
        long minutos = Math.max(1, (millis + 59_999) / 60_000);
        long dias = minutos / (24 * 60);
        long horas = (minutos % (24 * 60)) / 60;
        long mins = minutos % 60;
        StringBuilder sb = new StringBuilder();
        if (dias > 0) sb.append(dias).append(" d ");
        if (horas > 0) sb.append(horas).append(" h ");
        if (mins > 0 && dias == 0) sb.append(mins).append(" min");
        return sb.toString().trim();
    }

    private void pedirPermisoSiHaceFalta() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !NotificacionAlarma.tienePermiso(requireContext())) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void actualizarAviso() {
        if (!isAdded()) {
            return;
        }
        boolean hayAlarmas = !alarmas.isEmpty();
        if (hayAlarmas && !NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
            tvAviso.setText(R.string.alarmas_sin_notificaciones);
            tvAviso.setVisibility(View.VISIBLE);
        } else if (hayAlarmas && !NotificacionAlarma.puedePantallaCompleta(requireContext())) {
            tvAviso.setText(R.string.alarmas_sin_pantalla_completa);
            tvAviso.setVisibility(View.VISIBLE);
        } else if (hayAlarmas && !AlarmScheduler.puedeProgramarExactas(requireContext())) {
            tvAviso.setText(R.string.alarmas_sin_exactas);
            tvAviso.setVisibility(View.VISIBLE);
        } else {
            tvAviso.setVisibility(View.GONE);
        }
    }

    /** Abre los ajustes del sistema que corresponden al aviso visible. */
    private void abrirAjustes() {
        Intent intent;
        if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && !NotificacionAlarma.puedePantallaCompleta(requireContext())) {
            intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:" + requireContext().getPackageName()));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + requireContext().getPackageName()));
        } else {
            return;
        }
        startActivity(intent);
    }

    private void ordenar() {
        alarmas.sort(Comparator.comparingInt(Alarma::getMinutosDelDia));
    }

    private void actualizarVacio() {
        tvVacio.setVisibility(alarmas.isEmpty() ? View.VISIBLE : View.GONE);
        tvAyuda.setVisibility(alarmas.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
