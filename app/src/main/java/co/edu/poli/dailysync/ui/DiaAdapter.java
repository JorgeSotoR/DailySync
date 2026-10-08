package co.edu.poli.dailysync.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.model.CalculadoraCiclo.TipoDia;

/** Celdas del calendario del ciclo. Las posiciones vacías (antes del día 1) son null. */
public class DiaAdapter extends RecyclerView.Adapter<DiaAdapter.Holder> {

    public interface Acciones {
        void onDiaTocado(LocalDate dia);
        void onDiaMantenido(LocalDate dia);
    }

    private final List<LocalDate> dias = new ArrayList<>();
    private final Map<LocalDate, TipoDia> tipos = new HashMap<>();
    private final Acciones acciones;
    private LocalDate seleccionado;
    private final LocalDate hoy = LocalDate.now();

    public DiaAdapter(Acciones acciones) {
        this.acciones = acciones;
    }

    public void setDatos(List<LocalDate> nuevosDias, Map<LocalDate, TipoDia> nuevosTipos, LocalDate seleccionado) {
        dias.clear();
        dias.addAll(nuevosDias);
        tipos.clear();
        tipos.putAll(nuevosTipos);
        this.seleccionado = seleccionado;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dia, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        LocalDate dia = dias.get(position);
        if (dia == null) {
            h.texto.setText("");
            h.texto.setBackground(null);
            h.itemView.setOnClickListener(null);
            h.itemView.setOnLongClickListener(null);
            h.itemView.setClickable(false);
            return;
        }
        Context ctx = h.itemView.getContext();
        TipoDia tipo = tipos.containsKey(dia) ? tipos.get(dia) : TipoDia.NINGUNO;
        boolean esSeleccionado = dia.equals(seleccionado);
        boolean esHoy = dia.equals(hoy);
        float densidad = ctx.getResources().getDisplayMetrics().density;

        GradientDrawable fondo = new GradientDrawable();
        fondo.setCornerRadius(6 * densidad);
        int colorTexto = ContextCompat.getColor(ctx, R.color.texto);

        if (esSeleccionado) {
            fondo.setColor(ContextCompat.getColor(ctx, R.color.azul));
            colorTexto = ContextCompat.getColor(ctx, R.color.blanco);
        } else if (tipo == TipoDia.PERIODO) {
            fondo.setColor(ContextCompat.getColor(ctx, R.color.rosa));
        } else if (tipo == TipoDia.FERTIL) {
            fondo.setColor(ContextCompat.getColor(ctx, R.color.verde_claro));
        } else if (tipo == TipoDia.PERIODO_ESTIMADO) {
            fondo.setColor(ContextCompat.getColor(ctx, R.color.blanco));
            fondo.setStroke((int) (1.5f * densidad), ContextCompat.getColor(ctx, R.color.rosa_borde),
                    3 * densidad, 2 * densidad);
        } else {
            fondo.setColor(ContextCompat.getColor(ctx, R.color.blanco));
        }
        if (esHoy && !esSeleccionado) {
            fondo.setStroke((int) (2 * densidad), ContextCompat.getColor(ctx, R.color.azul));
        }
        if (dia.isAfter(hoy) && !esSeleccionado) {
            colorTexto = ContextCompat.getColor(ctx, R.color.texto_secundario);
        }

        h.texto.setBackground(fondo);
        h.texto.setText(String.valueOf(dia.getDayOfMonth()));
        h.texto.setTextColor(colorTexto);
        h.texto.setTypeface(null, esHoy ? Typeface.BOLD : Typeface.NORMAL);
        h.itemView.setOnClickListener(v -> acciones.onDiaTocado(dia));
        h.itemView.setOnLongClickListener(v -> {
            acciones.onDiaMantenido(dia);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return dias.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView texto;

        Holder(@NonNull View v) {
            super(v);
            texto = v.findViewById(R.id.tv_dia);
        }
    }
}
