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

    private final OnActionClickListener listener;
    private List<QuickAction> actions;

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

    public interface OnActionClickListener {
        void onActionClick(QuickAction action);
    }

    class ActionViewHolder extends RecyclerView.ViewHolder {
        ImageView actionIcon;
        TextView actionLabel;
        View actionBackground;

        ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            actionIcon = itemView.findViewById(R.id.actionIcon);
            actionLabel = itemView.findViewById(R.id.actionLabel);
            actionBackground = itemView.findViewById(R.id.actionBackground);
        }

        void bind(QuickAction action) {
            actionIcon.setImageResource(action.getIconResId());
            actionLabel.setText(action.getLabel());

            // Set gradient background
            if (actionBackground != null) {
                // Determine color
                int color = action.getBackgroundColor();
                // Default to blue if 0
                if (color == 0) color = android.graphics.Color.parseColor("#2196F3");
                
                // Create lighter variant for gradient start
                float[] hsv = new float[3];
                android.graphics.Color.colorToHSV(color, hsv);
                hsv[1] = Math.max(0, hsv[1] - 0.3f); // reduce saturation significantly
                hsv[2] = Math.min(1, hsv[2] + 0.3f); // increase brightness
                int lighterColor = android.graphics.Color.HSVToColor(hsv);
                
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                        new int[] { lighterColor, color });
                gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                
                actionBackground.setBackground(gd);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionClick(action);
                }
            });
        }
    }
}
