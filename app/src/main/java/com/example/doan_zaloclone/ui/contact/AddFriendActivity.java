package com.example.doan_zaloclone.ui.contact;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class AddFriendActivity extends AppCompatActivity {

    private EditText etSearch;
    private Button btnSearch;
    private FrameLayout searchResultContainer;
    private LinearLayout otherOptions;
    private TextView tvNotFound;
    
    // Result View
    private ImageView imgAvatar;
    private TextView tvName;
    private TextView tvPhone;
    private Button btnAddFriend;
    
    private ContactViewModel viewModel;
    private User foundUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend); // Ensure this matches the layout file name

        viewModel = new ViewModelProvider(this).get(ContactViewModel.class);

        initViews();
        setupListeners();
        observeViewModel();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        searchResultContainer = findViewById(R.id.searchResultContainer);
        otherOptions = findViewById(R.id.otherOptions);
        tvNotFound = findViewById(R.id.tvNotFound);
        
        imgAvatar = findViewById(R.id.imgAvatar);
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        btnAddFriend = findViewById(R.id.btnAddFriend);
    }

    private void setupListeners() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSearch.setEnabled(s.length() > 0);
                if (s.length() > 0) {
                    btnSearch.setBackgroundTintList(getColorStateList(R.color.colorPrimary)); // Adjust color if needed
                } else {
                     // Dim color
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });
        
        btnAddFriend.setOnClickListener(v -> {
            if (foundUser != null) {
                sendFriendRequest(foundUser);
            }
        });
    }

    private void performSearch(String query) {
        viewModel.searchUsers(query);
    }
    
    private void sendFriendRequest(User user) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        String currentUserName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (currentUserName == null) currentUserName = "User";
        
        viewModel.sendFriendRequest(currentUserId, user.getId(), currentUserName)
            .observe(this, resource -> {
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    Toast.makeText(this, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
                    btnAddFriend.setText("ĐÃ GỬI LỜI MỜI");
                    btnAddFriend.setEnabled(false);
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    Toast.makeText(this, "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void observeViewModel() {
        viewModel.getSearchResults().observe(this, resource -> {
            if (resource == null) return;
            
            if (resource.getStatus() == Resource.Status.LOADING) {
                // Show loading?
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                List<User> users = resource.getData();
                processSearchResults(users);
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, "Lỗi tìm kiếm: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processSearchResults(List<User> users) {
        otherOptions.setVisibility(View.GONE);
        searchResultContainer.setVisibility(View.VISIBLE);
        
        if (users != null && !users.isEmpty()) {
            // Take the first match for simplicity as per Zalo logic (usually exact match phone)
            // Or if list, maybe show a list? Current layout is for single user.
            // Let's assume we show the first one.
            foundUser = users.get(0);
            
            tvNotFound.setVisibility(View.GONE);
            ((View)imgAvatar.getParent().getParent()).setVisibility(View.VISIBLE); // Show result layout
            
            tvName.setText(foundUser.getName());
            tvPhone.setText(foundUser.getEmail()); // Or phone if available
            
            if (foundUser.getAvatarUrl() != null && !foundUser.getAvatarUrl().isEmpty()) {
                Glide.with(this).load(foundUser.getAvatarUrl()).into(imgAvatar);
            } else {
                imgAvatar.setImageResource(R.drawable.ic_avatar);
            }
            
            // Check status to update button (Friend, Sent, None)
            // Implementation skipped for brevity (requires checkFriendStatus call)
            btnAddFriend.setText("KẾT BẠN");
            btnAddFriend.setEnabled(true);
            
        } else {
            foundUser = null;
            tvNotFound.setVisibility(View.VISIBLE);
            ((View)imgAvatar.getParent().getParent()).setVisibility(View.GONE); // Hide result layout
        }
    }
}
