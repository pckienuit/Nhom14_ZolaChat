package com.example.doan_zaloclone.ui.room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Sticker;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for sticker grid in the picker
 * Displays stickers in a 4-column grid
 */
public class StickerGridAdapter extends RecyclerView.Adapter<StickerGridAdapter.StickerViewHolder> {

    private List<Sticker> stickers = new ArrayList<>();
    private OnStickerClickListener listener;

    public void setOnStickerClickListener(OnStickerClickListener listener) {
        this.listener = listener;
    }

    public void setStickers(List<Sticker> stickers) {
        if (stickers == null) {
            this.stickers = new ArrayList<>();
        } else {
            // Filter invalid stickers
            this.stickers = new ArrayList<>();
            for (Sticker s : stickers) {
                if (s.getId() != null && s.getImageUrl() != null && !s.getImageUrl().isEmpty()) {
                    this.stickers.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StickerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sticker_grid, parent, false);
        return new StickerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StickerViewHolder holder, int position) {
        Sticker sticker = stickers.get(position);
        holder.bind(sticker);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStickerClick(sticker);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onStickerLongClick(sticker);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return stickers.size();
    }

    public interface OnStickerClickListener {
        void onStickerClick(Sticker sticker);

        void onStickerLongClick(Sticker sticker); // For preview
    }

    static class StickerViewHolder extends RecyclerView.ViewHolder {
        private final ImageView stickerImage;

        public StickerViewHolder(@NonNull View itemView) {
            super(itemView);
            stickerImage = itemView.findViewById(R.id.stickerImage);
        }

        public void bind(Sticker sticker) {
            String imageUrl = sticker.getThumbnailUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = sticker.getImageUrl();
            }

            // Debug log
            android.util.Log.d("StickerGridAdapter", "Loading sticker - ID: " + sticker.getId()
                    + ", imageUrl: " + imageUrl
                    + ", thumbnailUrl: " + sticker.getThumbnailUrl());

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_sticker)
                        .error(R.drawable.ic_sticker)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .fitCenter()
                        .into(stickerImage);
            } else {
                stickerImage.setImageResource(R.drawable.ic_sticker);
            }
        }
    }
}
