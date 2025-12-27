package com.example.doan_zaloclone.ui.file;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.SenderInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for sender selection dialog
 */
public class SenderFilterAdapter extends RecyclerView.Adapter<SenderFilterAdapter.SenderViewHolder> {

    private List<SenderInfo> senders = new ArrayList<>();
    private Set<String> selectedSenderIds = new HashSet<>();

    public SenderFilterAdapter() {
    }

    @NonNull
    @Override
    public SenderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sender_filter, parent, false);
        return new SenderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SenderViewHolder holder, int position) {
        SenderInfo sender = senders.get(position);
        holder.bind(sender);
    }

    @Override
    public int getItemCount() {
        return senders.size();
    }

    public void setSenders(List<SenderInfo> senders) {
        this.senders = senders != null ? senders : new ArrayList<>();
        notifyDataSetChanged();
    }

    public Set<String> getSelectedSenders() {
        return new HashSet<>(selectedSenderIds);
    }

    public void setSelectedSenders(Set<String> selectedIds) {
        this.selectedSenderIds = selectedIds != null ? new HashSet<>(selectedIds) : new HashSet<>();
        notifyDataSetChanged();
    }

    class SenderViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBox;
        private final ImageView avatar;
        private final TextView nameText;
        private final TextView countText;

        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.senderCheckBox);
            avatar = itemView.findViewById(R.id.senderAvatar);
            nameText = itemView.findViewById(R.id.senderName);
            countText = itemView.findViewById(R.id.fileCount);
        }

        public void bind(SenderInfo sender) {
            nameText.setText(sender.getSenderName());
            countText.setText(itemView.getContext().getString(R.string.files_count, sender.getFileCount()));

            // Load avatar
            if (sender.getSenderAvatarUrl() != null && !sender.getSenderAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(sender.getSenderAvatarUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(avatar);
            } else {
                avatar.setImageResource(R.drawable.ic_person);
            }

            // Set checkbox state
            boolean isSelected = selectedSenderIds.contains(sender.getSenderId());
            checkBox.setChecked(isSelected);

            // Handle checkbox clicks
            checkBox.setOnClickListener(v -> {
                if (checkBox.isChecked()) {
                    selectedSenderIds.add(sender.getSenderId());
                } else {
                    selectedSenderIds.remove(sender.getSenderId());
                }
            });

            // Handle item clicks
            itemView.setOnClickListener(v -> {
                checkBox.setChecked(!checkBox.isChecked());
                if (checkBox.isChecked()) {
                    selectedSenderIds.add(sender.getSenderId());
                } else {
                    selectedSenderIds.remove(sender.getSenderId());
                }
            });
        }
    }
}
