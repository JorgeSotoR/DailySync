package co.edu.poli.dailysync.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.LocalRepository;
import co.edu.poli.dailysync.model.ItemCompra;

/**
 * Lista de compras por texto (RF-06).
 * Pendiente para la siguiente entrega: fotos con cámara/galería y contenido compartido (ACTION_SEND).
 */
public class ComprasFragment extends Fragment implements CompraAdapter.Acciones {

    private LocalRepository repositorio;
    private List<ItemCompra> items;
    private CompraAdapter adapter;
    private TextInputEditText etProducto;
    private TextView tvVacio;

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

        RecyclerView rv = view.findViewById(R.id.rv_compras);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CompraAdapter(items, this);
        rv.setAdapter(adapter);

        view.findViewById(R.id.btn_agregar).setOnClickListener(v -> agregarItem());
        etProducto.setOnEditorActionListener((v, accion, evento) -> {
            if (accion == EditorInfo.IME_ACTION_DONE) {
                agregarItem();
                return true;
            }
            return false;
        });
        actualizarVacio();
    }

    private void agregarItem() {
        String nombre = etProducto.getText() == null ? "" : etProducto.getText().toString().trim();
        if (TextUtils.isEmpty(nombre)) {
            return;
        }
        items.add(new ItemCompra(System.currentTimeMillis(), nombre));
        repositorio.guardarCompras(items);
        adapter.notifyItemInserted(items.size() - 1);
        etProducto.setText("");
        actualizarVacio();
    }

    @Override
    public void onCompradoCambiado(ItemCompra item, boolean comprado) {
        item.setComprado(comprado);
        repositorio.guardarCompras(items);
    }

    @Override
    public void onEliminar(ItemCompra item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.eliminar_pregunta, item.getNombre()))
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    items.remove(item);
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
