package com.example.doan_zaloclone.ui.contact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;

import java.util.ArrayList;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private final OnFriendClickListener listener;
    private List<User> friends;

    public FriendsAdapter(List<User> friends, OnFriendClickListener listener) {
        this.friends = friends != null ? friends : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(friends.get(position));
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public void updateFriends(List<User> newFriends) {
        if (newFriends == null) newFriends = new ArrayList<>();

        android.util.Log.d("FriendsAdapter", "updateFriends - Old size: " + this.friends.size() +
                ", New size: " + newFriends.size());

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new FriendsDiffCallback(this.friends, newFriends));
        this.friends = new ArrayList<>(newFriends); // Create new copy
        diffResult.dispatchUpdatesTo(this);

        android.util.Log.d("FriendsAdapter", "updateFriends completed - Current size: " + this.friends.size());
    }

    public interface OnFriendClickListener {
        void onMessageClick(User friend);

        void onUnfriendClick(User friend);
    }

    // DiffUtil Callback for efficient list updates
    private static class FriendsDiffCallback extends DiffUtil.Callback {
        private final List<User> oldList;
        private final List<User> newList;

        public FriendsDiffCallback(List<User> oldList, List<User> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            User oldUser = oldList.get(oldItemPosition);
            User newUser = newList.get(newItemPosition);

            // Null safety check
            if (oldUser == null || newUser == null) {
                return false;
            }
            if (oldUser.getId() == null || newUser.getId() == null) {
                return false;
            }

            return oldUser.getId().equals(newUser.getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            User oldUser = oldList.get(oldItemPosition);
            User newUser = newList.get(newItemPosition);

            // Null safety check
            if (oldUser == null || newUser == null) {
                return false;
            }

            return oldUser.equals(newUser);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView friendAvatar;
        private final TextView friendName;
        private final TextView friendEmail;
        private final Button messageButton;
        private final Button unfriendButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            friendAvatar = itemView.findViewById(R.id.friendAvatar);
            friendName = itemView.findViewById(R.id.friendName);
            friendEmail = itemView.findViewById(R.id.friendEmail);
            messageButton = itemView.findViewById(R.id.messageButton);
            unfriendButton = itemView.findViewById(R.id.unfriendButton);

            // Click avatar to view profile
            friendAvatar.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    User friend = friends.get(position);
                    com.example.doan_zaloclone.utils.ProfileNavigator.openUserProfile(
                            itemView.getContext(), friend.getId());
                }
            });

            messageButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageClick(friends.get(position));
                }
            });

            unfriendButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onUnfriendClick(friends.get(position));
                }
            });
        }

        public void bind(User friend) {
            friendName.setText(friend.getName());
            friendEmail.setText(friend.getEmail());

            if (friend.getName() != null && !friend.getName().isEmpty()) {
                friendAvatar.setText(String.valueOf(friend.getName().charAt(0)).toUpperCase());
            } else {
                friendAvatar.setText("F");
            }
        }
    }
}
