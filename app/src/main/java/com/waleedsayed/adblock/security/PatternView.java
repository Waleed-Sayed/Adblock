package com.waleedsayed.adblock.security;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class PatternView extends View {

    public interface OnPatternListener {
        void onPatternComplete(List<Integer> pattern);
    }

    private final List<Integer> selectedNodes = new ArrayList<>(9);
    private final boolean[] nodeSelected = new boolean[9];
    private final float[] nodeX = new float[9];
    private final float[] nodeY = new float[9];

    private float currentX;
    private float currentY;
    private boolean isDragging = false;
    private boolean isError = false;

    private final Paint ringPaint;
    private final Paint dotPaint;
    private final Paint activePaint;
    private final Paint errorPaint;
    private final Paint linePaint;

    private final float ringRadius;
    private final float dotRadius;
    private final float hitRadius;

    private OnPatternListener listener;

    public PatternView(Context context) {
        this(context, null);
    }

    public PatternView(Context context, AttributeSet attrs) {
        super(context, attrs);

        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(dp(2.0f));
        ringPaint.setColor(0xFF2B3A52);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(0xFF64748B);

        activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activePaint.setStyle(Paint.Style.FILL);
        activePaint.setColor(0xFF34D399);

        errorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        errorPaint.setStyle(Paint.Style.FILL);
        errorPaint.setColor(0xFFF87171);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(5.0f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(0xAA34D399);

        ringRadius = dp(28.0f);
        dotRadius = dp(8.0f);
        hitRadius = dp(40.0f);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    public void setOnPatternListener(OnPatternListener listener) {
        this.listener = listener;
    }

    public void clearPattern() {
        selectedNodes.clear();
        for (int i = 0; i < 9; i++) {
            nodeSelected[i] = false;
        }
        isDragging = false;
        isError = false;
        invalidate();
    }

    public void showError() {
        clearPattern();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float size = Math.min(w, h);
        float offsetX = (w - size) / 2.0f;
        float offsetY = (h - size) / 2.0f;
        float cell = size / 3.0f;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int idx = r * 3 + c;
                nodeX[idx] = offsetX + cell * (c + 0.5f);
                nodeY[idx] = offsetY + cell * (r + 0.5f);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        ringPaint.setColor(0xFF2B3A52);
        for (int i = 0; i < 9; i++) {
            float cx = nodeX[i];
            float cy = nodeY[i];
            canvas.drawCircle(cx, cy, ringRadius, ringPaint);
            canvas.drawCircle(cx, cy, dotRadius, dotPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                clearPattern();
                isDragging = true;
                currentX = x;
                currentY = y;
                handleHit(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                currentX = x;
                currentY = y;
                handleHit(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                isDragging = false;
                if (!selectedNodes.isEmpty() && listener != null) {
                    listener.onPatternComplete(new ArrayList<>(selectedNodes));
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleHit(float x, float y) {
        int hit = findHitNode(x, y);
        if (hit < 0 || nodeSelected[hit]) {
            return;
        }
        if (!selectedNodes.isEmpty()) {
            int prev = selectedNodes.get(selectedNodes.size() - 1);
            int r1 = prev / 3, c1 = prev % 3;
            int r2 = hit / 3, c2 = hit % 3;
            if ((r1 + r2) % 2 == 0 && (c1 + c2) % 2 == 0) {
                int mid = ((r1 + r2) / 2) * 3 + ((c1 + c2) / 2);
                if (!nodeSelected[mid]) {
                    nodeSelected[mid] = true;
                    selectedNodes.add(mid);
                }
            }
        }
        nodeSelected[hit] = true;
        selectedNodes.add(hit);
    }

    private int findHitNode(float x, float y) {
        float r2 = hitRadius * hitRadius;
        for (int i = 0; i < 9; i++) {
            float dx = x - nodeX[i];
            float dy = y - nodeY[i];
            if (dx * dx + dy * dy <= r2) {
                return i;
            }
        }
        return -1;
    }
}
