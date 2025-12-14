package com.example.doan_zaloclone.ui.room.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for quick actions menu
 */
public class QuickActionAdapter extends RecyclerView.Adapter<QuickActionAdapter.ActionViewHolder> {

    private List<QuickAction> actions;
    private OnActionClickListener listener;

    public interface OnActionClickListener {
        void onActionClick(QuickAction action);
    }

    public QuickActionAdapter(List<QuickAction> actions, OnActionClickListener listener) {
        this.actions = actions != null ? actions : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_action, parent, false);
        return new ActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        QuickAction action = actions.get(position);
        holder.bind(action);
    }

    @Override
    public int getItemCount() {
        return actions.size();
    }

    public void updateActions(List<QuickAction> newActions) {
        this.actions = newActions != null ? newActions : new ArrayList<>();
        notifyDataSetChanged();
    }

    class ActionViewHolder extends RecyclerView.ViewHolder {
        ImageView actionIcon;
        TextView actionLabel;

        ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            actionIcon = itemView.findViewById(R.id.actionIcon);
            actionLabel = itemView.findViewById(R.id.actionLabel);
        }

        void bind(QuickAction action) {
            actionIcon.setImageResource(action.getIconResId());
            actionLabel.setText(action.getLabel());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionClick(action);
                }
            });
        }
    }
}
