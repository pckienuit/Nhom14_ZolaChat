package com.example.doan_zaloclone.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.ui.room.RoomActivity;
import com.example.doan_zaloclone.models.Conversation;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView conversationsRecyclerView;
    private ConversationAdapter conversationAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
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
        // Demo data
        List<Conversation> conversations = new ArrayList<>();
        conversations.add(new Conversation("1", "User Test 1", "Chào bạn!", System.currentTimeMillis()));
        conversations.add(new Conversation("2", "User Test 2", "Hẹn gặp lại!", System.currentTimeMillis()));
        conversations.add(new Conversation("3", "User Test 3", "Cảm ơn bạn", System.currentTimeMillis()));
        
        conversationAdapter.updateConversations(conversations);
    }
}
