package co.edu.poli.dailysync.ui;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.data.Imagenes;
import co.edu.poli.dailysync.model.ItemCompra;

public class CompraAdapter extends RecyclerView.Adapter<CompraAdapter.Holder> {

    public interface Acciones {
        void onCompradoCambiado(ItemCompra item, boolean comprado);
        void onVerFoto(ItemCompra item);
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

        if (item.tieneFoto()) {
            h.foto.setVisibility(View.VISIBLE);
            h.foto.setImageBitmap(Imagenes.miniatura(item.getFotoUri()));
            h.foto.setOnClickListener(v -> acciones.onVerFoto(item));
        } else {
            h.foto.setVisibility(View.GONE);
            h.foto.setImageDrawable(null);
            h.foto.setOnClickListener(null);
        }

        h.check.setOnCheckedChangeListener(null);   // evita eventos al reciclar la vista
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
        final ImageView foto;
        final TextView nombre;

        Holder(@NonNull View v) {
            super(v);
            check = v.findViewById(R.id.cb_comprado);
            foto = v.findViewById(R.id.img_foto);
            nombre = v.findViewById(R.id.tv_nombre);
        }
    }
}
