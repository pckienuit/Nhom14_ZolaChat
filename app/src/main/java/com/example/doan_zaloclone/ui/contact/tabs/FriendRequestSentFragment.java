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
import com.example.doan_zaloclone.websocket.SocketManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class FriendRequestSentFragment extends Fragment implements FriendRequestSentAdapter.OnRequestActionListener {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private FriendRequestSentAdapter adapter;
    private ContactViewModel viewModel;
    private String currentUserId;
    
    // WebSocket listener for friend events
    private SocketManager.OnFriendEventListener friendEventListener;

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

        // Setup WebSocket listener for friend request updates
        setupWebSocketListener();
        
        loadRequests();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh when fragment becomes visible (e.g., coming back from other tab/screen)
        loadRequests();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove WebSocket listener
        if (friendEventListener != null) {
            SocketManager.getInstance().removeFriendEventListener(friendEventListener);
            friendEventListener = null;
        }
    }
    
    /**
     * Setup WebSocket listener for friend request events
     * When a request is rejected/accepted, refresh the list
     */
    private void setupWebSocketListener() {
        friendEventListener = new SocketManager.OnFriendEventListener() {
            @Override
            public void onFriendRequestReceived(String senderId, String senderName) {
                // Not relevant for sent requests
            }

            @Override
            public void onFriendRequestAccepted(String userId) {
                // Request was accepted - reload to remove from pending list
                android.util.Log.d("FriendRequestSent", "Request accepted, refreshing list");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadRequests());
                }
            }

            @Override
            public void onFriendRequestRejected(String userId) {
                // Request was rejected - reload to remove from list
                android.util.Log.d("FriendRequestSent", "Request rejected by: " + userId + ", refreshing list");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Lời mời kết bạn đã bị từ chối", Toast.LENGTH_SHORT).show();
                        loadRequests();
                    });
                }
            }
            
            @Override
            public void onFriendRequestCancelled(String senderId) {
                // Not relevant - we are the sender, we already know
            }

            @Override
            public void onFriendAdded(String userId) {
                // Not directly relevant, but refresh anyway
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadRequests());
                }
            }

            @Override
            public void onFriendRemoved(String userId) {
                // Not relevant for sent requests
            }
        };
        
        SocketManager.getInstance().addFriendEventListener(friendEventListener);
    }

    private void loadRequests() {
        if (currentUserId == null) return;

        // Use dedicated API for sent requests
        viewModel.getSentFriendRequests(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                List<FriendRequest> sentRequests = resource.getData();

                if (sentRequests != null && !sentRequests.isEmpty()) {
                    adapter.setRequests(sentRequests);
                    emptyView.setVisibility(View.GONE);
                } else {
                    adapter.setRequests(null);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText("Chưa gửi lời mời nào");
                }
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(getContext(), "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                adapter.setRequests(null);
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setText("Không thể tải danh sách");
            }
        });
    }

    @Override
    public void onRecall(FriendRequest request) {
        // Show confirmation dialog before cancelling
        if (getContext() == null) return;
        
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Thu hồi lời mời")
                .setMessage("Bạn có chắc muốn thu hồi lời mời kết bạn gửi đến " + 
                        (request.getToUserName() != null ? request.getToUserName() : "người này") + "?")
                .setPositiveButton("Thu hồi", (dialog, which) -> {
                    // Call ViewModel to cancel the request
                    viewModel.cancelFriendRequest(request).observe(getViewLifecycleOwner(), resource -> {
                        if (resource.getStatus() == Resource.Status.SUCCESS) {
                            Toast.makeText(getContext(), "Đã thu hồi lời mời", Toast.LENGTH_SHORT).show();
                            // Reload the list
                            loadRequests();
                        } else if (resource.getStatus() == Resource.Status.ERROR) {
                            Toast.makeText(getContext(), "Lỗi: " + resource.getMessage(), Toast.LENGTH_SHORT).show();
                        } else if (resource.getStatus() == Resource.Status.LOADING) {
                            // Could show a progress indicator here
                            android.util.Log.d("FriendRequestSent", "Cancelling request...");
                        }
                    });
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }
}
