package com.example.doan_zaloclone.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.ui.room.RoomActivity;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.viewmodel.HomeViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView conversationsRecyclerView;
    private ConversationAdapter conversationAdapter;
    
    private HomeViewModel homeViewModel;
    private FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Initialize ViewModel
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        firebaseAuth = FirebaseAuth.getInstance();
        
        initViews(view);
        setupRecyclerView();
        observeViewModel();
        loadConversations();
        
        return view;
    }

    private void initViews(View view) {
        conversationsRecyclerView = view.findViewById(R.id.conversationsRecyclerView);
    }

    private void setupRecyclerView() {
        String currentUserId = firebaseAuth.getCurrentUser() != null 
            ? firebaseAuth.getCurrentUser().getUid() 
            : "";
            
        conversationAdapter = new ConversationAdapter(new ArrayList<>(), conversation -> {
            Intent intent = new Intent(getActivity(), RoomActivity.class);
            intent.putExtra("conversationId", conversation.getId());
            
            // Pass the other user's name for display
            String conversationName = conversation.getName();
            if (conversationName == null || conversationName.isEmpty()) {
                conversationName = conversation.getOtherUserName(currentUserId);
            }
            intent.putExtra("conversationName", conversationName);
            startActivity(intent);
        }, currentUserId);
        
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        conversationsRecyclerView.setHasFixedSize(true); // Optimize RecyclerView performance
        conversationsRecyclerView.setAdapter(conversationAdapter);
    }
    
    private void observeViewModel() {
        // Observe conversations LiveData
        String currentUserId = firebaseAuth.getCurrentUser() != null 
            ? firebaseAuth.getCurrentUser().getUid() 
            : "";
            
        homeViewModel.getConversations(currentUserId).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null) return;
            
            if (resource.isLoading()) {
                // Optional: Show loading indicator
                android.util.Log.d("HomeFragment", "Loading conversations...");
            } else if (resource.isSuccess()) {
                List<Conversation> conversations = resource.getData();
                if (conversations != null) {
                    android.util.Log.d("HomeFragment", "Received " + conversations.size() + " conversations");
                    conversationAdapter.updateConversations(conversations);
                }
            } else if (resource.isError()) {
                // Show error message
                String errorMessage = resource.getMessage() != null 
                    ? resource.getMessage() 
                    : "Error loading conversations";
                android.util.Log.e("HomeFragment", "Error: " + errorMessage);
                if (getContext() != null) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadConversations() {
        // Check if user is logged in
        if (firebaseAuth.getCurrentUser() == null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        android.util.Log.d("HomeFragment", "Current User ID: " + currentUserId);
        
        // Conversations are automatically loaded via ViewModel observer
        // The observer in observeViewModel() handles the real-time updates
    }
}

