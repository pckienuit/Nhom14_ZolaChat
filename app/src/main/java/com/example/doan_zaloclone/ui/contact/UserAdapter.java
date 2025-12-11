package com.example.doan_zaloclone.ui.contact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private OnUserActionListener listener;
    private Map<String, String> friendRequestStatus = new HashMap<>();

    public interface OnUserActionListener {
        void onUserClick(User user);
        void onAddFriendClick(User user);
    }

    public UserAdapter(List<User> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        String status = friendRequestStatus.get(user.getId());
        holder.bind(user, status);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    public void updateFriendRequestStatus(String userId, String status) {
        friendRequestStatus.put(userId, status);
        notifyDataSetChanged();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private TextView userAvatar;
        private TextView userName;
        private TextView userEmail;
        private Button addFriendButton;
        private TextView statusBadge;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userAvatar = itemView.findViewById(R.id.userAvatar);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
            addFriendButton = itemView.findViewById(R.id.addFriendButton);
            statusBadge = itemView.findViewById(R.id.statusBadge);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUserClick(users.get(position));
                }
            });

            addFriendButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAddFriendClick(users.get(position));
                }
            });
        }

        public void bind(User user, String status) {
            userName.setText(user.getName());
            userEmail.setText(user.getEmail());
            
            // Set avatar
            if (user.getName() != null && !user.getName().isEmpty()) {
                userAvatar.setText(String.valueOf(user.getName().charAt(0)).toUpperCase());
            } else {
                userAvatar.setText("U");
            }

            // Update UI based on friend request status
            if (status == null || "NONE".equals(status) || "REJECTED".equals(status)) {
                // Show Add Friend button
                addFriendButton.setVisibility(View.VISIBLE);
                statusBadge.setVisibility(View.GONE);
                addFriendButton.setEnabled(true);
            } else if ("PENDING".equals(status)) {
                // Show Pending badge
                addFriendButton.setVisibility(View.GONE);
                statusBadge.setVisibility(View.VISIBLE);
                statusBadge.setText("Pending");
            } else if ("ACCEPTED".equals(status)) {
                // Show Friends badge
                addFriendButton.setVisibility(View.GONE);
                statusBadge.setVisibility(View.VISIBLE);
                statusBadge.setText("Friends");
            }
        }
    }
}
