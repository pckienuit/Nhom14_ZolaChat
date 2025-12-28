package com.example.doan_zaloclone.adapters;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Message;
import com.example.doan_zaloclone.models.MessageReaction;
import com.example.doan_zaloclone.ui.room.MessageAdapter;
import com.example.doan_zaloclone.ui.room.WaveformView;

import java.util.List;
import java.util.Map;

/**
 * Phase 4D-4: ViewHolder for voice message display with playback controls
 * Updated: Added support for pin, forward, recall, reply, and reaction features
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
    
    /**
     * Legacy bind method for compatibility
     */
    public void bind(Message message) {
        bind(message, null, null, null, null, null, null, null, null, false, false, null);
    }
    
    /**
     * Full bind method with all listeners for context menu and reactions
     */
    public void bind(Message message,
                     MessageAdapter.OnMessageLongClickListener longClickListener,
                     MessageAdapter.OnMessageReplyListener replyListener,
                     MessageAdapter.OnReplyPreviewClickListener replyPreviewClickListener,
                     MessageAdapter.OnMessageRecallListener recallListener,
                     MessageAdapter.OnMessageForwardListener forwardListener,
                     MessageAdapter.OnMessageEditListener editListener,
                     MessageAdapter.OnMessageDeleteListener deleteListener,
                     String currentUserId,
                     boolean isPinned,
                     boolean isHighlighted,
                     MessageAdapter.OnMessageReactionListener reactionListener) {
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
        
        // Apply pin indicator and highlight effect
        applyPinAndHighlight(itemView, isPinned, isHighlighted);
        
        // Bind reaction indicator
        if (currentUserId != null && reactionListener != null) {
            bindReactionIndicator(itemView, message, currentUserId, reactionListener);
        }
        
        // Setup long click for context menu
        if (longClickListener != null || replyListener != null || recallListener != null || forwardListener != null) {
            itemView.setOnLongClickListener(v -> {
                showMessageContextMenu(v, message, longClickListener, replyListener, 
                        recallListener, forwardListener, editListener, deleteListener, 
                        isPinned, currentUserId);
                return true;
            });
        }
    }
    
    /**
     * Helper method to apply pin indicator and highlight effect
     */
    private static void applyPinAndHighlight(View itemView, boolean isPinned, boolean isHighlighted) {
        View pinIndicator = itemView.findViewById(R.id.pinIndicator);
        if (pinIndicator != null) {
            pinIndicator.setVisibility(isPinned ? View.VISIBLE : View.GONE);
        }
        
        if (isHighlighted) {
            itemView.setBackgroundColor(0x40FFEB3B); // Semi-transparent yellow
        } else {
            itemView.setBackground(null);
        }
    }
    
    /**
     * Show context menu for message actions
     */
    private static void showMessageContextMenu(View view, Message message,
                                               MessageAdapter.OnMessageLongClickListener listener,
                                               MessageAdapter.OnMessageReplyListener replyListener,
                                               MessageAdapter.OnMessageRecallListener recallListener,
                                               MessageAdapter.OnMessageForwardListener forwardListener,
                                               MessageAdapter.OnMessageEditListener editListener,
                                               MessageAdapter.OnMessageDeleteListener deleteListener,
                                               boolean isPinned,
                                               String currentUserId) {
        if (message.isRecalled()) {
            return;
        }

        PopupMenu popup = new PopupMenu(view.getContext(), view);

        // Add reply option
        popup.getMenu().add(0, 3, 0, "↩️ Trả lời");

        // Add forward option
        popup.getMenu().add(0, 5, 0, "↪️ Chuyển tiếp");

        // Add pin/unpin option
        if (isPinned) {
            popup.getMenu().add(0, 2, 0, "📌 Bỏ ghim tin nhắn");
        } else {
            popup.getMenu().add(0, 1, 0, "📌 Ghim tin nhắn");
        }

        // Add recall option (only for sender)
        if (message.canBeRecalled(currentUserId)) {
            popup.getMenu().add(0, 4, 0, "🔄 Thu hồi");
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1 && listener != null) {
                listener.onPinMessage(message);
                return true;
            } else if (itemId == 2 && listener != null) {
                listener.onUnpinMessage(message);
                return true;
            } else if (itemId == 3 && replyListener != null) {
                replyListener.onReplyMessage(message);
                return true;
            } else if (itemId == 4 && recallListener != null) {
                recallListener.onRecallMessage(message);
                return true;
            } else if (itemId == 5 && forwardListener != null) {
                forwardListener.onForwardMessage(message);
                return true;
            }
            return false;
        });

        popup.show();
    }
    
    /**
     * Helper method to bind reaction indicator
     */
    private static void bindReactionIndicator(View itemView, Message message,
                                              String currentUserId,
                                              MessageAdapter.OnMessageReactionListener reactionListener) {
        View reactionContainer = itemView.findViewById(R.id.reactionContainer);
        View existingReactionsLayout = itemView.findViewById(R.id.existingReactionsLayout);
        ImageView addReactionButton = itemView.findViewById(R.id.addReactionButton);

        if (reactionContainer == null || addReactionButton == null) {
            return; // Layout doesn't have reaction views
        }

        reactionContainer.setVisibility(View.VISIBLE);
        addReactionButton.setVisibility(View.VISIBLE);

        Map<String, String> reactions = message.getReactions();
        Map<String, Integer> reactionCounts = MessageReaction.getReactionTypeCounts(
                reactions, message.getReactionCounts());
        String userReaction = message.getUserReaction(currentUserId);

        boolean hasReactions = message.hasReactions();

        List<String> topReactions;
        if (message.getReactionCounts() != null && !message.getReactionCounts().isEmpty()) {
            topReactions = MessageReaction.getTopReactionsFromCounts(message.getReactionCounts(), 2);
        } else {
            topReactions = MessageReaction.getTopReactions(reactions, 2);
        }

        boolean hasValidReactions = false;
        for (String reactionType : topReactions) {
            Integer count = reactionCounts.get(reactionType);
            if (count != null && count > 0) {
                hasValidReactions = true;
                break;
            }
        }

        if (hasReactions && hasValidReactions && existingReactionsLayout != null) {
            existingReactionsLayout.setVisibility(View.VISIBLE);
            hideAllReactionLayouts(itemView);

            for (String reactionType : topReactions) {
                int layoutId = getReactionLayoutId(reactionType);
                int countTextId = getReactionCountTextId(reactionType);
                Integer count = reactionCounts.get(reactionType);
                if (layoutId != 0 && countTextId != 0 && count != null && count > 0) {
                    bindSingleReaction(itemView, layoutId, countTextId, count);
                }
            }

            existingReactionsLayout.setOnClickListener(v -> {
                if (reactionListener != null) {
                    reactionListener.onReactionStatsClick(message);
                }
            });
        } else if (existingReactionsLayout != null) {
            existingReactionsLayout.setVisibility(View.GONE);
        }

        // Update add reaction button icon based on user's reaction
        if (userReaction != null) {
            int iconRes = getReactionIconResource(userReaction);
            addReactionButton.setImageResource(iconRes);
        } else {
            addReactionButton.setImageResource(R.drawable.ic_reaction_heart_outline);
        }

        // Click on add reaction button
        addReactionButton.setOnClickListener(v -> {
            if (reactionListener != null) {
                String currentUserReaction = message.getUserReaction(currentUserId);
                String reactionToAdd = currentUserReaction != null
                        ? currentUserReaction
                        : MessageReaction.REACTION_HEART;
                reactionListener.onReactionClick(message, reactionToAdd);
            }
        });

        // Long press for reaction picker
        addReactionButton.setOnLongClickListener(v -> {
            if (reactionListener != null) {
                reactionListener.onReactionLongPress(message, v);
                return true;
            }
            return false;
        });
    }
    
    private static void bindSingleReaction(View itemView, int layoutId, int countTextId, int count) {
        View layout = itemView.findViewById(layoutId);
        TextView countText = itemView.findViewById(countTextId);
        if (layout == null) return;
        
        if (count > 0) {
            layout.setVisibility(View.VISIBLE);
            if (countText != null) {
                countText.setText(String.valueOf(count));
            }
        } else {
            layout.setVisibility(View.GONE);
        }
    }
    
    private static void hideAllReactionLayouts(View itemView) {
        int[] layoutIds = {
                R.id.heartReactionLayout,
                R.id.likeReactionLayout,
                R.id.hahaReactionLayout,
                R.id.sadReactionLayout,
                R.id.angryReactionLayout,
                R.id.wowReactionLayout
        };
        
        for (int layoutId : layoutIds) {
            View layout = itemView.findViewById(layoutId);
            if (layout != null) {
                layout.setVisibility(View.GONE);
            }
        }
    }
    
    private static int getReactionIconResource(String reactionType) {
        if (reactionType == null) return R.drawable.ic_reaction_heart_outline;
        switch (reactionType) {
            case MessageReaction.REACTION_HEART: return R.drawable.ic_reaction_heart;
            case MessageReaction.REACTION_HAHA: return R.drawable.ic_reaction_haha;
            case MessageReaction.REACTION_SAD: return R.drawable.ic_reaction_sad;
            case MessageReaction.REACTION_ANGRY: return R.drawable.ic_reaction_angry;
            case MessageReaction.REACTION_WOW: return R.drawable.ic_reaction_wow;
            case MessageReaction.REACTION_LIKE: return R.drawable.ic_reaction_like;
            default: return R.drawable.ic_reaction_heart_outline;
        }
    }
    
    private static int getReactionLayoutId(String reactionType) {
        if (reactionType == null) return 0;
        switch (reactionType) {
            case MessageReaction.REACTION_HEART: return R.id.heartReactionLayout;
            case MessageReaction.REACTION_LIKE: return R.id.likeReactionLayout;
            case MessageReaction.REACTION_HAHA: return R.id.hahaReactionLayout;
            case MessageReaction.REACTION_SAD: return R.id.sadReactionLayout;
            case MessageReaction.REACTION_ANGRY: return R.id.angryReactionLayout;
            case MessageReaction.REACTION_WOW: return R.id.wowReactionLayout;
            default: return 0;
        }
    }
    
    private static int getReactionCountTextId(String reactionType) {
        if (reactionType == null) return 0;
        switch (reactionType) {
            case MessageReaction.REACTION_HEART: return R.id.heartReactionCount;
            case MessageReaction.REACTION_LIKE: return R.id.likeReactionCount;
            case MessageReaction.REACTION_HAHA: return R.id.hahaReactionCount;
            case MessageReaction.REACTION_SAD: return R.id.sadReactionCount;
            case MessageReaction.REACTION_ANGRY: return R.id.angryReactionCount;
            case MessageReaction.REACTION_WOW: return R.id.wowReactionCount;
            default: return 0;
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
