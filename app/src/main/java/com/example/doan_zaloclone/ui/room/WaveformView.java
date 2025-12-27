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
    
    private Paint playedPaint;   // Paint for the played part
    private Paint unplayedPaint; // Paint for the unplayed part
    private List<Float> amplitudes; // Store amplitude values (0-100)
    private int barWidth = 6; // Width of each bar in pixels
    private int barSpacing = 4; // Spacing between bars
    private float progress = 0f; // 0.0 to 1.0

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
        // Unplayed part (Lighter/Grayer)
        unplayedPaint = new Paint();
        unplayedPaint.setColor(0xFFB0B0B0); // Gray for unplayed
        unplayedPaint.setStyle(Paint.Style.FILL);
        unplayedPaint.setStrokeCap(Paint.Cap.ROUND);
        unplayedPaint.setAntiAlias(true);

        // Played part (Accent Color/Red)
        playedPaint = new Paint();
        playedPaint.setColor(0xFF0068FF); // Zalo Blue for played
        playedPaint.setStyle(Paint.Style.FILL);
        playedPaint.setStrokeCap(Paint.Cap.ROUND);
        playedPaint.setAntiAlias(true);
        
        amplitudes = new ArrayList<>();
    }
    
    /**
     * Add new amplitude value (0-100) for recording
     */
    public void addAmplitude(float amplitude) {
        amplitude = Math.max(0, Math.min(100, amplitude));
        amplitudes.add(amplitude);
        
        // Remove old bars if too many (based on approximate width)
        int maxBars = getWidth() > 0 ? getWidth() / (barWidth + barSpacing) : 50;
        if (amplitudes.size() > maxBars) {
            amplitudes.remove(0);
        }
        invalidate();
    }
    
    public void clear() {
        amplitudes.clear();
        progress = 0f;
        invalidate();
    }
    
    /**
     * Set playback progress (0.0 to 1.0)
     */
    public void setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }

    /**
     * Set a static waveform for playback filling the width
     */
    public void setStaticWaveform(int durationSeconds) {
        amplitudes.clear();
        
        // Wait for layout to know width, or assume a default width if not measured yet
        post(() -> {
            int width = getWidth();
            if (width == 0) return;
            
            // Calculate how many bars fit in the width
            int barCount = width / (barWidth + barSpacing);
            
            // Use seeded random for consistent waveform per duration
            java.util.Random random = new java.util.Random(durationSeconds * 1000L);
            
            for (int i = 0; i < barCount; i++) {
                // Generate varied heights
                float baseAmplitude = 20 + random.nextFloat() * 70; // 20-90 range
                float wave = (float) Math.sin(i * 0.2) * 15; // Smooth wave
                float amplitude = Math.max(15, Math.min(100, baseAmplitude + wave));
                amplitudes.add(amplitude);
            }
            invalidate();
        });
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
        
        // Draw left-aligned to fill space
        int startX = 0;
        
        float progressX = width * progress; // X coordinate where color changes

        for (int i = 0; i < amplitudes.size(); i++) {
            float amplitude = amplitudes.get(i);
            
            // Calculate bar height based on amplitude (0-100)
            float barHeight = (amplitude / 100f) * (height * 0.8f);
            barHeight = Math.max(height * 0.1f, barHeight);
            
            float x = startX + i * (barWidth + barSpacing);
            float top = centerY - (barHeight / 2);
            float bottom = centerY + (barHeight / 2);
            float right = x + barWidth;
            
            // Determine paint based on progress position
            // If the bar is mostly within the played area, use playedPaint
            Paint paint = (x < progressX) ? playedPaint : unplayedPaint;

            canvas.drawRoundRect(
                x, 
                top, 
                right, 
                bottom, 
                barWidth / 2f,
                barWidth / 2f, 
                paint
            );
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int desiredHeight = 80;
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
