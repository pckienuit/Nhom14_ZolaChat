package com.example.doan_zaloclone.adapters;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Message;
import com.example.doan_zaloclone.ui.room.WaveformView;

/**
 * Phase 4D-4: ViewHolder for voice message display with playback controls
 */
public class VoiceMessageViewHolder extends RecyclerView.ViewHolder {
    
    private ImageButton playPauseButton;
    private WaveformView waveformView;
    private TextView durationText;
    private TextView speedButton;
    
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private float playbackSpeed = 1.0f;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    
    private String currentVoiceUrl;
    private int totalDuration;
    
    public VoiceMessageViewHolder(@NonNull View itemView) {
        super(itemView);
        
        playPauseButton = itemView.findViewById(R.id.voicePlayPauseButton);
        waveformView = itemView.findViewById(R.id.voiceWaveform);
        durationText = itemView.findViewById(R.id.voiceDurationText);
        speedButton = itemView.findViewById(R.id.voiceSpeedButton);
        
        // Setup click listeners
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        speedButton.setOnClickListener(v -> showSpeedMenu());
    }
    
    public static VoiceMessageViewHolder create(View itemView) {
        return new VoiceMessageViewHolder(itemView);
    }
    
    public void bind(Message message) {
        if (message == null) {
            android.util.Log.e("VoiceViewHolder", "Message is null");
            return;
        }
        
        currentVoiceUrl = message.getVoiceUrl();
        totalDuration = message.getVoiceDuration();
        
        android.util.Log.d("VoiceViewHolder", "Binding voice message - URL: " + currentVoiceUrl + ", Duration: " + totalDuration);
        
        // Reset state
        stopPlayback();
        updateDurationDisplay(0, totalDuration);
        waveformView.setProgress(0f);
        
        // Update speed display
        speedButton.setText(String.format("%.1fx", playbackSpeed).replace(".0", ""));
        
        // Check if data is available
        if (currentVoiceUrl == null || currentVoiceUrl.isEmpty()) {
            android.util.Log.e("VoiceViewHolder", "Voice URL is null or empty!");
            durationText.setText("Error: No audio file");
            waveformView.clear();
        } else {
            // Generate static waveform visualization
            waveformView.setStaticWaveform(totalDuration);
        }
    }
    
    private void togglePlayPause() {
        if (isPlaying) {
            pausePlayback();
        } else {
            startPlayback();
        }
    }
    
    private void startPlayback() {
        if (currentVoiceUrl == null || currentVoiceUrl.isEmpty()) {
            Toast.makeText(itemView.getContext(), "Không tìm thấy file âm thanh", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Release existing player
            releaseMediaPlayer();
            
            // Create new player
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(currentVoiceUrl);
            mediaPlayer.prepareAsync();
            
            mediaPlayer.setOnPreparedListener(mp -> {
                // Apply speed
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    try {
                        android.media.PlaybackParams params = mp.getPlaybackParams();
                        params.setSpeed(playbackSpeed);
                        mp.setPlaybackParams(params);
                    } catch (Exception e) {
                        android.util.Log.e("VoiceViewHolder", "Error setting speed", e);
                    }
                }
                
                mp.start();
                isPlaying = true;
                updatePlayPauseButton();
                startProgressTracking();
            });
            
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                updatePlayPauseButton();
                stopProgressTracking();
                updateDurationDisplay(0, totalDuration);
                waveformView.setProgress(0f);
            });
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(itemView.getContext(), "Lỗi phát âm thanh", Toast.LENGTH_SHORT).show();
                stopPlayback();
                return true;
            });
            
        } catch (Exception e) {
            android.util.Log.e("VoiceViewHolder", "Error starting playback", e);
            Toast.makeText(itemView.getContext(), "Lỗi phát âm thanh", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            updatePlayPauseButton();
            stopProgressTracking();
        }
    }
    
    private void stopPlayback() {
        pausePlayback();
        releaseMediaPlayer();
    }
    
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                // Ignore
            }
            mediaPlayer = null;
        }
        isPlaying = false;
        stopProgressTracking();
    }
    
    private void startProgressTracking() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    try {
                        int currentPos = mediaPlayer.getCurrentPosition() / 1000;
                        updateDurationDisplay(currentPos, totalDuration);
                        
                        // Update waveform progress
                        if (totalDuration > 0) {
                            float progress = (float) mediaPlayer.getCurrentPosition() / (totalDuration * 1000f);
                            waveformView.setProgress(progress);
                        }
                        
                        progressHandler.postDelayed(this, 100);
                    } catch (Exception e) {
                        // Player released
                    }
                }
            }
        };
        progressHandler.post(progressRunnable);
    }
    
    private void stopProgressTracking() {
        if (progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }
    
    private void updateDurationDisplay(int current, int total) {
        String currentStr = String.format("%02d:%02d", current / 60, current % 60);
        String totalStr = String.format("%02d:%02d", total / 60, total % 60);
        durationText.setText(currentStr + " / " + totalStr);
    }
    
    private void updatePlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }
    
    private void showSpeedMenu() {
        PopupMenu popup = new PopupMenu(itemView.getContext(), speedButton);
        popup.getMenu().add(0, 1, 0, "0.5x");
        popup.getMenu().add(0, 2, 1, "1x");
        popup.getMenu().add(0, 3, 2, "1.5x");
        popup.getMenu().add(0, 4, 3, "2x");
        
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    setSpeed(0.5f);
                    return true;
                case 2:
                    setSpeed(1.0f);
                    return true;
                case 3:
                    setSpeed(1.5f);
                    return true;
                case 4:
                    setSpeed(2.0f);
                    return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void setSpeed(float speed) {
        playbackSpeed = speed;
        speedButton.setText(String.format("%.1fx", speed).replace(".0", ""));
        
        // Apply to current playback
        if (mediaPlayer != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                android.media.PlaybackParams params = mediaPlayer.getPlaybackParams();
                params.setSpeed(playbackSpeed);
                mediaPlayer.setPlaybackParams(params);
            } catch (Exception e) {
                android.util.Log.e("VoiceViewHolder", "Error updating speed", e);
            }
        }
    }
    
    // Cleanup when ViewHolder is recycled
    public void onRecycled() {
        releaseMediaPlayer();
    }
}
