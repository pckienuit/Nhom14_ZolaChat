package com.example.doan_zaloclone.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.models.FileCategory;
import com.example.doan_zaloclone.models.FileItem;
import com.example.doan_zaloclone.models.Message;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Repository for file management operations
 * Handles querying, categorizing, and enriching file data from Firestore
 */
public class FileRepository {

    private static final String TAG = "FileRepository";
    private static final int DEFAULT_PAGE_SIZE = 50;

    // URL pattern for extracting links from text messages
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[^\\s]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final FirestoreManager firestoreManager;
    private ListenerRegistration fileListener;

    public FileRepository() {
        this.firestoreManager = FirestoreManager.getInstance();
    }

    /**
     * Get all files for a conversation with real-time updates
     *
     * @param conversationId ID of the conversation
     * @param limit          Maximum number of items to load
     * @return LiveData containing Resource with list of FileItems
     */
    public LiveData<Resource<List<FileItem>>> getFilesForConversation(
            @NonNull String conversationId,
            int limit) {

        MutableLiveData<Resource<List<FileItem>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // Remove existing listener if any
        if (fileListener != null) {
            fileListener.remove();
        }

        // Query messages with files or images, ordered by timestamp descending
        fileListener = firestoreManager.getFirestore()
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            android.util.Log.e(TAG, "Listen failed.", error);
                            result.setValue(Resource.error("Lỗi đồng bộ file: " + error.getMessage(), null));
                            return;
                        }

                        if (value != null) {
                            List<Message> messages = new ArrayList<>();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                Message message = doc.toObject(Message.class);
                                if (message != null) {
                                    message.setId(doc.getId());
                                    messages.add(message);
                                }
                            }

