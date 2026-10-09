package co.edu.poli.dailysync.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.RelativeCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.io.File;

import co.edu.poli.dailysync.R;
import co.edu.poli.dailysync.model.Usuario;

/**
 * Avatar circular reutilizable: muestra una imagen (predeterminada o de la galería) o un emoji.
 * Se usa en el perfil y en el encabezado del menú.
 */
public class AvatarView extends FrameLayout {

    private final ShapeableImageView imagen;
    private final TextView emoji;
    private final GradientDrawable fondoEmoji;

    public AvatarView(@NonNull Context context) {
        this(context, null);
    }

    public AvatarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        imagen = new ShapeableImageView(context);
        imagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imagen.setShapeAppearanceModel(ShapeAppearanceModel.builder()
                .setAllCornerSizes(new RelativeCornerSize(0.5f))
                .build());
        addView(imagen, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        fondoEmoji = new GradientDrawable();
        fondoEmoji.setShape(GradientDrawable.OVAL);
        fondoEmoji.setColor(0xFFCFE0F5);
        emoji = new TextView(context);
        emoji.setGravity(Gravity.CENTER);
        emoji.setIncludeFontPadding(false);
        emoji.setBackground(fondoEmoji);
        addView(emoji, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setAvatar(Usuario.AVATAR_1);
    }

    /** Agrega un borde de color azul con el grosor indicado en dp. */
    public void setBorde(float dp) {
        int px = Math.round(dp * getResources().getDisplayMetrics().density);
        int azul = ContextCompat.getColor(getContext(), R.color.azul);
        imagen.setStrokeWidth(px);
        imagen.setStrokeColor(ColorStateList.valueOf(azul));
        imagen.setPadding(px / 2, px / 2, px / 2, px / 2);
        fondoEmoji.setStroke(px, azul);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        emoji.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.min(w, h) * 0.5f);
    }

    /** Acepta "avatar_1", "avatar_2", "emoji:😎" o la ruta de un archivo de imagen. */
    public void setAvatar(@Nullable String avatar) {
        if (Usuario.esEmoji(avatar)) {
            emoji.setText(Usuario.emojiDe(avatar));
            emoji.setVisibility(VISIBLE);
            imagen.setVisibility(INVISIBLE);
            return;
        }
        emoji.setVisibility(GONE);
        imagen.setVisibility(VISIBLE);
        if (avatar != null && avatar.startsWith("/") && new File(avatar).exists()) {
            imagen.setImageURI(null);   // obliga a recargar si el archivo cambió
            imagen.setImageURI(Uri.fromFile(new File(avatar)));
        } else if (Usuario.AVATAR_2.equals(avatar)) {
            imagen.setImageResource(R.drawable.avatar_2);
        } else {
            imagen.setImageResource(R.drawable.avatar_1);
        }
    }
}
