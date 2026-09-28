package co.edu.poli.dailysync.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.Usuario;

/** Perfil del usuario: apodo, fecha de nacimiento, género y avatar (RF-01). */
public class PerfilFragment extends Fragment {

    /** La actividad recibe el perfil guardado para actualizar el menú (RF-02). */
    public interface OnPerfilGuardadoListener {
        void onPerfilGuardado(Usuario usuario);
    }

    private static final String ESTADO_AVATAR = "avatar";
    private static final String ESTADO_FECHA = "fecha";

    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private LocalRepository repositorio;
    private OnPerfilGuardadoListener listener;

    private ShapeableImageView imgAvatar;
    private ChipGroup chipsAvatar;
    private TextInputEditText etApodo;
    private TextInputEditText etFecha;
    private RadioGroup rgGenero;

    private String avatarSeleccionado = Usuario.AVATAR_1;
    private long fechaNacimiento = 0;

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

        imgAvatar = view.findViewById(R.id.img_avatar);
        chipsAvatar = view.findViewById(R.id.chips_avatar);
        etApodo = view.findViewById(R.id.et_apodo);
        etFecha = view.findViewById(R.id.et_fecha);
        rgGenero = view.findViewById(R.id.rg_genero);

        if (savedInstanceState != null) {
            avatarSeleccionado = savedInstanceState.getString(ESTADO_AVATAR, Usuario.AVATAR_1);
            fechaNacimiento = savedInstanceState.getLong(ESTADO_FECHA, 0);
            mostrarFecha();
        } else {
            cargarUsuario();
        }
        mostrarAvatar();

        view.findViewById(R.id.chip_avatar_1).setOnClickListener(v -> elegirAvatar(Usuario.AVATAR_1));
        view.findViewById(R.id.chip_avatar_2).setOnClickListener(v -> elegirAvatar(Usuario.AVATAR_2));
        view.findViewById(R.id.chip_galeria).setOnClickListener(v -> selectorImagen.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        etFecha.setOnClickListener(v -> abrirSelectorFecha());
        view.findViewById(R.id.btn_guardar).setOnClickListener(v -> guardarPerfil());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_AVATAR, avatarSeleccionado);
        outState.putLong(ESTADO_FECHA, fechaNacimiento);
    }

    private void cargarUsuario() {
        Usuario usuario = repositorio.obtenerUsuario();
        if (usuario == null) {
            return;
        }
        etApodo.setText(usuario.getApodo());
        fechaNacimiento = usuario.getFechaNacimiento();
        mostrarFecha();
        if (usuario.getAvatar() != null) {
            avatarSeleccionado = usuario.getAvatar();
        }
        if (Usuario.GENERO_FEMENINO.equals(usuario.getGenero())) {
            rgGenero.check(R.id.rb_femenino);
        } else if (Usuario.GENERO_MASCULINO.equals(usuario.getGenero())) {
            rgGenero.check(R.id.rb_masculino);
        } else {
            rgGenero.check(R.id.rb_no_especificado);
        }
    }

    private void elegirAvatar(String avatar) {
        avatarSeleccionado = avatar;
        mostrarAvatar();
    }

    private void onImagenElegida(@Nullable Uri uri) {
        if (uri == null) {          // el usuario cerró el selector sin elegir
            mostrarAvatar();
            return;
        }
        try {
            avatarSeleccionado = repositorio.guardarImagen(uri);
        } catch (IOException | SecurityException e) {
            Toast.makeText(requireContext(), R.string.perfil_error_imagen, Toast.LENGTH_SHORT).show();
        }
        mostrarAvatar();
    }

    /** Muestra la imagen del avatar y marca el chip correspondiente. */
    private void mostrarAvatar() {
        if (Usuario.AVATAR_2.equals(avatarSeleccionado)) {
            imgAvatar.setImageResource(R.drawable.avatar_2);
            chipsAvatar.check(R.id.chip_avatar_2);
        } else if (esArchivo(avatarSeleccionado)) {
            imgAvatar.setImageURI(null);   // obliga a recargar si la ruta cambió
            imgAvatar.setImageURI(Uri.fromFile(new File(avatarSeleccionado)));
            chipsAvatar.check(R.id.chip_galeria);
        } else {
            avatarSeleccionado = Usuario.AVATAR_1;
            imgAvatar.setImageResource(R.drawable.avatar_1);
            chipsAvatar.check(R.id.chip_avatar_1);
        }
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
            Calendar elegida = Calendar.getInstance();
            elegida.clear();
            elegida.set(anio, mes, dia);
            fechaNacimiento = elegida.getTimeInMillis();
            mostrarFecha();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialogo.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialogo.show();
    }

    private void mostrarFecha() {
        if (fechaNacimiento != 0) {
            etFecha.setText(formatoFecha.format(new Date(fechaNacimiento)));
        }
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
}
