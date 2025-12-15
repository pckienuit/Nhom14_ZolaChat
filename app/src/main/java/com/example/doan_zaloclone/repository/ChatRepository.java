package com.example.doan_zaloclone.repository;

import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.models.Message;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class for handling chat/messaging operations with Firestore
 * Now supports both LiveData (for ViewModels) and callbacks (for backward compatibility)
 */
public class ChatRepository {

    private final FirebaseFirestore firestore;
    private final FirestoreManager firestoreManager;
    private ListenerRegistration messagesListener;

    public ChatRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.firestoreManager = FirestoreManager.getInstance();
    }

    /**
     * Send a message to a conversation (LiveData version)
     * @param conversationId ID of the conversation
     * @param message Message object to send (ID will be auto-generated)
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> sendMessageLiveData(@NonNull String conversationId, 
                                                             @NonNull Message message) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        sendMessage(conversationId, message, new SendMessageCallback() {
            @Override
            public void onSuccess() {
                result.setValue(Resource.success(true));
            }
            
            @Override
            public void onError(String error) {
                result.setValue(Resource.error(error));
            }
        });
        
        return result;
    }

    /**
     * Send a message to a conversation (callback version - for backward compatibility)
     * @param conversationId ID of the conversation
     * @param message Message object to send (ID will be auto-generated)
     * @param callback Callback for success/error
     */
    public void sendMessage(String conversationId, Message message, SendMessageCallback callback) {
        // Auto-generate message ID
        DocumentReference messageRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document();

        // Set the message ID
        message.setId(messageRef.getId());

        // Convert message to Map for Firestore
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("id", message.getId());
        messageData.put("senderId", message.getSenderId());
        messageData.put("content", message.getContent());
        messageData.put("type", message.getType());
        messageData.put("timestamp", message.getTimestamp());
        
        // Add file metadata if this is a file message
        if (Message.TYPE_FILE.equals(message.getType())) {
            messageData.put("fileName", message.getFileName());
            messageData.put("fileSize", message.getFileSize());
            messageData.put("fileMimeType", message.getFileMimeType());
        }

        // Save message to Firestore
        messageRef.set(messageData)
                .addOnSuccessListener(aVoid -> {
                    // Update conversation's lastMessage
                    String lastMessageText = message.getContent();
                    
                    // For file messages, show user-friendly text instead of URL
                    if (Message.TYPE_FILE.equals(message.getType())) {
                        lastMessageText = "Bạn đã gửi một file";
                    }
                    
                    updateConversationLastMessage(conversationId, lastMessageText, message.getTimestamp());
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to send message";
                    callback.onError(errorMessage);
                });
    }

    /**
     * Listen to messages in a conversation with real-time updates (LiveData version)
     * @param conversationId ID of the conversation
     * @return LiveData containing Resource with list of messages
     */
    public LiveData<Resource<List<Message>>> getMessagesLiveData(@NonNull String conversationId) {
        MutableLiveData<Resource<List<Message>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // Remove previous listener if exists
        if (messagesListener != null) {
            messagesListener.remove();
        }
        
        // Set up real-time listener
        messagesListener = listenToMessages(conversationId, new MessagesListener() {
            @Override
            public void onMessagesChanged(List<Message> messages) {
                result.setValue(Resource.success(messages));
            }
            
            @Override
            public void onError(String error) {
                result.setValue(Resource.error(error));
            }
        });
        
        return result;
    }

    /**
     * Listen to messages in a conversation (callback version - for backward compatibility)
     * @param conversationId ID of the conversation
     * @param listener Listener for message updates
     * @return ListenerRegistration for cleanup
     */
    public ListenerRegistration listenToMessages(String conversationId, MessagesListener listener) {
        return firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage() != null ? error.getMessage() : "Failed to load messages");
                        return;
                    }

                    if (querySnapshot != null) {
                        List<Message> messages = new ArrayList<>();
                        querySnapshot.forEach(document -> {
                            Message message = document.toObject(Message.class);
                            messages.add(message);
                        });
                        listener.onMessagesChanged(messages);
                    }
                });
    }

    /**
     * Update conversation's lastMessage and timestamp
     * @param conversationId ID of the conversation
     * @param lastMessage Content of the last message
     * @param timestamp Timestamp of the last message
     */
    private void updateConversationLastMessage(String conversationId, String lastMessage, long timestamp) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("timestamp", timestamp);

        firestore.collection("conversations")
                .document(conversationId)
                .update(updates)
                .addOnFailureListener(e -> {
                    // Log error but don't block message sending
                    // In production, you might want to handle this differently
                });
    }

    /**
     * Upload image to Cloudinary and send as message (LiveData version)
     * @param conversationId ID of the conversation
     * @param imageUri Local URI of the image to upload
     * @param senderId ID of the sender
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> uploadImageAndSendMessageLiveData(@NonNull String conversationId,
                                                                          @NonNull Uri imageUri,
                                                                          @NonNull String senderId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        uploadImageAndSendMessage(conversationId, imageUri, senderId, new SendMessageCallback() {
            @Override
            public void onSuccess() {
                result.setValue(Resource.success(true));
            }
            
            @Override
            public void onError(String error) {
                result.setValue(Resource.error(error));
            }
        });
        
        return result;
    }

    /**
     * Upload image to Cloudinary and send as message (callback version)
     * @param conversationId ID of the conversation
     * @param imageUri Local URI of the image to upload
     * @param senderId ID of the sender
     * @param callback Callback for success/error
     */
    public void uploadImageAndSendMessage(String conversationId, Uri imageUri, String senderId, SendMessageCallback callback) {
        try {
            // Upload to Cloudinary (signed - no preset needed)
            MediaManager.get().upload(imageUri)
                    .option("folder", "zalo_chat/" + conversationId)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d("Cloudinary", "Upload started");
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // Optional: track upload progress
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            // Get the secure URL from Cloudinary response
                            String imageUrl = (String) resultData.get("secure_url");
                            
                            // Create IMAGE type message with Cloudinary URL
                            Message imageMessage = new Message(
                                    null,
                                    senderId,
                                    imageUrl,
                                    Message.TYPE_IMAGE,
                                    System.currentTimeMillis()
                            );
                            
                            // Send message to Firestore
                            sendMessage(conversationId, imageMessage, callback);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            callback.onError("Upload failed: " + error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.d("Cloudinary", "Upload rescheduled");
                        }
                    })
                    .dispatch();
        } catch (Exception e) {
            callback.onError("Failed to start upload: " + e.getMessage());
        }
    }
    
    /**
     * Upload file to Cloudinary and send as message (LiveData version)
     * @param conversationId ID of the conversation
     * @param fileUri Local URI of the file to upload
     * @param senderId ID of the sender
     * @param fileName Original file name
     * @param fileSize File size in bytes
     * @param fileMimeType File MIME type
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> uploadFileAndSendMessageLiveData(@NonNull String conversationId,
                                                                          @NonNull Uri fileUri,
                                                                          @NonNull String senderId,
                                                                          @NonNull String fileName,
                                                                          long fileSize,
                                                                          @NonNull String fileMimeType) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        uploadFileAndSendMessage(conversationId, fileUri, senderId, fileName, fileSize, fileMimeType,
                new SendMessageCallback() {
                    @Override
                    public void onSuccess() {
                        result.setValue(Resource.success(true));
                    }
                    
                    @Override
                    public void onError(String error) {
                        result.setValue(Resource.error(error));
                    }
                });
        
        return result;
    }
    
    /**
     * Upload file to Cloudinary and send as message (callback version)
     * @param conversationId ID of the conversation
     * @param fileUri Local URI of the file to upload
     * @param senderId ID of the sender
     * @param fileName Original file name
     * @param fileSize File size in bytes
     * @param fileMimeType File MIME type
     * @param callback Callback for success/error
     */
    public void uploadFileAndSendMessage(String conversationId, Uri fileUri, String senderId,
                                         String fileName, long fileSize, String fileMimeType,
                                         SendMessageCallback callback) {
        try {
            // Determine resource type based on MIME type
            String resourceType = "auto"; // Cloudinary auto-detects type
            if (fileMimeType.startsWith("image/")) {
                resourceType = "image";
            } else if (fileMimeType.startsWith("video/")) {
                resourceType = "video";
            } else {
                resourceType = "raw"; // For documents, audio, etc.
            }
            
            // Upload to Cloudinary
            MediaManager.get().upload(fileUri)
                    .option("folder", "zalo_chat/" + conversationId + "/files")
                    .option("resource_type", resourceType)
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d("Cloudinary", "File upload started: " + fileName);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // Optional: track upload progress
                            int progress = (int) ((bytes * 100) / totalBytes);
                            Log.d("Cloudinary", "Upload progress: " + progress + "%");
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            // Get the secure URL from Cloudinary response
                            String fileUrl = (String) resultData.get("secure_url");
                            
                            // Create FILE type message with Cloudinary URL and metadata
                            Message fileMessage = new Message(
                                    null,
                                    senderId,
                                    fileUrl,  // URL in content field
                                    Message.TYPE_FILE,
                                    System.currentTimeMillis(),
                                    fileName,
                                    fileSize,
                                    fileMimeType
                            );
                            
                            // Send message to Firestore
                            sendMessage(conversationId, fileMessage, callback);
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            callback.onError("File upload failed: " + error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.d("Cloudinary", "File upload rescheduled");
                        }
                    })
                    .dispatch();
        } catch (Exception e) {
            callback.onError("Failed to start file upload: " + e.getMessage());
        }
    }
    
    /**
     * Clean up listeners when repository is no longer needed
     */
    public void cleanup() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    /**
     * Create a group conversation (LiveData version)
     * @param adminId ID of the user creating the group (becomes admin)
     * @param groupName Name of the group
     * @param memberIds List of member IDs including admin
     * @return LiveData containing Resource with the created Conversation
     */
    public LiveData<Resource<Conversation>> createGroup(@NonNull String adminId,
                                                        @NonNull String groupName,
                                                        @NonNull List<String> memberIds) {
        MutableLiveData<Resource<Conversation>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // Validate inputs
        if (groupName.trim().isEmpty()) {
            result.setValue(Resource.error("Tên nhóm không được để trống"));
            return result;
        }
        
        if (memberIds.size() < 2) {
            result.setValue(Resource.error("Nhóm phải có ít nhất 2 thành viên"));
            return result;
        }
        
        if (!memberIds.contains(adminId)) {
            result.setValue(Resource.error("Admin phải là thành viên của nhóm"));
            return result;
        }
        
        // Call FirestoreManager to create group
        firestoreManager.createGroupConversation(
            adminId,
            groupName,
            memberIds,
            new FirestoreManager.OnConversationCreatedListener() {
                @Override
                public void onSuccess(Conversation conversation) {
                    result.setValue(Resource.success(conversation));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể tạo nhóm";
                    result.setValue(Resource.error(errorMessage));
                }
            }
        );
        
        return result;
    }

    /**
     * Update group name
     */
    public LiveData<Resource<Boolean>> updateGroupName(@NonNull String conversationId, 
                                                        @NonNull String newName) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        firestoreManager.updateGroupName(conversationId, newName,
            new FirestoreManager.OnGroupUpdatedListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }

                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể cập nhật tên nhóm";
                    result.setValue(Resource.error(errorMessage));
                }
            });

        return result;
    }

    /**
     * Update group avatar
     */
    public LiveData<Resource<Boolean>> updateGroupAvatar(@NonNull String conversationId,
                                                          @NonNull String avatarUrl) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        firestoreManager.updateGroupAvatar(conversationId, avatarUrl,
            new FirestoreManager.OnGroupUpdatedListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }

                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể cập nhật avatar";
                    result.setValue(Resource.error(errorMessage));
                }
            });

        return result;
    }

    /**
     * Add members to group
     */
    public LiveData<Resource<Boolean>> addGroupMembers(@NonNull String conversationId,
                                                        @NonNull List<String> memberIds) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        firestoreManager.addGroupMembers(conversationId, memberIds,
            new FirestoreManager.OnGroupUpdatedListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }

                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể thêm thành viên";
                    result.setValue(Resource.error(errorMessage));
                }
            });

        return result;
    }

    /**
     * Remove member from group
     */
    public LiveData<Resource<Boolean>> removeGroupMember(@NonNull String conversationId,
                                                          @NonNull String memberId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        firestoreManager.removeGroupMember(conversationId, memberId,
            new FirestoreManager.OnGroupUpdatedListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }

                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể xóa thành viên";
                    result.setValue(Resource.error(errorMessage));
                }
            });

        return result;
    }

    /**
     * Leave group
     */
    public LiveData<Resource<Boolean>> leaveGroup(@NonNull String conversationId,
                                                   @NonNull String userId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        firestoreManager.leaveGroup(conversationId, userId,
            new FirestoreManager.OnGroupUpdatedListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }

                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể rời nhóm";
                    result.setValue(Resource.error(errorMessage));
                }
            });

        return result;
    }

    /**
     * Transfer admin rights to member
     */
    public LiveData<Resource<Boolean>> transferAdmin(@NonNull String conversationId,
                                                     @NonNull String newAdminId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        firestoreManager.transferAdmin(conversationId, newAdminId,
            new FirestoreManager.OnGroupUpdatedListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }

                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể chuyển quyền quản trị";
                    result.setValue(Resource.error(errorMessage));
                }
            });

        return result;
    }

    /**
     * Delete group
     */
    public LiveData<Resource<Boolean>> deleteGroup(@NonNull String conversationId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        firestoreManager.deleteGroup(conversationId,
            new FirestoreManager.OnGroupUpdatedListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }

                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể xóa nhóm";
                    result.setValue(Resource.error(errorMessage));
                }
            });

        return result;
    }

    /**
     * Callback interface for send message operations
     */
    public interface SendMessageCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Listener interface for real-time message updates
     */
    public interface MessagesListener {
        void onMessagesChanged(List<Message> messages);
        void onError(String error);
    }
}
