package com.example.doan_zaloclone.ui.room;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4D-2: Custom View for realtime waveform visualization during voice recording
 */
public class WaveformView extends View {
    
    private Paint wavePaint;
    private List<Float> amplitudes; // Store amplitude values (0-100)
    private int maxBars = 50; // Maximum number of bars to display
    private int barWidth = 6; // Width of each bar in pixels
    private int barSpacing = 4; // Spacing between bars
    
    public WaveformView(Context context) {
        super(context);
        init();
    }
    
    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        wavePaint = new Paint();
        wavePaint.setColor(0xFFFF3B30); // Red color
        wavePaint.setStyle(Paint.Style.FILL);
        wavePaint.setStrokeCap(Paint.Cap.ROUND);
        wavePaint.setAntiAlias(true);
        
        amplitudes = new ArrayList<>();
    }
    
    /**
     * Add new amplitude value (0-100)
     * @param amplitude Volume level from 0 to 100
     */
    public void addAmplitude(float amplitude) {
        // Normalize to 0-100 range
        amplitude = Math.max(0, Math.min(100, amplitude));
        
        amplitudes.add(amplitude);
        
        // Keep only maxBars recent values
        if (amplitudes.size() > maxBars) {
            amplitudes.remove(0);
        }
        
        // Trigger redraw
        invalidate();
    }
    
    /**
     * Clear all amplitude data
     */
    public void clear() {
        amplitudes.clear();
        invalidate();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        if (amplitudes.isEmpty()) {
            return;
        }
        
        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;
        
        // Calculate starting X position (right-aligned)
        int totalWidth = amplitudes.size() * (barWidth + barSpacing);
        int startX = Math.max(0, width - totalWidth);
        
        // Draw each bar
        for (int i = 0; i < amplitudes.size(); i++) {
            float amplitude = amplitudes.get(i);
            
            // Calculate bar height based on amplitude (0-100)
            // Map 0-100 to 10%-90% of view height
            float barHeight = (amplitude / 100f) * (height * 0.8f);
            barHeight = Math.max(height * 0.1f, barHeight); // Minimum 10% height
            
            float x = startX + i * (barWidth + barSpacing);
            float top = centerY - (barHeight / 2);
            float bottom = centerY + (barHeight / 2);
            
            // Draw rounded rectangle bar
            canvas.drawRoundRect(
                x, 
                top, 
                x + barWidth, 
                bottom, 
                barWidth / 2f, // Corner radius
                barWidth / 2f, 
                wavePaint
            );
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        // Default height if not specified
        int desiredHeight = 80; // 80dp default height
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        
        int height;
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }
        
        setMeasuredDimension(getMeasuredWidth(), height);
    }
}
