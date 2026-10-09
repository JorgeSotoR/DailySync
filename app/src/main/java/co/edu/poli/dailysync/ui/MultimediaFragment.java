package co.edu.poli.dailysync.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;
import java.util.List;

import co.edu.poli.dailysync.R;

/**
 * Multimedia (RF-07): reproductor de video con controles estándar (MediaController)
 * y galería de consejos con desplazamiento horizontal que muestra la descripción al tocarla.
 */
public class MultimediaFragment extends Fragment {

    /** Video de ejemplo de dominio público alojado por Google (requiere internet). */
    private static final String VIDEO_EJEMPLO =
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4";

    private static final String ESTADO_POSICION = "posicion";
    private static final String ESTADO_URI = "uri";

    private static final List<ConsejoAdapter.Consejo> CONSEJOS = Arrays.asList(
            new ConsejoAdapter.Consejo("💧", "Hidratación",
                    "Toma entre 6 y 8 vasos de agua al día. Ten una botella a la mano y bebe antes de sentir sed.",
                    0xFFCFE8FA),
            new ConsejoAdapter.Consejo("🏃", "Actividad física",
                    "Procura al menos 150 minutos de actividad moderada a la semana, por ejemplo 30 minutos, 5 días.",
                    0xFFD9F2E6),
            new ConsejoAdapter.Consejo("😴", "Sueño",
                    "Intenta dormir de 7 a 9 horas. Evita las pantallas 30 minutos antes de acostarte.",
                    0xFFE6E0F8),
            new ConsejoAdapter.Consejo("🥗", "Alimentación",
                    "Incluye frutas y verduras en cada comida y reduce los azúcares y los alimentos ultraprocesados.",
                    0xFFFDE9C8),
            new ConsejoAdapter.Consejo("🧘", "Pausas activas",
                    "Cada hora de estudio o trabajo, levántate y estírate durante 2 o 3 minutos.",
                    0xFFFBD9DF),
            new ConsejoAdapter.Consejo("📵", "Descanso digital",
                    "Programa momentos sin el celular y usa las alarmas de DailySync para recordarlos.",
                    0xFFDDE7F0)
    );

    private VideoView video;
    private View progresoVideo;
    private View errorVideo;
    private Uri uriActual;
    private int posicion;

    private final ActivityResultLauncher<PickVisualMediaRequest> elegirVideo =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    reproducir(uri, 0, true);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_multimedia, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        video = view.findViewById(R.id.video);
        progresoVideo = view.findViewById(R.id.progreso_video);
        errorVideo = view.findViewById(R.id.tv_error_video);

        MediaController controles = new MediaController(requireContext());
        controles.setAnchorView(video);
        video.setMediaController(controles);
        video.setOnPreparedListener(mp -> {
            progresoVideo.setVisibility(View.GONE);
            video.seekTo(posicion);
            controles.show(3000);
        });
        video.setOnErrorListener((mp, what, extra) -> {
            progresoVideo.setVisibility(View.GONE);
            errorVideo.setVisibility(View.VISIBLE);
            return true;   // evita el diálogo de error del sistema
        });

        view.findViewById(R.id.btn_video_ejemplo).setOnClickListener(v ->
                reproducir(Uri.parse(VIDEO_EJEMPLO), 0, true));
        view.findViewById(R.id.btn_video_telefono).setOnClickListener(v -> elegirVideo.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.VideoOnly.INSTANCE)
                        .build()));

        RecyclerView galeria = view.findViewById(R.id.rv_galeria);
        galeria.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        galeria.setAdapter(new ConsejoAdapter(CONSEJOS, this::mostrarConsejo));

        // Al rotar se continúa el mismo video desde la misma posición (sin reproducir automáticamente).
        Uri inicial = Uri.parse(VIDEO_EJEMPLO);
        int posInicial = 0;
        if (savedInstanceState != null) {
            String guardada = savedInstanceState.getString(ESTADO_URI);
            if (guardada != null) {
                inicial = Uri.parse(guardada);
            }
            posInicial = savedInstanceState.getInt(ESTADO_POSICION, 0);
        }
        reproducir(inicial, posInicial, false);
    }

    private void reproducir(Uri uri, int desde, boolean iniciar) {
        uriActual = uri;
        posicion = desde;
        errorVideo.setVisibility(View.GONE);
        progresoVideo.setVisibility(View.VISIBLE);
        video.setVideoURI(uri);
        if (iniciar) {
            video.start();
        }
    }

    private void mostrarConsejo(ConsejoAdapter.Consejo consejo) {
        LinearLayout contenido = new LinearLayout(requireContext());
        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setGravity(Gravity.CENTER_HORIZONTAL);
        int relleno = Math.round(20 * getResources().getDisplayMetrics().density);
        contenido.setPadding(relleno, relleno / 2, relleno, 0);

        TextView emoji = new TextView(requireContext());
        emoji.setText(consejo.emoji);
        emoji.setTextSize(64);
        TextView descripcion = new TextView(requireContext());
        descripcion.setText(consejo.descripcion);
        descripcion.setTextSize(16);
        descripcion.setGravity(Gravity.CENTER);
        descripcion.setPadding(0, relleno / 2, 0, 0);
        contenido.addView(emoji);
        contenido.addView(descripcion);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(consejo.titulo)
                .setView(contenido)
                .setPositiveButton(R.string.cerrar, null)
                .show();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (video != null) {
            posicion = video.getCurrentPosition();
            video.pause();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (uriActual != null && "https".equals(uriActual.getScheme())) {
            // Un video elegido del teléfono no se guarda: su permiso de lectura es temporal.
            outState.putString(ESTADO_URI, uriActual.toString());
        }
        outState.putInt(ESTADO_POSICION, posicion);
    }

    @Override
    public void onDestroyView() {
        if (video != null) {
            video.stopPlayback();
        }
        super.onDestroyView();
    }
}
