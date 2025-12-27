package com.example.doan_zaloclone.ui.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.GroupMember;

import java.util.ArrayList;
import java.util.List;

public class GroupMemberAdapter extends RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder> {

    private final String currentUserId;
    private final boolean isCurrentUserAdmin;
    private final OnMemberActionListener listener;
    private List<GroupMember> members = new ArrayList<>();

    public GroupMemberAdapter(String currentUserId, boolean isCurrentUserAdmin, OnMemberActionListener listener) {
        this.currentUserId = currentUserId;
        this.isCurrentUserAdmin = isCurrentUserAdmin;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_member, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        GroupMember member = members.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public void updateMembers(List<GroupMember> newMembers) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return members.size();
            }

            @Override
            public int getNewListSize() {
                return newMembers.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return members.get(oldItemPosition).getUserId()
                        .equals(newMembers.get(newItemPosition).getUserId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                GroupMember oldMember = members.get(oldItemPosition);
                GroupMember newMember = newMembers.get(newItemPosition);
                return oldMember.getUserId().equals(newMember.getUserId()) &&
                        oldMember.getName().equals(newMember.getName()) &&
                        oldMember.isAdmin() == newMember.isAdmin();
            }
        });

        members = new ArrayList<>(newMembers);
        diffResult.dispatchUpdatesTo(this);
    }

    public interface OnMemberActionListener {
        void onRemoveMember(GroupMember member);

        void onMemberLongClick(GroupMember member);
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarImageView;
        private final TextView nameTextView;
        private final TextView emailTextView;
        private final TextView adminBadgeTextView;
        private final ImageView removeButton;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            emailTextView = itemView.findViewById(R.id.emailTextView);
            adminBadgeTextView = itemView.findViewById(R.id.adminBadgeTextView);
            removeButton = itemView.findViewById(R.id.removeButton);
        }

        public void bind(GroupMember member) {
            nameTextView.setText(member.getName());
            emailTextView.setText(member.getEmail());

            // Show admin badge
            if (member.isAdmin()) {
                adminBadgeTextView.setVisibility(View.VISIBLE);
            } else {
                adminBadgeTextView.setVisibility(View.GONE);
            }

            // Load avatar
            if (member.getAvatarUrl() != null && !member.getAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(member.getAvatarUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(avatarImageView);
            } else {
                avatarImageView.setImageResource(R.drawable.ic_person);
            }

            // Click avatar to view profile
            avatarImageView.setOnClickListener(v -> {
                com.example.doan_zaloclone.utils.ProfileNavigator.openUserProfile(
                        itemView.getContext(), member.getUserId());
            });

            // Long click for admin transfer
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onMemberLongClick(member);
                    return true;
                }
                return false;
            });

            // Show remove button only if:
            // 1. Current user is admin
            // 2. The member is not the admin
            // 3. The member is not the current user (can't remove self)
            if (isCurrentUserAdmin && !member.isAdmin() && !member.getUserId().equals(currentUserId)) {
                removeButton.setVisibility(View.VISIBLE);
                removeButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRemoveMember(member);
                    }
                });
            } else {
                removeButton.setVisibility(View.GONE);
            }
        }
    }
}
