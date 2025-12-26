package com.example.doan_zaloclone.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doan_zaloclone.api.ApiService;
import com.example.doan_zaloclone.api.RetrofitClient;
import com.example.doan_zaloclone.api.models.ApiResponse;
import com.example.doan_zaloclone.models.User;
import com.example.doan_zaloclone.utils.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for user-related operations
 * Migrated to use REST API instead of direct Firestore access
 */
public class UserRepository {

    private static final String TAG = "UserRepository";
    private final ApiService apiService;
    private final java.util.concurrent.ExecutorService backgroundExecutor;
    private final android.os.Handler mainHandler;
    private final com.example.doan_zaloclone.websocket.SocketManager socketManager;

    private final MutableLiveData<Boolean> userRefreshNeeded = new MutableLiveData<>();

    public UserRepository() {
        this.apiService = RetrofitClient.getApiService();
        this.backgroundExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        this.mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        this.socketManager = com.example.doan_zaloclone.websocket.SocketManager.getInstance();
        
        // Future: Listen for profile update events
        // socketManager.addUserEventListener(...)
    }

    public LiveData<Boolean> getUserRefreshNeeded() {
        return userRefreshNeeded;
    }

    /**
     * Get user by ID via REST API
     *
     * @param userId ID of the user to fetch
     * @return LiveData containing Resource with User data
     */
    public LiveData<Resource<User>> getUser(@NonNull String userId) {
        MutableLiveData<Resource<User>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        Call<User> call = apiService.getUser(userId);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    Log.d(TAG, "✅ User fetched: " + user.getName());
                    result.setValue(Resource.success(user));
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "Failed to fetch user: " + error);
                    result.setValue(Resource.error(error));
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                String error = t.getMessage() != null ? t.getMessage() : "Network error";
                Log.e(TAG, "Network error fetching user", t);
                result.setValue(Resource.error(error));
            }
        });

        return result;
    }

    /**
     * Get username by user ID
     *
     * @param userId ID of the user
     * @return LiveData containing Resource with username string
     */
    public LiveData<Resource<String>> getUserName(@NonNull String userId) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        Call<User> call = apiService.getUser(userId);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    String name = user.getName();
                    result.setValue(Resource.success(name != null && !name.isEmpty() ? name : "User"));
                } else {
                    result.setValue(Resource.success("User")); // Default on error
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                result.setValue(Resource.success("User")); // Default on error
            }
        });

        return result;
    }

    /**
     * Update user information via REST API
     *
     * @param userId  ID of the user to update
     * @param updates Map of field updates
     * @return LiveData containing Resource with update success status
     */
    public LiveData<Resource<Boolean>> updateUser(@NonNull String userId,
                                                  @NonNull Map<String, Object> updates) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        backgroundExecutor.execute(() -> {
            try {
                Call<ApiResponse<Void>> call = apiService.updateUser(userId, updates);
                Response<ApiResponse<Void>> response = call.execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ User updated successfully");
                    mainHandler.post(() -> {
                        result.setValue(Resource.success(true));
                        userRefreshNeeded.setValue(true); // Signal refresh
                    });
                } else {
                    String error = "HTTP " + response.code();
                    Log.e(TAG, "Failed to update user: " + error);
                    mainHandler.post(() -> result.setValue(Resource.error(error)));
                }
            } catch (Exception e) {
                String error = e.getMessage() != null ? e.getMessage() : "Network error";
                Log.e(TAG, "Network error updating user", e);
                mainHandler.post(() -> result.setValue(Resource.error(error)));
            }
        });

        return result;
    }

    /**
     * Search users by name or email via REST API
     *
     * @param query Search query string
     * @return LiveData containing Resource with list of users
     */
    public LiveData<Resource<List<User>>> searchUsers(@NonNull String query) {
        MutableLiveData<Resource<List<User>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        if (query.length() < 2) {
            result.setValue(Resource.success(new ArrayList<>()));
            return result;
        }

        Map<String, String> searchQuery = new HashMap<>();
        searchQuery.put("query", query);

        Call<Map<String, Object>> call = apiService.searchUsers(searchQuery);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> usersData = (List<Map<String, Object>>) response.body().get("users");
                    List<User> users = new ArrayList<>();

                    if (usersData != null) {
                        for (Map<String, Object> userData : usersData) {
                            User user = parseUserFromMap(userData);
                            if (user != null) {
                                users.add(user);
                            }
                        }
                    }

                    Log.d(TAG, "✅ Found " + users.size() + " users for query: " + query);
                    result.setValue(Resource.success(users));
                } else {
                    result.setValue(Resource.error("HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Search error", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });

        return result;
    }

    /**
     * Get multiple users by list of IDs (batch operation)
     *
     * @param userIds List of user IDs
     * @return LiveData containing Resource with list of users
     */
    public LiveData<Resource<List<User>>> getUsersByIds(@NonNull List<String> userIds) {
        MutableLiveData<Resource<List<User>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        if (userIds.isEmpty()) {
            result.setValue(Resource.success(new ArrayList<>()));
            return result;
        }

        Map<String, List<String>> requestBody = new HashMap<>();
        requestBody.put("userIds", userIds);

        Call<Map<String, Object>> call = apiService.getUsersBatch(requestBody);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> usersData = (List<Map<String, Object>>) response.body().get("users");
                    List<User> users = new ArrayList<>();

                    if (usersData != null) {
                        for (Map<String, Object> userData : usersData) {
                            User user = parseUserFromMap(userData);
                            if (user != null) {
                                users.add(user);
                            }
                        }
                    }

                    Log.d(TAG, "✅ Batch fetched " + users.size() + " users");
                    result.setValue(Resource.success(users));
                } else {
                    result.setValue(Resource.error("HTTP " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e(TAG, "Batch fetch error", t);
                result.setValue(Resource.error(t.getMessage()));
            }
        });

        return result;
    }

    // ========== Callback-based methods for backward compatibility ==========

    /**
     * Get user by ID with callback (backward compatible)
     */
    public void getUserById(@NonNull String userId, @NonNull OnUserLoadedListener listener) {
        LiveData<Resource<User>> liveData = getUser(userId);
        liveData.observeForever(resource -> {
            if (resource != null) {
                if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                    listener.onUserLoaded(resource.getData());
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    listener.onError(resource.getMessage());
                }
            }
        });
    }

    /**
     * Update user name
     */
    public void updateUserName(@NonNull String userId, @NonNull String newName,
                               @NonNull OnUpdateListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);

        LiveData<Resource<Boolean>> liveData = updateUser(userId, updates);
        liveData.observeForever(resource -> {
            if (resource != null) {
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    listener.onSuccess();
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    listener.onError(resource.getMessage());
                }
            }
        });
    }

    /**
     * Update user bio
     */
    public void updateUserBio(@NonNull String userId, @NonNull String newBio,
                              @NonNull OnUpdateListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("bio", newBio);

        LiveData<Resource<Boolean>> liveData = updateUser(userId, updates);
        liveData.observeForever(resource -> {
            if (resource != null) {
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    listener.onSuccess();
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    listener.onError(resource.getMessage());
                }
            }
        });
    }

    /**
     * Update user avatar URL
     */
    public void updateUserAvatar(@NonNull String userId, @NonNull String avatarUrl,
                                 @NonNull OnUpdateListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("avatarUrl", avatarUrl);

        LiveData<Resource<Boolean>> liveData = updateUser(userId, updates);
        liveData.observeForever(resource -> {
            if (resource != null) {
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    listener.onSuccess();
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    listener.onError(resource.getMessage());
                }
            }
        });
    }

    /**
     * Update user cover URL
     */
    public void updateUserCover(@NonNull String userId, @NonNull String coverUrl,
                                @NonNull OnUpdateListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("coverUrl", coverUrl);

        LiveData<Resource<Boolean>> liveData = updateUser(userId, updates);
        liveData.observeForever(resource -> {
            if (resource != null) {
                if (resource.getStatus() == Resource.Status.SUCCESS) {
                    listener.onSuccess();
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    listener.onError(resource.getMessage());
                }
            }
        });
    }

    /**
     * Update user online status and lastSeen timestamp via REST API
     *
     * @param userId   ID of the user
     * @param isOnline true if user is online, false if offline
     * @param listener Callback for result
     */
    public void updateUserStatus(@NonNull String userId, boolean isOnline,
                                 @NonNull OnUpdateListener listener) {
        backgroundExecutor.execute(() -> {
            try {
                Map<String, Boolean> status = new HashMap<>();
                status.put("isOnline", isOnline);

                Call<ApiResponse<Void>> call = apiService.updateUserStatus(userId, status);
                Response<ApiResponse<Void>> response = call.execute();

                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ User status updated: " + (isOnline ? "online" : "offline"));
                    mainHandler.post(listener::onSuccess);
                } else {
                    Log.e(TAG, "Failed to update status: HTTP " + response.code());
                    mainHandler.post(() -> listener.onError("HTTP " + response.code()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Network error updating status", e);
                mainHandler.post(() -> listener.onError(e.getMessage()));
            }
        });
    }

    /**
     * Parse User object from Map data (from API response)
     */
    private User parseUserFromMap(Map<String, Object> userData) {
        try {
            User user = new User();
            user.setId((String) userData.get("id"));
            user.setName((String) userData.get("name"));
            user.setEmail((String) userData.get("email"));
            user.setAvatarUrl((String) userData.get("avatarUrl"));
            user.setBio((String) userData.get("bio"));
            user.setPhoneNumber((String) userData.get("phone"));

            // Note: User model doesn't have isOnline and lastSeen fields in Android
            // Online status is handled separately via presence system

            return user;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing user from map", e);
            return null;
        }
    }

    public interface OnUserLoadedListener {
        void onUserLoaded(User user);

        void onError(String error);
    }

    // ========== Helper methods ==========

    public interface OnUpdateListener {
        void onSuccess();

        void onError(String error);
    }
}
