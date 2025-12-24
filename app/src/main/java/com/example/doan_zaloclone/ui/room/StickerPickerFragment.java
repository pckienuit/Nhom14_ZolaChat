package com.example.doan_zaloclone.ui.room;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Sticker;
import com.example.doan_zaloclone.models.StickerPack;
import com.example.doan_zaloclone.repository.StickerRepository;
import com.example.doan_zaloclone.utils.Resource;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * Bottom sheet fragment for picking stickers
 * Shows sticker packs as tabs and stickers in a swipeable grid
 */
public class StickerPickerFragment extends BottomSheetDialogFragment {
    private static final String TAG = "StickerPickerFragment";

    // UI Components
    private RecyclerView packTabsRecycler;
    private ViewPager2 stickerPager;
    private FrameLayout tabRecent;
    private ImageButton btnAddSticker;
    private ImageButton btnStickerStore;
    private TextView btnMyStickers;

    // Adapters
    private StickerPackTabAdapter packTabAdapter;
    private StickerPagerAdapter stickerPagerAdapter;

    // Data
    private StickerRepository stickerRepository;
    private String currentUserId;
    private List<StickerPack> stickerPacks = new ArrayList<>();

    // Callback
    private OnStickerSelectedListener stickerSelectedListener;

    public interface OnStickerSelectedListener {
        void onStickerSelected(Sticker sticker);
    }

    public void setOnStickerSelectedListener(OnStickerSelectedListener listener) {
        this.stickerSelectedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stickerRepository = StickerRepository.getInstance();
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sticker_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupAdapters();
        setupListeners();
        loadData();
    }

    private void initViews(View view) {
        packTabsRecycler = view.findViewById(R.id.packTabsRecycler);
        stickerPager = view.findViewById(R.id.stickerPager);
        tabRecent = view.findViewById(R.id.tabRecent);
        btnAddSticker = view.findViewById(R.id.btnAddSticker);
        btnStickerStore = view.findViewById(R.id.btnStickerStore);
        btnMyStickers = view.findViewById(R.id.btnMyStickers);
    }

    private void setupAdapters() {
        // Pack tabs adapter
        packTabAdapter = new StickerPackTabAdapter();
        packTabsRecycler.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        packTabsRecycler.setAdapter(packTabAdapter);

        // Sticker pager adapter
        stickerPagerAdapter = new StickerPagerAdapter();
        stickerPager.setAdapter(stickerPagerAdapter);
        
        // Set sticker click listener
        stickerPagerAdapter.setOnStickerClickListener(new StickerGridAdapter.OnStickerClickListener() {
            @Override
            public void onStickerClick(Sticker sticker) {
                onStickerSelected(sticker);
            }

            @Override
            public void onStickerLongClick(Sticker sticker) {
                // TODO: Show sticker preview
                Toast.makeText(requireContext(), "Preview: " + sticker.getId(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners() {
        // Pack tab selection
        packTabAdapter.setOnPackSelectedListener((pack, position) -> {
            // +1 because position 0 is Recent tab
            stickerPager.setCurrentItem(position + 1, true);
        });

        // Pager page change
        stickerPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    // Recent tab selected - deselect pack tabs
                    packTabAdapter.setSelectedPosition(-1);
                    highlightRecentTab(true);
                } else {
                    // Pack tab selected
                    packTabAdapter.setSelectedPosition(position - 1);
                    highlightRecentTab(false);
                    
                    // Load stickers for this pack if not loaded
                    StickerPack pack = stickerPagerAdapter.getPackAtPosition(position);
                    if (pack != null) {
                        loadStickersForPack(pack.getId());
                    }
                }
            }
        });

        // Recent tab click
        tabRecent.setOnClickListener(v -> {
            stickerPager.setCurrentItem(0, true);
        });

        // Add sticker button
        btnAddSticker.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.doan_zaloclone.ui.sticker.CreateStickerActivity.class);
            startActivity(intent);
            dismiss(); // Close sticker picker
        });


        // Sticker store button
        btnStickerStore.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), com.example.doan_zaloclone.ui.sticker.StickerStoreActivity.class);
            startActivity(intent);
        });


        // My stickers button
        btnMyStickers.setOnClickListener(v -> {
            // TODO: Open My Stickers section
            Toast.makeText(requireContext(), "Sticker của tôi (coming soon)", Toast.LENGTH_SHORT).show();
        });
    }

    private void highlightRecentTab(boolean highlight) {
        // Change background of recent tab based on selection
        tabRecent.setBackgroundColor(highlight ? 
                getResources().getColor(R.color.colorPrimaryLight, null) : 
                getResources().getColor(android.R.color.transparent, null));
    }

    private void loadData() {
        // Load user's saved packs
        if (currentUserId != null) {
            loadUserSavedPacks();
            loadRecentStickers();
        }
        
        // Load official packs
        loadOfficialPacks();
    }

    private void loadUserSavedPacks() {
        stickerRepository.getUserSavedPacks(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.getData() != null) {
                // Add user packs to the beginning
                List<StickerPack> allPacks = new ArrayList<>(resource.getData());
                
                // Merge with official packs
                for (StickerPack pack : stickerPacks) {
                    boolean exists = false;
                    for (StickerPack savedPack : allPacks) {
                        if (savedPack.getId().equals(pack.getId())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        allPacks.add(pack);
                    }
                }
                
                updatePacksUI(allPacks);
            }
        });
    }

    private void loadOfficialPacks() {
        stickerRepository.getOfficialPacks().observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.getData() != null) {
                stickerPacks = resource.getData();
                updatePacksUI(stickerPacks);
                
                // Load stickers for the first pack
                if (!stickerPacks.isEmpty()) {
                    loadStickersForPack(stickerPacks.get(0).getId());
                }
            } else if (resource.isError()) {
                Log.e(TAG, "Error loading official packs: " + resource.getMessage());
            }
        });
    }

    private void loadRecentStickers() {
        stickerRepository.getRecentStickers(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.getData() != null) {
                stickerPagerAdapter.setRecentStickers(resource.getData());
            }
        });
    }

    private void loadStickersForPack(String packId) {
        stickerRepository.getStickersInPack(packId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.getData() != null) {
                stickerPagerAdapter.setPackStickers(packId, resource.getData());
            } else if (resource.isError()) {
                Log.e(TAG, "Error loading stickers for pack " + packId + ": " + resource.getMessage());
            }
        });
    }

    private void updatePacksUI(List<StickerPack> packs) {
        stickerPacks = packs;
        packTabAdapter.setPacks(packs);
        stickerPagerAdapter.setPacks(packs);
        
        // Load stickers for all packs
        for (StickerPack pack : packs) {
            if (pack.getId() != null) {
                loadStickersForPack(pack.getId());
            }
        }
    }

    private void onStickerSelected(Sticker sticker) {
        // Add to recent stickers
        if (currentUserId != null) {
            stickerRepository.addToRecentStickers(currentUserId, sticker);
        }

        // Notify listener
        if (stickerSelectedListener != null) {
            stickerSelectedListener.onStickerSelected(sticker);
        }

        // Dismiss picker
        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Expand bottom sheet by default
        View view = getView();
        if (view != null) {
            View parent = (View) view.getParent();
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(parent);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Reload saved packs when returning from store
        if (currentUserId != null) {
            loadUserSavedPacks();
        }
    }
}
