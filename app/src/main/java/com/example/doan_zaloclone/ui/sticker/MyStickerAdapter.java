package com.example.doan_zaloclone.ui.sticker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Sticker;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying user's personal stickers in MyStickerActivity
 */
public class MyStickerAdapter extends RecyclerView.Adapter<MyStickerAdapter.ViewHolder> {

    private List<Sticker> stickers;
    private OnStickerClickListener listener;
    private boolean editMode = false;

    public interface OnStickerClickListener {
        void onStickerClick(Sticker sticker);
        void onStickerLongClick(Sticker sticker);
        void onDeleteClick(Sticker sticker);
    }

    public MyStickerAdapter() {
        this.stickers = new ArrayList<>();
    }

    public void setListener(OnStickerClickListener listener) {
        this.listener = listener;
    }

    public void updateStickers(List<Sticker> newStickers) {
        this.stickers = newStickers != null ? newStickers : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return editMode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_sticker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sticker sticker = stickers.get(position);
        holder.bind(sticker);
    }

    @Override
    public int getItemCount() {
        return stickers.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSticker;
        ImageView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSticker = itemView.findViewById(R.id.imgSticker);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // Click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null && !editMode) {
                    listener.onStickerClick(stickers.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onStickerLongClick(stickers.get(position));
                    return true;
                }
                return false;
            });

            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDeleteClick(stickers.get(position));
                }
            });
        }

        void bind(Sticker sticker) {
            // Show/hide delete button based on edit mode
            btnDelete.setVisibility(editMode ? View.VISIBLE : View.GONE);

            // Load sticker image
            Glide.with(itemView.getContext())
                    .load(sticker.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_sticker)
                    .error(R.drawable.ic_sticker)
                    .into(imgSticker);
        }
    }
}
