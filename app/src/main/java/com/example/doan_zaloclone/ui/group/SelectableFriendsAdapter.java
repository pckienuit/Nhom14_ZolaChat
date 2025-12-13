package com.example.doan_zaloclone.ui.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for displaying friends with checkboxes for multi-selection
 */
public class SelectableFriendsAdapter extends RecyclerView.Adapter<SelectableFriendsAdapter.ViewHolder> {

    private List<User> friends;
    private Set<String> selectedFriendIds;
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public SelectableFriendsAdapter(List<User> friends, OnSelectionChangedListener listener) {
        this.friends = friends;
        this.selectedFriendIds = new HashSet<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selectable_friend, parent, false);
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

    /**
     * Get list of selected friend IDs
     */
    public List<String> getSelectedFriendIds() {
        return new ArrayList<>(selectedFriendIds);
    }

    /**
     * Update friends list
     */
    public void updateFriends(List<User> newFriends) {
        this.friends = newFriends;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private CheckBox friendCheckBox;
        private TextView friendNameTextView;
        private TextView friendEmailTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            friendCheckBox = itemView.findViewById(R.id.friendCheckBox);
            friendNameTextView = itemView.findViewById(R.id.friendNameTextView);
            friendEmailTextView = itemView.findViewById(R.id.friendEmailTextView);
        }

        public void bind(User friend) {
            friendNameTextView.setText(friend.getName());
            friendEmailTextView.setText(friend.getEmail());

            // Set checkbox state
            boolean isSelected = selectedFriendIds.contains(friend.getId());
            friendCheckBox.setChecked(isSelected);

            // Handle checkbox clicks
            friendCheckBox.setOnClickListener(v -> {
                if (friendCheckBox.isChecked()) {
                    selectedFriendIds.add(friend.getId());
                } else {
                    selectedFriendIds.remove(friend.getId());
                }
                
                if (listener != null) {
                    listener.onSelectionChanged(selectedFriendIds.size());
                }
            });

            // Handle item clicks (toggle checkbox)
            itemView.setOnClickListener(v -> {
                friendCheckBox.setChecked(!friendCheckBox.isChecked());
                
                if (friendCheckBox.isChecked()) {
                    selectedFriendIds.add(friend.getId());
                } else {
                    selectedFriendIds.remove(friend.getId());
                }
                
                if (listener != null) {
                    listener.onSelectionChanged(selectedFriendIds.size());
                }
            });
        }
    }
}
