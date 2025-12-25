package com.example.doan_zaloclone.ui.room;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.repository.FriendRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for selecting a friend's contact to send as a business card
 */
public class SendContactDialog extends Dialog {

    private final FriendRepository friendRepository;
    private final String currentUserId;
    private final LifecycleOwner lifecycleOwner;
    private final List<User> filteredFriends = new ArrayList<>();
    private OnContactSelectedListener listener;
    private EditText searchInput;
    private RecyclerView friendsRecyclerView;
    private TextView emptyView;
    private MaterialButton btnCancel;
    private ContactSelectionAdapter adapter;
    private List<User> allFriends = new ArrayList<>();

    public SendContactDialog(@NonNull Context context, String currentUserId, LifecycleOwner lifecycleOwner) {
        super(context);
        this.currentUserId = currentUserId;
        this.lifecycleOwner = lifecycleOwner;
        this.friendRepository = new FriendRepository();
    }

    public void setOnContactSelectedListener(OnContactSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_send_contact);

        // Make dialog fill width
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        initViews();
        setupRecyclerView();
        setupSearch();
        loadFriends();
    }

    private void initViews() {
        searchInput = findViewById(R.id.searchInput);
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        emptyView = findViewById(R.id.emptyView);
        btnCancel = findViewById(R.id.btnCancel);

        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void setupRecyclerView() {
        adapter = new ContactSelectionAdapter(getContext(), filteredFriends, user -> {
            if (listener != null) {
                listener.onContactSelected(user.getId());
            }
            dismiss();
        });

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendsRecyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFriends(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadFriends() {
        friendRepository.getFriends(currentUserId).observe(lifecycleOwner, resource -> {
            if (resource != null && resource.isSuccess()) {
                allFriends = resource.getData();
                if (allFriends != null) {
                    filteredFriends.clear();
                    filteredFriends.addAll(allFriends);
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                }
            } else if (resource != null && resource.isError()) {
                Toast.makeText(getContext(), "Lỗi tải danh sách bạn bè",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterFriends(String query) {
        filteredFriends.clear();

        if (query.isEmpty()) {
            filteredFriends.addAll(allFriends);
        } else {
            String lowerQuery = query.toLowerCase();
            for (User friend : allFriends) {
                if (friend.getName() != null &&
                        friend.getName().toLowerCase().contains(lowerQuery)) {
                    filteredFriends.add(friend);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (filteredFriends.isEmpty()) {
            friendsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText(searchInput.getText().toString().isEmpty() ?
                    "Bạn chưa có bạn bè nào" : "Không tìm thấy bạn bè");
        } else {
            friendsRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    public interface OnContactSelectedListener {
        void onContactSelected(String userId);
    }
}
