package com.example.doan_zaloclone.ui.contact;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.FriendRequest;

import java.util.ArrayList;
import java.util.List;

public class ContactFragment extends Fragment {

    private RecyclerView friendRequestsRecyclerView;
    private FriendRequestAdapter friendRequestAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        
        initViews(view);
        setupRecyclerView();
        loadFriendRequests();
        
        return view;
    }

    private void initViews(View view) {
        friendRequestsRecyclerView = view.findViewById(R.id.friendRequestsRecyclerView);
    }

    private void setupRecyclerView() {
        friendRequestAdapter = new FriendRequestAdapter(new ArrayList<>(), 
            new FriendRequestAdapter.FriendRequestListener() {
                @Override
                public void onAccept(FriendRequest request) {
                    Toast.makeText(getContext(), "Accepted friend request", Toast.LENGTH_SHORT).show();
                    loadFriendRequests();
                }

                @Override
                public void onReject(FriendRequest request) {
                    Toast.makeText(getContext(), "Rejected friend request", Toast.LENGTH_SHORT).show();
                    loadFriendRequests();
                }
            });
        
        friendRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        friendRequestsRecyclerView.setAdapter(friendRequestAdapter);
    }

    private void loadFriendRequests() {
        // Demo data
        List<FriendRequest> requests = new ArrayList<>();
        requests.add(new FriendRequest("1", "User Test 1", "test1@mail.com", FriendRequest.Status.PENDING));
        requests.add(new FriendRequest("2", "User Test 2", "test2@mail.com", FriendRequest.Status.PENDING));
        
        friendRequestAdapter.updateRequests(requests);
    }
}
