package com.example.doan_zaloclone.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Map;

/**
 * Utility class to consolidate duplicate API callback handling code
 * Provides common patterns for success/error handling and logging
 */
public class ApiCallbackHelper {

    /**
     * Create a standard Retrofit callback that updates a LiveData result
     *
     * @param tag         Log tag for debugging
     * @param operation   Description of the operation (for logging)
     * @param result      MutableLiveData to update with success/error
     * @param onSuccess   Custom success handler (receives response body)
     * @param <T>         Type of response body
     */
    public static <T> Callback<T> createCallback(
            @NonNull String tag,
            @NonNull String operation,
            @NonNull MutableLiveData<Resource<Boolean>> result,
            @NonNull SuccessHandler<T> onSuccess
    ) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    T body = response.body();
                    
                    try {
                        onSuccess.handle(body, result);
                    } catch (Exception e) {
                        Log.e(tag, "❌ " + operation + " - Success handler error: " + e.getMessage());
                        result.setValue(Resource.error("Failed: " + e.getMessage()));
                    }
                } else {
                    String error = formatHttpError(response.code(), response.message());
                    Log.e(tag, "❌ " + operation + " failed: " + error);
                    result.setValue(Resource.error(error));
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                String error = formatNetworkError(t);
                Log.e(tag, "❌ " + operation + " network error: " + error);
                result.setValue(Resource.error(error));
            }
        };
    }

    /**
     * Create callback for Map<String, Object> API responses (common pattern)
     */
    public static Callback<Map<String, Object>> createMapCallback(
            @NonNull String tag,
            @NonNull String operation,
            @NonNull MutableLiveData<Resource<Boolean>> result
    ) {
        return createCallback(tag, operation, result, (body, res) -> {
            Boolean success = (Boolean) body.get("success");
            if (success != null && success) {
                Log.d(tag, "✅ " + operation + " successful");
                res.setValue(Resource.success(true));
            } else {
                res.setValue(Resource.error("Operation failed"));
            }
        });
    }

    /**
     * Format HTTP error message (user-friendly)
     */
    public static String formatHttpError(int code, String message) {
        // Use user-friendly messages from ErrorMessages utility
        return ErrorMessages.fromHttpCode(code);
    }

    /**
     * Format network error message (user-friendly)
     */
    public static String formatNetworkError(Throwable t) {
        // Use user-friendly messages from ErrorMessages utility
        return ErrorMessages.fromException(t);
    }
    
    /**
     * Check if error should trigger a retry
     */
    public static boolean shouldRetry(int httpCode) {
        return ErrorMessages.isRetryableHttpError(httpCode);
    }
    
    /**
     * Check if exception should trigger a retry
     */
    public static boolean shouldRetry(Throwable t) {
        return ErrorMessages.isRetryable(t);
    }

    /**
     * Functional interface for custom success handling
     */
    @FunctionalInterface
    public interface SuccessHandler<T> {
        void handle(T body, MutableLiveData<Resource<Boolean>> result);
    }
}
