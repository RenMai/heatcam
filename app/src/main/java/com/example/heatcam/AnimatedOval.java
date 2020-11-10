package com.example.heatcam;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.util.AttributeSet;
import android.view.View;

public class AnimatedOval extends View {

    private final int COLOR_RED = Color.rgb(255, 51,51);
    private final int COLOR_ORANGE = Color.rgb(255, 128, 51);
    private final int COLOR_YELLOW = Color.rgb(255, 218,51);
    private final int COLOR_GREEN = Color.rgb(76, 255, 51);

    private Paint paint;
    private Path path;

    private int width = 1200;
    private int height = 1920;

    private PathMeasure pMeasure;
    private float length;

    private ObjectAnimator animator;


    public AnimatedOval(Context context) {
        super(context);
    }

    public AnimatedOval(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatedOval(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void init() {
        float x =  width / 2.0f;
        float w = (950/2.0f);
        float h = 1450.0f;
        float fromTop = 80.0f;

        path = new Path();
        path.addOval(x - w, fromTop, x + w, h, Path.Direction.CW);

        paint = new Paint();
        paint.setColor(COLOR_RED);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);

        pMeasure = new PathMeasure(path, false);
        length = pMeasure.getLength();


        animator = ObjectAnimator.ofFloat(AnimatedOval.this, "phase", 1.0f, 0.0f);
        animator.setDuration(4000);
        animator.start();
    }

    public void setPhase(float phase) {
        float val = phase * 100;
        if (val < 66.0f) {
            paint.setColor(COLOR_ORANGE);
        }
        if (val < 44.0f) {
            paint.setColor(COLOR_YELLOW);
        }
        if (val < 16.0f) {
            paint.setColor(COLOR_GREEN);
        }
        paint.setPathEffect(createPathEffect(length, phase, 0.0f));
        invalidate();
    }

    private static PathEffect createPathEffect(float pathLength, float phase, float offset) {
        return new DashPathEffect(new float[] { pathLength, pathLength },
                Math.max(phase * pathLength, offset));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);
    }

    public void stopAnimation() {
        if (animator != null) {
            animator.cancel();
        }
    }

}