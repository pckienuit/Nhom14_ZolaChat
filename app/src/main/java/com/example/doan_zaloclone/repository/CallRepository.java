package com.example.doan_zaloclone.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.models.Call;
import com.example.doan_zaloclone.models.CallSignal;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository class for handling voice and video call operations with Firestore
 * Supports both LiveData (for ViewModels) and callbacks (for backward compatibility)
 */
public class CallRepository {
    private static final String TAG = "CallRepository";
    private static final String COLLECTION_CALLS = "calls";
    private static final String COLLECTION_SIGNALS = "signals";
    
    private final FirebaseFirestore db;
    
    public CallRepository() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    // ==================== LiveData Methods ====================
    
    /**
     * Create a new call (LiveData version)
     * @param call Call object to create
     * @return LiveData containing Resource with created Call
     */
    public LiveData<Resource<Call>> createCallLiveData(@NonNull Call call) {
        MutableLiveData<Resource<Call>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        DocumentReference callRef = db.collection(COLLECTION_CALLS).document();
        call.setId(callRef.getId());
        
        callRef.set(call)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Call created: " + call.getId());
                    result.setValue(Resource.success(call));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating call", e);
                    result.setValue(Resource.error(e.getMessage(), null));
                });
        
        return result;
    }
    
    /**
     * Get a call by ID with real-time updates (LiveData version)
     * @param callId ID of the call
     * @return LiveData containing Resource with Call
     */
    public LiveData<Resource<Call>> getCallLiveData(@NonNull String callId) {
        MutableLiveData<Resource<Call>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        db.collection(COLLECTION_CALLS)
                .document(callId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to call", e);
                        result.setValue(Resource.error(e.getMessage(), null));
                        return;
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        Call call = snapshot.toObject(Call.class);
                        if (call != null) {
                            call.setId(snapshot.getId());
                            Log.d(TAG, "Call updated: " + call.getId() + ", status: " + call.getStatus());
                            result.setValue(Resource.success(call));
                        }
                    } else {
                        result.setValue(Resource.error("Call not found", null));
                    }
                });
        
        return result;
    }
    
    /**
     * Update call status (LiveData version)
     * @param callId ID of the call
     * @param status New status
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> updateCallStatusLiveData(@NonNull String callId, @NonNull String status) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        
        db.collection(COLLECTION_CALLS)
                .document(callId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Call status updated: " + callId + " -> " + status);
                    result.setValue(Resource.success(true));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating call status", e);
                    result.setValue(Resource.error(e.getMessage(), false));
                });
        
        return result;
    }
    
    /**
     * End a call and set duration (LiveData version)
     * @param callId ID of the call
     * @param duration Duration in seconds
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> endCallLiveData(@NonNull String callId, long duration) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Call.STATUS_ENDED);
        updates.put("endTime", System.currentTimeMillis());
        updates.put("duration", duration);
        
        db.collection(COLLECTION_CALLS)
                .document(callId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Call ended: " + callId + ", duration: " + duration + "s");
                    result.setValue(Resource.success(true));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error ending call", e);
                    result.setValue(Resource.error(e.getMessage(), false));
                });
        
        return result;
    }
    
    /**
     * Send a call signal (WebRTC signaling)
     * @param callId ID of the call
     * @param signal CallSignal object to send
     * @return LiveData containing Resource with success status
     */
    public LiveData<Resource<Boolean>> sendSignalLiveData(@NonNull String callId, @NonNull CallSignal signal) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        DocumentReference signalRef = db.collection(COLLECTION_CALLS)
                .document(callId)
                .collection(COLLECTION_SIGNALS)
                .document();
        
        signal.setId(signalRef.getId());
        signal.setCallId(callId);
        
        signalRef.set(signal)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Signal sent: " + signal.getType() + " for call: " + callId);
                    result.setValue(Resource.success(true));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending signal", e);
                    result.setValue(Resource.error(e.getMessage(), false));
                });
        
        return result;
    }
    
    /**
     * Get call history for a user (LiveData version)
     * @param userId User ID
     * @return LiveData containing Resource with list of Calls
     */
    public LiveData<Resource<List<Call>>> getCallHistoryLiveData(@NonNull String userId) {
        MutableLiveData<Resource<List<Call>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        // Query calls where user is either caller or receiver
        db.collection(COLLECTION_CALLS)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(100)  // Limit to recent 100 calls
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Call> calls = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Call call = doc.toObject(Call.class);
                        if (call != null) {
                            call.setId(doc.getId());
                            // Filter for current user (caller or receiver)
                            if (userId.equals(call.getCallerId()) || userId.equals(call.getReceiverId())) {
                                calls.add(call);
                            }
                        }
                    }
                    Log.d(TAG, "Loaded " + calls.size() + " calls for user: " + userId);
                    result.setValue(Resource.success(calls));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading call history", e);
                    result.setValue(Resource.error(e.getMessage(), null));
                });
        
        return result;
    }
    
    // ==================== Callback Methods (Backward Compatibility) ====================
    
    /**
     * Create a new call (callback version)
     * @param call Call object to create
     * @param callback Callback for success/error
     */
    public void createCall(@NonNull Call call, @NonNull OnCallCreatedListener callback) {
        DocumentReference callRef = db.collection(COLLECTION_CALLS).document();
        call.setId(callRef.getId());
        
        callRef.set(call)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Call created: " + call.getId());
                    callback.onSuccess(call);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating call", e);
                    callback.onError(e.getMessage());
                });
    }
    
    /**
     * Update call status (callback version)
     * @param callId ID of the call
     * @param status New status
     * @param callback Callback for success/error
     */
    public void updateCallStatus(@NonNull String callId, @NonNull String status, @NonNull OnCallUpdatedListener callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        
        db.collection(COLLECTION_CALLS)
                .document(callId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Call status updated: " + callId + " -> " + status);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating call status", e);
                    callback.onError(e.getMessage());
                });
    }
    
    /**
     * End a call (callback version)
     * @param callId ID of the call
     * @param duration Duration in seconds
     * @param callback Callback for success/error
     */
    public void endCall(@NonNull String callId, long duration, @NonNull OnCallUpdatedListener callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Call.STATUS_ENDED);
        updates.put("endTime", System.currentTimeMillis());
        updates.put("duration", duration);
        
        db.collection(COLLECTION_CALLS)
                .document(callId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Call ended: " + callId + ", duration: " + duration + "s");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error ending call", e);
                    callback.onError(e.getMessage());
                });
    }
    
    /**
     * Listen to a call with real-time updates (callback version)
     * @param callId ID of the call
     * @param listener Listener for call updates
     * @return ListenerRegistration for cleanup
     */
    public ListenerRegistration listenToCall(@NonNull String callId, @NonNull OnCallChangedListener listener) {
        return db.collection(COLLECTION_CALLS)
                .document(callId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to call", e);
                        listener.onError(e.getMessage());
                        return;
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        Call call = snapshot.toObject(Call.class);
                        if (call != null) {
                            call.setId(snapshot.getId());
                            listener.onCallChanged(call);
                        }
                    }
                });
    }
    
    /**
     * Send a call signal (callback version)
     * @param callId ID of the call
     * @param signal CallSignal object to send
     * @param callback Callback for success/error
     */
    public void sendSignal(@NonNull String callId, @NonNull CallSignal signal, @NonNull OnSignalSentListener callback) {
        DocumentReference signalRef = db.collection(COLLECTION_CALLS)
                .document(callId)
                .collection(COLLECTION_SIGNALS)
                .document();
        
        signal.setId(signalRef.getId());
        signal.setCallId(callId);
        
        signalRef.set(signal)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Signal sent: " + signal.getType() + " for call: " + callId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending signal", e);
                    callback.onError(e.getMessage());
                });
    }
    
    /**
     * Listen to call signals with real-time updates (callback version)
     * @param callId ID of the call
     * @param listener Listener for signal updates
     * @return ListenerRegistration for cleanup
     */
    public ListenerRegistration listenToSignals(@NonNull String callId, @NonNull OnSignalsChangedListener listener) {
        return db.collection(COLLECTION_CALLS)
                .document(callId)
                .collection(COLLECTION_SIGNALS)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to signals", e);
                        listener.onError(e.getMessage());
                        return;
                    }
                    
                    if (querySnapshot != null) {
                        List<CallSignal> signals = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            CallSignal signal = doc.toObject(CallSignal.class);
                            if (signal != null) {
                                signal.setId(doc.getId());
                                signals.add(signal);
                            }
                        }
                        Log.d(TAG, "Loaded " + signals.size() + " signals for call: " + callId);
                        listener.onSignalsChanged(signals);
                    }
                });
    }
    
    /**
     * Listen to incoming calls for a user
     * Uses client-side timestamp filtering to avoid Firestore index requirement
     * @param userId User ID
     * @param listener Listener for incoming calls
     * @return ListenerRegistration for cleanup
     */
    public ListenerRegistration listenToIncomingCalls(@NonNull String userId, @NonNull OnIncomingCallListener listener) {
        Log.d(TAG, "Listening for incoming calls for user: " + userId);
        
        // Calculate timestamp on client side
        final long twoMinutesAgo = System.currentTimeMillis() - 120000;
        Log.d(TAG, "Will filter calls older than: " + twoMinutesAgo);
        
        return db.collection(COLLECTION_CALLS)
                .whereEqualTo("receiverId", userId)
                .whereIn("status", java.util.Arrays.asList(Call.STATUS_CALLING, Call.STATUS_RINGING))
                // No Firestore timestamp filter to avoid index requirement
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to incoming calls", e);
                        listener.onError(e.getMessage());
                        return;
                    }
                    
                    if (querySnapshot != null) {
                        Log.d(TAG, "Query snapshot received, size: " + querySnapshot.size());
                        
                        if (!querySnapshot.isEmpty()) {
                            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                Call call = doc.toObject(Call.class);
                                if (call != null) {
                                    call.setId(doc.getId());
                                    
                                    // CLIENT-SIDE TIMESTAMP CHECK (avoids Firestore index)
                                    if (call.getStartTime() < twoMinutesAgo) {
                                        Log.d(TAG, "Skipping old call: " + call.getId() + 
                                              " (started at " + call.getStartTime() + ")");
                                        continue;  // Skip old calls
                                    }
                                    
                                    Log.d(TAG, "Incoming call detected: " + call.getId() + 
                                          ", startTime: " + call.getStartTime() + 
                                          ", status: " + call.getStatus());
                                    listener.onIncomingCall(call);
                                    break;  // Handle one call at a time
                                }
                            }
                        } else {
                            Log.d(TAG, "No incoming calls found");
                        }
                    }
                });
    }
    
    // ==================== Callback Interfaces ====================
    
    public interface OnCallCreatedListener {
        void onSuccess(Call call);
        void onError(String error);
    }
    
    public interface OnCallUpdatedListener {
        void onSuccess();
        void onError(String error);
    }
    
    public interface OnCallChangedListener {
        void onCallChanged(Call call);
        void onError(String error);
    }
    
    public interface OnSignalSentListener {
        void onSuccess();
        void onError(String error);
    }
    
    public interface OnSignalsChangedListener {
        void onSignalsChanged(List<CallSignal> signals);
        void onError(String error);
    }
    
    public interface OnIncomingCallListener {
        void onIncomingCall(Call call);
        void onError(String error);
    }
}
