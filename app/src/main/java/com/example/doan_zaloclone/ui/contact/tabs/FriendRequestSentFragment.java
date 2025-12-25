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
import com.example.doan_zaloclone.ui.contact.FriendRequestSentAdapter;
import com.example.doan_zaloclone.viewmodel.ContactViewModel;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class FriendRequestSentFragment extends Fragment implements FriendRequestSentAdapter.OnRequestActionListener {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private FriendRequestSentAdapter adapter;
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
        
        adapter = new FriendRequestSentAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        currentUserId = FirebaseAuth.getInstance().getUid();

        loadRequests();
    }

    private void loadRequests() {
        if (currentUserId == null) return;

        // Currently re-using getFriendRequests, hoping it returns sent requests too
        // or uses a different endpoint in future.
        // For now this might return empty for Sent requests if API doesn't support it.
        viewModel.getFriendRequests(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                List<FriendRequest> allRequests = resource.getData();
                List<FriendRequest> sentRequests = new ArrayList<>();
                
                if (allRequests != null) {
                    for (FriendRequest req : allRequests) {
                        // Filter for requests where I am the sender
                        if (currentUserId.equals(req.getFromUserId())) {
                            sentRequests.add(req);
                        }
                    }
                }

                if (!sentRequests.isEmpty()) {
                    adapter.setRequests(sentRequests);
                    emptyView.setVisibility(View.GONE);
                } else {
                    adapter.setRequests(null);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("Chưa gửi lời mời nào");
                }
            }
        });
    }

    @Override
    public void onRecall(FriendRequest request) {
        // Implement recall logic
        // If API supports 'cancel', use it. Otherwise 'reject' or 'unfriend' might work depending on backend.
        // For now, show toast
        Toast.makeText(getContext(), "Tính năng thu hồi đang phát triển", Toast.LENGTH_SHORT).show();
    }
}
