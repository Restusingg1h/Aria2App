package com.gianlu.aria2app.Adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.gianlu.aria2app.NetIO.Aria2.DownloadWithUpdate;
import com.gianlu.aria2app.R;

import java.util.Arrays;
import java.util.Objects;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class BitfieldVisualizer extends View {
    private final int padding = 12;
    private final int square = 48;
    private final Paint paint;
    private String bitfield = null;
    private int pieces = -1;
    private int[] binary = null;
    private int columns;
    private int rows;
    private int hoff;

    public BitfieldVisualizer(Context context) {
        this(context, null, 0);
    }

    public BitfieldVisualizer(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BitfieldVisualizer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint();
        paint.setColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
    }

    @Nullable
    private static int[] hexToBinary(@Nullable String hex, int num) {
        if (hex == null) return null;
        int[] array = new int[hex.length()];
        for (int i = 0; i < hex.length(); i++) {
            switch (Character.toLowerCase(hex.charAt(i))) {
                case '0':
                    array[i] = 0;
                    break;
                case '1':
                case '2':
                case '4':
                case '8':
                    array[i] = 1;
                    break;
                case '3':
                case '5':
                case '6':
                case '9':
                case 'a':
                case 'c':
                    array[i] = 2;
                    break;
                case '7':
                case 'b':
                case 'd':
                case 'e':
                    array[i] = 3;
                    break;
                case 'f':
                    array[i] = 4;
                    break;
            }
        }

        return Arrays.copyOfRange(array, 0, num);
    }

    public void setColor(@ColorInt int color) {
        paint.setColor(color);
        invalidate();
    }

    public void update(DownloadWithUpdate.BigUpdate update) {
        if (update == null || Objects.equals(bitfield, update.bitfield)) return;

        bitfield = update.bitfield;
        pieces = update.numPieces / 4;
        binary = hexToBinary(bitfield, pieces);

        invalidate();
    }

    private int columns(int width) {
        return width / (square + padding);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (pieces == -1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        columns = columns(width);
        rows = (int) Math.ceil((double) pieces / (double) columns);

        hoff = (width - columns * (square + padding)) / 4;

        setMeasuredDimension(width, getDefaultSize(rows * (square + padding) + padding, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (binary == null || pieces == -1) return;

        int i = 0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                if (i < pieces && binary[i] != 0) {
                    paint.setAlpha(255 / 4 * binary[i]);
                    int px = padding + column * (square + padding) + hoff;
                    int py = padding + row * (square + padding);
                    canvas.drawRect(px, py, square + px, square + py, paint);
                }
                i++;
            }
        }
    }
}
