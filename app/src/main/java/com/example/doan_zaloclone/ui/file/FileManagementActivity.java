package com.example.doan_zaloclone.ui.file;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FileCategory;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Activity for managing and viewing files shared in a conversation
 */
public class FileManagementActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_CONVERSATION_NAME = "conversation_name";

    private String conversationId;
    private String conversationName;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FilePagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display BEFORE setContentView
        setupEdgeToEdge();
        
        setContentView(R.layout.activity_file_management);

        // Get data from Intent
        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        conversationName = getIntent().getStringExtra(EXTRA_CONVERSATION_NAME);

        if (conversationId == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupViewPager();
        setupTabs();
    }
    
    private void setupEdgeToEdge() {
        // Make status bar transparent
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Use WindowInsetsController
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            // Pre-Android 11 - Use flags
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupViewPager() {
        pagerAdapter = new FilePagerAdapter(this, conversationId);
        viewPager.setAdapter(pagerAdapter);

        // Disable over-scroll effect
        viewPager.setOverScrollMode(ViewPager2.OVER_SCROLL_NEVER);
    }

    private void setupTabs() {
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            FileCategory category = FileCategory.fromPosition(position);
            tab.setText(category.getDisplayName());
        }).attach();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
