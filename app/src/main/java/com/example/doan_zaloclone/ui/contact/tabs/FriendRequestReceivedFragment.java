package com.example.doan_zaloclone.ui.contact.tabs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FriendRequest;
import com.example.doan_zaloclone.ui.contact.FriendRequestReceivedAdapter;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class FriendRequestReceivedFragment extends Fragment implements FriendRequestReceivedAdapter.OnRequestActionListener {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private FriendRequestReceivedAdapter adapter;
    private ContactViewModel viewModel;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_request_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        
        adapter = new FriendRequestReceivedAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        currentUserId = FirebaseAuth.getInstance().getUid();

        loadRequests();
    }

    private void loadRequests() {
        if (currentUserId == null) return;

        viewModel.getFriendRequests(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                List<FriendRequest> requests = resource.getData();
                if (requests != null && !requests.isEmpty()) {
                    adapter.setRequests(requests);
                    emptyView.setVisibility(View.GONE);
                } else {
                    adapter.setRequests(null);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("Không có lời mời kết bạn nào");
                }
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(getContext(), resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAccept(FriendRequest request) {
        viewModel.acceptFriendRequest(request).observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                Toast.makeText(getContext(), "Đã đồng ý kết bạn", Toast.LENGTH_SHORT).show();
                loadRequests(); // Reload
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(getContext(), "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDecline(FriendRequest request) {
        viewModel.rejectFriendRequest(request).observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                Toast.makeText(getContext(), "Đã từ chối", Toast.LENGTH_SHORT).show();
                loadRequests(); // Reload
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(getContext(), "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
