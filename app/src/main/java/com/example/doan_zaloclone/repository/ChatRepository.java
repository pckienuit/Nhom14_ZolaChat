package com.example.doan_zaloclone.repository;

import com.example.doan_zaloclone.models.Conversation;
import com.example.doan_zaloclone.models.Message;
import com.example.doan_zaloclone.services.FirestoreManager;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
        if (message.getSenderName() != null) {
            messageData.put("senderName", message.getSenderName());
        }
        messageData.put("content", message.getContent());
        messageData.put("type", message.getType());
        messageData.put("timestamp", message.getTimestamp());
        
        // Add file metadata if this is a file OR image message
        if (Message.TYPE_FILE.equals(message.getType()) || 
            Message.TYPE_IMAGE.equals(message.getType())) {
            if (message.getFileName() != null) {
                messageData.put("fileName", message.getFileName());
            }
            if (message.getFileSize() > 0) {
                messageData.put("fileSize", message.getFileSize());
            }
            if (message.getFileMimeType() != null) {
                messageData.put("fileMimeType", message.getFileMimeType());
            }
        }
        
        // Add reply fields if this is a reply message
        if (message.getReplyToId() != null && !message.getReplyToId().isEmpty()) {
            messageData.put("replyToId", message.getReplyToId());
            if (message.getReplyToContent() != null) {
                messageData.put("replyToContent", message.getReplyToContent());
            }
            if (message.getReplyToSenderId() != null) {
                messageData.put("replyToSenderId", message.getReplyToSenderId());
            }
            if (message.getReplyToSenderName() != null) {
                messageData.put("replyToSenderName", message.getReplyToSenderName());
            }
        }
        
        // Add forward fields if this is a forwarded message
        if (message.isForwarded()) {
            messageData.put("isForwarded", true);
            if (message.getOriginalSenderId() != null) {
                messageData.put("originalSenderId", message.getOriginalSenderId());
            }
            if (message.getOriginalSenderName() != null) {
                messageData.put("originalSenderName", message.getOriginalSenderName());
            }
        }
        
        // Add contact user ID if this is a contact message (business card)
        if (Message.TYPE_CONTACT.equals(message.getType()) && message.getContactUserId() != null) {
            messageData.put("contactUserId", message.getContactUserId());
        }
        
        // Add location data if this is a location message
        if (Message.TYPE_LOCATION.equals(message.getType())) {
            messageData.put("latitude", message.getLatitude());
            messageData.put("longitude", message.getLongitude());
            if (message.getLocationName() != null) {
                messageData.put("locationName", message.getLocationName());
            }
            if (message.getLocationAddress() != null) {
                messageData.put("locationAddress", message.getLocationAddress());
            }
        }
        
        // Add live location data if this is a live location message
        if (Message.TYPE_LIVE_LOCATION.equals(message.getType())) {
            if (message.getLiveLocationSessionId() != null) {
                messageData.put("liveLocationSessionId", message.getLiveLocationSessionId());
            }
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
                            
                            // Extract metadata from Cloudinary response
                            String format = (String) resultData.get("format");  // e.g., "jpg", "png"
                            Object bytesObj = resultData.get("bytes");
                            long fileSize = 0;
                            if (bytesObj instanceof Number) {
                                fileSize = ((Number) bytesObj).longValue();
                            }
                            
                            // Generate filename and MIME type
                            String fileName = "image_" + System.currentTimeMillis();
                            if (format != null && !format.isEmpty()) {
                                fileName += "." + format;
                            } else {
                                fileName += ".jpg";  // Default
                            }
                            
                            // Determine MIME type from format
                            String mimeType = "image/jpeg";  // Default
                            if (format != null) {
                                switch (format.toLowerCase()) {
                                    case "png":
                                        mimeType = "image/png";
                                        break;
                                    case "gif":
                                        mimeType = "image/gif";
                                        break;
                                    case "webp":
                                        mimeType = "image/webp";
                                        break;
                                    case "jpg":
                                    case "jpeg":
                                        mimeType = "image/jpeg";
                                        break;
                                    default:
                                        mimeType = "image/" + format;
                                }
                            }
                            
                            Log.d("Cloudinary", "Image uploaded: " + fileName + 
                                ", size: " + fileSize + ", mimeType: " + mimeType);
                            
                            // Create IMAGE type message with metadata
                            Message imageMessage = new Message(
                                    null,
                                    senderId,
                                    imageUrl,
                                    Message.TYPE_IMAGE,
                                    System.currentTimeMillis(),
                                    fileName,
                                    fileSize,
                                    mimeType
                            );
                            
                            // Fetch sender name before sending
                            firestore.collection("users")
                                    .document(senderId)
                                    .get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc.exists()) {
                                            String name = doc.getString("name");
                                            if (name != null && !name.isEmpty()) {
                                                imageMessage.setSenderName(name);
                                            }
                                        }
                                        // Send message to Firestore
                                        sendMessage(conversationId, imageMessage, callback);
                                    })
                                    .addOnFailureListener(e -> {
                                        // Send anyway without name
                                        sendMessage(conversationId, imageMessage, callback);
                                    });
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
                            
                            // Fetch sender name before sending
                            firestore.collection("users")
                                    .document(senderId)
                                    .get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc.exists()) {
                                            String name = doc.getString("name");
                                            if (name != null && !name.isEmpty()) {
                                                fileMessage.setSenderName(name);
                                            }
                                        }
                                        // Send message to Firestore
                                        sendMessage(conversationId, fileMessage, callback);
                                    })
                                    .addOnFailureListener(e -> {
                                        // Send anyway without name
                                        sendMessage(conversationId, fileMessage, callback);
                                    });
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

    // ===================== PINNED MESSAGES METHODS =====================
    
    /**
     * Pin a message in a conversation
     * @param conversationId ID of the conversation
     * @param messageId ID of the message to pin
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> pinMessage(@NonNull String conversationId,
                                                   @NonNull String messageId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.pinMessage(conversationId, messageId,
            new FirestoreManager.OnPinMessageListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể ghim tin nhắn";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Unpin a message from a conversation
     * @param conversationId ID of the conversation
     * @param messageId ID of the message to unpin
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> unpinMessage(@NonNull String conversationId,
                                                     @NonNull String messageId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.unpinMessage(conversationId, messageId,
            new FirestoreManager.OnPinMessageListener() {
                @Override
                public void onSuccess() {
                    result.setValue(Resource.success(true));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể gỡ ghim tin nhắn";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }
    
    /**
     * Get all pinned messages for a conversation
     * @param conversationId ID of the conversation
     * @return LiveData containing Resource with list of pinned messages
     */
    public LiveData<Resource<List<Message>>> getPinnedMessages(@NonNull String conversationId) {
        MutableLiveData<Resource<List<Message>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        firestoreManager.getPinnedMessages(conversationId,
            new FirestoreManager.OnPinnedMessagesListener() {
                @Override
                public void onSuccess(List<Message> messages) {
                    result.setValue(Resource.success(messages));
                }
                
                @Override
                public void onFailure(Exception e) {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể tải tin nhắn đã ghim";
                    result.setValue(Resource.error(errorMessage));
                }
            });
        
        return result;
    }

    // ===================== MESSAGE REACTIONS METHODS =====================
    
    /**
     * Add or update a reaction to a message
     * @param conversationId ID of the conversation
     * @param messageId ID of the message to react to
     * @param userId ID of the user adding the reaction
     * @param reactionType Type of reaction (heart, haha, sad, angry, wow, like)
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> addReaction(@NonNull String conversationId,
                                                    @NonNull String messageId,
                                                    @NonNull String userId,
                                                    @NonNull String reactionType) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // Validate reaction type
        if (!com.example.doan_zaloclone.models.MessageReaction.isValidReactionType(reactionType)) {
            result.setValue(Resource.error("Loại cảm xúc không hợp lệ"));
            return result;
        }
        
        // Update the reactions map field in Firestore
        DocumentReference messageRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId);
        
        // Update both reactions (userId -> reactionType) and reactionCounts (reactionType -> count)
        Map<String, Object> updates = new HashMap<>();
        updates.put("reactions." + userId, reactionType);
        updates.put("reactionCounts." + reactionType, com.google.firebase.firestore.FieldValue.increment(1));
        
        messageRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatRepository", "Reaction added: " + reactionType + " by user: " + userId);
                    result.setValue(Resource.success(true));
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatRepository", "Error adding reaction", e);
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể thêm cảm xúc";
                    result.setValue(Resource.error(errorMessage));
                });
        
        return result;
    }
    
    /**
     * Remove ALL reactions from a message (clear all reactions and counts)
     * This is used when user clicks the X button to reset all reactions
     * @param conversationId ID of the conversation
     * @param messageId ID of the message
     * @param userId ID of the user removing reactions (not used but kept for consistency)
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> removeReaction(@NonNull String conversationId,
                                                       @NonNull String messageId,
                                                       @NonNull String userId) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        DocumentReference messageRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId);
        
        // Clear ALL reactions and counts (reset to empty maps)
        Map<String, Object> updates = new HashMap<>();
        updates.put("reactions", new HashMap<String, String>());
        updates.put("reactionCounts", new HashMap<String, Integer>());
        
        messageRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d("ChatRepository", "All reactions cleared for message: " + messageId);
                    result.setValue(Resource.success(true));
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatRepository", "Error clearing reactions", e);
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể xóa cảm xúc";
                    result.setValue(Resource.error(errorMessage));
                });
        
        return result;
    }
    
    /**
     * Toggle a reaction - if user already has this reaction type, remove it; otherwise add/update it
     * @param conversationId ID of the conversation
     * @param messageId ID of the message
     * @param userId ID of the user toggling the reaction
     * @param reactionType Type of reaction to toggle
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> toggleReaction(@NonNull String conversationId,
                                                       @NonNull String messageId,
                                                       @NonNull String userId,
                                                       @NonNull String reactionType) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        
        // First, fetch the current message to check existing reaction
        DocumentReference messageRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId);
        
        messageRef.get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        result.setValue(Resource.error("Tin nhắn không tồn tại"));
                        return;
                    }
                    
                    Message message = doc.toObject(Message.class);
                    if (message == null) {
                        result.setValue(Resource.error("Không thể đọc tin nhắn"));
                        return;
                    }
                    
                    // Zalo style: Always add/update reaction, never remove on same click
                    // This ensures clicking on the same reaction type keeps it (doesn't toggle off)
                    LiveData<Resource<Boolean>> addResult = addReaction(conversationId, messageId, userId, reactionType);
                    addResult.observeForever(resource -> result.setValue(resource));
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatRepository", "Error fetching message for toggle reaction", e);
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Không thể thay đổi cảm xúc";
                    result.setValue(Resource.error(errorMessage));
                });
        
        return result;
    }

    /**
     * Recall a message (mark as recalled and clear content)
     * @param conversationId ID of the conversation
     * @param messageId ID of the message to recall
     * @param callback Callback for success/error
     */
    public void recallMessage(String conversationId, String messageId, RecallMessageCallback callback) {
        DocumentReference messageRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId);

        // Update message to mark as recalled
        Map<String, Object> updates = new HashMap<>();
        updates.put("isRecalled", true);
        // Optionally clear sensitive content (keep metadata for audit)
        updates.put("content", "");

        messageRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null 
                            ? e.getMessage() 
                            : "Failed to recall message";
                    callback.onError(errorMessage);
                });
    }

    /**
     * Callback interface for recall message operations
     */
    public interface RecallMessageCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Forward a message to a target conversation
     * @param targetConversationId ID of the conversation to forward to
     * @param originalMessage Original message to forward
     * @param newSenderId ID of the user forwarding the message
     * @param originalSenderName Name of the original sender (for display)
     * @param callback Callback for success/error
     */
    public void forwardMessage(String targetConversationId, Message originalMessage, 
                               String newSenderId, String originalSenderName, 
                               ForwardMessageCallback callback) {
        // Clone the message with new sender
        Message forwardedMessage = new Message();
        forwardedMessage.setSenderId(newSenderId);
        forwardedMessage.setContent(originalMessage.getContent());
        forwardedMessage.setType(originalMessage.getType());
        forwardedMessage.setTimestamp(System.currentTimeMillis());
        
        // Copy file metadata if applicable
        if (Message.TYPE_FILE.equals(originalMessage.getType()) || 
            Message.TYPE_IMAGE.equals(originalMessage.getType())) {
            forwardedMessage.setFileName(originalMessage.getFileName());
            forwardedMessage.setFileSize(originalMessage.getFileSize());
            forwardedMessage.setFileMimeType(originalMessage.getFileMimeType());
        }
        
        // Set forward metadata
        forwardedMessage.setForwarded(true);
        forwardedMessage.setOriginalSenderId(originalMessage.getSenderId());
        forwardedMessage.setOriginalSenderName(originalSenderName);
        
        // Fetch sender name (the user who is forwarding) before sending
        firestore.collection("users")
                .document(newSenderId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            forwardedMessage.setSenderName(name);
                        }
                    }
                    // Send the forwarded message
                    sendMessage(targetConversationId, forwardedMessage, new SendMessageCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess();
                        }
                        
                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // Send anyway without sender name
                    sendMessage(targetConversationId, forwardedMessage, new SendMessageCallback() {
                        @Override
                        public void onSuccess() {
                            callback.onSuccess();
                        }
                        
                        @Override
                        public void onError(String error) {
                            callback.onError(error);
                        }
                    });
                });
    }

    /**
     * Callback interface for forward message operations
     */
    public interface ForwardMessageCallback {
        void onSuccess();
        void onError(String error);
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
    
    /**
     * Callback interface for conversation operations
     */
    public interface ConversationCallback {
        void onSuccess(String conversationId);
        void onError(String error);
    }
    
    /**
     * Get or create a conversation with a friend
     * If existing conversation is missing memberNames, it will be updated
     */
    public void getOrCreateConversationWithFriend(String currentUserId, String friendId, ConversationCallback callback) {
        android.util.Log.d("ChatRepository", "getOrCreateConversationWithFriend - currentUserId: " + currentUserId + ", friendId: " + friendId);
        
        // Query for existing conversation between these two users
        // Don't filter by type - old conversations may not have type field
        firestore.collection("conversations")
            .whereArrayContains("memberIds", currentUserId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                android.util.Log.d("ChatRepository", "Found " + querySnapshot.size() + " conversations containing currentUser");
                
                com.google.firebase.firestore.DocumentSnapshot existingDoc = null;
                
                // Find 1-1 conversation that contains both users
                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    java.util.List<String> memberIds = (java.util.List<String>) doc.get("memberIds");
                    String type = doc.getString("type");
                    
                    android.util.Log.d("ChatRepository", "Checking conversation " + doc.getId() + 
                        " - memberIds: " + memberIds + ", type: " + type + 
                        ", containsFriend: " + (memberIds != null && memberIds.contains(friendId)));
                    
                    // Check if this is a 1-1 conversation (2 members, and type is FRIEND or null/empty)
                    if (memberIds != null && memberIds.contains(friendId) && memberIds.size() == 2) {
                        // Accept conversations with type=FRIEND, type=null, or type="" (for backward compatibility)
                        if (type == null || type.isEmpty() || "FRIEND".equals(type)) {
                            android.util.Log.d("ChatRepository", "Found existing conversation: " + doc.getId());
                            existingDoc = doc;
                            break;
                        }
                    }
                }
                
                if (existingDoc != null) {
                    final String conversationId = existingDoc.getId();
                    android.util.Log.d("ChatRepository", "Using existing conversation: " + conversationId);
                    
                    // Check if conversation has memberNames and type
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, String> existingMemberNames = 
                        (java.util.Map<String, String>) existingDoc.get("memberNames");
                    String existingType = existingDoc.getString("type");
                    
                    if (existingMemberNames == null || existingMemberNames.isEmpty() || 
                        existingType == null || existingType.isEmpty()) {
                        // Missing memberNames or type - update the existing conversation
                        updateConversationMemberNamesAndType(conversationId, currentUserId, friendId, callback);
                    } else {
                        callback.onSuccess(conversationId);
                    }
                } else {
                    android.util.Log.d("ChatRepository", "No existing conversation found, creating new one");
                    // Create new conversation
                    createConversationWithFriend(currentUserId, friendId, callback);
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("ChatRepository", "Error querying conversations", e);
                callback.onError(e.getMessage());
            });
    }
    
    /**
     * Update memberNames and type for an existing conversation that's missing them
     */
    private void updateConversationMemberNamesAndType(String conversationId, String currentUserId, String friendId, ConversationCallback callback) {
        // Fetch both user names
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> currentUserTask = 
            firestore.collection("users").document(currentUserId).get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> friendTask = 
            firestore.collection("users").document(friendId).get();
        
        com.google.android.gms.tasks.Tasks.whenAllSuccess(currentUserTask, friendTask)
            .addOnSuccessListener(results -> {
                com.google.firebase.firestore.DocumentSnapshot currentUserDoc = 
                    (com.google.firebase.firestore.DocumentSnapshot) results.get(0);
                com.google.firebase.firestore.DocumentSnapshot friendDoc = 
                    (com.google.firebase.firestore.DocumentSnapshot) results.get(1);
                
                String currentUserName = currentUserDoc.exists() && currentUserDoc.getString("name") != null 
                    ? currentUserDoc.getString("name") : "User";
                String friendName = friendDoc.exists() && friendDoc.getString("name") != null 
                    ? friendDoc.getString("name") : "User";
                
                // Update conversation with memberNames and type
                java.util.Map<String, String> memberNames = new java.util.HashMap<>();
                memberNames.put(currentUserId, currentUserName);
                memberNames.put(friendId, friendName);
                
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("memberNames", memberNames);
                updates.put("type", "FRIEND");
                
                firestore.collection("conversations")
                    .document(conversationId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(conversationId))
                    .addOnFailureListener(e -> {
                        // Still return success even if update fails - the conversation exists
                        android.util.Log.e("ChatRepository", "Failed to update memberNames/type", e);
                        callback.onSuccess(conversationId);
                    });
            })
            .addOnFailureListener(e -> {
                // Still return success - the conversation exists
                android.util.Log.e("ChatRepository", "Failed to fetch user names for update", e);
                callback.onSuccess(conversationId);
            });
    }
    
    /**
     * Create a new conversation with a friend
     * Fetches user names from Firestore to properly populate memberNames
     */
    private void createConversationWithFriend(String currentUserId, String friendId, ConversationCallback callback) {
        // First, fetch both user names
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> currentUserTask = 
            firestore.collection("users").document(currentUserId).get();
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot> friendTask = 
            firestore.collection("users").document(friendId).get();
        
        com.google.android.gms.tasks.Tasks.whenAllSuccess(currentUserTask, friendTask)
            .addOnSuccessListener(results -> {
                com.google.firebase.firestore.DocumentSnapshot currentUserDoc = 
                    (com.google.firebase.firestore.DocumentSnapshot) results.get(0);
                com.google.firebase.firestore.DocumentSnapshot friendDoc = 
                    (com.google.firebase.firestore.DocumentSnapshot) results.get(1);
                
                String currentUserName = currentUserDoc.exists() && currentUserDoc.getString("name") != null 
                    ? currentUserDoc.getString("name") : "User";
                String friendName = friendDoc.exists() && friendDoc.getString("name") != null 
                    ? friendDoc.getString("name") : "User";
                
                // Create conversation with proper memberNames
                java.util.Map<String, Object> conversationData = new java.util.HashMap<>();
                java.util.List<String> memberIds = java.util.Arrays.asList(currentUserId, friendId);
                conversationData.put("memberIds", memberIds);
                conversationData.put("type", "FRIEND");
                conversationData.put("name", ""); // Empty for 1-1 chats
                conversationData.put("timestamp", System.currentTimeMillis());
                conversationData.put("lastMessage", "");
                
                // Store member names for proper display
                java.util.Map<String, String> memberNames = new java.util.HashMap<>();
                memberNames.put(currentUserId, currentUserName);
                memberNames.put(friendId, friendName);
                conversationData.put("memberNames", memberNames);
                
                firestore.collection("conversations")
                    .add(conversationData)
                    .addOnSuccessListener(docRef -> {
                        // Update document with its own ID
                        docRef.update("id", docRef.getId())
                            .addOnSuccessListener(aVoid -> callback.onSuccess(docRef.getId()))
                            .addOnFailureListener(e -> callback.onSuccess(docRef.getId())); // Still succeed even if id update fails
                    })
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
            })
            .addOnFailureListener(e -> callback.onError("Failed to fetch user names: " + e.getMessage()));
    }
    
    // ===================== POLL METHODS =====================
    
    /**
     * Send a poll message to conversation
     */
    public void sendPollMessage(String conversationId, String senderId, 
                                com.example.doan_zaloclone.models.Poll poll,
                                SendMessageCallback callback) {
        if (conversationId == null || senderId == null || poll == null) {
            if (callback != null) {
                callback.onError("Invalid parameters");
            }
            return;
        }
        
        // Create message document reference
        DocumentReference messageRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document();
        
        // Create message object
        com.example.doan_zaloclone.models.Message message = 
                new com.example.doan_zaloclone.models.Message();
        message.setId(messageRef.getId());
        message.setSenderId(senderId);
        message.setType(com.example.doan_zaloclone.models.Message.TYPE_POLL);
        message.setTimestamp(System.currentTimeMillis());
        message.setPollData(poll);
        
        // Fetch sender name before saving
        firestore.collection("users")
                .document(senderId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            message.setSenderName(name);
                        }
                    }
                    // Save to Firestore
                    savePollMessage(messageRef, message, conversationId, poll, callback);
                })
                .addOnFailureListener(e -> {
                    // Save anyway without sender name
                    savePollMessage(messageRef, message, conversationId, poll, callback);
                });
    }
    
    private void savePollMessage(DocumentReference messageRef, 
                                 com.example.doan_zaloclone.models.Message message,
                                 String conversationId,
                                 com.example.doan_zaloclone.models.Poll poll,
                                 SendMessageCallback callback) {
        messageRef.set(message)
                .addOnSuccessListener(aVoid -> {
                    // Update conversation's lastMessage
                    updateConversationLastMessage(conversationId, 
                            "📊 Bình chọn: " + poll.getQuestion(), 
                            message.getTimestamp());
                    
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }
    
    /**
     * Vote for a poll option
     */
    public void votePoll(String conversationId, String messageId, 
                        String optionId, String userId,
                        VotePollCallback callback) {
        if (conversationId == null || messageId == null || optionId == null || userId == null) {
            if (callback != null) {
                callback.onError("Invalid parameters");
            }
            return;
        }
        
        // Fetch user name first, then execute vote
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    String userName = "User";
                    if (doc.exists()) {
                        com.example.doan_zaloclone.models.User user = 
                                doc.toObject(com.example.doan_zaloclone.models.User.class);
                        if (user != null && user.getName() != null) {
                            userName = user.getName();
                        }
                    }
                    executeVotePoll(conversationId, messageId, optionId, userId, userName, callback);
                })
                .addOnFailureListener(e -> {
                    executeVotePoll(conversationId, messageId, optionId, userId, "User", callback);
                });
    }
    
    /**
     * Execute the actual vote transaction
     */
    private void executeVotePoll(String conversationId, String messageId, 
                                 String optionId, String userId, String userName,
                                 VotePollCallback callback) {
        DocumentReference messageRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId);
        
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(messageRef);
            com.example.doan_zaloclone.models.Message message = 
                    snapshot.toObject(com.example.doan_zaloclone.models.Message.class);
            
            if (message == null || message.getPollData() == null) {
                throw new RuntimeException("Poll not found");
            }
            
            com.example.doan_zaloclone.models.Poll poll = message.getPollData();
            
            // Check if poll is still open
            if (!poll.canVote()) {
                throw new RuntimeException("Poll is closed or expired");
            }
            
            // Find the option
            com.example.doan_zaloclone.models.PollOption option = poll.getOptionById(optionId);
            if (option == null) {
                throw new RuntimeException("Option not found");
            }
            
            // Check if user already voted for this option
            if (option.hasUserVoted(userId)) {
                // User already voted for this option - unvote it
                option.removeVote(userId);
            } else {
                // If single choice, remove user's other votes first
                if (!poll.isAllowMultipleChoice()) {
                    for (com.example.doan_zaloclone.models.PollOption opt : poll.getOptions()) {
                        opt.removeVote(userId);
                    }
                }
                
                // Add vote to selected option WITH USER NAME
                option.addVote(userId, userName);
            }
            
            // Update message
            message.setPollData(poll);
            transaction.set(messageRef, message);
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (callback != null) {
                callback.onSuccess();
            }
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Close a poll (only creator or group admin can close)
     */
    public void closePoll(String conversationId, String messageId, 
                         String userId, boolean isGroupAdmin,
                         VotePollCallback callback) {
        if (conversationId == null || messageId == null || userId == null) {
            if (callback != null) {
                callback.onError("Invalid parameters");
            }
            return;
        }
        
        DocumentReference messageRef = firestore
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId);
        
        firestore.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(messageRef);
            com.example.doan_zaloclone.models.Message message = 
                    snapshot.toObject(com.example.doan_zaloclone.models.Message.class);
            
            if (message == null || message.getPollData() == null) {
                throw new RuntimeException("Poll not found");
            }
            
            com.example.doan_zaloclone.models.Poll poll = message.getPollData();
            
            // Check permission
            if (!poll.canClosePoll(userId, isGroupAdmin)) {
                throw new RuntimeException("You don't have permission to close this poll");
            }
            
            poll.setClosed(true);
            message.setPollData(poll);
            transaction.set(messageRef, message);
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (callback != null) {
                callback.onSuccess();
            }
        }).addOnFailureListener(e -> {
            if (callback != null) {
                callback.onError(e.getMessage());
            }
        });
    }
    
    /**
     * Callback for poll vote operations
     */
    public interface VotePollCallback {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Update live location data in Firestore
     * @param liveLocation The updated live location object
     */
    public void updateLiveLocation(com.example.doan_zaloclone.models.LiveLocation liveLocation) {
        if (liveLocation == null || liveLocation.getSessionId() == null) return;
        
        firestore.collection("liveLocations")
                .document(liveLocation.getSessionId())
                .set(liveLocation)
                .addOnFailureListener(e -> android.util.Log.e("ChatRepo", "Failed to update live location", e));
    }
    
    /**
     * Stop live location sharing
     * @param sessionId Session ID to stop
     */
    public void stopLiveLocation(String sessionId) {
        if (sessionId == null) return;
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("active", false);
        
        firestore.collection("liveLocations")
                .document(sessionId)
                .update(updates)
                .addOnFailureListener(e -> android.util.Log.e("ChatRepo", "Failed to stop live location", e));
    }
}

