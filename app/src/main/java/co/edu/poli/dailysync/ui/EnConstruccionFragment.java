package co.edu.poli.dailysync.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import co.edu.poli.dailysync.R;

/** Base para los módulos que se completan en la siguiente entrega. */
public abstract class EnConstruccionFragment extends Fragment {

    @StringRes
    protected abstract int getTitulo();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_en_construccion, container, false);
        ((TextView) v.findViewById(R.id.tv_titulo)).setText(getTitulo());
        return v;
    }
}
