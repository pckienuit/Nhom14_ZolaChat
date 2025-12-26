package com.example.doan_zaloclone.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.ui.room.RoomActivity;
import com.example.doan_zaloclone.viewmodel.HomeViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView conversationsRecyclerView;
    private ConversationAdapter conversationAdapter;

    private HomeViewModel homeViewModel;
    private FirebaseAuth firebaseAuth;
    private LinearLayout filterChipsContainer;
    private String currentFilterTag = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize ViewModel
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        firebaseAuth = FirebaseAuth.getInstance();

        // Load custom tag colors from Firestore
        if (firebaseAuth.getCurrentUser() != null) {
            ConversationAdapter.loadCustomTagColors(firebaseAuth.getCurrentUser().getUid());
        }

        initViews(view);
        setupRecyclerView();
        observeViewModel();
        loadConversations();

        return view;
    }

    private void initViews(View view) {
        conversationsRecyclerView = view.findViewById(R.id.conversationsRecyclerView);
        filterChipsContainer = view.findViewById(R.id.filterChipsContainer);
        setupFilterChips();
        
        // Setup inline search EditText
        setupSearchEditText(view);
        
        // QR button - scan QR code
        View btnQr = view.findViewById(R.id.btn_qr);
        if (btnQr != null) {
            btnQr.setOnClickListener(v -> openQRScanner());
        }
        
        // Add button - show menu for adding friend or creating group
        View btnAdd = view.findViewById(R.id.btn_add);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showAddMenu(v));
        }
    }
    
    private void setupSearchEditText(View view) {
        android.widget.EditText searchEditText = view.findViewById(R.id.searchEditText);
        if (searchEditText == null) return;
        
        // TextWatcher for realtime filtering
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    private void filterConversations(String query) {
        if (conversationAdapter == null) return;
        
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";
        
        // Get all conversations from ViewModel
        homeViewModel.getConversations(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.isSuccess() && resource.getData() != null) {
                java.util.List<Conversation> allConversations = resource.getData();
                
                if (query == null || query.trim().isEmpty()) {
                    // Show all if search is empty
                    conversationAdapter.updateConversations(allConversations);
                } else {
                    // Filter by conversation name or last message
                    String lowerQuery = query.toLowerCase();
                    java.util.List<Conversation> filtered = new java.util.ArrayList<>();
                    
                    for (Conversation conv : allConversations) {
                        // Search in conversation name
                        String convName = conv.getName();
                        if (convName == null) {
                            convName = conv.getOtherUserName(currentUserId);
                        }
                        
                        if (convName != null && convName.toLowerCase().contains(lowerQuery)) {
                            filtered.add(conv);
                            continue;
                        }
                        
                        // Search in last message
                        if (conv.getLastMessage() != null && 
                            conv.getLastMessage().toLowerCase().contains(lowerQuery)) {
                            filtered.add(conv);
                        }
                    }
                    
                    conversationAdapter.updateConversations(filtered);
                }
            }
        });
    }
    
    private void openQRScanner() {
        Intent intent = new Intent(requireContext(), com.example.doan_zaloclone.ui.qr.QRScanActivity.class);
        startActivity(intent);
    }
    
    private void showAddMenu(View anchor) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(getContext(), anchor);
        popup.getMenu().add("Thêm bạn bè");
        popup.getMenu().add("Tạo nhóm");
        
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Thêm bạn bè")) {
                // Open AddFriendActivity
                Intent intent = new Intent(requireContext(), com.example.doan_zaloclone.ui.contact.AddFriendActivity.class);
                startActivity(intent);
                return true;
            } else if (title.equals("Tạo nhóm")) {
                // Open CreateGroupActivity
                Intent intent = new Intent(requireContext(), com.example.doan_zaloclone.ui.group.CreateGroupActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void setupRecyclerView() {
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";

        conversationAdapter = new ConversationAdapter(new ArrayList<>(), conversation -> {
            Intent intent = new Intent(getActivity(), RoomActivity.class);
            intent.putExtra("conversationId", conversation.getId());

            // Pass the other user's name for display
            String conversationName = conversation.getName();
            if (conversationName == null || conversationName.isEmpty()) {
                conversationName = conversation.getOtherUserName(currentUserId);
            }
            intent.putExtra("conversationName", conversationName);
            startActivity(intent);
        }, currentUserId);

        // Set long-click listener for context menu
        conversationAdapter.setOnConversationLongClickListener(conversation -> {
            showConversationContextMenu(conversation);
        });

        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        conversationsRecyclerView.setHasFixedSize(true); // Optimize RecyclerView performance
        conversationsRecyclerView.setAdapter(conversationAdapter);
    }

    private void setupFilterChips() {
        if (filterChipsContainer == null) return;

        // Add "All" chip
        addFilterChip(getString(R.string.filter_all), null, true);

        // Observe conversations to get all tags
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";

        homeViewModel.getConversations(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource != null && resource.isSuccess() && resource.getData() != null) {
                refreshFilterChips(resource.getData(), currentUserId);
            }
        });
    }

    private void refreshFilterChips(java.util.List<Conversation> conversations, String userId) {
        if (filterChipsContainer == null) return;

        // Collect all unique tags
        java.util.Set<String> allTags = new java.util.HashSet<>();
        for (Conversation conv : conversations) {
            java.util.List<String> tags = conv.getUserTagsForUser(userId);
            if (tags != null) {
                allTags.addAll(tags);
            }
        }

        // Clear existing chips except "All"
        int childCount = filterChipsContainer.getChildCount();
        if (childCount > 1) {
            filterChipsContainer.removeViews(1, childCount - 1);
        }

        // Add chips for each tag
        for (String tag : allTags) {
            addFilterChip(tag, tag, false);
        }
    }

    private void addFilterChip(String label, String tagValue, boolean selected) {
        TextView chip = new TextView(getContext());
        chip.setText(label);
        chip.setTextSize(12);
        chip.setPadding(24, 12, 24, 12);

        // Set style based on selection
        updateChipStyle(chip, selected);

        // Click listener
        chip.setOnClickListener(v -> {
            // Update filter
            currentFilterTag = tagValue;
            homeViewModel.setTagFilter(tagValue);

            // Update all chips appearance
            for (int i = 0; i < filterChipsContainer.getChildCount(); i++) {
                View child = filterChipsContainer.getChildAt(i);
                if (child instanceof TextView) {
                    updateChipStyle((TextView) child, child == chip);
                }
            }
        });

        // Layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(12);
        chip.setLayoutParams(params);

        filterChipsContainer.addView(chip);
    }

    private void updateChipStyle(TextView chip, boolean selected) {
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setCornerRadius(50);

        if (selected) {
            background.setColor(getResources().getColor(R.color.colorPrimary, null));
            chip.setTextColor(Color.WHITE);
        } else {
            background.setColor(Color.WHITE);
            background.setStroke(2, getResources().getColor(R.color.colorPrimary, null));
            chip.setTextColor(getResources().getColor(R.color.colorPrimary, null));
        }

        chip.setBackground(background);
    }

    private void showConversationContextMenu(Conversation conversation) {
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";

        boolean isPinned = conversation.isPinnedByUser(currentUserId);

        // Create options
        String[] options;
        if (isPinned) {
            options = new String[]{
                    getString(R.string.unpin_conversation),
                    getString(R.string.manage_tags),
                    getString(R.string.choose_display_tag)
            };
        } else {
            options = new String[]{
                    getString(R.string.pin_conversation),
                    getString(R.string.manage_tags),
                    getString(R.string.choose_display_tag)
            };
        }

        // Show dialog
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Pin or Unpin
                        if (isPinned) {
                            unpinConversation(conversation.getId(), currentUserId);
                        } else {
                            pinConversation(conversation.getId(), currentUserId);
                        }
                    } else if (which == 1) {
                        // Manage tags
                        showTagManagementDialog(conversation);
                    } else if (which == 2) {
                        // Choose display tag
                        java.util.List<String> userTags = conversation.getUserTagsForUser(currentUserId);
                        if (userTags != null && !userTags.isEmpty()) {
                            showDisplayTagDialog(conversation, currentUserId, userTags);
                        } else {
                            Toast.makeText(getContext(), "Chưa có thẻ nào", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private void showTagManagementDialog(Conversation conversation) {
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";

        // Load user's custom tags from Firestore first
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    java.util.List<String> globalCustomTags = new java.util.ArrayList<>();

                    if (documentSnapshot.exists() && documentSnapshot.contains("customTags")) {
                        // customTags is stored as a map for easier checking
                        java.util.Map<String, Object> customTagsMap =
                                (java.util.Map<String, Object>) documentSnapshot.get("customTags");
                        if (customTagsMap != null) {
                            globalCustomTags.addAll(customTagsMap.keySet());
                        }
                    }

                    showTagManagementDialogWithCustomTags(conversation, currentUserId, globalCustomTags);
                })
                .addOnFailureListener(e -> {
                    // Show dialog without custom tags if failed to load
                    showTagManagementDialogWithCustomTags(conversation, currentUserId, new java.util.ArrayList<>());
                });
    }

    private void showTagManagementDialogWithCustomTags(Conversation conversation, String currentUserId,
                                                       java.util.List<String> globalCustomTags) {
        // Get current tags
        java.util.List<String> currentTags = conversation.getUserTagsForUser(currentUserId);
        java.util.List<String> selectedTags = new java.util.ArrayList<>(currentTags);

        // Build available tags list: default tags + global custom tags
        java.util.List<String> availableTagsList = new java.util.ArrayList<>();

        // Add default tags
        availableTagsList.add(getString(R.string.tag_work));
        availableTagsList.add(getString(R.string.tag_family));
        availableTagsList.add(getString(R.string.tag_friends));

        // Add global custom tags
        for (String customTag : globalCustomTags) {
            if (!availableTagsList.contains(customTag)) {
                availableTagsList.add(customTag);
            }
        }

        String[] availableTags = availableTagsList.toArray(new String[0]);

        boolean[] checkedItems = new boolean[availableTags.length];
        for (int i = 0; i < availableTags.length; i++) {
            checkedItems[i] = currentTags.contains(availableTags[i]);
        }

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(R.string.manage_tags)
                .setMultiChoiceItems(availableTags, checkedItems, (dialog, which, isChecked) -> {
                    String tag = availableTags[which];
                    if (isChecked) {
                        if (!selectedTags.contains(tag)) {
                            selectedTags.add(tag);
                        }
                    } else {
                        selectedTags.remove(tag);
                    }
                })
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    // Update tags
                    updateConversationTags(conversation.getId(), currentUserId, selectedTags);
                })
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.add_custom_tag, (dialog, which) -> {
                    // Show custom tag dialog
                    showAddCustomTagDialog(conversation, selectedTags);
                })
                .show();
    }

    private void showDisplayTagDialog(Conversation conversation, String userId, java.util.List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            Toast.makeText(getContext(), "Chưa có thẻ nào", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current displayed preference
        String currentDisplay = conversation.getDisplayedTags().get(userId);

        // Build options: Latest, (All if multiple tags), and each tag
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add("Mới nhất (mặc định)");

        boolean hasMultipleTags = tags.size() >= 2;
        if (hasMultipleTags) {
            options.add("Hiển thị tất cả");
        }

        options.addAll(tags);

        // Find selected index
        int selectedIndex = 0; // Default to "Latest"
        if ("ALL".equals(currentDisplay) && hasMultipleTags) {
            selectedIndex = 1;
        } else if (currentDisplay != null) {
            int tagIndex = tags.indexOf(currentDisplay);
            if (tagIndex >= 0) {
                selectedIndex = tagIndex + (hasMultipleTags ? 2 : 1);
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Chọn thẻ hiển thị")
                .setSingleChoiceItems(options.toArray(new String[0]), selectedIndex, (dialog, which) -> {
                    String displayTag;
                    if (which == 0) {
                        displayTag = null; // Latest
                    } else if (hasMultipleTags && which == 1) {
                        displayTag = "ALL"; // All tags
                    } else {
                        int tagIndex = hasMultipleTags ? which - 2 : which - 1;
                        displayTag = tags.get(tagIndex); // Specific tag
                    }

                    // Update displayed tag
                    conversation.setDisplayedTag(userId, displayTag);
                    updateDisplayedTag(conversation.getId(), userId, displayTag);

                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateDisplayedTag(String conversationId, String userId, String displayTag) {
        // Update in Firestore
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        if (displayTag == null) {
            updates.put("displayedTags." + userId, com.google.firebase.firestore.FieldValue.delete());
        } else {
            updates.put("displayedTags." + userId, displayTag);
        }

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("conversations")
                .document(conversationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Success - adapter will update automatically via listener
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Không thể cập nhật hiển thị thẻ", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAddCustomTagDialog(Conversation conversation, java.util.List<String> currentTags) {
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";

        // Create EditText for tag name
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint(R.string.enter_tag_name);
        input.setSingleLine();

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(R.string.add_custom_tag)
                .setView(input)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    String tagName = input.getText().toString().trim();
                    if (!tagName.isEmpty()) {
                        // Show color picker
                        showColorPickerDialog(conversation, currentUserId, tagName, currentTags);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showColorPickerDialog(Conversation conversation, String userId,
                                       String tagName, java.util.List<String> currentTags) {
        // Color palette
        final int[] colors = com.example.doan_zaloclone.models.ConversationTag.COLOR_PALETTE;
        final int[] selectedColor = {colors[0]}; // Default to first color

        // Create color grid
        android.widget.GridLayout colorGrid = new android.widget.GridLayout(getContext());
        colorGrid.setColumnCount(4);
        colorGrid.setPadding(32, 32, 32, 32);

        for (int color : colors) {
            android.widget.ImageView colorView = new android.widget.ImageView(getContext());
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(color);
            drawable.setStroke(4, android.graphics.Color.TRANSPARENT);
            colorView.setBackground(drawable);

            android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
            params.width = 100;
            params.height = 100;
            params.setMargins(16, 16, 16, 16);
            colorView.setLayoutParams(params);

            colorView.setOnClickListener(v -> {
                selectedColor[0] = color;
                // Update all drawables to remove selection
                for (int i = 0; i < colorGrid.getChildCount(); i++) {
                    android.view.View child = colorGrid.getChildAt(i);
                    if (child instanceof android.widget.ImageView) {
                        android.graphics.drawable.GradientDrawable bg =
                                (android.graphics.drawable.GradientDrawable) child.getBackground();
                        bg.setStroke(4, android.graphics.Color.TRANSPARENT);
                    }
                }
                // Highlight selected
                drawable.setStroke(8, android.graphics.Color.WHITE);
            });

            colorGrid.addView(colorView);
        }

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Chọn màu cho \"" + tagName + "\"")
                .setView(colorGrid)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    // Save custom tag globally to user's Firestore document
                    saveCustomTagToFirestore(userId, tagName, selectedColor[0]);

                    // Add tag to current tags
                    currentTags.add(tagName);

                    // Update conversation tags
                    updateConversationTags(conversation.getId(), userId, currentTags);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveCustomTagToFirestore(String userId, String tagName, int color) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("customTags." + tagName, true); // Use map for easier checking
        updates.put("customTagColors." + tagName, color);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(updates)
                .addOnFailureListener(e -> {
                    // If update fails (document might not have these fields), set them
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .set(updates, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    private void updateConversationTags(String conversationId, String userId, java.util.List<String> tags) {
        homeViewModel.updateTags(conversationId, userId, tags).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                Toast.makeText(getContext(), R.string.tag_updated, Toast.LENGTH_SHORT).show();
            } else if (resource.isError()) {
                Toast.makeText(getContext(), R.string.tag_update_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pinConversation(String conversationId, String userId) {
        homeViewModel.pinConversation(conversationId, userId).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                Toast.makeText(getContext(), "Đã ghim cuộc hội thoại", Toast.LENGTH_SHORT).show();
            } else if (resource.isError()) {
                String errorMessage = resource.getMessage() != null
                        ? resource.getMessage()
                        : "Không thể ghim cuộc hội thoại";
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unpinConversation(String conversationId, String userId) {
        homeViewModel.unpinConversation(conversationId, userId).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isSuccess()) {
                Toast.makeText(getContext(), "Đã bỏ ghim", Toast.LENGTH_SHORT).show();
            } else if (resource.isError()) {
                String errorMessage = resource.getMessage() != null
                        ? resource.getMessage()
                        : "Không thể bỏ ghim";
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        // Check socket status
        com.example.doan_zaloclone.websocket.SocketManager socketManager = 
                com.example.doan_zaloclone.websocket.SocketManager.getInstance();
        if (!socketManager.isConnected()) {
             if (getContext() != null) 
                 android.widget.Toast.makeText(getContext(), "⚠️ Reconnecting Socket...", android.widget.Toast.LENGTH_SHORT).show();
             socketManager.connect();
        }
    }

    private void observeViewModel() {
        // Observe filtered conversations LiveData
        String currentUserId = firebaseAuth.getCurrentUser() != null
                ? firebaseAuth.getCurrentUser().getUid()
                : "";

        homeViewModel.getFilteredConversations(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;

            if (resource.isLoading()) {
                // Optional: Show loading indicator
                android.util.Log.d("HomeFragment", "Loading conversations...");
            } else if (resource.isSuccess()) {
                List<Conversation> conversations = resource.getData();
                if (conversations != null) {
                    android.util.Log.d("HomeFragment", "Received " + conversations.size() + " conversations");
                    conversationAdapter.updateConversations(conversations);
                    
                    // Calculate total unread and update badge in MainActivity
                    updateUnreadBadge(conversations, currentUserId);
                }
            } else if (resource.isError()) {
                // Show error message
                String errorMessage = resource.getMessage() != null
                        ? resource.getMessage()
                        : "Error loading conversations";
                android.util.Log.e("HomeFragment", "Error: " + errorMessage);
                if (getContext() != null) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Observe group_left event to remove conversation from UI
        homeViewModel.getGroupLeftEvent().observe(getViewLifecycleOwner(), conversationId -> {
            if (conversationId != null) {
                android.util.Log.d("HomeFragment", "Removing conversation from UI: " + conversationId);
                conversationAdapter.removeConversationById(conversationId);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Đã rời nhóm", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Observe conversation refresh events (new conversation, updates)
        homeViewModel.getConversationRefreshNeeded().observe(getViewLifecycleOwner(), needsRefresh -> {
            if (needsRefresh != null && needsRefresh) {
                // DEBUG TOAST
                // if (getContext() != null) android.widget.Toast.makeText(getContext(), "🔔 New message! Updating...", android.widget.Toast.LENGTH_SHORT).show();

                // Get current user ID at the time of refresh
                if (firebaseAuth.getCurrentUser() != null) {
                    final String userId = firebaseAuth.getCurrentUser().getUid();
                    
                    // Delay 1s to allow Firestore to index the new message
                    new android.os.Handler().postDelayed(() -> {
                        android.util.Log.d("HomeFragment", "Refreshing conversations due to WebSocket event for user: " + userId);
                        homeViewModel.refreshConversations(userId);
                    }, 1000);
                } else {
                    android.util.Log.w("HomeFragment", "Cannot refresh - user not logged in");
                }
            }
        });
    }

    private void loadConversations() {
        // Check if user is logged in
        if (firebaseAuth.getCurrentUser() == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        android.util.Log.d("HomeFragment", "Current User ID: " + currentUserId);

        // Conversations are automatically loaded via ViewModel observer
        // The observer in observeViewModel() handles the real-time updates
    }
    
    /**
     * Calculate total unread messages and update badge in MainActivity
     */
    private void updateUnreadBadge(List<Conversation> conversations, String userId) {
        if (conversations == null || getActivity() == null) return;
        
        int totalUnread = 0;
        for (Conversation conv : conversations) {
            totalUnread += conv.getUnreadCountForUser(userId);
        }
        
        // Update badge in MainActivity
        if (getActivity() instanceof com.example.doan_zaloclone.MainActivity) {
            ((com.example.doan_zaloclone.MainActivity) getActivity()).updateMessagesBadge(totalUnread);
        }
    }
}

