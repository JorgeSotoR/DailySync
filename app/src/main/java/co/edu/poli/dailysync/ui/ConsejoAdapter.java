package co.edu.poli.dailysync.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import co.edu.poli.dailysync.R;

/** Tarjetas de la galería de consejos de bienestar (Multimedia). */
public class ConsejoAdapter extends RecyclerView.Adapter<ConsejoAdapter.Holder> {

    /** Un consejo: imagen (emoji), título, descripción y color de fondo. */
    public static class Consejo {
        final String emoji;
        final String titulo;
        final String descripcion;
        final int color;

        public Consejo(String emoji, String titulo, String descripcion, int color) {
            this.emoji = emoji;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.color = color;
        }
    }

    public interface OnConsejoClick {
        void onClick(Consejo consejo);
    }

    private final List<Consejo> consejos;
    private final OnConsejoClick onClick;

    public ConsejoAdapter(List<Consejo> consejos, OnConsejoClick onClick) {
        this.consejos = consejos;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_consejo, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Consejo c = consejos.get(position);
        h.emoji.setText(c.emoji);
        h.titulo.setText(c.titulo);
        h.fondo.setBackgroundColor(c.color);
        h.itemView.setOnClickListener(v -> onClick.onClick(c));
    }

    @Override
    public int getItemCount() {
        return consejos.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View fondo;
        final TextView emoji;
        final TextView titulo;

        Holder(@NonNull View v) {
            super(v);
            fondo = v.findViewById(R.id.fondo);
            emoji = v.findViewById(R.id.tv_emoji);
            titulo = v.findViewById(R.id.tv_titulo);
        }
    }
}
