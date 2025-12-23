package com.example.doan_zaloclone.ui.sticker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.StickerPack;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying sticker packs in the store
 */
public class StickerPackStoreAdapter extends RecyclerView.Adapter<StickerPackStoreAdapter.ViewHolder> {
    
    private List<StickerPack> packs = new ArrayList<>();
    private OnPackActionListener listener;
    
    public interface OnPackActionListener {
        void onDownloadPack(StickerPack pack);
        void onPackClick(StickerPack pack);
    }
    
    public StickerPackStoreAdapter(OnPackActionListener listener) {
        this.listener = listener;
    }
    
    public void setPacks(List<StickerPack> packs) {
        this.packs = packs != null ? packs : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_store_sticker_pack, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(packs.get(position));
    }
    
    @Override
    public int getItemCount() {
        return packs.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView packIcon;
        private TextView packName;
        private TextView stickerCount;
        private TextView downloadCount;
        private MaterialButton btnDownload;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            packIcon = itemView.findViewById(R.id.packIcon);
            packName = itemView.findViewById(R.id.packName);
            stickerCount = itemView.findViewById(R.id.stickerCount);
            downloadCount = itemView.findViewById(R.id.downloadCount);
            btnDownload = itemView.findViewById(R.id.btnDownload);
        }
        
        public void bind(StickerPack pack) {
            packName.setText(pack.getName());
            stickerCount.setText(pack.getStickerCount() + " stickers");
            
            // Format download count
            long downloads = pack.getDownloadCount();
            String downloadText;
            if (downloads >= 1000) {
                downloadText = "↓ " + String.format("%.1fK", downloads / 1000.0);
            } else {
                downloadText = "↓ " + downloads;
            }
            downloadCount.setText(downloadText);
            
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
            
            // Download button
            btnDownload.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDownloadPack(pack);
                }
            });
            
            // Pack click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPackClick(pack);
                }
            });
        }
    }
}
