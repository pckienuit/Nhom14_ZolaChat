package com.example.doan_zaloclone.ui.file;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

            // Set subtitle with conversation name if provided
            if (conversationName != null && !conversationName.isEmpty()) {
                getSupportActionBar().setSubtitle(conversationName);
            }
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
