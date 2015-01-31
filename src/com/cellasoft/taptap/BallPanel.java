package com.cellasoft.taptap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class BallPanel extends SurfaceView implements SurfaceHolder.Callback {
    static final float SCALE = 10f;    // Maximum acceleration in m/s2
    GestureDetector gestureDetector;

    public BallPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    // SurfaceHolder.Callback
    @Override
    public void surfaceChanged(
            SurfaceHolder holder,
            int format,
            int width,
            int height) {
        this.panelHeight = (float) height;
        this.panelWidth = (float) width;
        Canvas c = holder.lockCanvas();
        drawBall(c, false, 0.0f, 0.0f, 0.0f);
        holder.unlockCanvasAndPost(c);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        GyroAccelActivity.ballPanel = this;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        GyroAccelActivity.ballPanel = this;
    }

    public void drawBall(Canvas c,
                         boolean ball,
                         float x,
                         float y,
                         float z) {
        c.drawRGB(0, 0, 0);
        Paint p = new Paint();
        p.setColor(Color.WHITE);
        float halfWidth = panelWidth / 2.0f;
        float halfHeight = panelHeight / 2.0f;
        c.drawLine(halfWidth, 0, halfWidth, panelHeight, p);
        c.drawLine(0, halfHeight, panelWidth, halfHeight, p);
        if (ball) {
            float cx = halfWidth + (halfWidth * (x / SCALE));
            float cy = halfHeight + (halfHeight * (y / SCALE));
            float radius = 100 + (100 * (z / SCALE));
            if (radius < 0.0f)
                radius = 1.0f;
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(cx, cy, radius, p);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return gestureDetector.onTouchEvent(e);
    }
    
    float panelHeight;
    float panelWidth;

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            Log.d("Double Tap", "Tapped at: (" + x + "," + y + ")");

            return true;
        }
    }
}
