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

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private List<User> friends;
    private OnFriendClickListener listener;

    public interface OnFriendClickListener {
        void onMessageClick(User friend);
    }

    public FriendsAdapter(List<User> friends, OnFriendClickListener listener) {
        this.friends = friends;
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
        this.friends = newFriends;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView friendAvatar;
        private TextView friendName;
        private TextView friendEmail;
        private Button messageButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            friendAvatar = itemView.findViewById(R.id.friendAvatar);
            friendName = itemView.findViewById(R.id.friendName);
            friendEmail = itemView.findViewById(R.id.friendEmail);
            messageButton = itemView.findViewById(R.id.messageButton);

            messageButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageClick(friends.get(position));
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
