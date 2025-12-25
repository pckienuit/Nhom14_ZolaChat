package com.example.doan_zaloclone.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.models.Call;
import com.example.doan_zaloclone.models.CallSignal;
import com.example.doan_zaloclone.models.Message;
import com.example.doan_zaloclone.repository.CallRepository;
import com.example.doan_zaloclone.repository.ChatRepository;
import com.example.doan_zaloclone.repository.WebRtcRepository;
import com.example.doan_zaloclone.utils.Resource;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * ViewModel for managing voice and video calls
 * Orchestrates CallRepository (Firestore) and WebRtcRepository (WebRTC)
 */
public class CallViewModel extends AndroidViewModel {
    private static final String TAG = "CallViewModel";
    // Call expiry timeout (milliseconds) - reject calls older than 60 seconds
    private static final long CALL_EXPIRY_TIMEOUT = 60000;  // 60 seconds
    private static final long MAX_DISCONNECT_TIMEOUT = 15000;  // 15 seconds
    // Concurrent call prevention
    private static boolean isInCall = false;
    private final CallRepository callRepository;
    private final ChatRepository chatRepository;
    private final WebRtcRepository webRtcRepository;
    // LiveData
    private final MutableLiveData<Resource<Call>> currentCall;
    private final MutableLiveData<String> connectionState;
    private final MutableLiveData<Boolean> isMicEnabled;
    private final MutableLiveData<Boolean> isCameraEnabled;
    private final MutableLiveData<String> error;
    // Track processed signals to avoid duplicates
    private final java.util.Set<String> processedSignalIds = new java.util.HashSet<>();
    // Network disconnection handling
    private final Handler disconnectHandler = new Handler(Looper.getMainLooper());
    // Listeners
    private ListenerRegistration callListener;
    private ListenerRegistration signalsListener;
    // Current call tracking
    private String currentCallId;
    private String currentUserId;
    private boolean isInitiator = false;
    private boolean currentCallIsVideo = false;  // Fix #6: Store video flag
    private long callInitiatedTimestamp = 0;  // Track when call started
    private Runnable disconnectRunnable;

    public CallViewModel(@NonNull Application application) {
        super(application);
        this.callRepository = new CallRepository();
        this.chatRepository = new ChatRepository();
        this.webRtcRepository = new WebRtcRepository(application.getApplicationContext());

        this.currentCall = new MutableLiveData<>();
        this.connectionState = new MutableLiveData<>();
        this.isMicEnabled = new MutableLiveData<>(true);
        this.isCameraEnabled = new MutableLiveData<>(true);
        this.error = new MutableLiveData<>();

        // Setup WebRTC callbacks
        setupWebRtcCallbacks();
    }