                            // Convert to FileItems and enrich with sender info
                            enrichWithSenderInfo(messages, enrichedItems -> {
                                result.setValue(Resource.success(enrichedItems));
                            }, errorMsg -> {
                                result.setValue(Resource.error(errorMsg, null));
                            });
                        }
                    }
                });

        return result;
    }

    /**
     * Load more files for pagination (remains one-time fetch)
     *
     * @param conversationId ID of the conversation
     * @param lastTimestamp  Timestamp of the last loaded message
     * @param limit          Maximum number of items to load
     * @return LiveData containing Resource with list of FileItems
     */
    public LiveData<Resource<List<FileItem>>> loadMoreFiles(
            @NonNull String conversationId,
            long lastTimestamp,
            int limit) {

        MutableLiveData<Resource<List<FileItem>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firestoreManager.getFirestore()
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .whereLessThan("timestamp", lastTimestamp)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Message> messages = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Message message = doc.toObject(Message.class);
                        if (message != null) {
                            message.setId(doc.getId());
                            messages.add(message);
                        }
                    }

                    enrichWithSenderInfo(messages, enrichedItems -> {
                        result.setValue(Resource.success(enrichedItems));
                    }, error -> {
                        result.setValue(Resource.error(error, null));
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "Error loading more files", e);
                    result.setValue(Resource.error("Lỗi tải thêm file: " + e.getMessage(), null));
                });

        return result;
    }

    /**
     * Categorize messages into MEDIA, FILES, and LINKS
     *
     * @param fileItems List of FileItems to categorize
     * @return Map of FileCategory to List of FileItems
     */
    public Map<FileCategory, List<FileItem>> categorizeFiles(@NonNull List<FileItem> fileItems) {
        Map<FileCategory, List<FileItem>> categorized = new HashMap<>();
        categorized.put(FileCategory.MEDIA, new ArrayList<>());
        categorized.put(FileCategory.FILES, new ArrayList<>());
        categorized.put(FileCategory.LINKS, new ArrayList<>());

        for (FileItem item : fileItems) {
            FileCategory category = item.getCategoryType();
            List<FileItem> categoryList = categorized.get(category);
            if (categoryList != null) {
                categoryList.add(item);
            }
        }

        return categorized;
    }

    /**
     * Extract links from text messages
     *
     * @param messages List of messages to extract links from
     * @return List of FileItems for links
     */
    public List<FileItem> extractLinksFromMessages(@NonNull List<Message> messages) {
        List<FileItem> linkItems = new ArrayList<>();

        for (Message message : messages) {
            if (Message.TYPE_TEXT.equals(message.getType())) {
                String content = message.getContent();
                if (content != null) {
                    Matcher matcher = URL_PATTERN.matcher(content);
                    if (matcher.find()) {
                        // Found a URL in this message
                        FileItem linkItem = new FileItem(message);
                        linkItems.add(linkItem);
                    }
                }
            }
        }

        return linkItems;
    }

    /**
     * Enrich FileItems with sender information
     *
     * @param messages  List of messages to enrich
     * @param onSuccess Callback when enrichment succeeds
     * @param onError   Callback when enrichment fails
     */
    private void enrichWithSenderInfo(
            @NonNull List<Message> messages,
            @NonNull OnEnrichCallback onSuccess,
            @NonNull OnErrorCallback onError) {

        if (messages.isEmpty()) {
            onSuccess.onEnriched(new ArrayList<>());
            return;
        }

        // Filter messages to only include relevant types
        // Exclude TEXT messages that don't contain URLs
        List<Message> relevantMessages = new ArrayList<>();
        for (Message message : messages) {
            if (Message.TYPE_IMAGE.equals(message.getType()) ||
                    Message.TYPE_FILE.equals(message.getType())) {
                relevantMessages.add(message);
            } else if (Message.TYPE_TEXT.equals(message.getType())) {
                // Only include text messages with URLs
                String content = message.getContent();
                if (content != null && URL_PATTERN.matcher(content).find()) {
                    relevantMessages.add(message);
                }
            }
        }

        // If no relevant messages after filtering
        if (relevantMessages.isEmpty()) {
            onSuccess.onEnriched(new ArrayList<>());
            return;
        }

        // Collect unique sender IDs from relevant messages
        Map<String, String> senderNames = new HashMap<>();
        Map<String, String> senderAvatars = new HashMap<>();
        List<String> uniqueSenderIds = new ArrayList<>();

        for (Message message : relevantMessages) {
            String senderId = message.getSenderId();
            if (senderId != null && !uniqueSenderIds.contains(senderId)) {
                uniqueSenderIds.add(senderId);
            }
        }

        // Batch fetch user data
        final int[] pendingRequests = {uniqueSenderIds.size()};
        final boolean[] hasError = {false};

        if (uniqueSenderIds.isEmpty()) {
            // No senders to fetch
            List<FileItem> fileItems = new ArrayList<>();
            for (Message message : relevantMessages) {
                fileItems.add(new FileItem(message, "Unknown", null));
            }
            onSuccess.onEnriched(fileItems);
            return;
        }

        for (String senderId : uniqueSenderIds) {
            firestoreManager.getFirestore()
                    .collection("users")
                    .document(senderId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            User user = doc.toObject(User.class);
                            if (user != null) {
                                senderNames.put(senderId, user.getName() != null ? user.getName() : "User");
                                senderAvatars.put(senderId, user.getAvatarUrl());
                            }
                        } else {
                            senderNames.put(senderId, "User");
                        }

                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0 && !hasError[0]) {
                            // All requests completed, create FileItems
                            List<FileItem> fileItems = new ArrayList<>();
                            for (Message message : relevantMessages) {
                                String senderName = senderNames.get(message.getSenderId());
                                String senderAvatar = senderAvatars.get(message.getSenderId());
                                fileItems.add(new FileItem(
                                        message,
                                        senderName != null ? senderName : "User",
                                        senderAvatar
                                ));
                            }
                            onSuccess.onEnriched(fileItems);
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.w(TAG, "Error fetching sender info for " + senderId, e);
                        senderNames.put(senderId, "User");

                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0 && !hasError[0]) {
                            hasError[0] = true;
                            // Continue with partial data
                            List<FileItem> fileItems = new ArrayList<>();
                            for (Message message : relevantMessages) {
                                String senderName = senderNames.get(message.getSenderId());
                                String senderAvatar = senderAvatars.get(message.getSenderId());
                                fileItems.add(new FileItem(
                                        message,
                                        senderName != null ? senderName : "User",
                                        senderAvatar
                                ));
                            }
                            onSuccess.onEnriched(fileItems);
                        }
                    });
        }
    }

    /**
     * Stop listening to updates
     */
    public void cleanup() {
        if (fileListener != null) {
            fileListener.remove();
            fileListener = null;
        }
    }

    // Callback interfaces
    public interface OnEnrichCallback {
        void onEnriched(List<FileItem> fileItems);
    }

    public interface OnErrorCallback {
        void onError(String error);
    }
}
