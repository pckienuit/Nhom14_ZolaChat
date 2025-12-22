package com.example.doan_zaloclone.ui.room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Poll;
import com.example.doan_zaloclone.models.PollOption;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying poll options with voting UI
 */
public class PollOptionAdapter extends RecyclerView.Adapter<PollOptionAdapter.OptionViewHolder> {
    
    private List<PollOption> options;
    private Poll poll;
    private String currentUserId;
    private OnOptionClickListener listener;
    
    public interface OnOptionClickListener {
        void onOptionClick(int optionIndex, PollOption option);
    }
    
    public PollOptionAdapter(Poll poll, String currentUserId, OnOptionClickListener listener) {
        this.poll = poll;
        this.options = poll != null ? poll.getOptions() : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poll_option, parent, false);
        return new OptionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        holder.bind(options.get(position), position);
    }
    
    @Override
    public int getItemCount() {
        return options.size();
    }
    
    /**
     * Update poll data (called when poll updates from Firestore)
     */
    public void updatePoll(Poll newPoll) {
        this.poll = newPoll;
        this.options = newPoll != null ? newPoll.getOptions() : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    class OptionViewHolder extends RecyclerView.ViewHolder {
        private View selectionIndicator;
        private TextView optionText;
        private View progressContainer;
        private ProgressBar progressBar;
        private TextView percentageText;
        private LinearLayout voterAvatarsContainer; // Added field
        
        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            selectionIndicator = itemView.findViewById(R.id.selectionIndicator);
            optionText = itemView.findViewById(R.id.optionText);
            progressContainer = itemView.findViewById(R.id.progressContainer);
            progressBar = itemView.findViewById(R.id.progressBar);
            percentageText = itemView.findViewById(R.id.percentageText);
            voterAvatarsContainer = itemView.findViewById(R.id.voterAvatarsContainer); // Initialized field
        }
        
        void bind(PollOption option, int position) {
            // Set option text
            optionText.setText(option.getText());
            
            // Check if user voted for this option
            boolean userVoted = option.hasUserVoted(currentUserId);
            boolean pollHasUserVote = poll.hasUserVoted(currentUserId);
            
            // Update selection indicator
            if (userVoted) {
                selectionIndicator.setBackgroundResource(R.drawable.bg_poll_option_checked);
            } else {
                selectionIndicator.setBackgroundResource(R.drawable.bg_poll_option_unchecked);
            }
            
            // Show/hide results based on settings
            boolean showResults = !poll.isHideResultsUntilVoted() || pollHasUserVote || poll.isClosed();
            
            if (showResults) {
                progressContainer.setVisibility(View.VISIBLE);
                
                // Calculate percentage
                int totalVotes = poll.getTotalVotes();
                int optionVotes = option.getVoteCount();
                int percentage = totalVotes > 0 ? (optionVotes * 100 / totalVotes) : 0;
                
                // Update progress bar
                progressBar.setProgress(percentage);
                percentageText.setText(percentage + "% (" + optionVotes + ")");
            } else {
                progressContainer.setVisibility(View.GONE);
            }
            
            // Show voter avatars (only if not anonymous)
            if (voterAvatarsContainer != null) {
                if (!poll.isAnonymous() && showResults && option.getVoteCount() > 0) {
                    voterAvatarsContainer.setVisibility(View.VISIBLE);
                    renderVoterAvatars(option);
                } else {
                    voterAvatarsContainer.setVisibility(View.GONE);
                }
            }
            
            // Handle click (only if poll is open)
            if (poll.canVote()) {
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onOptionClick(position, option);
                    }
                });
                itemView.setEnabled(true);
                itemView.setAlpha(1.0f);
            } else {
                itemView.setOnClickListener(null);
                itemView.setEnabled(false);
                itemView.setAlpha(0.6f);
            }
        }
        
        /**
         * Render voter avatars (max 3 + overflow indicator)
         */
        private void renderVoterAvatars(PollOption option) {
            voterAvatarsContainer.removeAllViews();
            
            List<String> voterIds = option.getVoterIds();
            if (voterIds == null || voterIds.isEmpty()) return;
            
            int maxDisplay = 3;
            int displayCount = Math.min(voterIds.size(), maxDisplay);
            
            for (int i = 0; i < displayCount; i++) {
                if (i == maxDisplay - 1 && voterIds.size() > maxDisplay) {
                    // Show "+N" indicator
                    android.widget.TextView overflowText = createOverflowIndicator(voterIds.size() - (maxDisplay - 1));
                    voterAvatarsContainer.addView(overflowText);
                    break;
                } else {
                    // Show avatar placeholder (will be replaced with real avatars later)
                    android.view.View avatarView = createAvatarPlaceholder(voterIds.get(i));
                    voterAvatarsContainer.addView(avatarView);
                }
            }
            
            // Click to show voter list
            voterAvatarsContainer.setOnClickListener(v -> showVoterListDialog(option));
        }
        
        /**
         * Create circular avatar placeholder
         */
        private android.view.View createAvatarPlaceholder(String userId) {
            android.widget.TextView avatar = new android.widget.TextView(itemView.getContext());
            
            int size = (int) (24 * itemView.getContext().getResources().getDisplayMetrics().density);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(size, size);
            params.setMarginEnd((int) (2 * itemView.getContext().getResources().getDisplayMetrics().density));
            avatar.setLayoutParams(params);
            
            // Circular background
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(0xFF0068FF);
            avatar.setBackground(drawable);
            
            // First letter of user ID as placeholder
            String initial = userId.substring(0, Math.min(1, userId.length())).toUpperCase();
            avatar.setText(initial);
            avatar.setTextColor(0xFFFFFFFF);
            avatar.setTextSize(10);
            avatar.setGravity(android.view.Gravity.CENTER);
            
            return avatar;
        }
        
        /**
         * Create "+N" overflow indicator
         */
        private android.widget.TextView createOverflowIndicator(int count) {
            android.widget.TextView indicator = new android.widget.TextView(itemView.getContext());
            
            int size = (int) (24 * itemView.getContext().getResources().getDisplayMetrics().density);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(size, size);
            indicator.setLayoutParams(params);
            
            // Circular background
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(0xFFE0E0E0);
            indicator.setBackground(drawable);
            
            indicator.setText("+" + count);
            indicator.setTextColor(0xFF666666);
            indicator.setTextSize(9);
            indicator.setGravity(android.view.Gravity.CENTER);
            
            return indicator;
        }
        
        /**
         * Show dialog with full voter list (using stored names)
         */
        private void showVoterListDialog(PollOption option) {
            List<String> voterIds = option.getVoterIds();
            if (voterIds == null || voterIds.isEmpty()) return;
            
            // Build voter list using stored names (no Firestore fetch needed)
            StringBuilder voterList = new StringBuilder();
            voterList.append("Đã chọn \"").append(option.getText()).append("\":\n\n");
            
            for (String voterId : voterIds) {
                String name = option.getVoterName(voterId);
                voterList.append("• ").append(name).append("\n");
            }
            
            new android.app.AlertDialog.Builder(itemView.getContext())
                    .setTitle(option.getVoteCount() + " người đã bình chọn")
                    .setMessage(voterList.toString())
                    .setPositiveButton("Đóng", null)
                    .show();
        }
    }
}
