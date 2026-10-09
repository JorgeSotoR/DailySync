package co.edu.poli.dailysync.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Usuario;

/**
 * Perfil del usuario (RF-01).
 * - Sin perfil guardado: muestra el formulario.
 * - Con perfil guardado: muestra un resumen con el avatar (imagen o emoji), el nombre y la insignia "Perfil guardado".
 */
public class PerfilFragment extends Fragment {

    /** La actividad recibe el perfil guardado para actualizar el menú (RF-02). */
    public interface OnPerfilGuardadoListener {
        void onPerfilGuardado(Usuario usuario);
    }

    private static final String ESTADO_AVATAR = "avatar";
    private static final String ESTADO_FECHA = "fecha";
    private static final String ESTADO_EDITANDO = "editando";

    /** Emojis disponibles (Unicode 9 o anterior para que se vean en Android 8+). */
    private static final String[] EMOJIS = {
            "😀", "😎", "🤓", "😊", "😄", "😇",
            "🐶", "🐱", "🦊", "🐼", "🐨", "🦁",
            "🐸", "🌸", "🌻", "⭐", "🌙", "🔥",
            "⚽", "🏀", "🎮", "🎧", "📚", "🚀"
    };

    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LocalRepository repositorio;
    private OnPerfilGuardadoListener listener;

    // Resumen
    private View layoutResumen;
    private AvatarView avatarResumen;
    private TextView tvSaludo;
    private TextView tvFechaResumen;
    private TextView tvGeneroResumen;

    // Formulario
    private View layoutFormulario;
    private TextView tvTituloFormulario;
    private AvatarView avatarEdicion;
    private ChipGroup chipsAvatar;
    private TextInputEditText etApodo;
    private TextInputEditText etFecha;
    private RadioGroup rgGenero;
    private View btnCancelar;

    private String avatarSeleccionado = Usuario.AVATAR_1;
    private long fechaNacimiento = 0;
    private boolean editando;

