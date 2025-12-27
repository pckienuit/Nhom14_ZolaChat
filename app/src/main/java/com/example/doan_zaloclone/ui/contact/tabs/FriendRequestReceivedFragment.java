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
import com.example.doan_zaloclone.websocket.SocketManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class FriendRequestReceivedFragment extends Fragment implements FriendRequestReceivedAdapter.OnRequestActionListener {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private FriendRequestReceivedAdapter adapter;
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
        
        adapter = new FriendRequestReceivedAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Setup WebSocket listener for new friend requests
        setupWebSocketListener();
        
        loadRequests();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Refresh when fragment becomes visible
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
     * Setup WebSocket listener for new friend request events
     */
    private void setupWebSocketListener() {
        friendEventListener = new SocketManager.OnFriendEventListener() {
            @Override
            public void onFriendRequestReceived(String senderId, String senderName) {
                // New request received - reload list
                android.util.Log.d("FriendRequestReceived", "New request from: " + senderName);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), senderName + " muốn kết bạn", Toast.LENGTH_SHORT).show();
                        loadRequests();
                    });
                }
            }

            @Override
            public void onFriendRequestAccepted(String userId) {
                // Not relevant for received requests
            }

            @Override
            public void onFriendRequestRejected(String userId) {
                // Not relevant for received requests
            }
            
            @Override
            public void onFriendRequestCancelled(String senderId) {
                // Sender cancelled their request - reload to remove from list
                android.util.Log.d("FriendRequestReceived", "Request cancelled by sender: " + senderId);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Lời mời kết bạn đã bị thu hồi", Toast.LENGTH_SHORT).show();
                        loadRequests();
                    });
                }
            }

            @Override
            public void onFriendAdded(String userId) {
                // After accepting, refresh to remove from pending
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadRequests());
                }
            }

            @Override
            public void onFriendRemoved(String userId) {
                // Not relevant for received requests
            }
            
            @Override
            public void onFriendStatusChanged(String friendId, boolean isOnline) {
                // Not relevant for friend requests
            }
        };
        
        SocketManager.getInstance().addFriendEventListener(friendEventListener);
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
