package com.example.doan_zaloclone.ui.file;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FileCategory;
import com.example.doan_zaloclone.models.FileItem;
import com.example.doan_zaloclone.models.SenderInfo;
import com.example.doan_zaloclone.viewmodel.FileViewModel;
import com.google.android.material.chip.Chip;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FileCategory;
import com.example.doan_zaloclone.models.FileItem;
import com.example.doan_zaloclone.viewmodel.FileViewModel;

/**
 * Fragment to display a list of files for a specific category
 */
public class FileListFragment extends Fragment {
    
    private static final String ARG_CATEGORY = "category";
    private static final String ARG_CONVERSATION_ID = "conversation_id";
    
    private FileCategory category;
    private String conversationId;
    
    // UI components
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyStateLayout;
    private ProgressBar progressBar;
    
    private FileViewModel viewModel;
    private FileAdapter adapter;
    
    // Filter UI components
    private SearchView searchView; // Will be from menu
    private Chip senderFilterChip;
    private Chip dateFilterChip;
    private Chip clearFiltersChip;
    
    // Media filter chips (only for MEDIA category)
    private View mediaFilterScrollView;
    private Chip allMediaChip;
    private Chip imagesOnlyChip;
    private Chip videosOnlyChip;
    
    // File type filter chips (only for FILES category)
    private View fileTypeFilterScrollView;
    private Chip allFilesChip;
    private Chip pdfFilesChip;
    private Chip wordFilesChip;
    private Chip excelFilesChip;
    private Chip powerpointFilesChip;
    private Chip archiveFilesChip;
    
    // Filter state
    private Set<String> selectedSenderIds = new HashSet<>();
    private Long filterStartDate = null;
    private Long filterEndDate = null;
    private TimeFilterDialog.TimeFilterPreset currentTimePreset = TimeFilterDialog.TimeFilterPreset.NONE;
    
    private boolean isLoading = false;
    private boolean isInitialLoad = true;
    
    public static FileListFragment newInstance(FileCategory category, String conversationId) {
        FileListFragment fragment = new FileListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CATEGORY, category.getPosition());
        args.putString(ARG_CONVERSATION_ID, conversationId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Enable options menu
        if (getArguments() != null) {
            int categoryPosition = getArguments().getInt(ARG_CATEGORY);
            category = FileCategory.fromPosition(categoryPosition);
            conversationId = getArguments().getString(ARG_CONVERSATION_ID);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_list, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupViewModel();
        setupRecyclerView();
        setupSwipeRefresh();
        setupFilterChips();
        setupMediaFilterChips();
        setupFileTypeFilterChips();
        observeData();
        
        // Load data only once on initial creation
        if (isInitialLoad) {
            loadData();
            isInitialLoad = false;
        }
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        progressBar = view.findViewById(R.id.progressBar);
        
        // Filter UI (searchView will be initialized from menu)
        senderFilterChip = view.findViewById(R.id.senderFilterChip);
        dateFilterChip = view.findViewById(R.id.dateFilterChip);
        clearFiltersChip = view.findViewById(R.id.clearFiltersChip);
        
        // Media filter chips
        mediaFilterScrollView = view.findViewById(R.id.mediaFilterScrollView);
        allMediaChip = view.findViewById(R.id.allMediaChip);
        imagesOnlyChip = view.findViewById(R.id.imagesOnlyChip);
        videosOnlyChip = view.findViewById(R.id.videosOnlyChip);
        
        // File type filter chips
        fileTypeFilterScrollView = view.findViewById(R.id.fileTypeFilterScrollView);
        allFilesChip = view.findViewById(R.id.allFilesChip);
        pdfFilesChip = view.findViewById(R.id.pdfFilesChip);
        wordFilesChip = view.findViewById(R.id.wordFilesChip);
        excelFilesChip = view.findViewById(R.id.excelFilesChip);
        powerpointFilesChip = view.findViewById(R.id.powerpointFilesChip);
        archiveFilesChip = view.findViewById(R.id.archiveFilesChip);
    }
    
    private void setupViewModel() {
        // Get shared ViewModel scoped to the parent Activity
        viewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);
    }
    