    /** Selector de fotos del sistema (no requiere permisos de almacenamiento). */
    private final ActivityResultLauncher<PickVisualMediaRequest> selectorImagen =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onImagenElegida);

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnPerfilGuardadoListener) {
            listener = (OnPerfilGuardadoListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repositorio = new LocalRepository(requireContext());

        layoutResumen = view.findViewById(R.id.layout_resumen);
        avatarResumen = view.findViewById(R.id.avatar_resumen);
        tvSaludo = view.findViewById(R.id.tv_saludo);
        tvFechaResumen = view.findViewById(R.id.tv_fecha_resumen);
        tvGeneroResumen = view.findViewById(R.id.tv_genero_resumen);

        layoutFormulario = view.findViewById(R.id.layout_formulario);
        tvTituloFormulario = view.findViewById(R.id.tv_titulo_formulario);
        avatarEdicion = view.findViewById(R.id.avatar_edicion);
        chipsAvatar = view.findViewById(R.id.chips_avatar);
        etApodo = view.findViewById(R.id.et_apodo);
        etFecha = view.findViewById(R.id.et_fecha);
        rgGenero = view.findViewById(R.id.rg_genero);
        btnCancelar = view.findViewById(R.id.btn_cancelar);

        avatarResumen.setBorde(3);
        avatarEdicion.setBorde(3);

        view.findViewById(R.id.chip_avatar_1).setOnClickListener(v -> elegirAvatar(Usuario.AVATAR_1));
        view.findViewById(R.id.chip_avatar_2).setOnClickListener(v -> elegirAvatar(Usuario.AVATAR_2));
        view.findViewById(R.id.chip_emoji).setOnClickListener(v -> mostrarSelectorEmoji());
        view.findViewById(R.id.chip_galeria).setOnClickListener(v -> selectorImagen.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        etFecha.setOnClickListener(v -> abrirSelectorFecha());
        view.findViewById(R.id.btn_guardar).setOnClickListener(v -> guardarPerfil());
        view.findViewById(R.id.btn_editar).setOnClickListener(v -> mostrarFormulario(repositorio.obtenerUsuario()));
        btnCancelar.setOnClickListener(v -> mostrarResumen(repositorio.obtenerUsuario()));

        Usuario usuario = repositorio.obtenerUsuario();
        if (savedInstanceState != null && savedInstanceState.getBoolean(ESTADO_EDITANDO, false)) {
            // Al rotar en medio de una edición se conservan el avatar y la fecha elegidos.
            mostrarFormulario(usuario);
            avatarSeleccionado = savedInstanceState.getString(ESTADO_AVATAR, Usuario.AVATAR_1);
            fechaNacimiento = savedInstanceState.getLong(ESTADO_FECHA, 0);
            mostrarFecha();
            mostrarAvatarEdicion();
        } else if (usuario == null) {
            mostrarFormulario(null);
        } else {
            mostrarResumen(usuario);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ESTADO_EDITANDO, editando);
        outState.putString(ESTADO_AVATAR, avatarSeleccionado);
        outState.putLong(ESTADO_FECHA, fechaNacimiento);
    }

    // ---------- Modo resumen ----------

    private void mostrarResumen(@Nullable Usuario usuario) {
        if (usuario == null) {
            mostrarFormulario(null);
            return;
        }
        editando = false;
        layoutFormulario.setVisibility(View.GONE);
        layoutResumen.setVisibility(View.VISIBLE);

        avatarResumen.setAvatar(usuario.getAvatar());
        tvSaludo.setText(getString(R.string.perfil_saludo, usuario.getApodo()));
        LocalDate nacimiento = aFecha(usuario.getFechaNacimiento());
        int edad = Period.between(nacimiento, LocalDate.now()).getYears();
        tvFechaResumen.setText(getString(R.string.perfil_edad, nacimiento.format(formatoFecha), edad));
        tvGeneroResumen.setText(textoGenero(usuario.getGenero()));
    }

    private String textoGenero(String genero) {
        if (Usuario.GENERO_FEMENINO.equals(genero)) {
            return getString(R.string.genero_femenino);
        } else if (Usuario.GENERO_MASCULINO.equals(genero)) {
            return getString(R.string.genero_masculino);
        }
        return getString(R.string.genero_no_especificado);
    }

    // ---------- Modo edición ----------

    private void mostrarFormulario(@Nullable Usuario usuario) {
        editando = true;
        layoutResumen.setVisibility(View.GONE);
        layoutFormulario.setVisibility(View.VISIBLE);
        tvTituloFormulario.setText(usuario == null ? R.string.perfil_nuevo_titulo : R.string.perfil_editar_titulo);
        btnCancelar.setVisibility(usuario == null ? View.GONE : View.VISIBLE);

        if (usuario != null) {
            etApodo.setText(usuario.getApodo());
            fechaNacimiento = usuario.getFechaNacimiento();
            avatarSeleccionado = usuario.getAvatar() != null ? usuario.getAvatar() : Usuario.AVATAR_1;
            if (Usuario.GENERO_FEMENINO.equals(usuario.getGenero())) {
                rgGenero.check(R.id.rb_femenino);
            } else if (Usuario.GENERO_MASCULINO.equals(usuario.getGenero())) {
                rgGenero.check(R.id.rb_masculino);
            } else {
                rgGenero.check(R.id.rb_no_especificado);
            }
        }
        mostrarFecha();
        mostrarAvatarEdicion();
    }

    private void elegirAvatar(String avatar) {
        avatarSeleccionado = avatar;
        mostrarAvatarEdicion();
    }

    /** Cuadrícula de emojis en un diálogo. */
    private void mostrarSelectorEmoji() {
        GridLayout grilla = new GridLayout(requireContext());
        grilla.setColumnCount(6);
        int relleno = dp(12);
        grilla.setPadding(relleno, relleno, relleno, 0);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.perfil_elegir_emoji)
                .setView(grilla)
                .setNegativeButton(R.string.cancelar, null)
                .create();
        dialogo.setOnCancelListener(d -> mostrarAvatarEdicion());
        dialogo.setOnDismissListener(d -> mostrarAvatarEdicion());

        TypedValue fondoToque = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, fondoToque, true);
        for (String e : EMOJIS) {
            TextView celda = new TextView(requireContext());
            celda.setText(e);
            celda.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
            celda.setGravity(Gravity.CENTER);
            celda.setBackgroundResource(fondoToque.resourceId);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f));
            lp.width = 0;
            lp.height = dp(48);
            celda.setLayoutParams(lp);
            celda.setOnClickListener(v -> {
                avatarSeleccionado = Usuario.PREFIJO_EMOJI + e;
                dialogo.dismiss();
            });
            grilla.addView(celda);
        }
        dialogo.show();
    }

    private void onImagenElegida(@Nullable Uri uri) {
        if (uri != null) {
            try {
                avatarSeleccionado = repositorio.guardarImagen(uri);
            } catch (IOException | SecurityException e) {
                Toast.makeText(requireContext(), R.string.perfil_error_imagen, Toast.LENGTH_SHORT).show();
            }
        }
        mostrarAvatarEdicion();
    }

    /** Muestra el avatar elegido y marca el chip correspondiente. */
    private void mostrarAvatarEdicion() {
        if (Usuario.esEmoji(avatarSeleccionado)) {
            chipsAvatar.check(R.id.chip_emoji);
        } else if (esArchivo(avatarSeleccionado)) {
            chipsAvatar.check(R.id.chip_galeria);
        } else if (Usuario.AVATAR_2.equals(avatarSeleccionado)) {
            chipsAvatar.check(R.id.chip_avatar_2);
        } else {
            avatarSeleccionado = Usuario.AVATAR_1;
            chipsAvatar.check(R.id.chip_avatar_1);
        }
        avatarEdicion.setAvatar(avatarSeleccionado);
    }

    private static boolean esArchivo(String avatar) {
        return avatar != null && avatar.startsWith("/") && new File(avatar).exists();
    }

    private void abrirSelectorFecha() {
        Calendar cal = Calendar.getInstance();
        if (fechaNacimiento != 0) {
            cal.setTimeInMillis(fechaNacimiento);
        } else {
            cal.add(Calendar.YEAR, -20);
        }
        DatePickerDialog dialogo = new DatePickerDialog(requireContext(), (picker, anio, mes, dia) -> {
            fechaNacimiento = LocalDate.of(anio, mes + 1, dia)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            mostrarFecha();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialogo.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialogo.show();
    }

    private void mostrarFecha() {
        etFecha.setText(fechaNacimiento == 0 ? "" : aFecha(fechaNacimiento).format(formatoFecha));
    }

    private void guardarPerfil() {
        String apodo = etApodo.getText() == null ? "" : etApodo.getText().toString().trim();
        if (TextUtils.isEmpty(apodo)) {
            etApodo.setError(getString(R.string.perfil_error_apodo));
            return;
        }
        if (fechaNacimiento == 0) {
            Toast.makeText(requireContext(), R.string.perfil_error_fecha, Toast.LENGTH_SHORT).show();
            return;
        }
        int generoId = rgGenero.getCheckedRadioButtonId();
        if (generoId == -1) {
            Toast.makeText(requireContext(), R.string.perfil_error_genero, Toast.LENGTH_SHORT).show();
            return;
        }
        String genero;
        if (generoId == R.id.rb_femenino) {
            genero = Usuario.GENERO_FEMENINO;
        } else if (generoId == R.id.rb_masculino) {
            genero = Usuario.GENERO_MASCULINO;
        } else {
            genero = Usuario.GENERO_NO_ESPECIFICADO;
        }

        borrarAvatarAnteriorSiCambio();
        Usuario usuario = new Usuario(apodo, fechaNacimiento, genero, avatarSeleccionado);
        repositorio.guardarUsuario(usuario);
        mostrarResumen(usuario);
        if (listener != null) {
            listener.onPerfilGuardado(usuario);
        }
    }

    /** Elimina la foto de galería anterior para no acumular archivos. */
    private void borrarAvatarAnteriorSiCambio() {
        Usuario anterior = repositorio.obtenerUsuario();
        if (anterior != null && esArchivo(anterior.getAvatar())
                && !anterior.getAvatar().equals(avatarSeleccionado)) {
            //noinspection ResultOfMethodCallIgnored
            new File(anterior.getAvatar()).delete();
        }
    }

    private static LocalDate aFecha(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
