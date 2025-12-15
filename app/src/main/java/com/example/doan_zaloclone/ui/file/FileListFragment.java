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
    
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyStateLayout;
    private ProgressBar progressBar;
    
    private FileViewModel viewModel;
    private FileAdapter adapter;
    
    // Filter UI components
    private SearchView searchView;
    private Chip senderFilterChip;
    private Chip dateFilterChip;
    private Chip clearFiltersChip;
    
    // Filter state
    private Set<String> selectedSenderIds = new HashSet<>();
    private Long filterStartDate = null;
    private Long filterEndDate = null;
    
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
        setupSearchView();
        setupFilterChips();
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
        
        // Filter UI
        searchView = view.findViewById(R.id.searchView);
        senderFilterChip = view.findViewById(R.id.senderFilterChip);
        dateFilterChip = view.findViewById(R.id.dateFilterChip);
        clearFiltersChip = view.findViewById(R.id.clearFiltersChip);
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
    
    private void setupSearchView() {
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
        Calendar calendar = Calendar.getInstance();
        
        // Show start date picker
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar startCal = Calendar.getInstance();
            startCal.set(year, month, dayOfMonth, 0, 0, 0);
            filterStartDate = startCal.getTimeInMillis();
            
            // Show end date picker
            new DatePickerDialog(requireContext(), (view2, year2, month2, dayOfMonth2) -> {
                Calendar endCal = Calendar.getInstance();
                endCal.set(year2, month2, dayOfMonth2, 23, 59, 59);
                filterEndDate = endCal.getTimeInMillis();
                
                viewModel.setDateRange(filterStartDate, filterEndDate);
                updateDateChipText();
                updateClearFiltersVisibility();
            }, year, month, dayOfMonth).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }
    
    private void clearAllFilters() {
        searchView.setQuery("", false);
        selectedSenderIds.clear();
        filterStartDate = null;
        filterEndDate = null;
        
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
            dateFilterChip.setText(getString(R.string.filter_by_date) + " ✓");
            dateFilterChip.setChecked(true);
        } else {
            dateFilterChip.setText(R.string.filter_by_date);
            dateFilterChip.setChecked(false);
        }
    }
    
    private void updateClearFiltersVisibility() {
        String query = searchView.getQuery().toString();
        boolean hasFilters = !query.isEmpty() || !selectedSenderIds.isEmpty() 
                || filterStartDate != null || filterEndDate != null;
        clearFiltersChip.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
    }
}
