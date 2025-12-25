package com.example.doan_zaloclone.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Utility for handling automatic retries with exponential backoff
 * Usage: Wrap API calls that should be retried on failure
 */
public class RetryHandler {

    private static final String TAG = "RetryHandler";
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_INITIAL_DELAY_MS = 1000; // 1 second
    private static final int DEFAULT_BACKOFF_MULTIPLIER = 2;

    private final Handler handler;
    private final int maxRetries;
    private final long initialDelayMs;
    private final int backoffMultiplier;

    public RetryHandler() {
        this(DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY_MS, DEFAULT_BACKOFF_MULTIPLIER);
    }

    public RetryHandler(int maxRetries, long initialDelayMs, int backoffMultiplier) {
        this.handler = new Handler(Looper.getMainLooper());
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    /**
     * Execute an operation with automatic retry on failure
     * 
     * @param operation The operation to execute
     * @param onSuccess Called when operation succeeds
     * @param onFailure Called when all retries are exhausted
     */
    public void execute(RetryableOperation operation, Runnable onSuccess, FailureCallback onFailure) {
        executeWithRetry(operation, onSuccess, onFailure, 0);
    }

    private void executeWithRetry(RetryableOperation operation, Runnable onSuccess, 
                                   FailureCallback onFailure, int attemptNumber) {
        if (attemptNumber >= maxRetries) {
            Log.e(TAG, "Max retries (" + maxRetries + ") exceeded");
            onFailure.onFailure("Đã thử " + maxRetries + " lần. " + ErrorMessages.TRY_AGAIN);
            return;
        }

        Log.d(TAG, "Attempt " + (attemptNumber + 1) + " of " + maxRetries);

        operation.execute(new OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "✅ Operation succeeded on attempt " + (attemptNumber + 1));
                onSuccess.run();
            }

            @Override
            public void onFailure(Throwable error) {
                boolean shouldRetry = ErrorMessages.isRetryable(error);
                
                if (!shouldRetry) {
                    Log.e(TAG, "❌ Non-retryable error: " + error.getMessage());
                    onFailure.onFailure(ErrorMessages.fromException(error));
                    return;
                }

                long delay = calculateDelay(attemptNumber);
                Log.w(TAG, "⚠️ Attempt " + (attemptNumber + 1) + " failed. Retrying in " + delay + "ms...");

                handler.postDelayed(() -> {
                    executeWithRetry(operation, onSuccess, onFailure, attemptNumber + 1);
                }, delay);
            }
        });
    }

    /**
     * Calculate delay with exponential backoff
     * Attempt 0: 1s, Attempt 1: 2s, Attempt 2: 4s, etc.
     */
    private long calculateDelay(int attemptNumber) {
        return (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptNumber));
    }

    /**
     * Cancel any pending retries
     */
    public void cancelPendingRetries() {
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Cancelled all pending retries");
    }

    /**
     * Interface for retryable operations
     */
    public interface RetryableOperation {
        void execute(OperationCallback callback);
    }

    /**
     * Callback for operation result
     */
    public interface OperationCallback {
        void onSuccess();
        void onFailure(Throwable error);
    }

    /**
     * Callback for final failure after all retries
     */
    public interface FailureCallback {
        void onFailure(String userFriendlyMessage);
    }
}
