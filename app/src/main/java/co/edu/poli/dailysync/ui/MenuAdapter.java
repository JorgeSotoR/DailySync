package co.edu.poli.dailysync.ui;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import co.edu.poli.dailysync.R;

/** Adaptador de las opciones del menú izquierdo. */
public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.Holder> {

    public interface OnOpcionClick {
        void onClick(String opcion);
    }

    private final List<String> opciones;
    private final OnOpcionClick onClick;
    private String seleccionada;

    public MenuAdapter(List<String> opciones, String seleccionada, OnOpcionClick onClick) {
        this.opciones = opciones;
        this.seleccionada = seleccionada;
        this.onClick = onClick;
    }

    public void setSeleccionada(String opcion) {
        seleccionada = opcion;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        String opcion = opciones.get(position);
        boolean activa = opcion.equals(seleccionada);

        h.nombre.setText(opcion);
        h.icono.setText(letra(opcion));
        h.itemView.setBackgroundResource(activa ? R.drawable.bg_menu_seleccionado : 0);
        h.icono.setBackgroundResource(activa ? R.drawable.bg_menu_icono_seleccionado : R.drawable.bg_menu_icono);
        h.nombre.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                activa ? R.color.texto : R.color.texto_menu));
        h.nombre.setTypeface(null, activa ? Typeface.BOLD : Typeface.NORMAL);
        h.itemView.setOnClickListener(v -> onClick.onClick(opcion));
    }

    @Override
    public int getItemCount() {
        return opciones.size();
    }

    /** Letra del ícono. Alarmas usa "R" porque la "A" ya es de Actividades. */
    private static String letra(String opcion) {
        return MenuFragment.ALARMAS.equals(opcion) ? "R" : opcion.substring(0, 1);
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView icono;
        final TextView nombre;

        Holder(@NonNull View v) {
            super(v);
            icono = v.findViewById(R.id.tv_icono);
            nombre = v.findViewById(R.id.tv_nombre);
        }
    }
}
