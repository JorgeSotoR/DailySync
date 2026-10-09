package co.edu.poli.dailysync.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.model.Alarma;

public class AlarmaAdapter extends RecyclerView.Adapter<AlarmaAdapter.Holder> {

    public interface Acciones {
        void onActivaCambiada(Alarma alarma, boolean activa);
        void onEditar(Alarma alarma);
        void onEliminar(Alarma alarma);
    }

    private final List<Alarma> alarmas;
    private final Acciones acciones;

    public AlarmaAdapter(List<Alarma> alarmas, Acciones acciones) {
        this.alarmas = alarmas;
        this.acciones = acciones;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarma, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Alarma a = alarmas.get(position);
        h.hora.setText(a.getHoraTexto());
        h.etiqueta.setText(a.getTextoRepeticion() + " · " + a.getEtiqueta());
        h.hora.setAlpha(a.isActiva() ? 1f : 0.45f);
        h.etiqueta.setAlpha(a.isActiva() ? 1f : 0.45f);

        h.activa.setOnCheckedChangeListener(null);   // evita eventos al reciclar la vista
        h.activa.setChecked(a.isActiva());
        h.activa.setOnCheckedChangeListener((b, marcado) -> {
            h.hora.setAlpha(marcado ? 1f : 0.45f);
            h.etiqueta.setAlpha(marcado ? 1f : 0.45f);
            acciones.onActivaCambiada(a, marcado);
        });
        h.itemView.setOnClickListener(v -> acciones.onEditar(a));
        h.itemView.setOnLongClickListener(v -> {
            acciones.onEliminar(a);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return alarmas.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView hora;
        final TextView etiqueta;
        final MaterialSwitch activa;

        Holder(@NonNull View v) {
            super(v);
            hora = v.findViewById(R.id.tv_hora);
            etiqueta = v.findViewById(R.id.tv_etiqueta);
            activa = v.findViewById(R.id.sw_activa);
        }
    }
}
