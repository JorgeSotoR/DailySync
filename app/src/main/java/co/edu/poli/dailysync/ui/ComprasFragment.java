package co.edu.poli.dailysync.ui;

import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.IOException;
import java.util.List;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.Imagenes;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.ItemCompra;

/**
 * Lista de compras multimodal (RF-06): productos por texto, con foto de la cámara o de la galería,
 * o recibidos desde otra app con "Compartir" (ACTION_SEND, lo procesa MainActivity).
 */
public class ComprasFragment extends Fragment implements CompraAdapter.Acciones {

    private static final String ARG_TEXTO = "texto";
    private static final String ARG_FOTO = "foto";
    private static final String ESTADO_FOTO = "foto_pendiente";
    private static final int LADO_MAXIMO_FOTO = 1024;

    private LocalRepository repositorio;
    private List<ItemCompra> items;
    private CompraAdapter adapter;
    private TextInputEditText etProducto;
    private TextView tvVacio;
    private View llPendiente;
    private ImageView imgPendiente;

    /** Foto tomada o elegida que todavía no se ha agregado a un producto. */
    private String fotoPendiente;

    private final ActivityResultLauncher<Uri> tomarFoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), this::onFotoTomada);

    private final ActivityResultLauncher<PickVisualMediaRequest> elegirFoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onFotoElegida);

    /** Abre Compras con el contenido que otra app compartió (texto y/o foto ya copiada). */
    public static ComprasFragment conContenidoCompartido(@Nullable String texto, @Nullable String rutaFoto) {
        ComprasFragment fragmento = new ComprasFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEXTO, texto);
        args.putString(ARG_FOTO, rutaFoto);
        fragmento.setArguments(args);
        return fragmento;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_compras, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repositorio = new LocalRepository(requireContext());
        items = repositorio.obtenerCompras();

        tvVacio = view.findViewById(R.id.tv_vacio);
        tvVacio.setText(R.string.compras_vacio);
        etProducto = view.findViewById(R.id.et_producto);
        llPendiente = view.findViewById(R.id.ll_pendiente);
        imgPendiente = view.findViewById(R.id.img_pendiente);

        RecyclerView rv = view.findViewById(R.id.rv_compras);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CompraAdapter(items, this);
        rv.setAdapter(adapter);

        view.findViewById(R.id.btn_agregar).setOnClickListener(v -> agregarItem());
        view.findViewById(R.id.btn_camara).setOnClickListener(v -> abrirCamara());
        view.findViewById(R.id.btn_galeria).setOnClickListener(v -> elegirFoto.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        view.findViewById(R.id.btn_quitar_foto).setOnClickListener(v -> {
            repositorio.eliminarImagen(fotoPendiente);
            mostrarPendiente(null);
        });
        etProducto.setOnEditorActionListener((v, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_DONE) {
                agregarItem();
                return true;
            }
            return false;
        });

        if (savedInstanceState != null) {
            mostrarPendiente(savedInstanceState.getString(ESTADO_FOTO));
        } else if (getArguments() != null) {
            // Contenido compartido desde otra app
            String texto = getArguments().getString(ARG_TEXTO);
            if (texto != null) {
                etProducto.setText(texto);
            }
            mostrarPendiente(getArguments().getString(ARG_FOTO));
            Toast.makeText(requireContext(), R.string.compras_recibido, Toast.LENGTH_LONG).show();
        }
        actualizarVacio();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ESTADO_FOTO, fotoPendiente);
    }

    // ---------- Fotos ----------

    /** Archivo temporal donde la app de cámara guarda la foto (compartido con FileProvider). */
    private File archivoCamara() {
        File carpeta = new File(requireContext().getCacheDir(), "camara");
        //noinspection ResultOfMethodCallIgnored
        carpeta.mkdirs();
        return new File(carpeta, "captura.jpg");
    }

    private void abrirCamara() {
        Uri destino = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", archivoCamara());
        try {
            tomarFoto.launch(destino);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.compras_sin_camara, Toast.LENGTH_SHORT).show();
        }
    }

    private void onFotoTomada(Boolean guardada) {
        File temporal = archivoCamara();
        if (Boolean.TRUE.equals(guardada) && temporal.length() > 0) {
            guardarComoPendiente(Uri.fromFile(temporal));
        }
        //noinspection ResultOfMethodCallIgnored
        temporal.delete();
    }

    private void onFotoElegida(@Nullable Uri uri) {
        if (uri != null) {
            guardarComoPendiente(uri);
        }
    }

    private void guardarComoPendiente(Uri uri) {
        try {
            String ruta = repositorio.guardarImagen(uri, "compra", LADO_MAXIMO_FOTO);
            repositorio.eliminarImagen(fotoPendiente);   // reemplaza una foto pendiente anterior
            mostrarPendiente(ruta);
        } catch (IOException | SecurityException e) {
            Toast.makeText(requireContext(), R.string.compras_error_foto, Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarPendiente(@Nullable String ruta) {
        fotoPendiente = (ruta != null && new File(ruta).exists()) ? ruta : null;
        if (fotoPendiente == null) {
            llPendiente.setVisibility(View.GONE);
            imgPendiente.setImageDrawable(null);
        } else {
            llPendiente.setVisibility(View.VISIBLE);
            imgPendiente.setImageBitmap(Imagenes.miniatura(fotoPendiente));
        }
    }

    // ---------- Lista ----------

    private void agregarItem() {
        String nombre = etProducto.getText() == null ? "" : etProducto.getText().toString().trim();
        if (TextUtils.isEmpty(nombre)) {
            if (fotoPendiente == null) {
                return;
            }
            nombre = getString(R.string.compras_sin_nombre);
        }
        items.add(new ItemCompra(System.currentTimeMillis(), nombre, fotoPendiente));
        repositorio.guardarCompras(items);
        adapter.notifyItemInserted(items.size() - 1);
        etProducto.setText("");
        mostrarPendiente(null);   // la foto ya quedó asociada al producto
        actualizarVacio();
    }

    @Override
    public void onCompradoCambiado(ItemCompra item, boolean comprado) {
        item.setComprado(comprado);
        repositorio.guardarCompras(items);
    }

    @Override
    public void onVerFoto(ItemCompra item) {
        ImageView imagen = new ImageView(requireContext());
        imagen.setAdjustViewBounds(true);
        imagen.setImageBitmap(Imagenes.decodificar(item.getFotoUri(), 1080));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.getNombre())
                .setView(imagen)
                .setPositiveButton(R.string.cerrar, null)
                .show();
    }

    @Override
    public void onEliminar(ItemCompra item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.eliminar_pregunta, item.getNombre()))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    items.remove(item);
                    repositorio.eliminarImagen(item.getFotoUri());
                    repositorio.guardarCompras(items);
                    adapter.notifyDataSetChanged();
                    actualizarVacio();
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void actualizarVacio() {
        tvVacio.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