    /**
     * Setup WebRTC callbacks for connection state and ICE candidates
     */
    private void setupWebRtcCallbacks() {
        // Connection state callback
        webRtcRepository.setConnectionStateCallback(new WebRtcRepository.ConnectionStateCallback() {
            @Override
            public void onConnected() {
                Log.d(TAG, "WebRTC connected");
                connectionState.postValue("CONNECTED");

                // Cancel disconnect timer if reconnected
                cancelDisconnectTimer();

                // Update call status in Firestore
                if (currentCallId != null) {
                    Call call = currentCall.getValue() != null ? currentCall.getValue().getData() : null;
                    boolean hasTimestamp = call != null && call.getConnectedAt() > 0;
                    
                    if (hasTimestamp) {
                         callRepository.updateCallStatus(currentCallId, Call.STATUS_ONGOING,
                            new CallRepository.OnCallUpdatedListener() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "Call status updated to ONGOING");
                                }

                                @Override
                                public void onError(String errorMsg) {
                                    Log.e(TAG, "Failed to update call status: " + errorMsg);
                                }
                            });
                    } else {
                        // First connection sets the start time
                        long timestamp = System.currentTimeMillis();
                        callRepository.updateCallStatusWithTimestamp(currentCallId, Call.STATUS_ONGOING, timestamp,
                            new CallRepository.OnCallUpdatedListener() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "Call status updated to ONGOING with timestamp: " + timestamp);
                                }

                                @Override
                                public void onError(String errorMsg) {
                                    Log.e(TAG, "Failed to update call status: " + errorMsg);
                                }
                            });
                    }
                }
            }

            @Override
            public void onDisconnected() {
                Log.d(TAG, "WebRTC disconnected");
                connectionState.postValue("DISCONNECTED");

                // Start timer to auto-end call if not reconnected
                startDisconnectTimer();
            }

            @Override
            public void onFailed() {
                Log.e(TAG, "WebRTC connection failed");
                connectionState.postValue("FAILED");
                error.postValue("Kết nối thất bại. Vui lòng thử lại.");
            }
        });

        // ICE candidate callback
        webRtcRepository.setIceCandidateCallback((candidate, sdpMid, sdpMLineIndex) -> {
            Log.d(TAG, "ICE candidate generated, sending to peer");

            if (currentCallId != null) {
                // Create ICE candidate signal
                CallSignal signal = new CallSignal(
                        null,
                        currentCallId,
                        currentUserId,
                        CallSignal.createIceCandidateMap(candidate, sdpMid, sdpMLineIndex)
                );

                // Send to Firestore
                callRepository.sendSignal(currentCallId, signal, new CallRepository.OnSignalSentListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "ICE candidate sent successfully");
                    }

                    @Override
                    public void onError(String errorMsg) {
                        Log.e(TAG, "Failed to send ICE candidate: " + errorMsg);
                    }
                });
            }
        });
    }

    /**
     * Initiate a call (caller side)
     *
     * @param conversationId Conversation ID
     * @param callerId       Caller user ID
     * @param receiverId     Receiver user ID
     * @param isVideo        Whether this is a video call
     */
    public void initiateCall(@NonNull String conversationId,
                             @NonNull String callerId,
                             @NonNull String receiverId,
                             boolean isVideo) {
        Log.d(TAG, "Initiating call: " + (isVideo ? "VIDEO" : "VOICE"));

        // Check if already in a call
        if (isInCall) {
            error.postValue("Bạn đang trong cuộc gọi khác");
            Log.w(TAG, "Cannot initiate call - already in a call");
            return;
        }

        isInCall = true;
        this.currentUserId = callerId;
        this.isInitiator = true;
        this.currentCallIsVideo = isVideo;  // Store video flag for later use

        currentCall.setValue(Resource.loading(null));
        connectionState.setValue("INITIALIZING");

        // Create call object
        Call call = new Call(
                null,
                conversationId,
                callerId,
                receiverId,
                isVideo ? Call.TYPE_VIDEO : Call.TYPE_VOICE
        );

        // Create call in Firestore
        callRepository.createCall(call, new CallRepository.OnCallCreatedListener() {
            @Override
            public void onSuccess(Call createdCall) {
                Log.d(TAG, "Call created in Firestore: " + createdCall.getId());
                currentCallId = createdCall.getId();
                currentCall.setValue(Resource.success(createdCall));

                // Initialize WebRTC
                webRtcRepository.initializePeerConnection(currentCallId, isVideo);

                // Create SDP offer
                webRtcRepository.createOffer(new WebRtcRepository.OfferCallback() {
                    @Override
                    public void onOfferCreated(String sdp) {
                        Log.d(TAG, "SDP offer created, sending to peer");

                        // Send OFFER signal to Firestore
                        CallSignal offerSignal = new CallSignal(
                                null,
                                currentCallId,
                                callerId,
                                CallSignal.TYPE_OFFER,
                                sdp
                        );

                        callRepository.sendSignal(currentCallId, offerSignal,
                                new CallRepository.OnSignalSentListener() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Offer sent successfully");
                                        // Fix #5: Listen to call updates so caller receives status changes
                                        listenToCall(currentCallId);
                                        // Listen for answer and ICE candidates
                                        listenToSignals(currentCallId);
                                    }

                                    @Override
                                    public void onError(String errorMsg) {
                                        Log.e(TAG, "Failed to send offer: " + errorMsg);
                                        error.postValue("Không thể gửi yêu cầu cuộc gọi");
                                        endCall();
                                    }
                                });
                    }

                    @Override
                    public void onError(String errorMsg) {
                        Log.e(TAG, "Failed to create offer: " + errorMsg);
                        error.postValue("Không thể khởi tạo cuộc gọi");
                        endCall();
                    }
                });
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Failed to create call: " + errorMsg);
                currentCall.setValue(Resource.error(errorMsg, null));
                error.postValue("Không thể tạo cuộc gọi");
            }
        });
    }

    /**
     * Accept incoming call (receiver side)
     *
     * @param callId     Call ID
     * @param receiverId Receiver user ID
     * @param isVideo    Whether this is a video call (Fix #6)
     */
    public void acceptCall(@NonNull String callId, @NonNull String receiverId, boolean isVideo) {
        Log.d(TAG, "===== ACCEPT CALL START =====");
        Log.d(TAG, "callId: " + callId);
        Log.d(TAG, "receiverId: " + receiverId);
        Log.d(TAG, "isVideo: " + isVideo);
        Log.d(TAG, "Current userId will be set to: " + receiverId);

        // CRITICAL: Cleanup any previous call state first
        if (isInCall || currentCallId != null) {
            Log.w(TAG, "Previous call still active, forcing cleanup before accept");
            forceCleanup();
        }

        // Check if already in a call (after cleanup)
        if (isInCall) {
            Log.w(TAG, "Cannot accept call - already in another call, auto-rejecting");
            rejectCall(callId);
            return;
        }

        // Validate call is not too old (prevent accepting stale calls)
        // Note: This requires getting call timestamp from Firestore
        // For now, we track from accept time

        isInCall = true;
        this.currentCallId = callId;
        this.currentUserId = receiverId;
        this.isInitiator = false;
        this.currentCallIsVideo = isVideo;  // Fix #6: Store video flag
        this.callInitiatedTimestamp = System.currentTimeMillis();  // Track accept time

        connectionState.setValue("ACCEPTING");

        // CRITICAL FIX: Initialize WebRTC BEFORE listening to signals
        // This prevents race condition where OFFER is processed before PeerConnection is ready
        Log.d(TAG, "Initializing WebRTC for " + (isVideo ? "video" : "voice") + " call");
        webRtcRepository.initializePeerConnection(callId, isVideo);

        // Update call status to RINGING
        callRepository.updateCallStatus(callId, Call.STATUS_RINGING,
                new CallRepository.OnCallUpdatedListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Call status updated to RINGING");

                        // Listen to call updates
                        listenToCall(callId);

                        // Listen to signals to get OFFER
                        // WebRTC is now ready to handle OFFER
                        listenToSignals(callId);
                    }

                    @Override
                    public void onError(String errorMsg) {
                        Log.e(TAG, "Failed to update call status: " + errorMsg);
                        error.postValue("Không thể chấp nhận cuộc gọi");
                    }
                });
    }

    /**
     * Reject incoming call
     *
     * @param callId Call ID
     */
    public void rejectCall(@NonNull String callId) {
        Log.d(TAG, "Rejecting call: " + callId);

        callRepository.updateCallStatus(callId, Call.STATUS_REJECTED,
                new CallRepository.OnCallUpdatedListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Call rejected");
                        cleanup();
                    }

                    @Override
                    public void onError(String errorMsg) {
                        Log.e(TAG, "Failed to reject call: " + errorMsg);
                    }
                });
    }

    /**
     * Handle incoming call - start service for background notification
     *
     * @param context    Application context
     * @param call       Incoming call object
     * @param callerName Name of the caller
     */
    public void handleIncomingCall(@NonNull Context context, @NonNull Call call, @NonNull String callerName) {
        Log.d(TAG, "Handling incoming call from: " + callerName);

        Intent serviceIntent = new Intent(context, com.example.doan_zaloclone.services.IncomingCallService.class);
        serviceIntent.putExtra("CALL_ID", call.getId());
        serviceIntent.putExtra("CALLER_NAME", callerName);
        serviceIntent.putExtra("IS_VIDEO", call.isVideoCall());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    /**
     * End current call
     */
    public void endCall() {
        Log.d(TAG, "Ending call: " + currentCallId);

        if (currentCallId == null) {
            return;
        }

        // Calculate duration if call was ongoing
        // TODO: Track start time to calculate accurate duration
        long duration = 0;  // For now, set to 0

        callRepository.endCall(currentCallId, duration, new CallRepository.OnCallUpdatedListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Call ended successfully");
                cleanup();
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Failed to end call: " + errorMsg);
                // Cleanup anyway
                cleanup();
            }
        });
    }

    /**
     * Toggle microphone on/off
     */
    public void toggleMicrophone() {
        boolean newState = !webRtcRepository.isMicrophoneEnabled();
        webRtcRepository.toggleMicrophone(newState);
        isMicEnabled.postValue(newState);
        Log.d(TAG, "Microphone " + (newState ? "enabled" : "muted"));
    }

    /**
     * Toggle camera on/off (video calls only)
     */
    public void toggleCamera() {
        boolean newState = !webRtcRepository.isCameraEnabled();
        webRtcRepository.toggleCamera(newState);
        isCameraEnabled.postValue(newState);
        Log.d(TAG, "Camera " + (newState ? "enabled" : "disabled"));
    }

    /**
     * Switch between front and back camera (video calls only)
     */
    public void switchCamera() {
        webRtcRepository.switchCamera();
        Log.d(TAG, "Switching camera");
    }

    /**
     * Get WebRtcRepository for video renderer attachment
     *
     * @return WebRtcRepository instance
     */
    public WebRtcRepository getWebRtcRepository() {
        return webRtcRepository;
    }

    /**
     * Listen to call updates
     */
    private void listenToCall(@NonNull String callId) {
        if (callListener != null) {
            callListener.remove();
        }

        callListener = callRepository.listenToCall(callId, new CallRepository.OnCallChangedListener() {
            @Override
            public void onCallChanged(Call call) {
                Log.d(TAG, "Call updated: " + call.getStatus());
                currentCall.postValue(Resource.success(call));

                // Handle call status changes
                if (call.isEnded()) {
                    cleanup();
                }
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Error listening to call: " + errorMsg);
                error.postValue("Lỗi kết nối cuộc gọi");
            }
        });
    }

    /**
     * Listen to call signals (SDP and ICE candidates)
     */
    private void listenToSignals(@NonNull String callId) {
        if (signalsListener != null) {
            signalsListener.remove();
        }

        // Clear processed signals when starting new listener
        processedSignalIds.clear();
        Log.d(TAG, "Starting fresh signals listener for call: " + callId);

        signalsListener = callRepository.listenToSignals(callId, new CallRepository.OnSignalsChangedListener() {
            @Override
            public void onSignalsChanged(List<CallSignal> signals) {
                Log.d(TAG, "===== SIGNALS LISTENER FIRED =====");
                Log.d(TAG, "Received " + signals.size() + " total signals");

                // Check if call is too old (prevent processing stale signals)
                if (callInitiatedTimestamp > 0) {
                    long callAge = System.currentTimeMillis() - callInitiatedTimestamp;
                    if (callAge > CALL_EXPIRY_TIMEOUT) {
                        Log.w(TAG, "Call too old (" + callAge + "ms), ignoring signals");
                        return;
                    }
                }

                int newSignalsCount = 0;
                int nullIdCount = 0;

                for (CallSignal signal : signals) {
                    Log.d(TAG, "Processing signal: type=" + signal.getType() +
                            ", senderId=" + signal.getSenderId() +
                            ", id=" + signal.getId());

                    // Handle signals without IDs (process them anyway)
                    if (signal.getId() == null) {
                        nullIdCount++;
                        // Process signals without IDs (can't deduplicate them)
                        if (currentUserId != null && currentUserId.equals(signal.getSenderId())) {
                            continue;  // Still skip own signals
                        }
                        handleSignal(signal);
                        newSignalsCount++;
                        continue;
                    }

                    // Skip if already processed
                    if (processedSignalIds.contains(signal.getId())) {
                        continue;
                    }

                    // Skip own signals
                    if (currentUserId != null && currentUserId.equals(signal.getSenderId())) {
                        Log.d(TAG, "Skipping own signal");
                        processedSignalIds.add(signal.getId());
                        continue;
                    }

                    // Mark as processed
                    processedSignalIds.add(signal.getId());
                    newSignalsCount++;

                    handleSignal(signal);
                }

                if (nullIdCount > 0) {
                    Log.d(TAG, "Processed " + newSignalsCount + " new signals (" + nullIdCount +
                            " without IDs) out of " + signals.size() + " total");
                } else {
                    Log.d(TAG, "Processed " + newSignalsCount + " new signals out of " +
                            signals.size() + " total");
                }
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Error listening to signals: " + errorMsg);
            }
        });
    }

    /**
     * Handle incoming signal from peer
     */
    private void handleSignal(@NonNull CallSignal signal) {
        Log.d(TAG, "===== HANDLING SIGNAL =====");
        Log.d(TAG, "Signal type: " + signal.getType());
        Log.d(TAG, "Signal senderId: " + signal.getSenderId());
        Log.d(TAG, "Current isInitiator: " + isInitiator);

        if (signal.isOfferSignal()) {
            // Only receiver should handle OFFER, skip if we're the initiator (caller)
            if (isInitiator) {
                Log.d(TAG, "Skipping OFFER signal - we are the initiator (caller)");
                return;
            }
            Log.d(TAG, "Processing OFFER signal (we are receiver)");
            handleOffer(signal.getSdp());
        } else if (signal.isAnswerSignal()) {
            // Only caller should handle ANSWER, skip if we're the receiver
            if (!isInitiator) {
                Log.d(TAG, "Skipping ANSWER signal - we are the receiver");
                return;
            }
            Log.d(TAG, "Processing ANSWER signal (we are caller)");
            handleAnswer(signal.getSdp());
        } else if (signal.isIceCandidateSignal()) {
            Log.d(TAG, "Processing ICE_CANDIDATE signal");
            // Add ICE candidate
            handleIceCandidate(signal);
        } else {
            Log.w(TAG, "Unknown signal type: " + signal.getType());
        }
    }

    /**
     * Handle SDP offer (receiver side)
     */
    private void handleOffer(@NonNull String sdp) {
        Log.d(TAG, "Handling OFFER");

        // WebRTC is already initialized in acceptCall(), no need to re-initialize
        // Just create the answer
        webRtcRepository.createAnswer(sdp, new WebRtcRepository.AnswerCallback() {
            @Override
            public void onAnswerCreated(String answerSdp) {
                Log.d(TAG, "Answer created, sending to peer");

                // Send ANSWER signal
                CallSignal answerSignal = new CallSignal(
                        null,
                        currentCallId,
                        currentUserId,
                        CallSignal.TYPE_ANSWER,
                        answerSdp
                );

                callRepository.sendSignal(currentCallId, answerSignal,
                        new CallRepository.OnSignalSentListener() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "Answer sent successfully");
                            }

                            @Override
                            public void onError(String errorMsg) {
                                Log.e(TAG, "Failed to send answer: " + errorMsg);
                            }
                        });
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Failed to create answer: " + errorMsg);
                error.postValue("Không thể trả lời cuộc gọi");
            }
        });
    }

    /**
     * Handle SDP answer (caller side)
     */
    private void handleAnswer(@NonNull String sdp) {
        Log.d(TAG, "Handling ANSWER");
        webRtcRepository.setRemoteDescription(sdp);
    }

    /**
     * Handle ICE candidate
     */
    private void handleIceCandidate(@NonNull CallSignal signal) {
        String candidate = signal.getCandidateString();
        String sdpMid = signal.getSdpMid();
        Integer sdpMLineIndex = signal.getSdpMLineIndex();

        if (candidate != null && sdpMid != null && sdpMLineIndex != null) {
            Log.d(TAG, "Adding ICE candidate");
            webRtcRepository.addIceCandidate(candidate, sdpMid, sdpMLineIndex);
        }
    }

    /**
     * Start disconnect timer - auto-end call after MAX_DISCONNECT_TIMEOUT if not reconnected
     */
    private void startDisconnectTimer() {
        if (disconnectRunnable != null) {
            disconnectHandler.removeCallbacks(disconnectRunnable);
        }

        disconnectRunnable = () -> {
            Log.w(TAG, "Connection timeout (" + MAX_DISCONNECT_TIMEOUT + "ms) - ending call");
            error.postValue("Mất kết nối");
            endCall();
        };

        disconnectHandler.postDelayed(disconnectRunnable, MAX_DISCONNECT_TIMEOUT);
        Log.d(TAG, "Disconnect timer started: " + MAX_DISCONNECT_TIMEOUT + "ms");
    }

    /**
     * Cancel disconnect timer - call reconnected
     */
    private void cancelDisconnectTimer() {
        if (disconnectRunnable != null) {
            disconnectHandler.removeCallbacks(disconnectRunnable);
            disconnectRunnable = null;
        }
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        Log.d(TAG, "===== CLEANING UP CALL RESOURCES =====");

        // Cancel any pending timers
        cancelDisconnectTimer();

        // Remove listeners
        if (callListener != null) {
            callListener.remove();
            callListener = null;
            Log.d(TAG, "Call listener removed");
        }

        if (signalsListener != null) {
            signalsListener.remove();
            signalsListener = null;
            Log.d(TAG, "Signals listener removed");
        }

        // Clear processed signals
        int signalsCleared = processedSignalIds.size();
        processedSignalIds.clear();
        Log.d(TAG, "Cleared " + signalsCleared + " processed signal IDs");

        // Close WebRTC connection
        webRtcRepository.closePeerConnection();

        // Reset state
        String oldCallId = currentCallId;
        currentCallId = null;
        currentUserId = null;
        isInitiator = false;
        callInitiatedTimestamp = 0;
        isInCall = false;  // Reset concurrent call flag
        connectionState.postValue("IDLE");

        Log.d(TAG, "Cleanup complete for call: " + oldCallId);
        Log.d(TAG, "===== CLEANUP FINISHED =====");
    }

    /**
     * Force cleanup - more aggressive, used when state is inconsistent
     */
    private void forceCleanup() {
        Log.w(TAG, "===== FORCE CLEANUP INITIATED =====");

        try {
            // Cancel all timers
            cancelDisconnectTimer();

            // Force remove all listeners
            if (callListener != null) {
                try {
                    callListener.remove();
                } catch (Exception e) {
                    Log.e(TAG, "Error removing call listener: " + e.getMessage());
                }
                callListener = null;
            }

            if (signalsListener != null) {
                try {
                    signalsListener.remove();
                } catch (Exception e) {
                    Log.e(TAG, "Error removing signals listener: " + e.getMessage());
                }
                signalsListener = null;
            }

            // Clear all tracking
            processedSignalIds.clear();

            // Force close WebRTC
            try {
                webRtcRepository.closePeerConnection();
            } catch (Exception e) {
                Log.e(TAG, "Error closing WebRTC: " + e.getMessage());
            }

            // Reset ALL state
            currentCallId = null;
            currentUserId = null;
            isInitiator = false;
            callInitiatedTimestamp = 0;
            isInCall = false;
            connectionState.postValue("IDLE");

            Log.w(TAG, "Force cleanup complete");

        } catch (Exception e) {
            Log.e(TAG, "Critical error during force cleanup: " + e.getMessage());
            // Last resort - reset everything
            currentCallId = null;
            currentUserId = null;
            callListener = null;
            signalsListener = null;
            isInCall = false;
            callInitiatedTimestamp = 0;
        }

        Log.w(TAG, "===== FORCE CLEANUP FINISHED =====");
    }

    /**
     * Log call history to chat conversation
     *
     * @param conversationId Conversation ID
     * @param callType       "MISSED", "INCOMING", "OUTGOING"
     * @param isVideo        Whether it was a video call
     * @param duration       Call duration in seconds (0 if not applicable)
     * @param isIncoming     True if this is from receiver's perspective, false if from caller's perspective
     */
    public void logCallHistory(String conversationId, String callType, boolean isVideo, long duration, boolean isIncoming) {
        if (conversationId == null) {
            Log.w(TAG, "Cannot log call history - conversationId is null");
            return;
        }

        // Get current user ID to use as senderId
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "SYSTEM";

        String callTypeText;
        switch (callType) {
            case "MISSED":
                // Missed calls always show "nhỡ" (red color)
                callTypeText = isVideo ? "📹 Cuộc gọi video nhỡ" : "📞 Cuộc gọi thoại nhỡ";
                break;
            case "INCOMING":
                // Incoming accepted calls show "đến" (standard gray)
                callTypeText = isVideo ? "📹 Cuộc gọi video đến" : "📞 Cuộc gọi thoại đến";
                if (duration > 0) {
                    callTypeText += " (" + formatDuration(duration) + ")";
                }
                break;
            case "OUTGOING":
                // Outgoing calls show "đi" (light gray)
                callTypeText = isVideo ? "📹 Cuộc gọi video đi" : "📞 Cuộc gọi thoại đi";
                if (duration > 0) {
                    callTypeText += " (" + formatDuration(duration) + ")";
                }
                break;
            default:
                callTypeText = "📞 Cuộc gọi";
        }

        // Append timestamp to call message
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        String timestamp = timeFormat.format(new java.util.Date());
        callTypeText += " - " + timestamp;

        // Create call history message with current user as sender
        // This allows filtering: each user only sees their own call history perspective
        Message callMessage = new Message(
                null,  // Auto-generated ID
                currentUserId,  // Use actual userId instead of "SYSTEM"
                callTypeText,
                Message.TYPE_CALL,
                System.currentTimeMillis()
        );

        // Save to Firestore
        chatRepository.sendMessage(conversationId, callMessage, new ChatRepository.SendMessageCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Call history logged: " + callType + " (isIncoming=" + isIncoming + ", senderId=" + currentUserId + ")");
            }

            @Override
            public void onError(String errorMsg) {
                Log.e(TAG, "Failed to log call history: " + errorMsg);
            }
        });
    }


    /**
     * Format duration in seconds to MM:SS
     */
    private String formatDuration(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    // ==================== LiveData Getters ====================

    public LiveData<Resource<Call>> getCurrentCall() {
        return currentCall;
    }

    /**
     * Start listening to call status changes in realtime
     * This is essential so the receiver knows when caller cancels
     */
    public void startListeningToCall(String callId) {
        if (callId == null) return;

        callRepository.listenToCall(callId, new CallRepository.OnCallChangedListener() {
            @Override
            public void onCallChanged(Call call) {
                currentCall.postValue(Resource.success(call));
            }

            @Override
            public void onError(String errorMsg) {
                currentCall.postValue(Resource.error(errorMsg, null));
            }
        });
    }

    public LiveData<String> getConnectionState() {
        return connectionState;
    }

    public LiveData<Boolean> getIsMicEnabled() {
        return isMicEnabled;
    }

    public LiveData<Boolean> getIsCameraEnabled() {
        return isCameraEnabled;
    }

    public LiveData<String> getError() {
        return error;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "===== ViewModel onCleared =====");

        // Force cleanup to ensure everything is released
        forceCleanup();

        // Dispose WebRTC factory
        try {
            webRtcRepository.dispose();
        } catch (Exception e) {
            Log.e(TAG, "Error disposing WebRTC: " + e.getMessage());
        }

        Log.d(TAG, "ViewModel cleared and disposed");
    }
}