    private void setupRecyclerView() {
        // Use GridLayoutManager for MEDIA, LinearLayoutManager for FILES and LINKS
        if (category == FileCategory.MEDIA) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 3);
            recyclerView.setLayoutManager(gridLayoutManager);
        } else {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(linearLayoutManager);
        }
        
        // Setup adapter
        adapter = new FileAdapter(category);
        adapter.setOnItemClickListener(this::onFileItemClick);
        recyclerView.setAdapter(adapter);
        
        // Setup pagination scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (!isLoading && viewModel.hasMore(category)) {
                    RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                    
                    int visibleItemCount = layoutManager != null ? layoutManager.getChildCount() : 0;
                    int totalItemCount = layoutManager != null ? layoutManager.getItemCount() : 0;
                    int firstVisibleItemPosition;
                    
                    if (layoutManager instanceof GridLayoutManager) {
                        firstVisibleItemPosition = ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
                    } else if (layoutManager instanceof LinearLayoutManager) {
                        firstVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                    } else {
                        return;
                    }
                    
                    // Load more when near the end (5 items before the end)
                    if ((visibleItemCount + firstVisibleItemPosition + 5) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMoreFiles();
                    }
                }
            }
        });
    }
    
    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            viewModel.refreshFiles(conversationId);
        });
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            return true; // Handled by SearchView
        }
        return false;
    }
    
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_file_list, menu);
        
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
            setupSearchView();
        }
        
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    private void setupSearchView() {
        if (searchView == null) return;
        
        searchView.setQueryHint(getString(R.string.search_files));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                updateClearFiltersVisibility();
                return true;
            }
        });
    }
    
    private void setupFilterChips() {
        senderFilterChip.setOnClickListener(v -> showSenderFilterDialog());
        dateFilterChip.setOnClickListener(v -> showDateRangePickerDialog());
        clearFiltersChip.setOnClickListener(v -> clearAllFilters());
    }
    
    private void setupMediaFilterChips() {
        // Show media filter chips only for MEDIA category
        if (category == FileCategory.MEDIA) {
            mediaFilterScrollView.setVisibility(View.VISIBLE);
            
            // Set up chip listeners
            allMediaChip.setOnClickListener(v -> {
                viewModel.setMediaTypeFilter(FileViewModel.MediaTypeFilter.ALL);
            });
            
            imagesOnlyChip.setOnClickListener(v -> {
                viewModel.setMediaTypeFilter(FileViewModel.MediaTypeFilter.IMAGES_ONLY);
            });
            
            videosOnlyChip.setOnClickListener(v -> {
                viewModel.setMediaTypeFilter(FileViewModel.MediaTypeFilter.VIDEOS_ONLY);
            });
        } else {
            mediaFilterScrollView.setVisibility(View.GONE);
        }
    }
    
    private void setupFileTypeFilterChips() {
        // Show file type filter chips only for FILES category
        if (category == FileCategory.FILES) {
            fileTypeFilterScrollView.setVisibility(View.VISIBLE);
            
            // Set up chip listeners
            allFilesChip.setOnClickListener(v -> {
                viewModel.setFileTypeFilter(FileViewModel.FileTypeFilter.ALL);
            });
            
            pdfFilesChip.setOnClickListener(v -> {
                viewModel.setFileTypeFilter(FileViewModel.FileTypeFilter.PDF);
            });
            
            wordFilesChip.setOnClickListener(v -> {
                viewModel.setFileTypeFilter(FileViewModel.FileTypeFilter.WORD);
            });
            
            excelFilesChip.setOnClickListener(v -> {
                viewModel.setFileTypeFilter(FileViewModel.FileTypeFilter.EXCEL);
            });
            
            powerpointFilesChip.setOnClickListener(v -> {
                viewModel.setFileTypeFilter(FileViewModel.FileTypeFilter.POWERPOINT);
            });
            
            archiveFilesChip.setOnClickListener(v -> {
                viewModel.setFileTypeFilter(FileViewModel.FileTypeFilter.ARCHIVE);
            });
        } else {
            fileTypeFilterScrollView.setVisibility(View.GONE);
        }
    }
    
    private void observeData() {
        // Observe FILTERED LiveData instead of original
        switch (category) {
            case MEDIA:
                viewModel.getFilteredMediaFilesLiveData().observe(getViewLifecycleOwner(), resource -> {
                    handleResourceUpdate(resource);
                });
                break;
            case FILES:
                viewModel.getFilteredFilesLiveData().observe(getViewLifecycleOwner(), resource -> {
                    handleResourceUpdate(resource);
                });
                break;
            case LINKS:
                viewModel.getFilteredLinksLiveData().observe(getViewLifecycleOwner(), resource -> {
                    handleResourceUpdate(resource);
                });
                break;
        }
    }
    
    private void handleResourceUpdate(com.example.doan_zaloclone.utils.Resource<java.util.List<FileItem>> resource) {
        if (resource == null) return;
        
        if (resource.isLoading()) {
            showLoading(true);
            swipeRefresh.setRefreshing(false);
        } else if (resource.isSuccess()) {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            isLoading = false;
            
            java.util.List<FileItem> files = resource.getData();
            if (files != null && !files.isEmpty()) {
                showEmptyState(false);
                adapter.updateFiles(files);
            } else {
                showEmptyState(true);
            }
        } else if (resource.isError()) {
            showLoading(false);
            swipeRefresh.setRefreshing(false);
            isLoading = false;
            
            String errorMsg = resource.getMessage();
            if (errorMsg != null && getContext() != null) {
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
            
            // Show empty state if no data
            if (adapter.getItemCount() == 0) {
                showEmptyState(true);
            }
        }
    }
    
    private void loadData() {
        if (conversationId != null && viewModel != null) {
            viewModel.loadFiles(conversationId);
        }
    }
    
    private void loadMoreFiles() {
        if (isLoading || conversationId == null) return;
        
        isLoading = true;
        viewModel.loadMoreFiles(conversationId, category);
    }
    
    private void onFileItemClick(FileItem item, int position) {
        // TODO Phase 5: Handle file actions
        if (getContext() != null) {
            Toast.makeText(getContext(), 
                "Clicked: " + item.getDisplayName(), 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showEmptyState(boolean show) {
        emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    public FileCategory getCategory() {
        return category;
    }
    
    // ========== FILTER DIALOG METHODS ==========
    
    private void showSenderFilterDialog() {
        viewModel.getUniqueSenders().observe(getViewLifecycleOwner(), senders -> {
            if (senders == null || senders.isEmpty()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Chưa có người gửi", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            // Create dialog
            View dialogView = LayoutInflater.from(getContext())
                    .inflate(R.layout.dialog_sender_filter, null);
            
            RecyclerView senderRecyclerView = dialogView.findViewById(R.id.senderRecyclerView);
            senderRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            
            SenderFilterAdapter senderAdapter = new SenderFilterAdapter();
            senderAdapter.setSenders(senders);
            senderAdapter.setSelectedSenders(selectedSenderIds);
            senderRecyclerView.setAdapter(senderAdapter);
            
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create();
            
            dialogView.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
            dialogView.findViewById(R.id.applyButton).setOnClickListener(v -> {
                selectedSenderIds = senderAdapter.getSelectedSenders();
                viewModel.setSelectedSenders(selectedSenderIds);
                updateSenderChipText();
                updateClearFiltersVisibility();
                dialog.dismiss();
            });
            
            dialog.show();
        });
    }
    
    private void showDateRangePickerDialog() {
        TimeFilterDialog dialog = TimeFilterDialog.newInstance();
        dialog.setTimeFilterListener((preset, startDate, endDate) -> {
            currentTimePreset = preset;
            filterStartDate = startDate;
            filterEndDate = endDate;
            
            if (startDate != null && endDate != null) {
                viewModel.setDateRange(startDate, endDate);
            } else {
                viewModel.setDateRange(null, null);
            }
            
            updateDateChipText();
            updateClearFiltersVisibility();
        });
        dialog.show(getParentFragmentManager(), "TimeFilterDialog");
    }
    
    private void clearAllFilters() {
        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.setIconified(true); // Collapse search
        }
        selectedSenderIds.clear();
        filterStartDate = null;
        filterEndDate = null;
        currentTimePreset = TimeFilterDialog.TimeFilterPreset.NONE;
        
        viewModel.clearFilters();
        updateSenderChipText();
        updateDateChipText();
        updateClearFiltersVisibility();
    }
    
    private void updateSenderChipText() {
        if (selectedSenderIds.isEmpty()) {
            senderFilterChip.setText(R.string.filter_by_sender);
        } else {
            senderFilterChip.setText(getString(R.string.filter_by_sender) + " (" + selectedSenderIds.size() + ")");
        }
        senderFilterChip.setChecked(!selectedSenderIds.isEmpty());
    }
    
    private void updateDateChipText() {
        if (filterStartDate != null || filterEndDate != null) {
            String presetText = "";
            switch (currentTimePreset) {
                case YESTERDAY:
                    presetText = getString(R.string.yesterday);
                    break;
                case LAST_WEEK:
                    presetText = getString(R.string.last_week);
                    break;
                case LAST_MONTH:
                    presetText = getString(R.string.last_month);
                    break;
                case CUSTOM:
                    presetText = getString(R.string.custom);
                    break;
                default:
                    presetText = "✓";
                    break;
            }
            dateFilterChip.setText(getString(R.string.filter_by_date) + " (" + presetText + ")");
            dateFilterChip.setChecked(true);
        } else {
            dateFilterChip.setText(R.string.filter_by_date);
            dateFilterChip.setChecked(false);
        }
    }
    
    private void updateClearFiltersVisibility() {
        String query = (searchView != null) ? searchView.getQuery().toString() : "";
        boolean hasFilters = !query.isEmpty() || !selectedSenderIds.isEmpty() 
                || filterStartDate != null || filterEndDate != null;
        clearFiltersChip.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
    }
}
