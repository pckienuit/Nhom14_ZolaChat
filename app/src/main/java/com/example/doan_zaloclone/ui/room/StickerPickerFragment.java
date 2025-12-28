package com.example.doan_zaloclone.ui.room;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Sticker;
import com.example.doan_zaloclone.models.StickerPack;
import com.example.doan_zaloclone.repository.StickerRepository;
import com.example.doan_zaloclone.ui.sticker.MyStickerActivity;
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
    private View btnAddSticker;
    private View btnStickerStore;
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

    // ActivityResultLauncher để xử lý kết quả từ MyStickerActivity
    private final ActivityResultLauncher<Intent> myStickerLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String stickerId = data.getStringExtra("sticker_id");
                    String stickerUrl = data.getStringExtra("sticker_url");
                    String stickerPackId = data.getStringExtra("sticker_pack_id");
                    
                    if (stickerId != null && stickerUrl != null) {
                        // Tạo Sticker object từ dữ liệu trả về
                        Sticker sticker = new Sticker();
                        sticker.setId(stickerId);
                        sticker.setImageUrl(stickerUrl);
                        sticker.setPackId(stickerPackId != null ? stickerPackId : currentUserId);
                        
                        // Gửi sticker
                        onStickerSelected(sticker);
                    }
                }
            });

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


        // My stickers button - sử dụng launcher để nhận kết quả
        btnMyStickers.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MyStickerActivity.class);
            intent.putExtra(MyStickerActivity.EXTRA_RETURN_STICKER, true);
            myStickerLauncher.launch(intent);
        });
    }

    private void highlightRecentTab(boolean highlight) {
        // Change background of recent tab based on selection
        tabRecent.setBackgroundColor(highlight ?
                getResources().getColor(R.color.colorPrimaryLight, null) :
                getResources().getColor(android.R.color.transparent, null));
    }


    private void loadData() {
        if (currentUserId != null) {
            // Đảm bảo pack cá nhân luôn có
            stickerRepository.getOrCreateUserStickerPack(currentUserId, new StickerRepository.PackCallback() {
                @Override
                public void onSuccess(String myPackId) {
                    // Sau khi chắc chắn pack cá nhân tồn tại, load saved packs và recent
                    loadUserSavedPacksWithMyPack(myPackId);
                    loadRecentStickers();
                }
                @Override
                public void onError(String error) {
                    // Nếu lỗi vẫn load bình thường (không có pack cá nhân)
                    loadUserSavedPacksWithMyPack(null);
                    loadRecentStickers();
                }
            });
        } else {
            loadUserSavedPacksWithMyPack(null);
            loadRecentStickers();
        }
        loadOfficialPacks();
    }

    // Thay thế cho loadUserSavedPacks, đảm bảo pack cá nhân luôn ở đầu
    private void loadUserSavedPacksWithMyPack(String myPackId) {
        stickerRepository.getUserSavedPacks(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.isSuccess() && resource.getData() != null) {
                List<StickerPack> allPacks = new ArrayList<>();
                // Nếu có pack cá nhân, thêm vào đầu
                if (myPackId != null) {
                    stickerRepository.getStickersInPack(myPackId).observe(getViewLifecycleOwner(), stickersRes -> {
                        // Tạo StickerPack tạm nếu chưa có trong saved
                        boolean found = false;
                        for (StickerPack p : resource.getData()) {
                            if (p.getId().equals(myPackId)) {
                                allPacks.add(p);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            // Nếu chưa có, tạo StickerPack tạm
                            StickerPack myPack = new StickerPack();
                            myPack.setId(myPackId);
                            myPack.setName("Sticker của tôi");
                            myPack.setType(StickerPack.TYPE_USER);
                            allPacks.add(myPack);
                        }
                        // Thêm các pack còn lại
                        for (StickerPack p : resource.getData()) {
                            if (!p.getId().equals(myPackId)) {
                                allPacks.add(p);
                            }
                        }
                        // Merge với official packs
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
                    });
                } else {
                    // Không có pack cá nhân, giữ nguyên logic cũ
                    allPacks.addAll(resource.getData());
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
            }
        });
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

    public interface OnStickerSelectedListener {
        void onStickerSelected(Sticker sticker);
    }
}
