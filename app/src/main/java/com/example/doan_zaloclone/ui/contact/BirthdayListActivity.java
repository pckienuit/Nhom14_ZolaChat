package com.example.doan_zaloclone.ui.contact;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BirthdayListActivity - Display friends' birthdays sorted by nearest date
 */
public class BirthdayListActivity extends AppCompatActivity {

    private RecyclerView birthdayRecyclerView;
    private BirthdayAdapter birthdayAdapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ContactViewModel contactViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_birthday_list);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sinh nhật");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize views
        birthdayRecyclerView = findViewById(R.id.birthdayRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // Setup RecyclerView
        birthdayAdapter = new BirthdayAdapter(new ArrayList<>());
        birthdayRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        birthdayRecyclerView.setAdapter(birthdayAdapter);

        // Setup ViewModel
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);

        // Load friends with birthdays
        loadBirthdays();
    }

    private void loadBirthdays() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        contactViewModel.getFriends(currentUserId).observe(this, resource -> {
            progressBar.setVisibility(View.GONE);

            if (resource == null) return;

            if (resource.isSuccess()) {
                List<User> friends = resource.getData();
                if (friends != null && !friends.isEmpty()) {
                    // Filter and sort friends by birthday
                    List<User> friendsWithBirthdays = filterAndSortByBirthday(friends);
                    
                    if (friendsWithBirthdays.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("Không có bạn bè nào có ngày sinh nhật");
                    } else {
                        birthdayAdapter.updateBirthdays(friendsWithBirthdays);
                    }
                } else {
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("Chưa có bạn bè");
                }
            } else if (resource.isError()) {
                Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("Không thể tải danh sách");
            }
        });
    }

    /**
     * Filter friends who have birthday info and sort by nearest birthday
     */
    private List<User> filterAndSortByBirthday(List<User> friends) {
        List<User> friendsWithBirthdays = new ArrayList<>();
        Calendar today = Calendar.getInstance();
        int currentDayOfYear = today.get(Calendar.DAY_OF_YEAR);

        for (User friend : friends) {
            if (friend.getBirthday() != null && !friend.getBirthday().isEmpty()) {
                friendsWithBirthdays.add(friend);
            }
        }

        // Sort by nearest birthday (from today)
        Collections.sort(friendsWithBirthdays, (u1, u2) -> {
            int days1 = getDaysUntilBirthday(u1.getBirthday(), currentDayOfYear);
            int days2 = getDaysUntilBirthday(u2.getBirthday(), currentDayOfYear);
            return Integer.compare(days1, days2);
        });

        return friendsWithBirthdays;
    }

    /**
     * Calculate days until next birthday from today
     */
    private int getDaysUntilBirthday(String birthdayStr, int currentDayOfYear) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date birthday = sdf.parse(birthdayStr);
            if (birthday == null) return Integer.MAX_VALUE;

            Calendar birthdayCal = Calendar.getInstance();
            birthdayCal.setTime(birthday);
            
            // Set to this year for comparison
            birthdayCal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
            int birthdayDayOfYear = birthdayCal.get(Calendar.DAY_OF_YEAR);

            int daysUntil = birthdayDayOfYear - currentDayOfYear;
            
            // If birthday already passed this year, calculate for next year
            if (daysUntil < 0) {
                daysUntil += 365; // Add days in a year
            }

            return daysUntil;
        } catch (Exception e) {
            return Integer.MAX_VALUE; // Invalid date, put at end
        }
    }
}
