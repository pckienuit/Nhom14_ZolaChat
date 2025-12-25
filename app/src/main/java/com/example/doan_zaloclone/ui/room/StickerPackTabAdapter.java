package com.example.doan_zaloclone.ui.room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.StickerPack;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for sticker pack tabs in the picker
 * Displays pack icons horizontally
 */
public class StickerPackTabAdapter extends RecyclerView.Adapter<StickerPackTabAdapter.PackTabViewHolder> {

    private List<StickerPack> packs = new ArrayList<>();
    private int selectedPosition = 0;
    private OnPackSelectedListener listener;

    public void setOnPackSelectedListener(OnPackSelectedListener listener) {
        this.listener = listener;
    }

    public void setPacks(List<StickerPack> packs) {
        this.packs = packs != null ? packs : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = this.selectedPosition;
        this.selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public PackTabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sticker_pack_tab, parent, false);
        return new PackTabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackTabViewHolder holder, int position) {
        StickerPack pack = packs.get(position);
        holder.bind(pack, position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                setSelectedPosition(adapterPosition);
                if (listener != null) {
                    listener.onPackSelected(pack, adapterPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return packs.size();
    }

    public interface OnPackSelectedListener {
        void onPackSelected(StickerPack pack, int position);
    }

    static class PackTabViewHolder extends RecyclerView.ViewHolder {
        private final ImageView packIcon;
        private final View selectionIndicator;

        public PackTabViewHolder(@NonNull View itemView) {
            super(itemView);
            packIcon = itemView.findViewById(R.id.packIcon);
            selectionIndicator = itemView.findViewById(R.id.selectionIndicator);
        }

        public void bind(StickerPack pack, boolean isSelected) {
            // Load pack icon
            if (pack.getIconUrl() != null && !pack.getIconUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(pack.getIconUrl())
                        .placeholder(R.drawable.ic_sticker)
                        .error(R.drawable.ic_sticker)
                        .centerCrop()
                        .into(packIcon);
            } else {
                packIcon.setImageResource(R.drawable.ic_sticker);
            }

            // Show/hide selection indicator
            selectionIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }
    }
}
