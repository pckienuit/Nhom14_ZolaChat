package com.example.doan_zaloclone.ui.file;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FileCategory;

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
        setupRecyclerView();
        setupSwipeRefresh();
        
        // TODO Phase 2: Load data from ViewModel
        showEmptyState(true);
    }
    
    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        progressBar = view.findViewById(R.id.progressBar);
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
        
        // TODO Phase 2: Set adapter
    }
    
    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            // TODO Phase 2: Reload data
            swipeRefresh.setRefreshing(false);
        });
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
}
