package com.example.doan_zaloclone.ui.contact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for search results in AddFriendActivity
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<User> users = new ArrayList<>();
    private final OnUserActionListener listener;
    private final String currentUserId;
    
    // Track button states and request IDs for each user
    private Map<String, String> buttonStates = new HashMap<>(); // userId -> button text
    private Map<String, String> requestIds = new HashMap<>(); // userId -> request ID

    public interface OnUserActionListener {
        void onAddFriendClick(User user);
        void onCancelRequestClick(String userId, String requestId);
    }

    public SearchResultAdapter(String currentUserId, OnUserActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * Update button state for a specific user
     */
    public void updateButtonState(String userId, String buttonText, String requestId) {
        buttonStates.put(userId, buttonText);
        if (requestId != null) {
            requestIds.put(userId, requestId);
        } else {
            requestIds.remove(userId);
        }
        
        // Find and notify item changed
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(userId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgAvatar;
        private final TextView tvName;
        private final TextView tvEmail;
        private final Button btnAddFriend;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            btnAddFriend = itemView.findViewById(R.id.btnAddFriend);
        }

        public void bind(User user) {
            tvName.setText(user.getName());
            tvEmail.setText(user.getEmail());

            // Load avatar
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getAvatarUrl())
                        .circleCrop()
                        .into(imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.ic_avatar);
            }

            // Get button state for this user
            String buttonText = buttonStates.get(user.getId());
            if (buttonText == null) {
                buttonText = "KẾT BẠN"; // Default
            }

            // Kiểm tra nếu là chính mình
            if (currentUserId.equals(user.getId())) {
                btnAddFriend.setText("CHÍNH BẠN");
                btnAddFriend.setEnabled(false);
                btnAddFriend.setBackgroundTintList(
                        itemView.getContext().getColorStateList(android.R.color.darker_gray));
                btnAddFriend.setOnClickListener(null);
            } else if (buttonText.equals("ĐÃ LÀ BẠN BÈ")) {
                btnAddFriend.setText(buttonText);
                btnAddFriend.setEnabled(false);
                btnAddFriend.setBackgroundTintList(
                        itemView.getContext().getColorStateList(android.R.color.darker_gray));
                btnAddFriend.setOnClickListener(null);
            } else if (buttonText.equals("THU HỒI")) {
                btnAddFriend.setText(buttonText);
                btnAddFriend.setEnabled(true);
                btnAddFriend.setBackgroundTintList(
                        itemView.getContext().getColorStateList(android.R.color.holo_red_light));
                
                btnAddFriend.setOnClickListener(v -> {
                    if (listener != null) {
                        String requestId = requestIds.get(user.getId());
                        listener.onCancelRequestClick(user.getId(), requestId);
                    }
                });
            } else {
                // KẾT BẠN
                btnAddFriend.setText(buttonText);
                btnAddFriend.setEnabled(true);
                btnAddFriend.setBackgroundTintList(
                        itemView.getContext().getColorStateList(R.color.colorPrimary));
                
                btnAddFriend.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onAddFriendClick(user);
                    }
                });
            }
        }
    }
}
