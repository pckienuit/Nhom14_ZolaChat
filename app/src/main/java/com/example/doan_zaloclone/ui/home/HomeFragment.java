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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.ui.room.RoomActivity;
import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView conversationsRecyclerView;
    private ConversationAdapter conversationAdapter;
    
    private FirestoreManager firestoreManager;
    private FirebaseAuth firebaseAuth;
    private ListenerRegistration conversationsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        firestoreManager = FirestoreManager.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        
        initViews(view);
        setupRecyclerView();
        loadConversations();
        
        return view;
    }

    private void initViews(View view) {
        conversationsRecyclerView = view.findViewById(R.id.conversationsRecyclerView);
    }

    private void setupRecyclerView() {
        conversationAdapter = new ConversationAdapter(new ArrayList<>(), conversation -> {
            Intent intent = new Intent(getActivity(), RoomActivity.class);
            intent.putExtra("conversationId", conversation.getId());
            intent.putExtra("conversationName", conversation.getName());
            startActivity(intent);
        });
        
        conversationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        conversationsRecyclerView.setAdapter(conversationAdapter);
    }

    private void loadConversations() {
        // Check if user is logged in
        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        
        // Listen to conversations from Firestore
        conversationsListener = firestoreManager.listenToConversations(currentUserId, 
            new FirestoreManager.OnConversationsChangedListener() {
                @Override
                public void onConversationsChanged(List<Conversation> conversations) {
                    if (getActivity() != null) {
                        conversationAdapter.updateConversations(conversations);
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (getActivity() != null) {
                        Toast.makeText(getContext(), 
                            "Error loading conversations: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up Firestore listener
        if (conversationsListener != null) {
            conversationsListener.remove();
        }
    }
}

