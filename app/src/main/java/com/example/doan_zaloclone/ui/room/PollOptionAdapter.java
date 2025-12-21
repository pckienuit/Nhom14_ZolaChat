package com.example.doan_zaloclone.ui.room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        
        OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            selectionIndicator = itemView.findViewById(R.id.selectionIndicator);
            optionText = itemView.findViewById(R.id.optionText);
            progressContainer = itemView.findViewById(R.id.progressContainer);
            progressBar = itemView.findViewById(R.id.progressBar);
            percentageText = itemView.findViewById(R.id.percentageText);
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
    }
}
