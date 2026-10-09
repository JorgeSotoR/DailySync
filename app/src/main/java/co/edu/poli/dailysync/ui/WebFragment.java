package co.edu.poli.dailysync.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import co.edu.poli.dailysync.R;

/**
 * Explorador web (RF-08): caja de texto para la URL, botón Ir y un WebView embebido
 * para consultar artículos de salud o recetas sin salir de la app.
 */
public class WebFragment extends Fragment {

    private static final String INICIO = "https://www.who.int/es/news-room/fact-sheets/detail/healthy-diet";
    private static final String ATAJO_ALIMENTACION = INICIO;
    private static final String ATAJO_ACTIVIDAD = "https://www.who.int/es/news-room/fact-sheets/detail/physical-activity";
    private static final String ATAJO_RECETAS = "https://www.google.com/search?q=recetas+saludables+f%C3%A1ciles";

    private WebView web;
    private TextInputEditText etUrl;
    private LinearProgressIndicator progreso;
    private View tvError;

    /** El botón Atrás navega hacia atrás dentro de la página antes de salir del módulo. */
    private final OnBackPressedCallback atrasEnWeb = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (web != null && web.canGoBack()) {
                web.goBack();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_web, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")   // muchas páginas de salud y recetas necesitan JavaScript
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        web = view.findViewById(R.id.web);
        etUrl = view.findViewById(R.id.et_url);
        progreso = view.findViewById(R.id.progreso);
        tvError = view.findViewById(R.id.tv_error_web);

        WebSettings ajustes = web.getSettings();
        ajustes.setJavaScriptEnabled(true);
        ajustes.setDomStorageEnabled(true);
        ajustes.setLoadWithOverviewMode(true);
        ajustes.setUseWideViewPort(true);
        ajustes.setBuiltInZoomControls(true);
        ajustes.setDisplayZoomControls(false);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                String esquema = request.getUrl().getScheme();
                // Solo se abren dentro de la app los enlaces web; los demás (tel:, mailto:…) se ignoran.
                return !"http".equals(esquema) && !"https".equals(esquema);
            }

            @Override
            public void onPageStarted(WebView v, String url, Bitmap icono) {
                tvError.setVisibility(View.GONE);
                etUrl.setText(url);
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    tvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void doUpdateVisitedHistory(WebView v, String url, boolean recarga) {
                atrasEnWeb.setEnabled(v.canGoBack());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int nuevoProgreso) {
                progreso.setProgressCompat(nuevoProgreso, true);
                progreso.setVisibility(nuevoProgreso < 100 ? View.VISIBLE : View.INVISIBLE);
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), atrasEnWeb);

        view.findViewById(R.id.btn_ir).setOnClickListener(v -> cargarTexto());
        etUrl.setOnEditorActionListener((v, accion, evento) -> {
            boolean enter = evento != null && evento.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (accion == EditorInfo.IME_ACTION_GO || enter) {
                cargarTexto();
                return true;
            }
            return false;
        });
        view.findViewById(R.id.chip_alimentacion).setOnClickListener(v -> cargar(ATAJO_ALIMENTACION));
        view.findViewById(R.id.chip_actividad).setOnClickListener(v -> cargar(ATAJO_ACTIVIDAD));
        view.findViewById(R.id.chip_recetas).setOnClickListener(v -> cargar(ATAJO_RECETAS));

        if (savedInstanceState != null && web.restoreState(savedInstanceState) != null) {
            atrasEnWeb.setEnabled(web.canGoBack());
        } else {
            cargar(INICIO);
        }
    }

    private void cargarTexto() {
        String texto = etUrl.getText() == null ? "" : etUrl.getText().toString().trim();
        if (TextUtils.isEmpty(texto)) {
            return;
        }
        ocultarTeclado();
        cargar(normalizar(texto));
    }

    private void cargar(String url) {
        etUrl.setText(url);
        web.loadUrl(url);
    }

    /**
     * Convierte lo escrito en una dirección válida:
     * "minsalud.gov.co" → "https://minsalud.gov.co"; "recetas con avena" → búsqueda en Google.
     */
    static String normalizar(String texto) {
        String t = texto.trim();
        String minus = t.toLowerCase(Locale.ROOT);
        if (minus.startsWith("http://") || minus.startsWith("https://")) {
            return t;
        }
        if (!t.contains(" ") && t.contains(".")) {
            return "https://" + t;
        }
        return "https://www.google.com/search?q=" + Uri.encode(t);
    }

    private void ocultarTeclado() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etUrl.getWindowToken(), 0);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) {
            web.saveState(outState);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (web != null) {
            web.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (web != null) {
            web.onResume();
        }
    }

    @Override
    public void onDestroyView() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
            web = null;
        }
        super.onDestroyView();
    }
}
