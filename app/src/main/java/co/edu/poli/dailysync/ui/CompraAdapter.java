package co.edu.poli.dailysync.ui;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.model.ItemCompra;

public class CompraAdapter extends RecyclerView.Adapter<CompraAdapter.Holder> {

    public interface Acciones {
        void onCompradoCambiado(ItemCompra item, boolean comprado);
        void onEliminar(ItemCompra item);
    }

    private final List<ItemCompra> items;
    private final Acciones acciones;

    public CompraAdapter(List<ItemCompra> items, Acciones acciones) {
        this.items = items;
        this.acciones = acciones;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_compra, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        ItemCompra item = items.get(position);
        h.nombre.setText(item.getNombre());
        tachar(h.nombre, item.isComprado());
        h.check.setOnCheckedChangeListener(null);
        h.check.setChecked(item.isComprado());
        h.check.setOnCheckedChangeListener((b, marcado) -> {
            tachar(h.nombre, marcado);
            acciones.onCompradoCambiado(item, marcado);
        });
        h.itemView.setOnLongClickListener(v -> {
            acciones.onEliminar(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static void tachar(TextView tv, boolean tachado) {
        int flags = tv.getPaintFlags();
        tv.setPaintFlags(tachado ? flags | Paint.STRIKE_THRU_TEXT_FLAG : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        tv.setAlpha(tachado ? 0.6f : 1f);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final CheckBox check;
        final TextView nombre;

        Holder(@NonNull View v) {
            super(v);
            check = v.findViewById(R.id.cb_comprado);
            nombre = v.findViewById(R.id.tv_nombre);
        }
    }
}
