package com.example.doan_zaloclone.ui.room;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doan_zaloclone.R;
import com.example.doan_zaloclone.models.Poll;
import com.example.doan_zaloclone.models.PollOption;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

/**
 * Activity for creating a new poll
 */
public class CreatePollActivity extends AppCompatActivity {
    
    public static final String EXTRA_CONVERSATION_NAME = "conversation_name";
    public static final String EXTRA_POLL_DATA = "poll_data";
    
    private EditText questionEditText;
    private RecyclerView optionsRecyclerView;
    private TextView addOptionButton;
    private TextView createButton;
    private TextView subtitleTextView;
    private TextView deadlineValueText;
    private CheckBox pinCheckBox;
    private SwitchCompat anonymousSwitch;
    private SwitchCompat hideResultsSwitch;
    private SwitchCompat multipleChoiceSwitch;
    private SwitchCompat allowAddOptionsSwitch;
    
    private PollOptionInputAdapter adapter;
    private List<String> options;
    private long selectedDeadline = 0; // 0 = no deadline
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_poll);
        
        initViews();
        setupRecyclerView();
        setupListeners();
        
        // Set conversation name in subtitle
        String conversationName = getIntent().getStringExtra(EXTRA_CONVERSATION_NAME);
        if (conversationName != null && !conversationName.isEmpty()) {
            subtitleTextView.setText(conversationName);
        }
    }
    
    private void initViews() {
        ImageButton backButton = findViewById(R.id.backButton);
        createButton = findViewById(R.id.createButton);
        subtitleTextView = findViewById(R.id.subtitleTextView);
        questionEditText = findViewById(R.id.questionEditText);
        optionsRecyclerView = findViewById(R.id.optionsRecyclerView);
        addOptionButton = findViewById(R.id.addOptionButton);
        deadlineValueText = findViewById(R.id.deadlineValueText);
        pinCheckBox = findViewById(R.id.pinCheckBox);
        anonymousSwitch = findViewById(R.id.anonymousSwitch);
        hideResultsSwitch = findViewById(R.id.hideResultsSwitch);
        multipleChoiceSwitch = findViewById(R.id.multipleChoiceSwitch);
        allowAddOptionsSwitch = findViewById(R.id.allowAddOptionsSwitch);
        
        backButton.setOnClickListener(v -> finish());
    }
    
    private void setupRecyclerView() {
        // Initialize with 2 empty options
        options = new ArrayList<>();
        options.add("");
        options.add("");
        
        adapter = new PollOptionInputAdapter(options, this::updateUI);
        optionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        optionsRecyclerView.setAdapter(adapter);
    }
    
    private void setupListeners() {
        // Add option button
        addOptionButton.setOnClickListener(v -> {
            adapter.addOption();
            // Scroll to the new option
            optionsRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
        });
        
        // Deadline picker
        findViewById(R.id.deadlineContainer).setOnClickListener(v -> showDeadlinePicker());
        
        // Create button
        createButton.setOnClickListener(v -> createPoll());
    }
    
    private void showDeadlinePicker() {
        // Show date picker first
        Calendar calendar = Calendar.getInstance();
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    // Then show time picker
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            this,
                            (timeView, hourOfDay, minute) -> {
                                // Set deadline
                                Calendar selectedCalendar = Calendar.getInstance();
                                selectedCalendar.set(year, month, dayOfMonth, hourOfDay, minute, 0);
                                selectedDeadline = selectedCalendar.getTimeInMillis();
                                
                                // Update UI
                                updateDeadlineText();
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        
        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        
        // Add "No deadline" button
        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Không giới hạn", (dialog, which) -> {
            selectedDeadline = 0;
            updateDeadlineText();
        });
        
        datePickerDialog.show();
    }
    
    private void updateDeadlineText() {
        if (selectedDeadline == 0) {
            deadlineValueText.setText("Không có thời hạn");
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selectedDeadline);
            
            String dateStr = String.format("%02d/%02d/%d %02d:%02d",
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE));
            
            deadlineValueText.setText(dateStr);
        }
    }
    
    private void updateUI() {
        // This is called when options change
        // Could add validation or UI updates here
    }
    
    private void createPoll() {
        // Validate question
        String question = questionEditText.getText().toString().trim();
        if (question.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập câu hỏi bình chọn", Toast.LENGTH_SHORT).show();
            questionEditText.requestFocus();
            return;
        }
        
        // Validate options
        List<String> validOptions = adapter.getValidOptions();
        if (validOptions.size() < 2) {
            Toast.makeText(this, "Cần ít nhất 2 phương án", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check deadline is in future
        if (selectedDeadline > 0 && selectedDeadline <= System.currentTimeMillis()) {
            Toast.makeText(this, "Thời hạn phải trong tương lai", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get current user ID
        String creatorId = FirebaseAuth.getInstance().getCurrentUser() != null 
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
                : "";
        
        if (creatorId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create Poll object
        Poll poll = new Poll(UUID.randomUUID().toString(), question, creatorId);
        
        // Add options
        for (String optionText : validOptions) {
            PollOption option = new PollOption(
                    UUID.randomUUID().toString(), 
                    optionText
            );
            poll.addOption(option);
        }
        
        // Set settings
        poll.setPinned(pinCheckBox.isChecked());
        poll.setAnonymous(anonymousSwitch.isChecked());
        poll.setHideResultsUntilVoted(hideResultsSwitch.isChecked());
        poll.setAllowMultipleChoice(multipleChoiceSwitch.isChecked());
        poll.setAllowAddOptions(allowAddOptionsSwitch.isChecked());
        poll.setExpiresAt(selectedDeadline);
        
        // Return result
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_POLL_DATA, poll);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
