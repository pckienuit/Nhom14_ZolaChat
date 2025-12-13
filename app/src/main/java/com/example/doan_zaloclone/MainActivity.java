package com.example.doan_zaloclone;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.doan_zaloclone.ui.contact.ContactFragment;
import com.example.doan_zaloclone.ui.home.HomeFragment;
import com.example.doan_zaloclone.ui.login.LoginActivity;
import com.example.doan_zaloclone.viewmodel.MainViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private MainViewModel mainViewModel;
    
    // Cache fragments to preserve state
    private HomeFragment homeFragment;
    private ContactFragment contactFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        
        if (savedInstanceState == null) {
            // Show home fragment on first launch
            showFragment(0);
        }

        setupBottomNavigation();
        observeViewModel();
    }
    
    private void observeViewModel() {
        // Observe logout state
        mainViewModel.getLogoutState().observe(this, resource -> {
            if (resource == null) return;
            
            if (resource.isSuccess()) {
                // Logout successful, navigate to login
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("FROM_LOGOUT", true);
                startActivity(intent);
                finish();
            } else if (resource.isError()) {
                // Show error message
                Toast.makeText(this, 
                    "Logout failed: " + resource.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            handleLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void handleLogout() {
        // Trigger logout operation via ViewModel
        mainViewModel.logout();
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                showFragment(0);
                return true;
            } else if (itemId == R.id.nav_contact) {
                showFragment(1);
                return true;
            }
            return false;
        });
    }

    private void showFragment(int position) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        // Disable default animations to prevent flicker
        transaction.setCustomAnimations(0, 0);
        
        // Hide all fragments first
        if (homeFragment != null) {
            transaction.hide(homeFragment);
        }
        if (contactFragment != null) {
            transaction.hide(contactFragment);
        }
        
        // Show or create the selected fragment
        switch (position) {
            case 0: // Home
                if (homeFragment == null) {
                    homeFragment = new HomeFragment();
                    transaction.add(R.id.fragmentContainer, homeFragment, "HOME");
                } else {
                    transaction.show(homeFragment);
                }
                break;
            case 1: // Contact
                if (contactFragment == null) {
                    contactFragment = new ContactFragment();
                    transaction.add(R.id.fragmentContainer, contactFragment, "CONTACT");
                } else {
                    transaction.show(contactFragment);
                }
                break;
        }
        
        transaction.commit();
    }
}