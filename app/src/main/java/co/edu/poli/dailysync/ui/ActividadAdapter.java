package co.edu.poli.dailysync.ui;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.model.Actividad;

/** Muestra las actividades agrupadas por día: filas de encabezado (fecha) y filas de actividad. */
public class ActividadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Acciones {
        void onCompletadaCambiada(Actividad actividad, boolean completada);
        void onEliminar(Actividad actividad);
    }

    /** Una fila es un encabezado (texto) o una actividad. */
    public static class Fila {
        final String encabezado;
        final Actividad actividad;

        private Fila(String encabezado, Actividad actividad) {
            this.encabezado = encabezado;
            this.actividad = actividad;
        }

        public static Fila encabezado(String texto) { return new Fila(texto, null); }
        public static Fila de(Actividad actividad) { return new Fila(null, actividad); }
    }

    private static final int TIPO_ENCABEZADO = 0;
    private static final int TIPO_ACTIVIDAD = 1;

    private final List<Fila> filas = new ArrayList<>();
    private final Acciones acciones;

    public ActividadAdapter(Acciones acciones) {
        this.acciones = acciones;
    }

    public void setFilas(List<Fila> nuevas) {
        filas.clear();
        filas.addAll(nuevas);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return filas.get(position).actividad == null ? TIPO_ENCABEZADO : TIPO_ACTIVIDAD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_ENCABEZADO) {
            return new EncabezadoHolder(inf.inflate(R.layout.item_encabezado, parent, false));
        }
        return new ActividadHolder(inf.inflate(R.layout.item_actividad, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Fila fila = filas.get(position);
        if (holder instanceof EncabezadoHolder) {
            ((EncabezadoHolder) holder).texto.setText(fila.encabezado);
            return;
        }
        ActividadHolder h = (ActividadHolder) holder;
        Actividad a = fila.actividad;
        h.titulo.setText(a.getTitulo());
        h.detalle.setText(a.getTipo().getEtiqueta());
        tachar(h.titulo, a.isCompletada());

        h.check.setOnCheckedChangeListener(null);   // evita eventos al reciclar la vista
        h.check.setChecked(a.isCompletada());
        h.check.setOnCheckedChangeListener((b, marcado) -> {
            tachar(h.titulo, marcado);
            acciones.onCompletadaCambiada(a, marcado);
        });
        h.itemView.setOnLongClickListener(v -> {
            acciones.onEliminar(a);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return filas.size();
    }

    private static void tachar(TextView tv, boolean tachado) {
        int flags = tv.getPaintFlags();
        tv.setPaintFlags(tachado ? flags | Paint.STRIKE_THRU_TEXT_FLAG : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        tv.setAlpha(tachado ? 0.6f : 1f);
    }

    static class EncabezadoHolder extends RecyclerView.ViewHolder {
        final TextView texto;

        EncabezadoHolder(@NonNull View v) {
            super(v);
            texto = v.findViewById(R.id.tv_encabezado);
        }
    }

    static class ActividadHolder extends RecyclerView.ViewHolder {
        final CheckBox check;
        final TextView titulo;
        final TextView detalle;

        ActividadHolder(@NonNull View v) {
            super(v);
            check = v.findViewById(R.id.cb_completada);
            titulo = v.findViewById(R.id.tv_titulo);
            detalle = v.findViewById(R.id.tv_detalle);
        }
    }
}
