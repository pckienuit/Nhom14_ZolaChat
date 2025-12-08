package com.example.doan_zaloclone.ui.room;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Message;

import java.util.ArrayList;
import java.util.List;

public class RoomActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView titleTextView;
    private RecyclerView messagesRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    
    private MessageAdapter messageAdapter;
    private String conversationId;
    private String conversationName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        conversationId = getIntent().getStringExtra("conversationId");
        conversationName = getIntent().getStringExtra("conversationName");

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadMessages();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        titleTextView = findViewById(R.id.titleTextView);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        titleTextView.setText(conversationName);
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(new ArrayList<>(), "currentUserId");
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void loadMessages() {
        // Demo data
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("1", "currentUserId", "Xin chào!", System.currentTimeMillis()));
        messages.add(new Message("2", "otherUserId", "Chào bạn!", System.currentTimeMillis()));
        messages.add(new Message("3", "currentUserId", "Bạn khỏe không?", System.currentTimeMillis()));
        
        messageAdapter.updateMessages(messages);
        messagesRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> handleSendMessage());
    }

    private void handleSendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (!messageText.isEmpty()) {
            // Demo: Add message to list
            Message newMessage = new Message(
                String.valueOf(System.currentTimeMillis()),
                "currentUserId",
                messageText,
                System.currentTimeMillis()
            );
            
            List<Message> currentMessages = new ArrayList<>(messageAdapter.getMessages());
            currentMessages.add(newMessage);
            messageAdapter.updateMessages(currentMessages);
            
            messageEditText.setText("");
            messagesRecyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }
}
