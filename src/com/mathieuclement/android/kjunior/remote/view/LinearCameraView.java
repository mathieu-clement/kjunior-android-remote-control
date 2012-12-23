package com.mathieuclement.android.kjunior.remote.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Author: Mathieu Clément
 * Date: 22.12.2012
 */
public class LinearCameraView extends View {
    public LinearCameraView(Context context) {
        super(context);
    }

    public LinearCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinearCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    // Pixels as read from KJunior linear camera
    public int[] linearCameraPixels = new int[102];

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int y = 0;
        Paint paint = new Paint();

        for (int i = 0; i < linearCameraPixels.length; i++) {
            int pixelValue = linearCameraPixels[i];
            paint.setARGB(255, pixelValue, pixelValue, pixelValue);

            y += 10;
            canvas.drawRect((float) y - 10, (float) 0, (float) y, (float) 400, paint);
        }
    }
}
