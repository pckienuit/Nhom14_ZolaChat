package com.example.doan_zaloclone.ui.room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Sticker;
import com.example.doan_zaloclone.models.StickerPack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for ViewPager2 to display sticker pages
 * Each page contains a grid of stickers from a pack
 */
public class StickerPagerAdapter extends RecyclerView.Adapter<StickerPagerAdapter.PageViewHolder> {

    private List<StickerPack> packs = new ArrayList<>();
    private Map<String, List<Sticker>> packStickersMap = new HashMap<>();
    private StickerGridAdapter.OnStickerClickListener stickerClickListener;

    // Special "Recent" pack has index 0
    private static final int POSITION_RECENT = 0;
    private List<Sticker> recentStickers = new ArrayList<>();
    private boolean hasRecentTab = true;

    public void setPacks(List<StickerPack> packs) {
        this.packs = packs != null ? packs : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setRecentStickers(List<Sticker> stickers) {
        this.recentStickers = stickers != null ? stickers : new ArrayList<>();
        if (hasRecentTab && getItemCount() > 0) {
            notifyItemChanged(POSITION_RECENT);
        }
    }

    public void setPackStickers(String packId, List<Sticker> stickers) {
        packStickersMap.put(packId, stickers != null ? stickers : new ArrayList<>());
        // Find position and notify
        for (int i = 0; i < packs.size(); i++) {
            if (packs.get(i).getId().equals(packId)) {
                int position = hasRecentTab ? i + 1 : i;
                notifyItemChanged(position);
                break;
            }
        }
    }

    public void setOnStickerClickListener(StickerGridAdapter.OnStickerClickListener listener) {
        this.stickerClickListener = listener;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.page_sticker_grid, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        List<Sticker> stickers;
        
        if (hasRecentTab && position == POSITION_RECENT) {
            // Recent stickers page
            stickers = recentStickers;
        } else {
            // Regular pack page
            int packIndex = hasRecentTab ? position - 1 : position;
            if (packIndex >= 0 && packIndex < packs.size()) {
                String packId = packs.get(packIndex).getId();
                stickers = packStickersMap.get(packId);
                if (stickers == null) {
                    stickers = new ArrayList<>();
                }
            } else {
                stickers = new ArrayList<>();
            }
        }
        
        holder.bind(stickers, stickerClickListener);
    }

    @Override
    public int getItemCount() {
        int count = packs.size();
        if (hasRecentTab) {
            count++; // +1 for recent stickers page
        }
        return count;
    }

    /**
     * Get pack at pager position (accounting for Recent tab)
     */
    public StickerPack getPackAtPosition(int position) {
        if (hasRecentTab) {
            if (position == POSITION_RECENT) {
                return null; // Recent tab doesn't have a pack
            }
            position--; // Adjust for Recent tab
        }
        
        if (position >= 0 && position < packs.size()) {
            return packs.get(position);
        }
        return null;
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        private final RecyclerView stickerGrid;
        private final StickerGridAdapter adapter;

        public PageViewHolder(@NonNull View itemView) {
            super(itemView);
            stickerGrid = itemView.findViewById(R.id.stickerGridRecycler);
            
            // Setup grid
            adapter = new StickerGridAdapter();
            stickerGrid.setLayoutManager(new GridLayoutManager(itemView.getContext(), 4));
            stickerGrid.setAdapter(adapter);
        }

        public void bind(List<Sticker> stickers, StickerGridAdapter.OnStickerClickListener listener) {
            adapter.setStickers(stickers);
            adapter.setOnStickerClickListener(listener);
        }
    }
}
